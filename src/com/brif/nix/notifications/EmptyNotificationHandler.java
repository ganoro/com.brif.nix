package com.brif.nix.notifications;

import java.util.Map;

public class EmptyNotificationHandler implements NotificationsHandler {

	@Override
	public void notifyGroupsEvent(String email, String eventType,
			Map<String, Object> data, String charset) {
		System.out.println("notifyGroupsEvent");
	}

	@Override
	public void notifyMessagesEvent(String email, String eventType,
			Map<String, Object> data, String charset) {
		System.out.println("notifyMessagesEvent");
	}

}
