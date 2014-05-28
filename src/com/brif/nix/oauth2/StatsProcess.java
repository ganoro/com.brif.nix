/**
 * Brif 
 */

package com.brif.nix.oauth2;

import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.brif.nix.gdrive.DriveManager;
import com.brif.nix.model.DataAccess;
import com.brif.nix.model.User;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailSSLStore;

/**
 * Calculates stats for a given person
 * 
 * <p>
 * Before using this class, you must call {@code initialize} to install the
 * OAuth2 SASL provider.
 */
public class StatsProcess {

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
		if (args.length == 0 || !isValidEmailAddress(args[0])) {
			System.out.println("Usage: java -jar nix.jar <user's email>");
			System.out.println("\t\tError loading user's email");
			return;
		}
		
		// initialize provider
		initialize();

		logStatus();

		// user info
		DataAccess dataAccess = new DataAccess();
		final List<String> findAllEmails = dataAccess.findAllEmails();
		for (String email : findAllEmails) {
			System.out.println(email);
			
			final User currentUser = dataAccess.findByEmail(email);
			
			if (currentUser == null) {
				System.out.println("user " + email + " couldn't be found");
				return;
			}

			// init google drive
			DriveManager drive = DriveManager.getSingelton();
			drive.setUser(currentUser);
			System.out.println(drive.count());

			// IMAP connection
			GmailSSLStore imapStore = connect(currentUser);
			if (imapStore == null) {
				// can't invalidate - user denied access
				continue;
			}

			// update with latest access_token
			String originalAccessToken = currentUser.access_token;
			if (currentUser.access_token != originalAccessToken) {
				dataAccess.updateUserToken(currentUser);
			}

			GmailFolder inbox = resolveFolder(imapStore);
			if (inbox == null) {
				continue;
			}
			inbox.open(Folder.READ_ONLY);

			// if after all folder is not open - quit
			if (!inbox.isOpen()) {
				continue;
			}
			
			// final Message[] messages = inbox.getMessagesByUID(1, uidNext);
			System.out.println(inbox.getMessages().length);			
		}
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
		throw new Exception("Cannot find [Gmail] folder");
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
	public static String refreshAccessToken(String refreshToken,
			String clientId, String clientSecret) throws IOException {
		try {

			TokenResponse response = new GoogleRefreshTokenRequest(
					new NetHttpTransport(), new JacksonFactory(), refreshToken,
					clientId, clientSecret).execute();
			String accessToken = response.getAccessToken();
			return accessToken;
		} catch (TokenResponseException e) {
			// TODO Auto-generated catch block
			if (e.getDetails() != null) {
				System.err.println("Error: " + e.getDetails().getError());
				if (e.getDetails().getErrorDescription() != null) {
					System.err.println(e.getDetails().getErrorDescription());
				}
				if (e.getDetails().getErrorUri() != null) {
					System.err.println(e.getDetails().getErrorUri());
				}
			} else {
				// TODO Auto-generated catch block
				System.err.println(e.getMessage());
			}
		}
		return null;
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

			// if cann't invalidate - out
			if (currentUser.access_token == null) {
				return null;
			}

			try {
				imapStore = connectToImap("imap.gmail.com", 993,
						currentUser.email, currentUser.access_token, debug);
				
			} catch (Exception e1) {
				System.out.println(e1.getMessage());
				throw e1;
				// TODO: invalid grant - application revoked???
			}
		}
		return imapStore;
	}

	protected static void invalidateAccessToken(User currentUser)
			throws IOException {

		OAuth2Configuration conf = OAuth2Configuration
				.getConfiguration(currentUser.origin);

		final String access_token = refreshAccessToken(
				currentUser.refresh_token, conf.get("client_id"),
				conf.get("client_secret"));

		currentUser.access_token = access_token;
	}

	private static boolean isValidEmailAddress(String email) {
		boolean result = true;
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException ex) {
			result = false;
		}
		return result;
	}

}
