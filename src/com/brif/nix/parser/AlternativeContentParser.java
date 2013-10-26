package com.brif.nix.parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class AlternativeContentParser extends MultiPartParser implements MimePraser {

	public AlternativeContentParser(MimeMultipart body) {
		super(body);
	}

	private static final String DEFAULT_CHARSET = "UTF-8";
	private String charset;

	public String getContent() {
		try {
			String text = null;
			for (int i = 0; i < body.getCount(); i++) {
				Part bp = body.getBodyPart(i);
				if (bp.isMimeType("text/plain")) {
					text = (String) bp.getContent();
				} else if (bp.isMimeType("text/html")) {
					String charset = getMessageCharset(bp);
					Document doc = Jsoup
							.parse(bp.getInputStream(), charset, "");
					doc.select(".gmail_quote").remove();
					doc.select("blockquote").remove();
					if (doc.text().trim().length() != 0) {
						return doc.outerHtml();
					} else {
						return "";
					}
				} else {
					final MimePraser parser = MimeParserFactory.getParser(bp);
					return parser.getContent();
				}
			}
			return text;

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
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
}
