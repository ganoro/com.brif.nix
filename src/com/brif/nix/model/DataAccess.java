package com.brif.nix.model;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.mail.MessagingException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.brif.nix.notifications.EmptyNotificationHandler;
import com.brif.nix.notifications.NotificationsHandler;
import com.brif.nix.parse.Parse;
import com.brif.nix.parse.ParseBatch;
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
	private static final String SETTINGS_SCHEMA = "Settings";

	private static final String ACCESS_KEY = "NoVHzsTel7csA1aGoMBNyVz2mHzed4LaSb1d4lme";
	private static final String APP = "mMS3oCiZOHC15v8OGTidsRgHI0idYut39QKrIhIH";

	private NotificationsHandler notificationsHandler;
	private User user;

	public DataAccess(NotificationsHandler notificationsHandler) {
		Parse.initialize(APP, ACCESS_KEY);
		this.notificationsHandler = notificationsHandler;
	}

	public DataAccess() {
		this(new EmptyNotificationHandler());
	}

	public DataAccess(String first_argument) {
		this();
		this.setUser(this.find(first_argument));
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User getUser() {
		return this.user;
	}

	public User find(String string) {
		User user = this.findBy("objectId", string);
		if (user != null) {
			return user;
		}
		user = this.findBy("email", string);
		if (user != null) {
			return user;
		}
		return null;
	}

	public User get(int index) {
		ParseQuery query = new ParseQuery(USERS_SCHEMA);
		query.setSkip(index);
		query.setLimit(1);
		List<ParseObject> next = null;
		try {
			next = query.find();
		} catch (ParseException e) {
			return null;
		}
		if (next.isEmpty()) {
			return null;
		}
		return new User(next.get(0), 0);

	}

	public User findBy(String field, String email) {
		ParseQuery query1 = new ParseQuery(USERS_SCHEMA);
		query1.whereEqualTo(field, email);
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

		final Long next_uid = findLatestMessageId(parseObject.getObjectId());

		return new User(parseObject, next_uid);
	}

	public List<String> findAllEmails() {
		ParseQuery query1 = new ParseQuery(USERS_SCHEMA);
		query1.select("email");
		query1.setLimit(200);
		List<ParseObject> profiles;
		try {
			profiles = query1.find();
		} catch (ParseException e) {
			return null;
		}
		if (profiles.size() == 0) {
			return null;
		}

		List<String> emails = new ArrayList<String>(profiles.size());
		for (ParseObject parseObject : profiles) {
			emails.add(parseObject.getString("email"));
		}
		return emails;
	}

	private Long findLatestMessageId(String objectId) {
		ParseQuery query1 = new ParseQuery(getMsgTableByUser(objectId));
		query1.orderByDescending("message_id").setLimit(1);
		List<ParseObject> messages;
		try {
			messages = query1.find();
		} catch (ParseException e) {
			return (long) 1;
		}

		if (messages.size() == 0) {
			System.out.println("No messages found  for a cleanup");
			return (long) 1;
		}

		final ParseObject parseObject = messages.get(0);
		final long message_id = parseObject.getLong("message_id", 1);
		System.out.println("latest message_id is " + message_id);
		return message_id;
	}

	private void createMessageDocument(String userObjectId,
			Map<String, Object> data) throws IOException, MessagingException {
		ParseObject parseMessage = new ParseObject(
				getMsgTableByUser(userObjectId));

		for (Map.Entry<String, Object> kv : data.entrySet()) {
			if (kv.getValue() != null) {
				parseMessage.put(kv.getKey(), kv.getValue());
			}
		}

		parseMessage.saveInBackground();
	}

	private static String getMsgTableByUser(String userObjectId) {
		return MESSAGES_SCHEMA + "_" + userObjectId;
	}

	private JSONObject getISO(Date date) throws MessagingException {
		if (date == null) {
			date = new Date();
		}
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
		// build the message data map
		Map<String, Object> data = getMessageData(currentUser, mp);

		// store in table
		createMessageDocument(currentUser.objectId, data);

		// add the seen tag to the notification (not to table) and post
		// notification
		data.put("unseen", !mp.isSeen());
		data.put("unique_id", mp.getBrifUniqueId());
		notifyMessageAdded(currentUser, data);
	}

	private Map<String, Object> getMessageData(User currentUser,
			MessageParser mp) throws MessagingException, IOException {

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("user_id", currentUser.objectId);
		final long messageId = mp.getMessageId();
		m.put("message_id", messageId);
		final long googleThreadId = mp.getGoogleThreadId();
		m.put("google_trd_id", Long.toString(googleThreadId));
		final long googleMessageId = mp.getGoogleMessageId();
		m.put("google_msg_id", Long.toString(googleMessageId));

		// sender details
		final String[] sentBy = mp.getSender();
		if (sentBy != null && sentBy[0] != null) {
			m.put("sender_email", sentBy[0]);
			if (sentBy[1] != null) {
				m.put("sender_name", sentBy[1]);
			}
		}

		final Date sentDate = mp.getSentDate();
		m.put("sent_date", getISO(sentDate));
		final String subject = mp.getSubject();
		m.put("subject", subject);

		// cleanup variables in case of equality of original recipients and
		// recipients
		String originalRecipients = mp.getOriginalRecipients();
		String originalRecipientsId = mp.getOriginalRecipientsId();
		final String recipients = mp.getRecipients();
		final String recipientsNames = mp.getRecipientsName();
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
		m.put("recipients_names", recipientsNames);

		m.put("content", mp.getContent());
		m.put("intro", mp.getIntro());
		m.put("unsubscribe", mp.getUnsubscribe());

		final JSONArray attachments = mp.getAttachmentsAsJSON();
		if (attachments != null) {
			m.put("attachments", attachments);
		}

		return m;
	}

	private void notifyMessageAdded(User currentUser, Map<String, Object> data) {
		if (notificationsHandler != null) {
			notificationsHandler.notifyMessagesEvent(currentUser.email,
					"added", data);
		}
	}

	public void updateUserToken(final User currentUser) {
		ParseObject user = new ParseObject(USERS_SCHEMA);
		user.setObjectId(currentUser.objectId);
		user.put("access_token", currentUser.access_token);
		user.updateInBackground();
	}

	public void removeMessage(final String userObjectId, final long uid) {
		ParseObject parseMessage = new ParseObject(
				getMsgTableByUser(userObjectId));
		parseMessage.put("message_id", uid);
		parseMessage.deleteInBackground();
	}

	public void cleanupUnregisteredMessages(User currentUser) {
		final String msgTableByUser = getMsgTableByUser(currentUser.objectId);
		ParseQuery query1 = new ParseQuery(msgTableByUser);
		query1.whereGreaterThan("message_id", currentUser.next_uid);
		query1.setLimit(3000);
		List<ParseObject> messages;
		try {
			messages = query1.find();
		} catch (ParseException e) {
			return;
		}

		if (messages.size() == 0) {
			System.out.println("No messages found  for a cleanup");
			return;
		}

		System.out.println("found " + messages.size() + " messages to cleanup");
		final ParseBatch parseBatch = new ParseBatch();
		for (ParseObject message : messages) {
			parseBatch.delete(msgTableByUser, message.getObjectId());
		}

		try {
			parseBatch.batch();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void enableUser() {
		if (this.user == null || user.objectId == null) {
			return;
		}

		// search for existing setting for this user
		ParseQuery query = new ParseQuery(SETTINGS_SCHEMA);
		query.whereEqualTo("key", "ready");
		query.whereEqualTo("user_id", user.objectId);
		try {
			List<ParseObject> find = query.find();

			// if found - update its value
			if (find != null && find.size() > 0) {
				final ParseObject parseObject = find.get(0);
				if (!"true".equalsIgnoreCase(parseObject.getString("value"))) {
					parseObject.put("value", "true");
					parseObject.save();
				}
			} else {
				// add the setting to the user
				ParseObject settings = new ParseObject(SETTINGS_SCHEMA);
				settings.put("key", "ready");
				settings.put("user_id", user.objectId);
				settings.put("value", "true");
				settings.save();
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("User enabled");

	}

	public void notifyNixListening() throws ParseException {
		final User u = this.getUser();
		if (u != null) {
			ParseObject user = new ParseObject(USERS_SCHEMA);
			user.setObjectId(u.objectId);
			user.put("nixer_status", "listening");
			user.updateInBackground();
		}
	}

	public void notifyNixDown() throws ParseException {
		final User u = this.getUser();
		if (u != null) {
			ParseObject user = new ParseObject(USERS_SCHEMA);
			user.setObjectId(u.objectId);
			user.put("nixer_status", "down");
			user.update();
		}
	}

	public void notifyNixRemoved() throws ParseException {
		System.out.println("account deactivated");
		final User u = this.getUser();
		if (u != null) {
			ParseObject user = new ParseObject(USERS_SCHEMA);
			user.setObjectId(u.objectId);
			user.put("nixer_status", "remove");
			user.update();
		}
	}
}
