package com.brif.nix.parser;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class RelatedContentParser extends MultiPartParser {

	private MessageParser mp;

	public RelatedContentParser(MimeMultipart body, MessageParser mp) {
		super(body, mp);
	}

	public String getContent() {
		try {
			// first is always the root???
			final BodyPart bodyPart = body.getBodyPart(0);
			final IMimePraser parser = MimeParserFactory.getParser(bodyPart, mp);
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
		try {
			// first is always the root???
			final BodyPart bodyPart = body.getBodyPart(0);
			final IMimePraser parser = MimeParserFactory.getParser(bodyPart, mp);
			final String intro = parser.getIntro();
			return intro;

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";

	}
}
