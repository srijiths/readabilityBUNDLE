package com.sree.textbytes.readabilityBUNDLE.extractor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sree.textbytes.readabilityBUNDLE.AddSiblings;
import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.nextpage.AppendNextPage;
import com.sree.textbytes.readabilityBUNDLE.Patterns;
import com.sree.textbytes.readabilityBUNDLE.ScoreInfo;
import com.sree.textbytes.StringHelpers.StopWords;
import com.sree.textbytes.StringHelpers.WordStats;

/**
 * 
 * Concept and original codes taken from Project Goose
 * 
 * source wise customization and code modifications by Sree
 * 
 * modified author : sree
 *
 */

public class GooseExtractor {
	
	public Logger logger = Logger.getLogger(GooseExtractor.class.getName());
	
	public Element grabArticle(Article article) {
		
		Element extractedContent = null;
		extractedContent = fetchArticleContent(article.getCleanedDocument());

		if(article.getMultiPageStatus()) {
			AppendNextPage appendNextPage = new AppendNextPage();
			Element finalConsolidated = appendNextPage.appendNextPageContent(article, extractedContent, "ReadabilityGoose");
			return finalConsolidated;
		}else 
			return extractedContent;
	}
	
	
	public Element fetchArticleContent(Document document) {
		return calculateBestNodeBasedOnClustering(document);
	}


	/**
	 * we're going to start looking for where the clusters of paragraphs are.
	 * We'll score a cluster based on the number of stopwords and the number of
	 * consecutive paragraphs together, which should form the cluster of text
	 * that this node is around also store on how high up the paragraphs are,
	 * comments are usually at the bottom and should get a lower score
	 * 
	 * @return
	 */
	private Element calculateBestNodeBasedOnClustering(Document doc) {
		Element topNode = null;

		// grab all the paragraph elements on the page to start to inspect the
		// likely hood of them being good peeps
		ArrayList<Element> nodesToCheck = getNodesToCheck(doc);

		double startingBoost = 1.0;
		int cnt = 0;
		int i = 0;

		// holds all the parents of the nodes we're checking
		Set<Element> parentNodes = new HashSet<Element>();
		ArrayList<Element> nodesWithText = new ArrayList<Element>();

		for (Element node : nodesToCheck) {
			String nodeText = node.text();
			WordStats wordStats = StopWords.getStopWordCount(nodeText);
			boolean highLinkDensity = isHighLinkDensity(node);

			if (wordStats.getStopWordCount() > 2 && !highLinkDensity) {
				nodesWithText.add(node);
			}

		}

		int numberOfNodes = nodesWithText.size();
		int negativeScoring = 0; // we shouldn't give more negatives than
									// positives
		// we want to give the last 20% of nodes negative scores in case they're
		// comments
		double bottomNodesForNegativeScore = (float) numberOfNodes * 0.25;

		logger.debug("About to inspect num of nodes with text: "+ numberOfNodes);

		for (Element node : nodesWithText) {
			logger.debug("NodesWithText : " + node);
			// add parents and grandparents to scoring
			// only add boost to the middle paragraphs, top and bottom is
			// usually jankz city
			// so basically what we're doing is giving boost scores to
			// paragraphs that appear higher up in the dom
			// and giving lower, even negative scores to those who appear lower
			// which could be commenty stuff

			float boostScore = 0;

			if (isOkToBoost(node)) {
				if (cnt >= 0) {
					boostScore = (float) ((1.0 / startingBoost) * 50);
					startingBoost++;
				}
			}

			// check for negative node values
			if (numberOfNodes > 15) {
				if ((numberOfNodes - i) <= bottomNodesForNegativeScore) {
					float booster = (float) bottomNodesForNegativeScore
							- (float) (numberOfNodes - i);
					boostScore = -(float) Math.pow(booster, (float) 2);

					// we don't want to score too highly on the negative side.
					float negscore = Math.abs(boostScore) + negativeScoring;
					if (negscore > 40) {
						boostScore = 5;
					}
				}
			}

			logger.debug("Location Boost Score: " + boostScore 	+ " on iteration: " + i + "' id='" + node.parent().id() + "' class='" + node.parent().attr("class"));
			String nodeText = node.text();
			WordStats wordStats = StopWords.getStopWordCount(nodeText);
			int upscore = (int) (wordStats.getStopWordCount() + boostScore);
			ScoreInfo.updateContentScore(node.parent(), upscore);
			ScoreInfo.updateContentScore(node.parent().parent(), upscore / 2);
			ScoreInfo.updateContentScore(node.parent(), 1);
			ScoreInfo.updateContentScore(node.parent().parent(), 1);

			if (!parentNodes.contains(node.parent())) {
				parentNodes.add(node.parent());
			}

			if (!parentNodes.contains(node.parent().parent())) {
				parentNodes.add(node.parent().parent());
			}

			cnt++;
			i++;

		}

		// now let's find the parent node who scored the highest
		double topNodeScore = 0;
		for (Element e : parentNodes) {
			logger.debug("ParentNode: score='" + e.attr("algoScore") + "' nodeCount='" + e.attr("algoNodes") + "' id='" + e.id()+ "' class='" + e.attr("class") + "' " + "    :  " + e);
			double score = ScoreInfo.getContentScore(e);
			if (score > topNodeScore) {
				topNode = e;
				topNodeScore = score;
			}

			if (topNode == null) {
				topNode = e;
			}
		}

		if (logger.isDebugEnabled()) {
			if (topNode == null) {
				logger.debug("ARTICLE NOT ABLE TO BE EXTRACTED!, WE FAILED!");
			} else {
				String logText;
				String targetText = "";
				Element topPara = topNode.getElementsByTag("p").first();
				if (topPara == null) {
					topNode.text();
				} else {
					topPara.text();
				}

				if (targetText.length() >= 51) {
					logText = targetText.substring(0, 50);
				} else {
					logText = targetText;
				}
				logger.debug("TOPNODE TEXT: " + logText.trim());
				logger.debug("Our TOPNODE: score='" + topNode.attr("algoScore")	+ "' nodeCount='" + topNode.attr("algoNodes")+ "' id='" + topNode.id() + "' class='"+ topNode.attr("class") + "' ");
			}
		}
		
		if(topNode != null) {
			topNode = AddSiblings.addSiblings(topNode);
		}

		return topNode;

	}
	
