package com.brif.nix.protobuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import com.brif.nix.protobuf.generated.MessageProtos.Message;
import com.brif.nix.protobuf.generated.MessageProtos.Message.Attachment;
import com.brif.nix.protobuf.generated.MessageProtos.Message.Builder;

/**
 * Builds a message This is not a thread safe class!
 */
public class MessageBuilder {

	final static Message.Attachment.Builder aBuilder = Message.Attachment
			.newBuilder();

	final static Builder mBuilder = Message.newBuilder();

	public static void main(String[] args) throws IOException {
		Message m1 = buildMessage("objectId", "content", 1,
				1, 1, "recipients_id", "recipients",
				"sender_email", "sent_date", "subject");
		
		final byte[] bytes = getMessageAsBytes(m1);
		
		final byte[] base64Bytes = Base64.encodeBase64(bytes);

		mBuilder.clear();
		mBuilder.mergeFrom(Base64.decodeBase64(base64Bytes));
		final Message m2 = mBuilder.build();
		System.out.println(m2.toString());
		
	}

	public static byte[] getMessageAsBytes(Message message) throws IOException {
		if (!message.isInitialized()) {
			throw new IllegalStateException();
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		message.writeTo(os);
		return os.toByteArray();
	}

	public static Message buildMessage(String objectId, String content,
			long google_msg_id, long google_trd_id, long message_id,
			String recipients_id, String recipients, String sender_email,
			String sent_date, String subject) {
		mBuilder.clear();
		mBuilder.setObjectId(objectId).setContent(content)
				.setGoogleMsgId(google_msg_id).setGoogleTrdId(google_trd_id)
				.setMessageId(message_id).setRecipients(recipients)
				.setRecipientsId(recipients_id).setSenderEmail(sender_email)
				.setSentDate(sent_date).setSubject(subject);

		return mBuilder.build();
	}

	public static Attachment buildAttachment(String application, String link,
			String name) {
		aBuilder.clear().setApplication(application).setLink(link)
				.setName(name);
		return aBuilder.build();
	}
}
