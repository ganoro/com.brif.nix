package com.brif.nix.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.IOUtils;

import com.brif.nix.parser.MessageParser.MessageAttachment;

public class MimeHelper {

	public static String unfold(String s) {
		if (s == null) {
			return null;
		}
		return s.replaceAll("\r|\n", "");
	}

	/**
	 * Returns the named parameter of a header field. If name is null the first
	 * parameter is returned, or if there are no additional parameters in the
	 * field the entire field is returned. Otherwise the named parameter is
	 * searched for in a case insensitive fashion and returned. If the parameter
	 * cannot be found the method returns null.
	 * 
	 * @param header
	 * @param name
	 * @return
	 */
	public static String getHeaderParameter(String header, String name) {
		if (header == null) {
			return null;
		}
		header = header.replaceAll("\r|\n", "");
		String[] parts = header.split(";");
		if (name == null) {
			return parts[0];
		}
		for (String part : parts) {
			if (part.trim().toLowerCase().startsWith(name.toLowerCase())) {
				String parameter = part.split("=", 2)[1].trim();
				if (parameter.startsWith("\"") && parameter.endsWith("\"")) {
					return parameter.substring(1, parameter.length() - 1);
				} else {
					return parameter;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the named parameter of a header name.
	 * 
	 * @see MimeHelper#getHeaderParameter(String, String)
	 * 
	 * @param header
	 *            name
	 * @param parameter
	 *            name
	 * @return String
	 * @throws MessagingException
	 */
	public static String getHeaderParameter(Part part, String headerName,
			String name) throws MessagingException {
		if (part == null) {
			return null;
		}
		final String[] header = part.getHeader(headerName);
		if (header == null || header.length == 0) {
			return null;
		}
		return getHeaderParameter(header[0], name);
	}

	public static final MessageAttachment getAttachment(Part part)
			throws MessagingException, IOException {

		String disposition = part.getDisposition();
		final String contentType = part.getContentType();
		final String dispositionType = getHeaderParameter(contentType, null);
		
		if (disposition == null && dispositionType != null && !dispositionType.toLowerCase().startsWith("image")) {
			return null;
		}
		
		String dispositionFilename = MimeHelper.getFilename(part);

		/*
		 * A best guess that this part is intended to be an attachment and not
		 * inline.
		 */
		boolean attachment = ("attachment".equalsIgnoreCase(disposition))
				|| "inline".equalsIgnoreCase(disposition)
				|| (dispositionFilename != null);

		return attachment ? new MessageAttachment(
				dispositionType, dispositionFilename) : null;
	}

	private static String getFilename(Part part) throws MessagingException {
		final String contentType = part.getContentType();
		String dispositionFilename = getHeaderParameter(contentType, "filename");
		if (dispositionFilename == null) {
			dispositionFilename = getHeaderParameter(contentType, "name");
		}
		if (dispositionFilename == null) {
			dispositionFilename = getHeaderParameter(part,
					"Content-Disposition", "filename");
		}
		if (dispositionFilename == null) {
			dispositionFilename = "unknown_filename";
		}
		try {
			dispositionFilename = MimeUtility.decodeText(dispositionFilename);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			dispositionFilename = "unknown_filename";
		}

		return dispositionFilename;
	}

	public static Part findFirstPartByMimeType(Part part, String mimeType)
			throws MessagingException, IOException {
		if (part.getContent() instanceof Multipart) {
			Multipart multipart = (Multipart) part.getContent();
			for (int i = 0, count = multipart.getCount(); i < count; i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				Part ret = findFirstPartByMimeType(bodyPart, mimeType);
				if (ret != null) {
					return ret;
				}
			}
		} else if (part.isMimeType(mimeType)) {
			return part;
		}
		return null;
	}

	public static Part findPartByContentId(Part part, String contentId)
			throws Exception {
		if (part.getContent() instanceof Multipart) {
			Multipart multipart = (Multipart) part.getContent();
			for (int i = 0, count = multipart.getCount(); i < count; i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				Part ret = findPartByContentId(bodyPart, contentId);
				if (ret != null) {
					return ret;
				}
			}
		}
		String[] header = part.getHeader("Content-ID");
		if (header != null) {
			for (String s : header) {
				if (s.equals(contentId)) {
					return part;
				}
			}
		}
		return null;
	}

	/**
	 * Reads the Part's body and returns a String based on any charset
	 * conversion that needed to be done.
	 * 
	 * @param part
	 * @return
	 * @throws IOException
	 */
	public static String getTextFromPart(Part part) {
		try {
			if (part != null && part.getContent() != null) {
				InputStream in = part.getInputStream();
				if (part.isMimeType("text/*")) {
					/*
					 * Now we read the part into a buffer for further
					 * processing. Because the stream is now wrapped we'll
					 * remove any transfer encoding at this point.
					 */
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					IOUtils.copy(in, out);

					byte[] bytes = out.toByteArray();
					in.close();
					out.close();

					String charset = getHeaderParameter(part.getContentType(),
							"charset");
					/*
					 * We've got a text part, so let's see if it needs to be
					 * processed further.
					 */
					if (charset != null) {
						/*
						 * See if there is conversion from the MIME charset to
						 * the Java one.
						 */
						charset = MimeUtility.javaCharset(charset);
					}
					if (charset != null) {
						/*
						 * We've got a charset encoding, so decode using it.
						 */
						return new String(bytes, 0, bytes.length, charset);
					} else {
						/*
						 * No encoding, so use us-ascii, which is the standard.
						 */
						return new String(bytes, 0, bytes.length, "ASCII");
					}
				}
			}

		} catch (Exception e) {
			/*
			 * If we are not able to process the body there's nothing we can do
			 * about it. Return null and let the upper layers handle the missing
			 * content.
			 */
			System.out.println(e.getMessage());
		}
		return null;
	}

	/**
	 * An unfortunately named method that makes decisions about a Part (usually
	 * a Message) as to which of it's children will be "viewable" and which will
	 * be attachments. The method recursively sorts the viewables and
	 * attachments into seperate lists for further processing.
	 * 
	 * @param part
	 * @param viewables
	 * @param attachments
	 * @throws MessagingException
	 * @throws IOException
	 */
	public static void collectParts(Part part, ArrayList<Part> viewables,
			ArrayList<Part> attachments) throws MessagingException, IOException {
		String disposition = part.getDisposition();
		String dispositionType = null;
		String dispositionFilename = null;
		if (disposition != null) {
			dispositionType = MimeHelper.getHeaderParameter(disposition, null);
			dispositionFilename = MimeHelper.getHeaderParameter(disposition,
					"filename");
		}

		/*
		 * A best guess that this part is intended to be an attachment and not
		 * inline.
		 */
		boolean attachment = ("attachment".equalsIgnoreCase(dispositionType))
				|| (dispositionFilename != null)
				&& (!"inline".equalsIgnoreCase(dispositionType));

		/*
		 * If the part is Multipart but not alternative it's either mixed or
		 * something we don't know about, which means we treat it as mixed per
		 * the spec. We just process it's pieces recursively.
		 */
		if (part.getContent() instanceof Multipart) {
			Multipart mp = (Multipart) part.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				collectParts(mp.getBodyPart(i), viewables, attachments);
			}
		}
		/*
		 * If the part is an embedded message we just continue to process it,
		 * pulling any viewables or attachments into the running list.
		 */
		else if (part.getContent() instanceof Message) {
			Message message = (Message) part.getContent();
			collectParts(message, viewables, attachments);
		}
		/*
		 * If the part is HTML and it got this far it's part of a mixed (et al)
		 * and should be rendered inline.
		 */
		else if ((!attachment) && (part.isMimeType("text/html"))) {
			viewables.add(part);
		}
		/*
		 * If the part is plain text and it got this far it's part of a mixed
		 * (et al) and should be rendered inline.
		 */
		else if ((!attachment) && (part.isMimeType("text/plain"))) {
			viewables.add(part);
		}
		/*
		 * Finally, if it's nothing else we will include it as an attachment.
		 */
		else {
			attachments.add(part);
		}
	}
}