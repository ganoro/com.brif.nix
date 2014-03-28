package com.brif.nix.parser;

import java.util.List;

public interface IMimePraser {

	public String getContent();
	
	public void collectAttachments(List<MessageParser.MessageAttachment> atts);
	
	public String getMetadata(String key);
	
}

