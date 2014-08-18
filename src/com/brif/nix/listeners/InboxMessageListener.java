package com.brif.nix.listeners;

import javax.mail.Message;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import com.brif.nix.model.DataAccess;
import com.brif.nix.model.User;

public class InboxMessageListener implements MessageCountListener {

	private User currentUser;
	private DataAccess dataAccess;

	public InboxMessageListener(User currentUser, DataAccess dataAccess) {
		this.currentUser = currentUser;
		this.dataAccess = dataAccess;
	}

	@Override
	public void messagesAdded(MessageCountEvent arg0) {
	}

	@Override
	public void messagesRemoved(MessageCountEvent event) {
		final Message[] messages = event.getMessages();
		if (messages.length == 0) {
			return;
		}
		final int messageNumber = messages[0].getMessageNumber();
		System.out.println(messageNumber);
		System.out.println(messages.length);
	}

}
