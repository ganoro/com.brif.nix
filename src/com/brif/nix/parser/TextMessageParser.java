package com.brif.nix.parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.Part;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.brif.nix.parser.MessageParser.MessageAttachment;

public class TextMessageParser implements IMimePraser {

	public static final String UNSUBSCRIBE = "unsubscribe";
	private Object content;
	private Part message;

	private static final String DEFAULT_CHARSET = "UTF-8";
	private String charset;
	private String unsubscribe = null;

	public TextMessageParser(Object content2, Part message) {
		this.content = content2;
		this.message = message;
	}

	public String getContent() {
		if (this.content instanceof String) {
			final String text = (String) this.content;
			try {
				if (message.isMimeType("text/html")) {
					String charset = getMessageCharset(message);
					Document doc = Jsoup.parse(message.getInputStream(),
							charset, "");
					doc.select("head").remove();
					doc.select("style").remove();

					final Elements href = doc.select("a");
					if (href != null && !href.isEmpty()) {
						unsubscribe = getUnsubscribeLinks(href);
					}

					// remove tail
					removeGmail(doc);
					removeMsOutlook(doc);
					removeIOS(doc);

					if (doc.text().trim().length() != 0) {
						return doc.outerHtml();
					} else {
						return "";
					}
				} else {
					return removeTail(text);
				}
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return "";
	}

	private String getUnsubscribeLinks(Elements href) {
		for (Element a : href) {
			final String text = a.text();
			if (text != null && text.length() > 10) {
				if (text.toLowerCase().contains(UNSUBSCRIBE)) {
					return a.attr("href");
				}
			}
		}
		return null;
	}

	private static final String r = "http(s)?://([\\w+?\\.\\w+])+([a-zA-Z0-9\\~\\!\\@\\#\\$\\%\\^\\&amp;\\*\\(\\)_\\-\\=\\+\\\\\\/\\?\\.\\:\\;\\'\\,]*)?";
	private final static Pattern pattern = Pattern.compile(r, Pattern.DOTALL
			| Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE);

	private String removeTail(String text) {
		final Scanner scanner = new Scanner(text);
		StringBuilder sb = new StringBuilder();

		while (scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();

			if (nextLine != null && !nextLine.startsWith(">")) {
				Matcher matcher = pattern.matcher(nextLine);
				// nextLine = matcher.replaceAll("<a href=\"$0\">$0</a>"); //
				// group 0 is the whole expression
				sb.append(matcher.replaceAll("<a href=\"$0\">link</a>"));
				sb.append("<br/>");
			}
		}

		return sb.toString();
	}

	@Override
	public void collectAttachments(List<MessageAttachment> atts) {
		if (!(this.content instanceof String)) {
			// TODO add attachment of text/* attachments
		}
		return;
	}

	protected static boolean removeGmail(Document doc) {
		final Elements select = doc.select(".gmail_quote");
		if (select.size() > 0) {
			select.remove();
			return true;
		}
		return false;
	}

	protected static boolean removeIOS(Document doc) {
		final Elements select = doc.select("blockquote");
		if (select.size() > 0) {
			final Node previousSibling = select.get(0).previousSibling();
			if (previousSibling != null) {
				previousSibling.remove();
			}
			select.get(0).remove();
			return true;
		}
		return false;
	}

	protected static boolean removeMsOutlook(Document doc) {
		final Elements select = doc.select(".MsoNormal");
		if (select.size() > 0) {
			Element e = select.get(0).nextElementSibling();
			while (e != null && e.hasClass("MsoNormal")) {
				e = e.nextElementSibling();
			}
			while (e != null) {
				Node previous = e;
				e = e.nextElementSibling();
				previous.remove();
			}
			return true;
		}
		return false;
	}

	public String getCharset() {
		return charset;
	}

	/**
	 * @param bodyPart
	 * @return message CharSet
	 * @throws MessagingException
	 */
	private String getMessageCharset(final Part bodyPart)
			throws MessagingException {
		final String header = bodyPart.getContentType();
		if (header == null) {
			System.out.println("couldn't parse content type");
			return DEFAULT_CHARSET;
		}

		final Pattern p = Pattern.compile("(\\w+)\\s*=\\s*\\\"?([^\\s;\\\"]*)");
		final Matcher matcher = p.matcher(header);
		if (!matcher.find()) {
			System.out.println("couldn't parse content type " + header);
			return DEFAULT_CHARSET;
		}
		String charset = matcher.group(2);
		try {
			charset = Charset.isSupported(charset) ? charset.toUpperCase()
					: DEFAULT_CHARSET;
		} catch (IllegalCharsetNameException e) {
			charset = DEFAULT_CHARSET;
		}
		return charset;
	}

	@Override
	public String getMetadata(String key) {
		return key == UNSUBSCRIBE ? this.unsubscribe : null;
	}

	@Override
	public String getIntro() {
		if (this.content instanceof String) {
			final String s = (String) this.content;
			final int start = getFirstNonWhitespace(s);
			final int end = getLastChar(s, start);
			String result = s.substring(start, end);
			return result.replaceAll("\\s+", " ");
		}
		return null;
	}

	private int getLastChar(String s, int start) {
		return Math.min(start + 50, s.length());
	}

	private int getFirstNonWhitespace(String s) {
		for (int i = 0; i < s.length(); i++) {
		    if (!Character.isWhitespace(s.charAt(i))) {
		        return i;
		    }
		}
		return 0;
	}
	
	
}
