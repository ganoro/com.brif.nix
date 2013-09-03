package com.brif.nix.parser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.digest.DigestUtils;

import com.sun.mail.gimap.GmailMessage;

public class MessageParser {

	private GmailMessage message;
	private String allRecipients;

	public MessageParser(Message message) {
		if (message == null || !(message instanceof GmailMessage)) {
			throw new IllegalArgumentException("empty message in "
					+ getClass().getCanonicalName());
		}

		this.message = (GmailMessage) message;
	}

	public String getSubject() throws MessagingException {
		return this.message.getSubject();
	}
	
	public long getMessageId() throws MessagingException {
		return message.getMsgId();
	}
	
	public long getThreadId() throws MessagingException {
		return message.getThrId();
	}

	public String getGroup() throws MessagingException {
		if (this.allRecipients == null) {
			this.allRecipients = this.getAllRecipients(
					message.getAllRecipients(), message.getFrom());
		}
		return allRecipients;
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

	public String getGroupUnique() throws MessagingException {
		return DigestUtils.md5Hex(this.getGroup());
	}

	public String getContent() throws IOException, MessagingException {
		final Object content = message.getContent();
		String result = "";
		if (content instanceof String) {
			final HTMLMessageParser htmlMessageParser = new HTMLMessageParser((String) content);
			result = htmlMessageParser.getContent();
		}
		
		if (content instanceof MimeMultipart) {
			final MimeMultipart multipart = (MimeMultipart) content;
			if (multipart.getContentType().startsWith("multipart/MIXED")) {
				GmailMixedMessageParser mixed = new GmailMixedMessageParser(multipart);
				result = mixed.getContent();
			} else if (multipart.getContentType().startsWith("multipart/ALTERNATIVE")) {
				GmailAlternativeMessageParser p = new GmailAlternativeMessageParser(multipart);
				result = p.getContent();	
			}
		}
		
		return result.length() == 0 ? getSubject() : result;
	}

	private String getAllRecipients(Address[] allRecipients, Address[] from) {
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
		StringBuffer result = new StringBuffer("{");
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
		result.append("}");
		return result.toString();
	}
	
	@Override
	public String toString() {
		try {

		StringBuffer sb = new StringBuffer();
		sb.append("{ msgId: '");
			sb.append(getMessageId());
		sb.append("', group: '");
		sb.append(getGroup());
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
}
