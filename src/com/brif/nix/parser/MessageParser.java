package com.brif.nix.parser;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.search.MessageIDTerm;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.brif.nix.model.User;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailFolder.FetchProfileItem;
import com.sun.mail.gimap.GmailMessage;

public class MessageParser {

	private static final String UTF_8 = "UTF-8";

	private GmailMessage message;
	private String allRecipients;
	private String allRecipientsNames;
	private String originalRecipients;

	private String content = null;
	private String subject = null;
	private String unsubscribe = null;

	private static FetchProfile fp = new FetchProfile();
	private static Message[] container = new Message[1];

	private User user;

	final private IMimePraser parser;

	private String intro = null;

	private static final Address[] emptyAddress = new Address[0];

	static {
		fp.add(FetchProfileItem.CONTENT_INFO);
		fp.add(FetchProfileItem.ENVELOPE);
		fp.add(FetchProfileItem.FLAGS);
		fp.add(FetchProfileItem.SIZE);
		fp.add(FetchProfileItem.LABELS);
		fp.add(FetchProfileItem.MSGID);
		fp.add(FetchProfileItem.THRID);
		fp.add(FetchProfile.Item.CONTENT_INFO);
		fp.add("BODY");
	}

	public static class MessageAttachment {
		String type;
		String name;
		String key;
		String from;

		public MessageAttachment(String dispositionType,
				String dispositionFilename, String from) {
			this.type = dispositionType;
			this.name = dispositionFilename;
			this.from = from;
		}

		public JSONObject toJSONObject() {
			JSONObject jo = new JSONObject();
			try {
				jo.put("type", type);
				jo.put("name", name);
				jo.put("key", key);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return jo;
		}
	}

