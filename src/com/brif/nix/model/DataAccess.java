package com.brif.nix.model;

import java.io.IOException;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;

import com.brif.nix.parse.Parse;
import com.brif.nix.parse.ParseException;
import com.brif.nix.parse.ParseObject;
import com.brif.nix.parse.ParseQuery;
import com.brif.nix.parser.MessageParser;
import com.sun.mail.gimap.GmailMessage;

/**
 * Data access is only allowed here, decoupling data access to single point
 * 
 * @author Roy, 2013
 */
public class DataAccess {

	// parse-related constants
	private static final String MESSAGES_SCHEMA = "Messages";
	private static final String GROUPS_SCHEMA = "Groups";
	private static final String USERS_SCHEMA = "Users";
	private static final String ACCESS_KEY = "NoVHzsTel7csA1aGoMBNyVz2mHzed4LaSb1d4lme";
	private static final String APP = "mMS3oCiZOHC15v8OGTidsRgHI0idYut39QKrIhIH";

	public DataAccess() {
		Parse.initialize(APP, ACCESS_KEY);
	}


	/**
	 * User representation
	 */
	public static class User {
		public User(String email, String access_token, String refresh_Token,
				String origin, long nextUID, String objectId) {
			super();
			this.email = email;
			this.access_token = access_token;
			this.refresh_token = refresh_Token;
			this.origin = origin;
			this.next_uid = nextUID;
			this.objectId = objectId;

		}

		public String email;
		public String access_token;
		public String refresh_token;
		public String origin;
		public String objectId;
		public long next_uid;
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
		final long next_uid = parseObject.getLong("next_uid");
		
		return new User(email, parseObject.getString("access_token"),
				parseObject.getString("refresh_token"),
				parseObject.getString("origin"),
				next_uid, parseObject.getObjectId());
	}

	public void createMessage(User currentUser, MessageParser mp, String groupId)
			throws IOException, MessagingException {
		ParseObject parseMessage = new ParseObject(MESSAGES_SCHEMA);
		parseMessage.put("message_id", mp.getMessageId());
		parseMessage.put("user", currentUser.objectId);
		parseMessage.put("thread", groupId);
		parseMessage.put("content", mp.getContent());
		parseMessage.setCharset(mp.getCharset());
		parseMessage.saveInBackground();
	}

	public String createGroup(User currentUser, MessageParser mp) {
		try {
			ParseQuery query = new ParseQuery(GROUPS_SCHEMA);
			query.whereEqualTo("md5", mp.getGroupUnique());
			ParseObject group = null;
			List<ParseObject> groups = query.find();
			for (ParseObject potentials : groups) {
				if (potentials.getString("user").equals(currentUser.objectId)) {
					group = potentials;
				}
			}

			if (group == null) {
				group = new ParseObject(GROUPS_SCHEMA);
				group.put("user", currentUser.objectId);
				group.put("recipients", mp.getGroup());
				group.put("md5", mp.getGroupUnique());
				group.save();
			}

			return group.getObjectId();

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public void storeMessage(User currentUser, MessageParser mp)
			throws IOException, MessagingException {
		final String groupId = createGroup(currentUser, mp);
		createMessage(currentUser, mp, groupId);
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

	public void removeMessage(Message message) {
		ParseObject parseMessage = new ParseObject(MESSAGES_SCHEMA);
		GmailMessage gm = (GmailMessage) message;
		try {
			parseMessage.put("message_id", gm.getMsgId());
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		parseMessage.deleteInBackground();
	}
}
