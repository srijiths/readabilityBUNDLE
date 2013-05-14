package com.sree.textbytes.readabilityBUNDLE;

/**
 * 
 * @author sree
 * 
 * Score tags in consideration in content extraction process
 * 
 */
public enum ScoreTags {
	div, pre, td, blockquote, address, ol, ul, dl, dd, dt, li, form, h1, h2, h3, h4, h5, h6, th, UNKNOWN;

	public static ScoreTags getTagName(String tag) {
		try {
			return valueOf(tag);

		} catch (Exception e) {
			return UNKNOWN;
		}

	}

}
