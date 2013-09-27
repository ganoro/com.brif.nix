package com.brif.nix.listeners;

import javax.mail.Message;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;

public final class NixMessageChangeListener implements
		MessageChangedListener {
	@Override
	public void messageChanged(MessageChangedEvent event) {
		final Message message = event.getMessage();
		final int messageChangeType = event.getMessageChangeType();
		final Object source = event.getSource();
		System.out.println(messageChangeType);
	}
}