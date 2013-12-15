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
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.model.File;

public abstract class MultiPartParser implements MimePraser {

	protected MimeMultipart body;

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
					final MessageAttachment attachment = MimeHelper
							.getAttachment(bp);
					if (attachment != null) {
						atts.add(attachment);
						if (attachment != null) {
							uploadCopy(attachment, bp);
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
		final String uploadStream = singelton.uploadStream(attachment.name, attachment.type.toLowerCase(), is);
		System.out.println("File ID: " + uploadStream);
		attachment.key = uploadStream;
		
	}

	protected static boolean isImage(final String dispositionType) {
		return "image/png".equalsIgnoreCase(dispositionType)
				|| "image/jpg".equalsIgnoreCase(dispositionType)
				|| "image/jpeg".equalsIgnoreCase(dispositionType);
	}

}