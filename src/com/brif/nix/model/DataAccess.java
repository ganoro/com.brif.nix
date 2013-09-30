package com.brif.nix.model;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.mail.MessagingException;

import org.json.JSONException;
import org.json.JSONObject;

import com.brif.nix.notifications.EmptyNotificationHandler;
import com.brif.nix.notifications.NotificationsHandler;
import com.brif.nix.parse.Parse;
import com.brif.nix.parse.ParseException;
import com.brif.nix.parse.ParseObject;
import com.brif.nix.parse.ParseQuery;
import com.brif.nix.parser.MessageParser;

/**
 * Data access is only allowed here, decoupling data access to single point
 * 
 * @author Roy, 2013
 */
public class DataAccess {

	// parse-related constants
	private static final String MESSAGES_SCHEMA = "Messages";
	private static final String USERS_SCHEMA = "Users";
	private static final String ACCESS_KEY = "NoVHzsTel7csA1aGoMBNyVz2mHzed4LaSb1d4lme";
	private static final String APP = "mMS3oCiZOHC15v8OGTidsRgHI0idYut39QKrIhIH";

	private NotificationsHandler notificationsHandler;

	public DataAccess(NotificationsHandler notificationsHandler) {
		this.notificationsHandler = notificationsHandler;
		Parse.initialize(APP, ACCESS_KEY);
	}

	public DataAccess() {
		this(new EmptyNotificationHandler());
	}

	public User findByEmail(String email) {
		ParseQuery query1 = new ParseQuery(USERS_SCHEMA);
		query1.whereEqualTo("email", email);
		List<ParseObject> profiles;
		try {
			profiles = query1.find();
		} catch (ParseException e) {
			return null;
		}
		if (profiles.size() == 0) {
			return null;
		}
		final ParseObject parseObject = profiles.get(0);
		final Long next_uid = parseObject.getLong("next_uid", 1);

		return new User(email, parseObject.getString("access_token"),
				parseObject.getString("refresh_token"),
				parseObject.getString("origin"), next_uid,
				parseObject.getObjectId());
	}

	private void createMessageDocument(Map<String, Object> data, String charset)
			throws IOException, MessagingException {
		ParseObject parseMessage = new ParseObject(MESSAGES_SCHEMA);

		for (Map.Entry<String, Object> kv : data.entrySet()) {
			if (kv.getValue() != null) {
				parseMessage.put(kv.getKey(), kv.getValue());
			}
		}

		parseMessage.setCharset(charset);
		parseMessage.saveInBackground();
	}

	private JSONObject getISO(Date date) throws MessagingException {
		// YYYY-MM-DDTHH:MM:SS.MMMZ
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(tz);
		String nowAsISO = df.format(date);

		JSONObject jo = new JSONObject();
		try {
			jo.put("__type", "Date");
			jo.put("iso", nowAsISO);
		} catch (JSONException e) {
			// TODO
		}
		return jo;
	}

	public void addMessage(User currentUser, MessageParser mp)
			throws IOException, MessagingException {
		Map<String, Object> data = getMessageData(currentUser, mp);
		createMessageDocument(data, mp.getCharset());
		notifyMessageAdded(currentUser, data, mp.getCharset());
	}

	private Map<String, Object> getMessageData(User currentUser,
			MessageParser mp) throws MessagingException, IOException {

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("user_id", currentUser.objectId);
		m.put("message_id", mp.getMessageId());
		m.put("google_trd_id", mp.getGoogleThreadId());
		m.put("google_msg_id", mp.getGoogleMessageId());
		
		//sender details
		final String[] sentBy = mp.getSender();
		if (sentBy != null && sentBy[0] != null) {
			m.put("sender_email", sentBy[0]);
			if (sentBy[1] != null) {
				m.put("sender_name", sentBy[1]);
			}	
		}

		m.put("sent_date", getISO(mp.getSentDate()));
		m.put("subject", mp.getSubject());

		// cleanup variables in case of equality of original recipients and
		// recipients
		String originalRecipients = mp.getOriginalRecipients();
		String originalRecipientsId = mp.getOriginalRecipientsId();
		final String recipients = mp.getRecipients();
		final String recipientsId = mp.getRecipientsId();
		if (originalRecipientsId == null || originalRecipientsId.length() == 0
				|| originalRecipientsId.equals(recipientsId)) {
			originalRecipientsId = recipientsId;
			originalRecipients = "";
		}
		// end cleanup

		m.put("original_recipients_id", originalRecipientsId);
		m.put("recipients_id", recipientsId);
		m.put("original_recipients", originalRecipients);
		m.put("recipients", recipients);

		m.put("content", mp.getContent());
		if (mp.getCharset() != null) {
			m.put("charset", mp.getCharset());
		}

		return m;
	}

	private void notifyMessageAdded(User currentUser, Map<String, Object> data,
			String charset) {
		if (notificationsHandler != null) {
			notificationsHandler.notifyMessagesEvent(currentUser.email,
					"added", data, charset);
		}
	}

	public void updateUserToken(final User currentUser) {
		ParseObject user = new ParseObject(USERS_SCHEMA);
		user.setObjectId(currentUser.objectId);
		user.put("access_token", currentUser.access_token);
		user.updateInBackground();
	}

	public void updateUserNextUID(final User currentUser) {
		ParseObject user = new ParseObject(USERS_SCHEMA);
		user.setObjectId(currentUser.objectId);
		user.put("next_uid", currentUser.next_uid);
		user.updateInBackground();
	}

	public void removeMessage(final long uid) {
		ParseObject parseMessage = new ParseObject(MESSAGES_SCHEMA);
		parseMessage.put("message_id", uid);
		parseMessage.deleteInBackground();
	}
}
