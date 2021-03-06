package com.brif.nix.parser;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class AlternativeContentParser extends MultiPartParser implements
		IMimePraser {

	private final IMimePraser proxyContent;

	public AlternativeContentParser(MimeMultipart body, MessageParser mp) {
		super(body, mp);

		// find the html and no_html parts
		BodyPart no_html = null;
		BodyPart html = null;
		try {
			for (int i = 0; i < body.getCount(); i++) {
				final BodyPart bodyPart = body.getBodyPart(i);
				BodyPart bp = bodyPart;

				if (bodyPart.isMimeType("text/html")) {
					html = bp;
				} else {
					no_html = bp;
				}
			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		proxyContent = MimeParserFactory.getParser(html != null ? html
				: no_html, this.getMessageParser());
	}

	/**
	 * prefer the html
	 */
	public String getContent() {
		return proxyContent != null ? proxyContent.getContent() : "";
	}

	@Override
	public String getMetadata(String key) {
		return proxyContent != null ? proxyContent.getMetadata(key) : null;
	}

	@Override
	public String getIntro() {
		return proxyContent != null ? proxyContent.getIntro() : "";
	}
}
