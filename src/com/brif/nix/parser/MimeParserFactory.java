package com.brif.nix.parser;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;

public class MimeParserFactory {

	public static MimePraser getParser(Part message) {

		MimePraser result = null;
		try {

			final Object content = message.getContent();
			if (message.isMimeType("text/*")) {
				result = new HTMLMessageParser((String) content);
			} else if (message.isMimeType("multipart/alternative")) {
				result = new AlternativeContentParser((MimeMultipart) content);
			} else if (message.isMimeType("multipart/mixed")) {
				result = new MixedContentParser((MimeMultipart) content);
			} else if (message.isMimeType("multipart/related")) {
				result = new RelatedContentParser((MimeMultipart) content);
			} else {
				// attachments - image, application/*,
				System.out.println(message.getContentType());
				result = new EmptyContentParser(content);
			}

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

}