	public MessageParser(Message message, User currentUser) {
		this.user = currentUser;
		if (message == null || !(message instanceof GmailMessage)) {
			throw new IllegalArgumentException("empty message in "
					+ getClass().getCanonicalName());
		}
		this.message = (GmailMessage) message;
		final GmailFolder folder = (GmailFolder) this.message.getFolder();
		container[0] = this.message;

		try {
			folder.fetch(container, fp);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		parser = MimeParserFactory.getParser(message, this);
	}

	public GmailFolder getFolder() {
		return (GmailFolder) message.getFolder();
	}

	public String[] getLabels() throws MessagingException {
		return this.message.getLabels();
	}

	public boolean hasNotArchived() throws MessagingException {
		final String[] labels = this.getLabels();
		for (String label : labels) {
			if ("\\Inbox".equals(label)) {
				return true;
			}
		}
		return false;
	}

	public boolean isSeen() throws MessagingException {
		return message.isSet(Flags.Flag.SEEN);
	}

	public boolean shouldBeProcessed() throws MessagingException {
		//  && (this.isSentByMe() || this.hasNotArchived())
		return !this.isDraft();  
	}

	protected boolean isDraft() throws MessagingException {
		final String[] labels = message.getLabels();
		return labels.length == 1 && "\\Draft".equals(labels[0]);
	}

	protected boolean isSentByMe() throws MessagingException {
		final String[] sender = this.getSender();
		return sender != null && sender[0].equalsIgnoreCase(user.email);
	}
	
	public String getSubject() throws MessagingException {
		if (this.subject != null) {
			return this.subject;
		}
		final String header = this.message.getHeader("Subject", null);
		final String c = getCharsetByHeader(header);
		this.subject = convertToUTF(message.getSubject(), c);
		return subject == null ? "" : subject;
	}

	public String getUnsubscribe() throws MessagingException {
		if (this.unsubscribe != null) {
			return this.unsubscribe;
		}
		final String header = this.message.getHeader("List-Unsubscribe", null);
		this.unsubscribe = header;
		return this.unsubscribe;
	}
	
	public boolean isIOS() {
		String header;
		try {
			header = this.message.getHeader("X-Mailer", null);
		} catch (MessagingException e) {
			return false;
		}
		return header != null && header.startsWith("iP");
	}

	private String getCharsetByHeader(final String rawvalue) {
		if (rawvalue == null || !rawvalue.startsWith("=?"))
			return UTF_8;

		int start = 2;
		int pos;
		if ((pos = rawvalue.indexOf('?', start)) != -1) {
			String c = rawvalue.substring(start, pos);
			int lpos = c.indexOf('*'); // RFC 2231 language specified?
			if (lpos >= 0) // yes, throw it away
				c = c.substring(0, lpos);
			return javaCharset(c);
		}
		return UTF_8;
	}

	public long getMessageId() throws MessagingException {
		final GmailFolder folder = (GmailFolder) message.getFolder();
		return folder.getUID(message);
	}
	
	public int getMessageNumber() {
		return this.message.getMessageNumber();
	}

	public long getGoogleThreadId() throws MessagingException {
		return message.getThrId();
	}

	public long getGoogleMessageId() throws MessagingException {
		return message.getMsgId();
	}

	public String getRecipients() throws MessagingException {
		if (this.allRecipients == null) {
			final String[] recipients = this.resolveRecipientsString(
					message.getAllRecipients(), message.getFrom(),
					message.getReplyTo());
			this.allRecipients = recipients[0];
			this.allRecipientsNames = recipients[1];
		}
		return allRecipients;
	}

	public String getRecipientsName() throws MessagingException {
		if (this.allRecipients == null) {
			getRecipients();
		}
		return allRecipientsNames;
	}

	public String getOriginalRecipientsId() throws MessagingException {
		String or = this.originalRecipients;
		if (or == null) {
			or = getOriginalRecipients();
		}

		if (or == null || or.length() == 0) {
			return "";
		}
		return DigestUtils.md5Hex(or);
	}

	public String getOriginalRecipients() throws MessagingException {
		if (this.originalRecipients == null) {
			final String[] ref = message.getHeader("References");
			if (ref == null || ref[0] == null || ref[0].indexOf(">") < 1) {
				return "";
			}

			final GmailFolder folder = (GmailFolder) message.getFolder();
			final Message[] search = folder.search(new MessageIDTerm(ref[0]
					.substring(1, ref[0].indexOf(">"))));
			originalRecipients = search.length > 0 ? this
					.resolveRecipientsString(search[0].getAllRecipients(),
							search[0].getFrom(), search[0].getReplyTo())[0]
					: "";
		}
		return originalRecipients;

	}

	public Date getSentDate() throws MessagingException {
		return message.getSentDate();
	}

	public String[] getSender() throws MessagingException {
		final Address[] from = message.getFrom();
		if (from.length > 0) {
			InternetAddress ia = (InternetAddress) from[0];
			final String c = getCharsetByHeader(message.getHeader("From", ":"));
			final String per = convertToUTF(ia.getPersonal(), c);
			final String add = convertToUTF(ia.getAddress(), c);
			return new String[] { add, per };
		}
		return null;
	}

	public static Address[] concat(Address zero, Address[] first,
			Address[] second) {
		if (first == null) {
			first = emptyAddress;
		}
		if (second == null) {
			second = emptyAddress;
		}
		Address[] result = Arrays.copyOf(first, first.length + second.length
				+ 1);
		System.arraycopy(second, 0, result, first.length, second.length);
		result[result.length - 1] = zero;
		return result;
	}

	public String getRecipientsId() throws MessagingException {
		String recipients = this.getRecipients();
		if (this.isPrecedence()) {
			recipients = Long.toString(this.message.getThrId());
		}
		return DigestUtils.md5Hex(recipients);
	}

	private boolean isPrecedence() {
		try {
			return this.message.getHeader("Precedence") != null;
		} catch (MessagingException e) {
			return false;
		}
	}
	
	public boolean isInReplyTo() {
		try {
			return this.message.getHeader("In-Reply-To") != null;
		} catch (MessagingException e) {
			return false;
		}
	}
	
	public String getContent() throws IOException, MessagingException {
		if (this.content != null) {
			return this.content;
		}
		String result = parser.getContent();
		if (result.length() == 0) {
			this.content = result;
			return result;
		}
		result = result.substring(0, Math.min(result.length(), 70000));
		this.content = convertToUTF(result, null);

		final String metadata = parser
				.getMetadata(TextMessageParser.UNSUBSCRIBE);
		if (metadata != null) {
			this.unsubscribe = metadata;
		}

		return content;
	}

	public String getIntro() {
		if (this.intro != null) {
			return this.intro;
		}
		this.intro = parser.getIntro();
		if (this.intro != null) {
			this.intro.trim();
		}
		return this.intro;
	}

	public JSONArray getAttachmentsAsJSON() {
		List<MessageAttachment> atts = getAttachments();
		if (atts == null) {
			return null;
		}
		final JSONArray jsonArray = new JSONArray();
		for (MessageAttachment messageAttachment : atts) {
			jsonArray.put(messageAttachment.toJSONObject());
		}
		return jsonArray;
	}

	protected List<MessageAttachment> getAttachments() {
		final IMimePraser parser = MimeParserFactory.getParser(message, this);
		List<MessageAttachment> atts = new ArrayList<MessageAttachment>();
		parser.collectAttachments(atts, getEmailAddress(this.message));
		if (atts.size() == 0) {
			return null;
		}
		return atts;
	}

	private String getEmailAddress(GmailMessage message) {
		Address[] from = null;
		try {
			from = message.getFrom();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (from == null || from.length == 0) {
			return "unknown";
		}

		InternetAddress ia = (InternetAddress) from[0];
		return ia.getAddress();
	}

	private String[] resolveRecipientsString(Address[] allRecipients,
			Address[] from, Address[] replyTo) {
		Address[] concat = null;
		try {
			concat = concat(new InternetAddress(user.email, user.email),
					replyTo == null || replyTo.length == 0 ? from
							: mixAddresses(from, replyTo), allRecipients);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		Arrays.sort(concat, new Comparator<Address>() {
			@Override
			public int compare(Address o1, Address o2) {
				InternetAddress a1 = (InternetAddress) o1;
				InternetAddress a2 = (InternetAddress) o2;
				return a1.getAddress().toLowerCase()
						.compareTo(a2.getAddress().toLowerCase());
			}
		});
		StringBuffer result1 = new StringBuffer();
		StringBuffer result2 = new StringBuffer();
		Set<String> s = new HashSet<String>();
		for (Address address : concat) {
			InternetAddress a = (InternetAddress) address;
			String lcAddress = a.getAddress().toLowerCase();
			if (s.add(lcAddress)) {
				result1.append(lcAddress);
				result1.append(",");
				final String personal = a.getPersonal();
				result2.append(personal == null ? lcAddress : personal.replace(
						',', ' '));
				result2.append(",");
			}
		}
		if (result1.length() > 1) {
			result1.deleteCharAt(result1.length() - 1);
			result2.deleteCharAt(result2.length() - 1);
		}
		return new String[] { result1.toString(), result2.toString() };
	}

	private Address[] mixAddresses(Address[] from, Address[] replyTo) {

		InternetAddress f = (InternetAddress) from[0];
		InternetAddress r = (InternetAddress) replyTo[0];

		try {
			f.setAddress(r.getAddress());
		} catch (Exception e) {
		}
		return from;

	}

	@Override
	public String toString() {
		try {

			StringBuffer sb = new StringBuffer();
			sb.append("{ msgId: '");
			sb.append(getMessageId());
			sb.append("', group: '");
			sb.append(getRecipients());
			sb.append("', subject: '");
			sb.append(getSubject());
			sb.append("', content: '");
			sb.append(getContent());
			sb.append("'}");

			return sb.toString();
		} catch (MessagingException e) {
			e.printStackTrace();
			return "error";
		} catch (IOException e) {
			e.printStackTrace();
			return "error";
		}
	}

	/**
	 * Convert a MIME charset name into a valid Java charset name.
	 * <p>
	 * 
	 * @param charset
	 *            the MIME charset name
	 * @return the Java charset equivalent. If a suitable mapping is not
	 *         available, the passed in charset is itself returned.
	 */
	public static String javaCharset(String charset) {
		return charset;
	}

	/**
	 * Temporary method to get all of a message
	 * 
	 * @throws MessagingException
	 * @throws IOException
	 */
	public void saveAttachments() throws MessagingException, IOException {
		final MimeMultipart content = (MimeMultipart) message.getContent();
		final int count = content.getCount();
		for (int i = 0; i < count; i++) {
			final BodyPart part = content.getBodyPart(i);
			final String disposition2 = part.getDisposition();
			final String disposition = disposition2 != null ? disposition2
					.toLowerCase() : null;
			if ((disposition != null)
					&& (disposition.equals(Part.ATTACHMENT) || disposition
							.equals(Part.INLINE))) {
				// Check if plain
				MimeBodyPart mbp = (MimeBodyPart) part;
				if (!mbp.isMimeType("text/plain")) {
					String name = decodeName(part.getFileName());
					System.out.println(name);
					File savedir = new File("/Users/roy/from_/");
					savedir.mkdirs();
					File savefile = new File("/Users/roy/from_/" + name);
					saveFile(savefile, part);
				}
			}

		}
	}

	protected int saveFile(File saveFile, Part part) throws IOException,
			MessagingException {

		BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(saveFile));

		byte[] buff = new byte[2048];
		InputStream is = part.getInputStream();
		int ret = 0, count = 0;
		while ((ret = is.read(buff)) > 0) {
			bos.write(buff, 0, ret);
			count += ret;
		}
		bos.close();
		is.close();
		return count;
	}

	protected static String decodeName(String name)
			throws UnsupportedEncodingException {
		if (name == null || name.length() == 0) {
			return "unknown";
		}

		return MimeUtility.decodeText(name);
	}

	protected String convertToUTF(final String original, String charset) {
		if (UTF_8.equals(charset))
			return original;

		String string;
		try {
			string = new String(original.getBytes(UTF_8), UTF_8);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			string = "Unsupported Encoding" + e.getMessage();
		}
		return string;
	}

	public String getBrifUniqueId() {
		String header = null;
		try {
			header = this.message.getHeader("X-Brif", null);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return header;
	}

	public boolean isAfter(long anchorTime) {
		Date sentDate = null;
		try {
			sentDate = this.message.getSentDate();
		} catch (MessagingException e) {
		}
		return sentDate == null ? false : sentDate.getTime() > anchorTime;
	}
}
