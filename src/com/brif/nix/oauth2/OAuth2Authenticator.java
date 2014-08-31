/**
 * Brif 
 */

package com.brif.nix.oauth2;

import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;

import com.brif.nix.gdrive.DriveManager;
import com.brif.nix.listeners.AllMessageListener;
import com.brif.nix.model.DataAccess;
import com.brif.nix.model.User;
import com.brif.nix.notifications.SapiNotificationsHandler;
import com.brif.nix.parse.ParseException;
import com.brif.nix.parser.MessageParser;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.IMAPProtocol;

/**
 * Performs OAuth2 authentication.
 * 
 * <p>
 * Before using this class, you must call {@code initialize} to install the
 * OAuth2 SASL provider.
 */
public class OAuth2Authenticator {

	private static DataAccess dataAccess;

	public static final class OAuth2Provider extends Provider {
		private static final long serialVersionUID = 1L;

		public OAuth2Provider() {
			super("Google OAuth2 Provider", 1.0,
					"Provides the XOAUTH2 SASL Mechanism");
			put("SaslClientFactory.XOAUTH2",
					"com.brif.nix.oauth2.OAuth2SaslClientFactory");
		}
	}

	/**
	 * Authenticates to IMAP with parameters passed in on the command-line.
	 */
	public static void main(String args[]) throws Exception {

		// command-line handling
		if (args.length == 0) {
			System.out.println("Usage: java -jar nix.jar <user's email>");
			System.out.println("\t\tError loading user's email");
			return;
		}

		String first_argument = args[0];
		boolean isSetupProcess = args.length > 1 ? "setup:true"
				.equalsIgnoreCase(args[1]) : false;

		long anchorTime = args.length > 2 ? Long.parseLong(args[2]) : 0;

		// initialize provider
		initialize();

		// keeping this loop forever with different access_token
		while (true) {

			logStatus();

			dataAccess = new DataAccess(first_argument);
			final User currentUser = dataAccess.getUser();
			if (currentUser == null) {
				System.out.println("user " + first_argument
						+ " couldn't be found");
				return;
			}

			// initialize google drive
			DriveManager drive = DriveManager.getSingelton();
			drive.setUser(currentUser);

			// IMAP connection
			String originalAccessToken = currentUser.access_token; // store the
																	// original
																	// token
			GmailSSLStore imapStore = connect(currentUser);
			if (imapStore == null) {
				// TODO Auto-generated catch block
				// internal error
				return;
			}

			// update with latest access_token
			if (currentUser.access_token != originalAccessToken) {
				dataAccess.updateUserToken(currentUser);
			}

			final GmailFolder allFolder = resolveFolder(imapStore);
			if (allFolder == null) {
				dataAccess.notifyNixRemoved();
				return;
			}
			allFolder.open(Folder.READ_ONLY);

			// if after all folder is not open - quit
			if (!allFolder.isOpen()) {
				throw new Exception("Folder does not open correctly.");
			}

			// TODO map reduce ?
			final long uidNext = allFolder.getUIDNext();
			long min = Math.max(currentUser.next_uid + 1, uidNext - 4000);

			final Message[] messages = allFolder.getMessagesByUID(min, uidNext);

			for (int i = messages.length - 1; i >= 0; i--) {
				Message message = messages[i];
				MessageParser mp = new MessageParser(message, currentUser);
				// for setup process, parse only from the given date
				if (isSetupProcess && anchorTime != 0 && mp.isAfter(anchorTime)) {
					continue;
				}
				// for listener process, parse only after the given date
				if (!isSetupProcess && anchorTime != 0
						&& !mp.isAfter(anchorTime)) {
					break;
				}
				if (mp.shouldBeProcessed()) {
					System.out.println("Adding message: " + mp.getMessageId());
					dataAccess.addMessage(currentUser, mp);
					labelMessage(mp, currentUser);
				}
			}

			// if it is the user's setup process - quit here...
			if (isSetupProcess) {
				return;
			}

			dataAccess = new DataAccess(new SapiNotificationsHandler(
					"http://api.brif.us:443"));
			dataAccess.setUser(currentUser);

			// https://bugzilla.mozilla.org/show_bug.cgi?id=518581
			final AllMessageListener allMessageListener = new AllMessageListener(
					currentUser, dataAccess);
			allFolder.addMessageCountListener(allMessageListener);

			try {
				// mark user as nix-enabled
				dataAccess.notifyNixListening();

				// start listening
				startKeepAliveListener(allFolder, currentUser);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private static final Pattern TAG_PATTERN = Pattern
			.compile("(?:^|\\s|[\\p{Punct}&&[^/]])(#[\\p{L}0-9-_]+)");

	public static void addLabels(MessageParser mp, GmailFolder writeFolder) {
		String subject = "";
		try {
			subject = mp.getSubject();
		} catch (MessagingException e) {
			return;
		}
		final Matcher matcher = TAG_PATTERN.matcher(subject);
		while (matcher.find()) {
			String label = matcher.group();
			System.out.println("label: " + label);
			try {
				final LabelOperation labelOperation = new LabelOperation(
						mp.getMessageNumber(), label);
				final boolean open = writeFolder.isOpen();
				if (!open) {
					writeFolder.open(Folder.READ_WRITE);
				}
				writeFolder.getMessage(mp.getMessageNumber());
				writeFolder.doCommand(labelOperation);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void labelMessage(MessageParser mp, User currentUser)
			throws IOException, Exception {
		GmailSSLStore imapStore = connect(currentUser);
		if (imapStore == null) {
			// internal error
			return;
		}

		final GmailFolder allFolderWrite = resolveFolder(imapStore);
		allFolderWrite.open(Folder.READ_WRITE);
		if (!allFolderWrite.isOpen()) {
			return;
		}
		addLabels(mp, allFolderWrite);
	}

	/**
	 * People can do crazy thing with their folder structure - find it!
	 * 
	 * @param imapStore
	 * @return
	 * @throws Exception
	 */
	protected static GmailFolder resolveFolder(GmailSSLStore imapStore)
			throws Exception {
		Folder main = imapStore.getFolder("[Gmail]");
		if (!main.exists()) {
			main = getGmailFolder(imapStore);
		}
		final Folder[] list = main.list();
		GmailFolder inbox = getAllMailFolder(list); // each locale has its
													// own \All directory
		return inbox;
	}

	protected static GmailFolder getGmailFolder(GmailSSLStore imapStore)
			throws Exception {
		final Folder[] list = imapStore.getDefaultFolder().list();
		for (Folder f : list) {
			GmailFolder gf = (GmailFolder) f;
			final String[] attributes = gf.getAttributes();
			if (attributes.length == 2) {
				int isGmail = 2;
				for (String string : attributes) {
					if ("\\Noselect".equals(string)
							|| "\\HasChildren".equals(string)) {
						isGmail--;
					}
				}
				if (isGmail == 0) {
					return gf;
				}
			}
		}
		return (GmailFolder) list[0];
	}

	/**
	 * People can do crazy thing with their folder structure - find it!
	 * 
	 * @param imapStore
	 * @return
	 * @throws Exception
	 */
	protected static GmailFolder resolveInbox(GmailSSLStore imapStore)
			throws Exception {
		return (GmailFolder) imapStore.getFolder("INBOX");

	}

	private static GmailFolder getAllMailFolder(Folder[] list) {
		for (Folder folder : list) {
			GmailFolder f = (GmailFolder) folder;
			try {
				final String[] attributes = f.getAttributes();
				for (String string : attributes) {
					if ("\\All".equals(string)) {
						return f;
					}
				}
			} catch (MessagingException e) {
				e.printStackTrace();
			}

		}
		return null;
	}

	private static void logStatus() {
		/* This will return Long.MAX_VALUE if there is no preset limit */
		long maxMemory = Runtime.getRuntime().maxMemory();
		/* Maximum amount of memory the JVM will attempt to use */
		System.out.println("Maximum memory (bytes): "
				+ (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

		/* Total memory currently in use by the JVM */
		System.out.println("Total memory (bytes): "
				+ Runtime.getRuntime().totalMemory());
	}

	/**
	 * Installs the OAuth2 SASL provider. This must be called exactly once
	 * before calling other methods on this class.
	 */
	public static void initialize() {
		Security.addProvider(new OAuth2Provider());
	}

	/**
	 * Connects and authenticates to an IMAP server with OAuth2. You must have
	 * called {@code initialize}.
	 * 
	 * @param host
	 *            Hostname of the imap server, for example
	 *            {@code imap.googlemail.com}.
	 * @param port
	 *            Port of the imap server, for example 993.
	 * @param userEmail
	 *            Email address of the user to authenticate, for example
	 *            {@code oauth@gmail.com}.
	 * @param oauthToken
	 *            The user's OAuth token.
	 * @param debug
	 *            Whether to enable debug logging on the IMAP connection.
	 * 
	 * @return An authenticated IMAPStore that can be used for IMAP operations.
	 */
	public static GmailSSLStore connectToImap(String host, int port,
			String userEmail, String oauthToken, boolean debug)
			throws Exception {
		Properties props = new Properties();
		props.put("mail.store.protocol", "gimaps");
		props.put("mail.gimaps.sasl.enable", "true");
		props.put("mail.gimaps.sasl.mechanisms", "XOAUTH2");

		props.put(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, oauthToken);

		Session session = Session.getDefaultInstance(props, null);
		session.setDebug(debug);
		session.getProperties().setProperty(
				OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, oauthToken);
		GmailSSLStore store = (GmailSSLStore) session.getStore("gimaps");
		store.connect(host, port, userEmail, "");

		return store;
	}

	/**
	 * @param refreshToken
	 * @param clientId
	 * @param clientSecret
	 * @return
	 * @throws IOException
	 */
	public static void refreshAccessToken(User currentUser,
			String refreshToken, String clientId, String clientSecret)
			throws IOException {
		try {

			TokenResponse response = new GoogleRefreshTokenRequest(
					new NetHttpTransport(), new JacksonFactory(), refreshToken,
					clientId, clientSecret).execute();
			String accessToken = response.getAccessToken();
			System.out.println("Access token: " + accessToken);
			currentUser.access_token = accessToken;
			if (response.getRefreshToken() != null) {
				currentUser.refresh_token = response.getRefreshToken();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
		}
	}

	private static GmailSSLStore connect(User currentUser) throws Exception,
			IOException {
		GmailSSLStore imapStore = null;
		final boolean debug = false;
		try {
			imapStore = connectToImap("imap.gmail.com", 993, currentUser.email,
					currentUser.access_token, debug);
		} catch (AuthenticationFailedException e) {

			// try again... first invalidate access token
			invalidateAccessToken(currentUser);

			try {
				imapStore = connectToImap("imap.gmail.com", 993,
						currentUser.email, currentUser.access_token, debug);
			} catch (AuthenticationFailedException e1) {
				dataAccess.notifyNixRemoved();
				System.out.println(e1.getMessage());
				throw e1;
				// TODO: invalid grant - application revoked???
			} catch (Exception e2) {
				System.out.println(e2.getMessage());
				throw e2;
			}
		}
		return imapStore;
	}

	protected static void invalidateAccessToken(User currentUser)
			throws IOException {

		OAuth2Configuration conf = OAuth2Configuration
				.getConfiguration(currentUser.origin);

		refreshAccessToken(currentUser, currentUser.refresh_token,
				conf.get("client_id"), conf.get("client_secret"));
	}

	public static void startKeepAliveListener(IMAPFolder imapFolder,
			User currentUser) throws MessagingException {
		// We need to create a new thread to keep alive the connection
		Thread t = new Thread(new KeepAliveRunnable(imapFolder),
				"IdleConnectionKeepAlive");

		t.start();

		while (!Thread.interrupted()) {
			try {
				System.out.println("Starting IDLE " + imapFolder.getName());
				imapFolder.idle();
			} catch (MessagingException e) {
				final String message = e.getMessage();
				System.out.println("Messaging exception during IDLE");
				if (message != null) {
					System.out.println(message);
				}
				throw e;
			}
		}

		// Shutdown keep alive thread
		if (t.isAlive()) {
			t.interrupt();
		}
	}

	/**
	 * Runnable used to keep alive the connection to the IMAP server
	 * 
	 * @author Juan Martin Sotuyo Dodero <jmsotuyo@monits.com>
	 */
	private static class KeepAliveRunnable implements Runnable {

		private static final long KEEP_ALIVE_FREQ = 300000; // 5 minutes

		private IMAPFolder folder;

		public KeepAliveRunnable(IMAPFolder imapFolder) {
			this.folder = imapFolder;
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					Thread.sleep(KEEP_ALIVE_FREQ);

					// Perform a NOOP just to keep alive the connection
					System.out
							.println("Performing a NOOP to keep alvie the connection");
					folder.doCommand(new IMAPFolder.ProtocolCommand() {
						public Object doCommand(IMAPProtocol p)
								throws ProtocolException {
							p.simpleCommand("NOOP", null);
							return null;
						}
					});
				} catch (InterruptedException e) {
					// Ignore, just aborting the thread...
				} catch (FolderClosedException ex) {
					try {
						dataAccess.notifyNixDown();
					} catch (ParseException e) {
						// TODO exit and alert in a different way?
						e.printStackTrace();
					}
				} catch (MessagingException e) {
					// Shouldn't really happen...
					System.out
							.println("Unexpected exception while keeping alive the IDLE connection");
				}
			}
		}
	}
}
