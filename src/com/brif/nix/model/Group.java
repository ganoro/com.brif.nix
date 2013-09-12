package com.brif.nix.model;

import java.util.HashMap;
import java.util.Map;

public class Group {

	public String recipients;
	public String md5;
	public long size;
	public long unseen;
	
	public Group(String recipients, String md5, long size, long unseen) {
		super();
		this.recipients = recipients;
		this.md5 = md5;
		this.size = size;
		this.unseen = unseen;
	}

	public Group(String recipients, String md5) {
		this(recipients, md5, 1, 1);
	}
	
	public Map<String, Object> toMap(String[]... entry) {
		Map<String, Object> result = new HashMap<String, Object>(4);
		result.put("recipients", recipients);
		result.put("md5", md5);
		result.put("size", size);
		result.put("unseen", unseen);
		
		for (String[] strings : entry) {
			result.put(strings[0], strings[1]);
		}
		
		return result;
	}

}
