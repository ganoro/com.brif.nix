package com.brif.nix.parser;

import java.util.List;

public interface IMimePraser {

	public String getContent();
	
	public String getIntro();
	
	public void collectAttachments(List<MessageParser.MessageAttachment> atts, String from);
	
	public String getMetadata(String key);
	
}

