package com.brif.nix.parser;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;

import com.brif.nix.parser.MessageParser.MessageAttachment;

public abstract class MultiPartParser implements MimePraser {

	protected MimeMultipart body ;
	
	public MultiPartParser(MimeMultipart body) {
		this.body = body;
	}

	@Override
	public void collectAttachments(List<MessageAttachment> atts) {
		try {
			for (int i = 0; i < body.getCount(); i++) {
				Part bp = body.getBodyPart(i);
				if (bp.isMimeType("multipart/*")) {
					final MimePraser parser = MimeParserFactory.getParser(bp);
					parser.collectAttachments(atts);
				} else {
					final MessageAttachment attachment = MimeHelper.getAttachment(bp);
					if (attachment != null) {
						atts.add(attachment);
					}
				}
				
			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}