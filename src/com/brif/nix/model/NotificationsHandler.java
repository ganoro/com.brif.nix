package com.brif.nix.model;

import java.util.Map;

public interface NotificationsHandler {

	public void notifyGroupsEvent(String email, Map<String, Object> data,
			String charset);

	public void notifyMessagesEvent(String email, Map<String, Object> data,
			String charset);

}
