package com.brif.nix.parser;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class AlternativeContentParser extends MultiPartParser implements
		IMimePraser {

	private final IMimePraser proxy;

	public AlternativeContentParser(MimeMultipart body) {
		super(body);

		// find the html and no_html parts
		BodyPart no_html = null;
		BodyPart html = null;
		try {
			for (int i = 0; i < body.getCount(); i++) {
				BodyPart bp = body.getBodyPart(i);
				if (body.getBodyPart(i).isMimeType("text/html")) {
					html = bp;
				} else {
					no_html = bp;
				}
			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		proxy = MimeParserFactory.getParser(html != null ? html : no_html);
	}

	/**
	 * prefer the html
	 */
	public String getContent() {
		return proxy != null ? proxy.getContent() : "";
	}

	@Override
	public String getMetadata(String key) {
		return proxy != null ? proxy.getMetadata(key) : null;
	}
}
