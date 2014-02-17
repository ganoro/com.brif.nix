package com.brif.nix.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactInformationParser {

	/**
	 * Extracts contact information from a string using regular expressions.
	 * This function is relatively tolerant of poor OCR, as long as it maintains
	 * letters and characters.
	 * 
	 * @param results
	 *            The result array from which to extract contact information.
	 * @return Returns a Bundle containing extracted Contact.Insert fields.
	 */
	public Map<String, String> extractInformation(String input) {
		Map<String, String> extras = new HashMap<String, String>();

		if (input == null || input.length() == 0) {
			return extras;
		}

		String str = input;
		Pattern p;
		Matcher m;

		/*
		 * Name-matching Expression - Matches: T.V. Raman Alan Viverette Charles
		 * L. Chen Julie Lythcott-Haimes - Does not match: Google Google User
		 * Experience Team 650-720-5555 cell
		 */
		p = Pattern.compile("^([A-Z]([a-z]*|\\.) *){1,2}([A-Z][a-z]+-?)+$",
				Pattern.MULTILINE);
		m = p.matcher(str);

		if (m.find()) {
			extras.put("NAME", m.group());
		}

		/*
		 * Address-matching Expression - Matches: 2600 Amphitheatre Pkwy. P.O.
		 * Box 26000 1600 Pennsylvania Avenue 1 Geary - Does not match: Google
		 * T.V. Raman 650-720-5555 cell
		 */
		p = Pattern.compile(
				"^(\\d+ ([A-Z][a-z]+.? +)*[A-Z][a-z]+.?|P.?O.? *Box +\\d+)$",
				Pattern.MULTILINE);
		m = p.matcher(str);

		if (m.find()) {
			extras.put("POSTAL", m.group());
			extras.put("POSTAL_TYPE", "TYPE_WORK");
			extras.put("POSTAL_ISPRIMARY", "true");
		}

		/*
		 * Address-matching Expression 2 - Matches: Mountain View, CA 94304
		 * Houston TX 77069 Stanford, CA 94309-2901 Salt Lake City, UT 12345 -
		 * Does not match: Cell 650-720-5555 Ext. 54085 Google 12345
		 */
		p = Pattern.compile(
				"^([A-Z][a-z]+.? *)+ *.? *[A-Z]{2}? *\\d{5}(-\\d[4])?",
				Pattern.MULTILINE);
		m = p.matcher(str);

		if (m.find()) {
			CharSequence address;
			if ((address = extras.get("POSTAL")) == null)
				address = m.group();
			else
				address = address + ", " + m.group();
			extras.put("POSTAL", address.toString());
			extras.put("POSTAL_TYPE", "TYPE_WORK");
			extras.put("POSTAL_ISPRIMARY", "true");
		}

		/*
		 * Email-matching Expression - Matches: email: raman@google.com
		 * spam@google.co.uk v0nn3gu7@ice9.org name @ host.com - Does not match:
		 * #@/.cJX Google c@t
		 */
		p = Pattern.compile(
				"([A-Za-z0-9]+ *@ *[A-Za-z0-9]+(\\.[A-Za-z]{2,4})+)$",
				Pattern.MULTILINE);
		m = p.matcher(str);

		if (m.find()) {
			extras.put("EMAIL", m.group(1));
			extras.put("EMAIL_TYPE", "TYPE_WORK");
			extras.put("EMAIL_ISPRIMARY", "true");
		}

		/*
		 * Phone-matching Expression - Matches: 1234567890 (650) 720-5678
		 * 650-720-5678 650.720.5678 - Does not match: 12345 12345678901
		 * 720-5678
		 */
		p = Pattern
				.compile("(?:^|\\D)(\\d{3})[)\\-. ]*?(\\d{3})[\\-. ]*?(\\d{4})(?:$|\\D)");
		m = p.matcher(str);

		if (m.find()) {
			String phone = "(" + m.group(1) + ") " + m.group(2) + "-"
					+ m.group(3);
			extras.put("PHONE", phone);
			extras.put("PHONE_TYPE", "TYPE_WORK");
			extras.put("PHONE_ISPRIMARY", "true");
		}

		return extras;
	}
}
