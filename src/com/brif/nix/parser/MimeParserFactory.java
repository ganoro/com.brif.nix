package com.brif.nix.parser;

import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;

import com.brif.nix.parser.MessageParser.MessageAttachment;

public class MimeParserFactory {

	public static IMimePraser theEmptyParser;
	static {
		theEmptyParser = new IMimePraser() {
			@Override
			public String getContent() {
				return "";
			}

			@Override
			public void collectAttachments(List<MessageAttachment> atts, String from) {
			}

			@Override
			public String getMetadata(String key) {
				return null;
			}

			@Override
			public String getIntro() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public MessageParser getMessageParser() {
				return null;
			}
		};
	}

	public static IMimePraser getParser(Part message, MessageParser messageParser) {
		if (message == null) {
			return theEmptyParser;
		}
		
		IMimePraser result = null;
		try {
			Object content = null;
			try {
				content = message.getContent();
			} catch (IOException e) {
				content = "<unknown encoding>";
			}

			if (message.isMimeType("text/*")) {
				result = new TextMessageParser(content, message, messageParser);
			} else if (message.isMimeType("multipart/alternative")) {
				result = new AlternativeContentParser((MimeMultipart) content, messageParser);
			} else if (message.isMimeType("multipart/mixed")) {
				result = new MixedContentParser((MimeMultipart) content, messageParser);
			} else if (message.isMimeType("multipart/related")) {
				result = new RelatedContentParser((MimeMultipart) content, messageParser);
			} else {
				result = theEmptyParser;
			}

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

}
