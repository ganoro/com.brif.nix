package com.brif.nix.parser;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class MixedContentParser implements MimePraser {

	private MimeMultipart multipart;

	public MixedContentParser(MimeMultipart multipart) {
		this.multipart = multipart;
	}

	public String getContent() {
		try {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < multipart.getCount(); i++) {
				final BodyPart bodyPart = multipart.getBodyPart(i);
				final MimePraser parser = MimeParserFactory.getParser(bodyPart);
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
