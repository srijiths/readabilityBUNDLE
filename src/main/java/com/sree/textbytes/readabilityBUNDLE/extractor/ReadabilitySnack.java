package com.sree.textbytes.readabilityBUNDLE.extractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.sree.textbytes.readabilityBUNDLE.AddSiblings;
import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.nextpage.AppendNextPage;
import com.sree.textbytes.readabilityBUNDLE.ScoreInfo;
import com.sree.textbytes.readabilityBUNDLE.ScoreTags;

/**
 * 
 * Concept and original codes taken from Snacktory
 * 
 * source wise customization and code modifications by Sree
 * 
 * modified author : sree
 *
 */

public class ReadabilitySnack {

	// Unlikely candidates
	private final Pattern UNLIKELY = Pattern
			.compile("^(com(bx|ment|munity)|dis(qus|cuss)|e(xtra|[-]?mail)|foot|"
					+ "header|menu|re(mark|ply)|rss|sh(are|outbox)|sponsor"
					+ "a(d|ll|gegate|rchive|ttachment)|(pag(er|ination))|popup|print|"
					+ "login|si(debar|gn|ngle))");
	// Most likely positive candidates
	private final Pattern POSITIVE = Pattern
			.compile("(^(body|content|h?entry|main|page|post|text|blog|story|haupt))"
					+ "|arti(cle|kel)|instapaper_body");
	// Most likely negative candidates
	private final Pattern NEGATIVE = Pattern
			.compile("nav($|igation)|user|com(ment|bx)|(^com-)|contact|"
					+ "foot|masthead|^(me(dia|ta))$|outbrain|promo|related|scroll|(sho(utbox|pping))|"
					+ "sidebar|sponsor|tags|tool|widget");
	
	public static Logger logger = Logger.getLogger(ReadabilitySnack.class.getName());
	
	public Element grabArticle(Article article) {
		Element extractedContent = null;
		extractedContent = fetchArticleContent(article.getCleanedDocument());
		
		if(article.getMultiPageStatus()) {
			AppendNextPage appendNextPage = new AppendNextPage();
			Element finalConsolidated = appendNextPage.appendNextPageContent(article, extractedContent, "ReadabilitySnack");
			return finalConsolidated;
			
		}else 
			return extractedContent;
	}
	
	public Element fetchArticleContent(Document document) {
		Element topNode = null;
		Set<Element> parentNodes = new HashSet<Element>();
		Collection<Element> nodesToCheck = getNodesToCheck(document);

		for (Element element : nodesToCheck) {
			if (element.text().length() < 25) {
				logger.debug("Inner Text less than critical , ignoring "
						+ element.tagName());
				continue;
			}
			double contentScore = getElementScore(element);
			logger.debug("Content Score : " + contentScore + "Element " + element.tagName());
			ScoreInfo.updateContentScore(element.parent(), contentScore);
			ScoreInfo.updateContentScore(element.parent().parent(), contentScore / 2);
			if (!parentNodes.contains(element.parent())) {
				parentNodes.add(element.parent());

			}
			if (!parentNodes.contains(element.parent().parent())) {
				parentNodes.add(element.parent().parent());

			}
		}

		double topNodeScore = 0;
		for (Element e : parentNodes) {
			double score = ScoreInfo.getContentScore(e);
			if (score > topNodeScore) {
				topNode = e;
				topNodeScore = score;

			}
			if (topNode == null) {
				topNode = e;

			}

		}
		
		if(topNode != null) {
			topNode = AddSiblings.addSiblings(topNode);
		}

		return topNode;
	}

	public double getElementScore(Element element) {
		double contentScore = 0;
		ScoreTags scoreTags = ScoreTags.getTagName(element.tagName());
		switch (scoreTags) {

		case div:
			contentScore += 5;
			break;
		case pre:
		case td:
		case blockquote:
			contentScore += 3;
			break;
		case address:
		case ol:
		case ul:
		case dl:
		case dd:
		case dt:
		case li:
		case form:
			contentScore -= 3;
			break;
		case h1:
		case h2:
		case h3:
		case h4:
		case h5:
		case h6:
		case th:
			contentScore -= 5;
			break;

		default:
			logger.debug("Scoreless Tag  " + element.tagName());
			break;
		}

		double weight = 0;
		weight = getClassWeight(element);
		contentScore += weight;
		return contentScore;

	}

	protected double getClassWeight(Element e) {
		double weight = 0;
		if (POSITIVE.matcher(e.className()).find())
			weight += 35;
		if (POSITIVE.matcher(e.id()).find())
			weight += 40;
		if (UNLIKELY.matcher(e.className()).find())
			weight -= 20;
		if (UNLIKELY.matcher(e.id()).find())
			weight -= 20;
		if (NEGATIVE.matcher(e.className()).find())
			weight -= 50;
		if (NEGATIVE.matcher(e.id()).find())
			weight -= 50;

		weight += (int) Math.round(e.ownText().length() / 100.0 * 10);
		weight += weightChildNodes(e);
		return weight;
	}

	protected int weightChildNodes(Element e) {
		int weight = 0;
		Element caption = null;
		List<Element> headerEls = new ArrayList<Element>(5);
		List<Element> pEls = new ArrayList<Element>(5);

		for (Element child : e.children()) {
			String ownText = child.ownText();
			int ownTextLength = ownText.length();
			if (ownTextLength < 20)
				continue;
			if (ownTextLength > 200)
				weight += Math.max(50, ownTextLength / 10);
			if (e.id().contains("caption") || e.className().contains("caption"))
				weight += 30;
			if (child.tagName().equals("h1") || child.tagName().equals("h2")) {
				weight += 30;
			} else if (child.tagName().equals("div")
					|| child.tagName().equals("p")) {
				weight += calcWeightForChild(child, e, ownText);
				if (child.tagName().equals("p") && ownTextLength > 50)
					pEls.add(child);
				if (child.className().toLowerCase().equals("caption"))
					caption = child;

			}

		}

		// use caption and image
		if (caption != null)
			weight += 30;
		if (pEls.size() >= 2) {
			for (Element subEl : e.children()) {
				if ("h1;h2;h3;h4;h5;h6".contains(subEl.tagName())) {
					weight += 20;
					headerEls.add(subEl);

				}

				if ("p".contains(subEl.tagName()))
					ScoreInfo.updateContentScore(subEl, 30);

			}

			weight += 60;

		}

		return weight;

	}

	public int getWordCount(String string, String subString) {
		int count = 0;
		int index = string.indexOf(subString);
		if (index >= 0) {
			count++;
			count += getWordCount(string.substring(index + subString.length()),
					subString);

		}

		return count;
	}

	public int calcWeightForChild(Element child, Element e, String ownText) {
		int count = getWordCount(ownText, "&quot;");
		count += getWordCount(ownText, "&lt;");
		count += getWordCount(ownText, "&gt;");
		count += getWordCount(ownText, "px");
		int val;
		int c = 0;
		if (c > 5)
			val = -30;

		else
			val = (int) Math.round(ownText.length() / 25.0);
		ScoreInfo.updateContentScore(child, val);
		return val;

	}

	private Collection<Element> getNodesToCheck(Document doc) {
		Map<Element, Object> nodesToCheck = new LinkedHashMap<Element, Object>(
				64);
		for (Element element : doc.select("body").select("*")) {
			if ("p;td;h1;h2;pre".contains(element.tagName())) {
				nodesToCheck.put(element, null);
			}
		}
		return nodesToCheck.keySet();
	}
}
