package com.sree.textbytes.readabilityBUNDLE;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sree.textbytes.StringHelpers.StopWords;
import com.sree.textbytes.StringHelpers.WordStats;
import com.sree.textbytes.StringHelpers.string;

public class AddSiblings {
	
	public static Logger logger = Logger.getLogger(AddSiblings.class.getName());
	
	/**
	 * adds any siblings that may have a decent score to this node
	 * 
	 * @param node
	 * @return
	 */
	public static Element addSiblings(Element node) {
		logger.debug("Starting to add siblings");
		int baselineScoreForSiblingParagraphs = getBaselineScoreForSiblings(node);
		Element currentSibling = node.previousElementSibling();
		while (currentSibling != null) {
			if (currentSibling.tagName().equals("p")) {
				node.child(0).before(currentSibling.outerHtml());
				currentSibling = currentSibling.previousElementSibling();
				continue;
			}

			// check for a paraph embedded in a containing element
			int insertedSiblings = 0;
			Elements potentialParagraphs = currentSibling.getElementsByTag("p");
			if (potentialParagraphs.first() == null) {
				currentSibling = currentSibling.previousElementSibling();
				continue;
			}
			for (Element firstParagraph : potentialParagraphs) {
				WordStats wordStats = StopWords.getStopWordCount(firstParagraph
						.text());
				int paragraphScore = wordStats.getStopWordCount();
				if ((float) (baselineScoreForSiblingParagraphs * .30) < paragraphScore) {
					logger.debug("This previous node looks like a good sibling, adding it");
					//node.child(insertedSiblings).before("<p>" + firstParagraph.text() + "<p>");
					node.child(insertedSiblings).before("<p>" + firstParagraph.html() + "<p>");
					insertedSiblings++;
				}
			}
			currentSibling = currentSibling.previousElementSibling();
		}
		
		//----
		Element nextSibling = node.nextElementSibling();
		logger.debug("Next element sibling : "+nextSibling);
		if(nextSibling != null) {
			//Elements iframeElements = nextSibling.getElementsByTag("iframe");
			Elements iframeElements = nextSibling.select("iframe|object");
			if(iframeElements.size() > 0) {
				for(Element iframe : iframeElements) {
					if(iframe.tagName().equals("iframe")) {
						String srcAttribute = iframe.attr("src");
						if(!string.isNullOrEmpty(srcAttribute)) {
							if(Patterns.exists(Patterns.VIDEOS, srcAttribute)) {
								logger.debug("Ifarme match found and its a video");
								node.appendElement("p").appendChild(iframe);
							}
						}
					} if(iframe.tagName().equals("object")) {
						Elements embedElements = iframe.getElementsByTag("embed");
						for(Element embedElement : embedElements) {
							String embedSrc = embedElement.attr("src");
							if(Patterns.exists(Patterns.VIDEOS, embedSrc)) {
								logger.debug("Embed Video match found in Next Sibiling");
								node.appendElement("p").appendChild(iframe);
							}
							
						}
				}
						
			}
		}

			int baseLineScoreForNextSibling = getBaselineScoreForSiblings(nextSibling);
			Elements nextParaSiblings = nextSibling.getElementsByTag("p");
			if(nextParaSiblings.size() > 0) {
				for(Element nextPara : nextParaSiblings) {
					WordStats nextParaWordStats = StopWords.getStopWordCount(nextPara.text());
					int nextParaScore = nextParaWordStats.getStopWordCount();
					if((float) (baseLineScoreForNextSibling * .30) < nextParaScore) {
						logger.debug("This next node looks like a good sibling, adding it :"+nextPara);
						//node.appendElement("p").text(nextPara.text());
						node.appendElement("p").html(nextPara.html());
					}
				}
			}
		}
		

		return node;
	}
	
	/**
	 * we could have long articles that have tons of paragraphs so if we tried
	 * to calculate the base score against the total text score of those
	 * paragraphs it would be unfair. So we need to normalize the score based on
	 * the average scoring of the paragraphs within the top node. For example if
	 * our total score of 10 paragraphs was 1000 but each had an average value
	 * of 100 then 100 should be our base.
	 * 
	 * @param topNode
	 * @return
	 */
	private static int getBaselineScoreForSiblings(Element topNode) {
		int base = 100000;
		int numberOfParagraphs = 0;
		int scoreOfParagraphs = 0;

		Elements nodesToCheck = topNode.getElementsByTag("p");

		for (Element node : nodesToCheck) {
			String nodeText = node.text();
			WordStats wordStats = StopWords.getStopWordCount(nodeText);
			boolean highLinkDensity = isHighLinkDensity(node);

			if (wordStats.getStopWordCount() > 2 && !highLinkDensity) {
				numberOfParagraphs++;
				scoreOfParagraphs += wordStats.getStopWordCount();
			}

		}

		if (numberOfParagraphs > 0) {
			base = scoreOfParagraphs / numberOfParagraphs;
			logger.debug("The base score for siblings to beat is: " + base + " NumOfParas: " + numberOfParagraphs + " scoreOfAll: "	+ scoreOfParagraphs);
		}

		return base;

	}
	
	/**
	 * checks the density of links within a node, is there not much text and
	 * most of it contains links? if so it's no good
	 * 
	 * @param e
	 * @return
	 */
	private static boolean isHighLinkDensity(Element e) {
		Elements links = e.getElementsByTag("a");
		if (links.size() == 0) {
			return false;
		}

		String text = e.text().trim();
		String[] words = Patterns.SPACE_SPLITTER.split(text);
		float numberOfWords = words.length;

		// let's loop through all the links and calculate the number of words
		// that make up the links
		StringBuilder sb = new StringBuilder();
		for (Element link : links) {
			sb.append(link.text());
		}
		String linkText = sb.toString();
		String[] linkWords = Patterns.SPACE_SPLITTER.split(linkText);
		float numberOfLinkWords = linkWords.length;

		float numberOfLinks = links.size();

		float linkDivisor = numberOfLinkWords / numberOfWords;
		float score = linkDivisor * numberOfLinks;

		if (logger.isDebugEnabled()) {
			String logText;
			if (e.text().length() >= 51) {
				logText = e.text().substring(0, 50);
			} else {
				logText = e.text();
			}
			logger.debug("Calulated link density score as: " + score + " for node: " + logText);
		}
		if (score > 1) {
			return true;
		}

		return false;
	}


}
