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
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class AlternativeContentParser extends MultiPartParser implements
		MimePraser {

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
					doc.select("head").remove();
					doc.select("style").remove();
					
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

	protected static boolean removeGmail(Document doc) {
		final Elements select = doc.select(".gmail_extra");
		if (select.size() > 0) {
			select.remove();
			return true;	
		}
		return false;
	}

	protected static boolean removeIOS(Document doc) {
		final Elements select = doc.select("blockquote");
		if (select.size() > 0) {
			select.get(0).previousSibling().remove();
			select.get(0).remove();
			return true;
		}
		return false;
	}
	
	protected static boolean removeMsOutlook(Document doc) {
		final Elements select = doc.select(".MsoNormal");
		if (select.size() > 0) {
			final Element appendElement = doc.prependElement("style");
			appendElement
					.html("p.MsoNormal, li.MsoNormal, div.MsoNormal {margin:0cm; margin-bottom:.0001pt; font-size:12.0pt; font-family:\"Times New Roman\",\"serif\";}");
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
}
