package com.brif.nix.model;

public class User {

	public User(String email, String access_token, String refresh_Token,
			String origin, long nextUID, String objectId, String locale) {
		super();
		this.email = email;
		this.access_token = access_token;
		this.refresh_token = refresh_Token;
		this.origin = origin;
		this.next_uid = nextUID;
		this.objectId = objectId;
		this.locale = locale;
	}

	public String email;
	public String access_token;
	public String refresh_token;
	public String origin;
	public String objectId;
	public long next_uid;
	public String locale;
	
}