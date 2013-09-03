package com.brif.nix.parser;

import java.io.IOException;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class GmailMixedMessageParser {

	private static final String MULTIPART_ALTERNATIVE = "multipart/ALTERNATIVE";
	private MimeMultipart multipart;

	public GmailMixedMessageParser(MimeMultipart multipart) {
		this.multipart = multipart;
	}

	public String getContent() throws MessagingException, IOException {
		for (int i = 0; i < multipart.getCount(); i++) {
			final BodyPart bodyPart = multipart.getBodyPart(i);
			if (bodyPart.getContentType().startsWith(MULTIPART_ALTERNATIVE)) {
				GmailAlternativeMessageParser p = new GmailAlternativeMessageParser(
						(MimeMultipart) bodyPart.getContent());
				return p.getContent();
			}
		}

		return "";
	}
}
