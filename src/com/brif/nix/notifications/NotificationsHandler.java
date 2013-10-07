package com.brif.nix.notifications;

import java.util.Map;

public interface NotificationsHandler {

	public void notifyMessagesEvent(String email, String eventType,
			Map<String, Object> data);

}
