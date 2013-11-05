package com.brif.nix.parser;

import java.util.List;

import javax.mail.Part;

import com.brif.nix.parser.MessageParser.MessageAttachment;

public class TextMessageParser implements MimePraser {

	private Object content;
	private Part message;

	public TextMessageParser(Object content2, Part message) {
		this.content = content2;
		this.message = message;
	}

	public String getContent() {
		if (this.content instanceof String) {
			final String content2 = (String) this.content;
			return "<pre>" + content2 + "</pre>";
		}
		return "";
	}

	@Override
	public void collectAttachments(List<MessageAttachment> atts) {
		if (!(this.content instanceof String)) {
			// TODO add attachment of text/* attachments
		}
		return;
	}
}
