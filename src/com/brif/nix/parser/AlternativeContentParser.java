package com.brif.nix.parser;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class AlternativeContentParser extends MultiPartParser implements
		IMimePraser {

	public AlternativeContentParser(MimeMultipart body) {
		super(body);
	}

	/**
	 * prefer the html, unless the html one is too promotional
	 */
	public String getContent() {
		try {
			// find the html and no_html parts
			BodyPart no_html = null;
			BodyPart html = null;
			for (int i = 0; i < body.getCount(); i++) {
				BodyPart bp = body.getBodyPart(i);
				if (body.getBodyPart(i).isMimeType("text/html")) {
					html = bp;
				} else {
					no_html = bp;
				}
			}

			// if there is an html and it's not promotional - take it!
			if (html != null) {
				TextMessageParser hParser = (TextMessageParser) MimeParserFactory
						.getParser(html);
				String hContent = hParser.getContent();
				if (!hParser.isPromotional() || no_html == null) {
					return hContent;
				}
			}

			// otherwise, just take the no html one
			return MimeParserFactory.getParser(no_html).getContent();

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

}
