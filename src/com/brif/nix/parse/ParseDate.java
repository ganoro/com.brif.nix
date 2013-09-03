package com.brif.nix.parse;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class ParseDate extends JSONObject {
	public ParseDate(Date date) {

		try {
			this.put("__type", "Date");
			this.put("iso", date);
		} catch (JSONException e) {

		}
		// TODO Auto-generated constructor stub
	}
}
