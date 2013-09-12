package com.brif.nix.listeners;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import com.brif.nix.model.DataAccess;
import com.brif.nix.model.User;
import com.brif.nix.parser.MessageParser;

public class NixListener implements MessageCountListener {
	
	private final User currentUser;
	private DataAccess dataAccess;

	public NixListener(User currentUser, DataAccess dataAccess) {
		this.currentUser = currentUser;
		this.dataAccess = dataAccess;
	}

	@Override
	public void messagesRemoved(MessageCountEvent arg0) {
		Message[] messages = arg0.getMessages();
		for (Message message : messages) {
			System.out.println(getTime() + "message removed");
			System.out.println(getTime() + message.getMessageNumber());
			dataAccess.removeMessage(message);		
		}
	}

	@Override
	public void messagesAdded(MessageCountEvent arg0) {
		Message[] messages = arg0.getMessages();
		for (Message message : messages) {
			try {
				System.out.println(getTime() + "message added");
				MessageParser mp = new MessageParser(message);
				dataAccess.storeMessage(currentUser, mp);
				
				// TODO better way to acquire / store the next uid? 
				currentUser.next_uid = mp.getFolder().getUIDNext();
				dataAccess.updateUserNextUID(currentUser);
				
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