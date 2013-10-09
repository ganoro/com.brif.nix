package com.brif.nix.parser;

public class HTMLMessageParser implements MimePraser {

	private String content;

	public HTMLMessageParser(String content) {
		this.content = content;
	}

	public String getContent() {
		return this.content;
	}
}