	/**
	 * returns a list of nodes we want to search on like paragraphs and tables
	 * 
	 * @return
	 */
	private ArrayList<Element> getNodesToCheck(Document doc) {
		ArrayList<Element> nodesToCheck = new ArrayList<Element>();

		nodesToCheck.addAll(doc.getElementsByTag("p"));
		nodesToCheck.addAll(doc.getElementsByTag("pre"));
		nodesToCheck.addAll(doc.getElementsByTag("td"));
		return nodesToCheck;
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

		if (score > 1) {
			return true;
		}

		return false;
	}

	/**
	 * alot of times the first paragraph might be the caption under an image so
	 * we'll want to make sure if we're going to boost a parent node that it
	 * should be connected to other paragraphs, at least for the first n
	 * paragraphs so we'll want to make sure that the next sibling is a
	 * paragraph and has at least some substatial weight to it
	 * 
	 * 
	 * @param node
	 * @return
	 */
	private boolean isOkToBoost(Element node) {
		int stepsAway = 0;

		Element sibling = node.nextElementSibling();
		while (sibling != null) {
			if (sibling.tagName().equals("p")) {
				if (stepsAway >= 3) {
					logger.debug("Next paragraph is too far away, not boosting");

					return false;
				}

				String paraText = sibling.text();
				WordStats wordStats = StopWords.getStopWordCount(paraText);
				if (wordStats.getStopWordCount() > 5) {
					logger.debug("We're gonna boost this node, seems contenty");
					return true;
				}

			}

			// increase how far away the next paragraph is from this node
			stepsAway++;

			sibling = sibling.nextElementSibling();
		}

		return false;
	}

}
