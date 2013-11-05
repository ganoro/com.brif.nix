package com.brif.nix.parser;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;

public class AlternativeContentParser extends MultiPartParser implements
		MimePraser {

	public AlternativeContentParser(MimeMultipart body) {
		super(body);
	}

	public String getContent() {
		try {
			Part bp = body.getBodyPart(0);
			for (int i = 0; i < body.getCount(); i++) {
				// prefer tge text/html one over the others 
				if (body.getBodyPart(i).isMimeType("text/html")) {
					bp = body.getBodyPart(i);
				}
			}

			final MimePraser parser = MimeParserFactory.getParser(bp);
			return parser.getContent();

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

}
