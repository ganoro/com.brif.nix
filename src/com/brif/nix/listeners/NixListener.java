package com.brif.nix.listeners;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;

import com.brif.nix.model.DataAccess;
import com.brif.nix.model.User;
import com.brif.nix.parser.MessageParser;
import com.sun.mail.gimap.GmailMessage;
import com.sun.mail.imap.protocol.FLAGS;

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
				if (iSself(message)) {
					break;
				} 
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

	private boolean iSself(Message message) throws MessagingException {
		GmailMessage gm = (GmailMessage) message;
		final String[] labels = gm.getLabels();
		return labels.length > 0 && "\\Draft".equals(labels[0]);  
	}

	public static String getTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return "[" + dateFormat.format(date) + "] ";
	}

}