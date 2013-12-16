package com.brif.nix.parser;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class MixedContentParser extends MultiPartParser {

	public MixedContentParser(MimeMultipart body) {
		super(body);
	}

	public String getContent() {
		try {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < body.getCount(); i++) {
				final BodyPart bodyPart = body.getBodyPart(i);
				final IMimePraser parser = MimeParserFactory.getParser(bodyPart);
				final String content = parser.getContent();
				sb.append(content);
			}
			return sb.toString();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
}
