package com.brif.nix.notifications;

import java.util.Map;

public class EmptyNotificationHandler implements NotificationsHandler {

	@Override
	public void notifyMessagesEvent(String email, String eventType,
			Map<String, Object> data) {
		System.out.println("notifyMessagesEvent");
	}

}
