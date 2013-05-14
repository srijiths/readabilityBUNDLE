package com.sree.textbytes.readabilityBUNDLE;

import org.jsoup.nodes.Element;
import com.sree.textbytes.StringHelpers.string;

/**
 * 
 * 
 * Methods to update,get and set content scores to {@link Element} as attributes
 *
 */

public class ScoreInfo {
	
	/**
	 * Increment the content score of an {@link Element}
	 * 
	 * @param node
	 * @param addToScore
	 */
	
	public static void updateContentScore(Element node, double addToScore) {
		double currentScore;
		try {
			currentScore = getContentScore(node);

		} catch (NumberFormatException e) {
			currentScore = 0;
		}
		double newScore = currentScore + addToScore;
		setContentScore(node, newScore);
	}

	/**
	 * Set content score as {@link Element} attribute
	 * 
	 * @param el
	 * @param score
	 */
	public static void setContentScore(Element el, double score) {
		el.attr("algoScore", Double.toString(score));

	}

	/**
	 * Get content score of an {@link Element}
	 * 
	 * @param node
	 * @return
	 */
	public static double getContentScore(Element node) {
		if (node == null)
			return 0;
		try {
			String grvScoreString = node.attr("algoScore");
			if (string.isNullOrEmpty(grvScoreString))
				return 0;
			return Double.parseDouble(grvScoreString);

		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	/**
	 * Check whether an {@link Element} is scored or not
	 * 
	 * @param node
	 * @return
	 */
	
    public static boolean isElementScored(Element node) {
        return node.hasAttr("algoScore");
    }

}
