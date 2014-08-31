package com.brif.nix.listeners;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import com.brif.nix.model.DataAccess;
import com.brif.nix.model.User;
import com.brif.nix.parser.MessageParser;
import com.sun.mail.gimap.GmailFolder;

public class AllMessageListener implements MessageCountListener {

	private final User currentUser;
	private DataAccess dataAccess;
	private GmailFolder allFolderWrite;

	public AllMessageListener(User currentUser, DataAccess dataAccess, GmailFolder allFolderWrite) {
		this.currentUser = currentUser;
		this.dataAccess = dataAccess;
		this.allFolderWrite = allFolderWrite;
	}

	@Override
	public void messagesRemoved(MessageCountEvent arg0) {
		Message[] messages = arg0.getMessages();
		for (Message message : messages) {
			System.out.println(getTime() + "message removed ("
					+ message.getMessageNumber() + ")");
			dataAccess.removeMessage(currentUser.objectId,
					message.getMessageNumber());
			System.out.println(getTime() + message.getMessageNumber());
		}
	}

	@Override
	public void messagesAdded(MessageCountEvent arg0) {
		Message[] messages = arg0.getMessages();
		for (Message message : messages) {
			try {
				final int messageNumber = message.getMessageNumber();
				System.out.println(getTime() + "adding message ("
						+ messageNumber + ")");

				if (message.isExpunged()) {
					continue;
				}

				MessageParser mp = new MessageParser(message, currentUser);
				if (mp.shouldBeProcessed()) {
					mp.addLabels(allFolderWrite);	
					dataAccess.addMessage(currentUser, mp);
				}
				System.out.println(getTime() + "message added ("
						+ messageNumber + ")");

			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static String getTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return "[" + dateFormat.format(date) + "] ";
	}

}