package com.sree.textbytes.readabilityBUNDLE;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Some common weighted methods that can be used across algorithms.
 * 
 * @author sree
 *
 */
public class WeightMethods {
	
    /**
     * Get the number of times a string s appears in the node e.
     * 
     * @param Element
     * @param string - what to split on. Default is ","
     * @return number (integer)
     **/
    public static int getCharCount(Element element, char s) {
        return element.text().split(Character.toString(s)).length - 1;
    }
    
	/**
	 * Get the density of links as a percentage of the content This is the
	 * amount of text that is inside a link divided by the total text in the
	 * node.
	 * 
	 * @param Element
	 * @return number (float)
	 **/

	public static double getLinkDensity(Element linkNode) {
		Elements linkElements = linkNode.getElementsByTag("a");
		int textLength = linkNode.text().length();
		if (textLength == 0)
			textLength = 1;
		int linkLength = 0;
		for (Element a : linkElements) {
			linkLength += a.text().length();

		}
		return linkLength / textLength;
	}
	
    /**
     * Get an elements class/id weight. Uses regular expressions to tell if this element looks good or bad.
     * 
     * @param Element
     * @return number (Integer)
     **/
    public static double getClassWeight(Element element) {
        double weight = 0;
        /* Look for a special classname */
        String className = element.className();
        if (!"".equals(className)) {
            if (Patterns.exists(Patterns.NEGATIVE, className)) {
                weight -= 25;
            }
            if (Patterns.exists(Patterns.POSITIVE, className)) {
                weight += 25;
            }
        }

        /* Look for a special ID */
        String id = element.id();
        if (!"".equals(id)) {
            if (Patterns.exists(Patterns.NEGATIVE, id)) {
                weight -= 25;
            }
            if (Patterns.exists(Patterns.POSITIVE, id)) {
                weight += 25;
            }
        }
        return weight;
    }

}
