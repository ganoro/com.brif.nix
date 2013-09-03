package com.brif.nix.oauth2;

import java.io.IOException;
import java.util.HashMap;

import com.google.api.client.json.JsonParser;
import com.google.api.client.json.jackson2.JacksonFactory;

public class OAuth2Configuration extends HashMap<String, String> {

	private static final long serialVersionUID = 1L;
	
	private static String google_config_staging = "{\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"client_secret\":\"iCesKUB5OyjwnnCaKstAuZx4\",\"token_uri\":\"https://accounts.google.com/o/oauth2/token\",\"client_email\":\"808248997275-ol6kol8h23j018iug3d5odi9vhrja9j5@developer.gserviceaccount.com\",\"redirect_uris\":\"http://api.brif.us/auth/signin\",\"client_x509_cert_url\":\"https://www.googleapis.com/robot/v1/metadata/x509/808248997275-ol6kol8h23j018iug3d5odi9vhrja9j5@developer.gserviceaccount.com\",\"client_id\":\"808248997275-ol6kol8h23j018iug3d5odi9vhrja9j5.apps.googleusercontent.com\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\",\"javascript_origins\":\"http://staging.brif.us\"}";
	private static String google_config_ofersarid = "{\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"client_secret\":\"hCILo9EpsWZzyzaz42TXDTNF\",\"token_uri\":\"https://accounts.google.com/o/oauth2/token\",\"client_email\":\"808248997275-jb3m6f7k32ebauk063qu8anqj6br1vk3@developer.gserviceaccount.com\",\"redirect_uris\":\"http://api.brif.us/auth/signin\",\"client_x509_cert_url\":\"https://www.googleapis.com/robot/v1/metadata/x509/808248997275-jb3m6f7k32ebauk063qu8anqj6br1vk3@developer.gserviceaccount.com\",\"client_id\":\"808248997275-jb3m6f7k32ebauk063qu8anqj6br1vk3.apps.googleusercontent.com\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\",\"javascript_origins\":\"http://brif.ofersarid.c9.io\"}";
	private static String google_config_ios = "{\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"client_secret\":\"Ucajgf8BPN4fXajscWXLdZ85\",\"token_uri\":\"https://accounts.google.com/o/oauth2/token\",\"client_email\":\"\",\"redirect_uris\":\"urn:ietf:wg:oauth:2.0:oob\",\"oob\",\"client_x509_cert_url\":\"\",\"client_id\":\"808248997275-td1l666khkenuda7irdhr27ullu7svps.apps.googleusercontent.com\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\"}";
	
	// TODO switch with enum???
	private final static OAuth2Configuration ios = new OAuth2Configuration(
			"ios", google_config_ios);
	private final static OAuth2Configuration ofersarid = new OAuth2Configuration(
			"ofersarid", google_config_ofersarid);
	private final static OAuth2Configuration staging = new OAuth2Configuration(
			"staging", google_config_staging);

	public static final OAuth2Configuration getConfiguration(String forOrigin) {
		return "ios".equals(forOrigin) ? ios
				: "ofersarid".equals(forOrigin) ? ofersarid : staging;
	}

	public OAuth2Configuration(String name, String data) {
		JacksonFactory f = new JacksonFactory();
		try {
			f.createJsonParser(data).parseAndClose(this);
		} catch (IOException e) {
			return;
		}
	}

}
