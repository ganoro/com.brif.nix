package com.brif.nix.parser;

public class EmptyContentParser implements MimePraser {

	private Object content;

	public EmptyContentParser(Object content) {
		this.content = content;
	}

	@Override
	public String getContent() {
		return "";
	}

}
