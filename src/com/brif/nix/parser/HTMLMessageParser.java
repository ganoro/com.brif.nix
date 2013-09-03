package com.brif.nix.parser;

import org.jsoup.Jsoup;

public class HTMLMessageParser {

	private String content;

	public HTMLMessageParser(String content) {
		this.content = content;
			}

	public String getContent() {
		return Jsoup.parse(this.content).text();
	}
}
