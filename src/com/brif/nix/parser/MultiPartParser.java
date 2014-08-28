package com.brif.nix.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;

import com.brif.nix.gdrive.DriveManager;
import com.brif.nix.parser.MessageParser.MessageAttachment;

public abstract class MultiPartParser implements IMimePraser {

	protected MimeMultipart body;
	private MessageParser mp;

	public MultiPartParser(MimeMultipart body, MessageParser mp) {
		this.body = body;
		this.mp = mp;
	}

	@Override
	public void collectAttachments(List<MessageAttachment> atts, String from) {
		try {
			for (int i = 0; i < body.getCount(); i++) {
				Part bp = body.getBodyPart(i);
				if (bp.isMimeType("multipart/*")) {
					final IMimePraser parser = MimeParserFactory.getParser(bp, this.getMessageParser());
					parser.collectAttachments(atts, from);
				} else {
					final MessageAttachment attachment = MimeHelper
							.getAttachment(bp, from);
					if (attachment != null) {
						uploadCopy(attachment, bp);
						if (attachment.key != null) {
							atts.add(attachment);
						}
					}
				}

			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void uploadCopy(MessageAttachment attachment, Part part)
			throws FileNotFoundException, IOException, MessagingException {
		// download attachment to disk
		InputStream is = part.getInputStream();
		
		// Insert a file
		final DriveManager singelton = DriveManager.getSingelton();
		final String uploadStream = singelton.uploadStream(attachment.name, attachment.type.toLowerCase(), is, attachment.from);
		System.out.println("File ID: " + uploadStream);
		attachment.key = uploadStream;	
	}

	protected static boolean isImage(final String dispositionType) {
		return "image/png".equalsIgnoreCase(dispositionType)
				|| "image/jpg".equalsIgnoreCase(dispositionType)
				|| "image/jpeg".equalsIgnoreCase(dispositionType);
	}
	
	@Override
	public MessageParser getMessageParser() {
		return mp;
	}

}