package com.brif.nix.parse;

import java.util.List;

public abstract class FindCallback
{
	public abstract void done(List<ParseObject> objects, ParseException e);
}
