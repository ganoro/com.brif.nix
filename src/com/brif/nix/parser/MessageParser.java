package com.brif.nix.parser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.MessageIDTerm;

import org.apache.commons.codec.digest.DigestUtils;

import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailMessage;

public class MessageParser {

	private GmailMessage message;
	private String allRecipients;
	private String originalRecipients;
	private String charset;

	public MessageParser(Message message) {
		if (message == null || !(message instanceof GmailMessage)) {
			throw new IllegalArgumentException("empty message in "
					+ getClass().getCanonicalName());
		}
		this.message = (GmailMessage) message;
	}

	public GmailFolder getFolder() {
		return (GmailFolder) message.getFolder();
	}

	public String[] getLabels() throws MessagingException {
		return this.message.getLabels();
	}

	public boolean isSeen() throws MessagingException {
		return message.isSet(Flags.Flag.SEEN);
	}

	public boolean isDraft() throws MessagingException {
		final String[] labels = message.getLabels();
		return labels.length > 0 && "\\Draft".equals(labels[0]);
	}

	public String getSubject() throws MessagingException {
		final String subject = this.message.getSubject();
		return subject == null ? "" : subject;
	}

	public long getMessageId() throws MessagingException {
		final GmailFolder folder = (GmailFolder) message.getFolder();
		return folder.getUID(message);
	}

	public long getGoogleThreadId() throws MessagingException {
		return message.getThrId();
	}

	public String getGoogleMessageId() throws MessagingException {
		return Long.toString(message.getMsgId());
	}
	
	public String getRecipients() throws MessagingException {
		if (this.allRecipients == null) {
			this.allRecipients = this.resolveRecipientsString(
					message.getAllRecipients(), message.getFrom());
		}
		return allRecipients;
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
			if (ref == null) {
				return "";
			}

			final GmailFolder folder = (GmailFolder) message.getFolder();
			final Message[] search = folder.search(new MessageIDTerm(ref[0]
					.substring(1, ref[0].indexOf(">"))));
			originalRecipients = search.length > 0 ? this
					.resolveRecipientsString(search[0].getAllRecipients(),
							search[0].getFrom()) : "";
		}
		return originalRecipients;

	}

	public Date getSentDate() throws MessagingException {
		return message.getSentDate();
	}

	public String getSentBy() throws MessagingException {
		final Address[] from = message.getFrom();
		if (from.length > 0) {
			InternetAddress ia = (InternetAddress) from[0];
			return ia.getAddress();
		}
		return "";
	}

	public static <T> T[] concat(T[] first, T[] second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public String getRecipientsId() throws MessagingException {
		return DigestUtils.md5Hex(this.getRecipients());
	}

	public String getContent() throws IOException, MessagingException {
		final Object content = message.getContent();
		String result = "";
		if (content instanceof String) {
			final HTMLMessageParser htmlMessageParser = new HTMLMessageParser(
					(String) content);
			result = htmlMessageParser.getContent();
		}

		if (content instanceof MimeMultipart) {
			final MimeMultipart multipart = (MimeMultipart) content;
			if (multipart.getContentType().startsWith("multipart/MIXED")) {
				GmailMixedMessageParser mixed = new GmailMixedMessageParser(
						multipart);
				result = mixed.getContent();
				charset = mixed.getCharset();
			} else if (multipart.getContentType().startsWith(
					"multipart/ALTERNATIVE")) {
				GmailAlternativeMessageParser p = new GmailAlternativeMessageParser(
						multipart);
				result = p.getContent();
				charset = p.getCharset();
			}
		}

		return result.length() != 0 ? result : getSubject() == null ? ""
				: getSubject();
	}

	private String resolveRecipientsString(Address[] allRecipients,
			Address[] from) {
		Address[] concat = concat(allRecipients, from);
		Arrays.sort(concat, new Comparator<Address>() {
			@Override
			public int compare(Address o1, Address o2) {
				InternetAddress a1 = (InternetAddress) o1;
				InternetAddress a2 = (InternetAddress) o2;
				return a1.getAddress().toLowerCase()
						.compareTo(a2.getAddress().toLowerCase());
			}
		});
		StringBuffer result = new StringBuffer();
		Set<String> s = new HashSet<String>();
		for (Address address : concat) {
			InternetAddress a = (InternetAddress) address;
			String lcAddress = a.getAddress().toLowerCase();
			if (s.add(lcAddress)) {
				result.append(lcAddress);
				result.append(",");
			}
		}
		if (result.length() > 1) {
			result.deleteCharAt(result.length() - 1);
		}
		return result.toString();
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

	public String getCharset() {
		return charset;
	}

}
