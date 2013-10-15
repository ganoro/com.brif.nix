package com.brif.nix.parser;

import java.util.List;

public interface MimePraser {

	public String getContent();
	
	public void collectAttachments(List<MessageParser.MessageAttachment> atts);
	
}

