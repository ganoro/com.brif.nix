package com.brif.nix.parser;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class RelatedContentParser extends MultiPartParser {

	public RelatedContentParser(MimeMultipart body) {
		super(body);
	}

	public String getContent() {
		try {
			// first is always the root???
			final BodyPart bodyPart = body.getBodyPart(0);
			final IMimePraser parser = MimeParserFactory.getParser(bodyPart);
			final String content = parser.getContent();
			return content;

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}

	@Override
	public String getMetadata(String key) {
		return null;
	}

	@Override
	public String getIntro() {
		return null;
	}

}
