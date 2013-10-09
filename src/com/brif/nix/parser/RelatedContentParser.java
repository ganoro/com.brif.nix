package com.brif.nix.parser;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class RelatedContentParser implements MimePraser {

	private MimeMultipart multipart;

	public RelatedContentParser(MimeMultipart multipart) {
		this.multipart = multipart;
	}

	public String getContent() {
		try {
			// first is root
			final BodyPart bodyPart = multipart.getBodyPart(0);
			final MimePraser parser = MimeParserFactory.getParser(bodyPart); 
			final String content = parser.getContent();
			
			for (int i = 1; i < multipart.getCount(); i++) {
				BodyPart bp = multipart.getBodyPart(i);
				System.out.println(bp.getContentType());
			}

			return content;

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}

}
