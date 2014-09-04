package com.brif.nix.oauth2;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.IMAPProtocol;

/**
 * Adds a label to a given message
 * 
 * @author Roy, 2014
 * 
 */
public class LabelOperation implements IMAPFolder.ProtocolCommand {

	private String label;
	private int msgNumber;

	public LabelOperation(int msgNumber, String label) {
		this.msgNumber = msgNumber;
		this.label = label;
	}

	@Override
	public Object doCommand(IMAPProtocol p) throws ProtocolException {
		String trimmedLabel = label.trim();
		if (trimmedLabel.length() == 1) {
			return null;
		}

		trimmedLabel = "[Brif]/#"
				+ Character.toUpperCase(trimmedLabel.charAt(1))
				+ (trimmedLabel.length() > 2 ? trimmedLabel.substring(2)
						.toLowerCase() : "");
		p.command(
				"STORE " + msgNumber + " +X-GM-LABELS (" + trimmedLabel + ")",
				null);
		return null;
	}
}
