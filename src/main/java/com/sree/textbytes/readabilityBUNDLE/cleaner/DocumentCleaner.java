package com.sree.textbytes.readabilityBUNDLE.cleaner;

import org.jsoup.nodes.Document;

import com.sree.textbytes.StringHelpers.ReplaceSequence;
import com.sree.textbytes.StringHelpers.string;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.sree.textbytes.readabilityBUNDLE.Patterns;

/**
 * Original code from Project Goose
 * 
 * modified author : sree
 * 
 * This class is used to pre clean documents(webpages) We go through 3
 * phases of parsing a website cleaning -> extraction -> output
 * formatter This is the cleaning phase that will try to remove
 * comments, known ad junk, social networking divs other things that
 * are known to not be content related.
 */

public class DocumentCleaner  {
	private static final Logger logger = Logger.getLogger(DocumentCleaner.class.getName());

	/**
	 * this regex is used to remove undesirable nodes from our doc indicate that
	 * something maybe isn't content but more of a comment, footer or some other
	 * undesirable node
	 */
	private static final String regExRemoveNodes;
	private static final String queryNaughtyIDs;
	private static final String queryNaughtyClasses;
	private static final String queryNaughtyNames;

	/**
	 * regex to detect if there are block level elements inside of a div element
	 */
	private static final Pattern divToPElementsPattern = Pattern
			.compile("<(a|blockquote|dl|div|img|ol|p|pre|table|ul)");
	private static final ReplaceSequence tabsAndNewLinesReplcesments;
	private static final Pattern captionPattern = Pattern.compile("^caption$");
	private static final Pattern googlePattern = Pattern.compile(" google ");
	private static final Pattern entriesPattern = Pattern
			.compile("^[^entry-]more.*$");
	private static final Pattern facebookPattern = Pattern
			.compile("[^-]facebook");
	private static final Pattern twitterPattern = Pattern
			.compile("[^-]twitter");

	static {

		StringBuilder sb = new StringBuilder();
		// create negative elements
		sb.append("^side$|combx|retweet|menucontainer|navbar|^comment$|^commentContent$|^comment-body$|PopularQuestions|contact|foot|footer|Footer|footnote|cnn_strycaptiontxt|links|meta$|scroll|shoutbox|sponsor");
		sb.append("|tags|socialnetworking|socialNetworking|cnnStryHghLght|cnn_stryspcvbx|^inset$|pagetools|post-attributes|welcome_form|contentTools2|the_answers");
		sb.append("|communitypromo|runaroundLeft|^subscribe$|vcard|articleheadings|^date$|^print$|popup|tools|socialtools|byline|konafilter|KonaFilter|breadcrumbs|^fn$|wp-caption-text|^column c160 left mb max$|^FL$");
		sb.append("|^job_inner_tab_content$|^newsItem newsMagazine$|^newsItem newsOnline$|^float$|^mod-featured-title$|^below$|^quotePeekContainer$");
		regExRemoveNodes = sb.toString();
		
		queryNaughtyIDs = "[id~=(" + regExRemoveNodes + ")]";
		queryNaughtyClasses = "[class~=(" + regExRemoveNodes + ")]";
		queryNaughtyNames = "[name~=(" + regExRemoveNodes + ")]";

		tabsAndNewLinesReplcesments = ReplaceSequence.create("\n", "\n\n")
				.append("\t").append("^\\s+$");
	}
	
	public Document docToClean;

	public Document clean(Document doc) {
		logger.debug("Starting cleaning phase with DocumentCleaner : "+doc);
		this.docToClean = doc;
		cleanEmTags(docToClean);
		removeDropCaps(docToClean);
		removeScriptsAndStyles(docToClean);
		removeStyleSheets(docToClean);
		convertNoScriptToDiv(docToClean);
		
		removeComments(docToClean);

		cleanBadTags(docToClean);

		removeNodesViaRegEx(docToClean, captionPattern);
		removeNodesViaRegEx(docToClean, googlePattern);
		removeNodesViaRegEx(docToClean, entriesPattern);

		/**
		 * remove twitter and facebook nodes, mashable has f'd up class names for this
		 */
		removeNodesViaRegEx(docToClean, facebookPattern);
		removeNodesViaRegEx(docToClean, twitterPattern);

		// turn any divs that aren't used as true layout items with block level
		// elements inside them into paragraph tags
		
		
		cleanUpSpanTagsInParagraphs(docToClean);
		convertDivsToParagraphs(docToClean, "div");
		convertDivsToParagraphs(docToClean, "span");
		
		//convertDoubleBrsToP(docToClean);
		convertFontToSpan(docToClean);
		removeEmptyParas(docToClean);
		
		//convertDivToParagraph(docToClean,"div");
		//convertDivToParagraph(docToClean,"span");
		

			
		return docToClean;
	}

	
	/**
	 * Convert Font tags to Span tags
	 *  
	 * @param docToClean
	 * @return
	 */
	private void convertFontToSpan(Document docToClean) {
		Elements fonts = docToClean.getElementsByTag("font");
		for(Element font : fonts) {
			changeElementTag(font, "span");
		}
	}
	
	/**
	 * Remove any style sheets attached
	 * 
	 * @param docToClean
	 * @return
	 */
	private void removeStyleSheets(Document docToClean) {
		Elements stylesheets = docToClean.select("link[rel='stylesheet']");
		logger.debug("Removing "+stylesheets.size() + " style sheets");
		stylesheets.remove();
	}
	
	/**
	 * Convert double br's in to p tag
	 * 
	 * @param docToClean
	 * @return
	 */
	
	private void convertDoubleBrsToP(Document docToClean) {
		 Elements doubleBrs = docToClean.select("br + br");
	        for (Element br : doubleBrs) {
	            // we hope that there's a 'p' up there....
	            Elements parents = br.parents();
	            Element parent = null;
	            for (Element aparent : parents) {
	                if (aparent.tag().getName().equals("p")) {
	                    parent = aparent;
	                    break;
	                }
	            }
	            if (parent == null) {
	                parent = br.parent();
	                parent.wrap("<p></p>");
	            }
	            // now it's safe to make the change.
	            String inner = parent.html();
	            inner = Patterns.REPLACE_BRS.matcher(inner).replaceAll("</p><p>");
	            parent.html(inner);
	        }
	}

	/**
	 * Remove #comment tags from document
	 * 
	 * @param docToClean
	 * @return
	 */
	
	private void removeComments(Document docToClean) {
		List<Node> childNodes = docToClean.childNodes();
		for(Node node : childNodes) {
			cleanComments(node);
		}
	}
	
	/**
	 * Convert noscript tag to div
	 * 
	 * @param docToClean
	 * @return
	 */
	private void convertNoScriptToDiv(Document docToClean) {
		Elements noScripts = docToClean.getElementsByTag("noscript");
		for(Element noScript : noScripts) {
			logger.debug("Converting NO SCRIPT to DIV");
			changeElementTag(noScript, "div");
		}
	}
	

	/**
	 * Change document element Tag
	 * 
	 * @param e
	 * @param newTag
	 * @return
	 */
	
    private Element changeElementTag(Element e, String newTag) {
        Element newElement = docToClean.createElement(newTag);
        /* JSoup gives us the live child list, so we need to make a copy. */
        List<Node> copyOfChildNodeList = new ArrayList<Node>();
        copyOfChildNodeList.addAll(e.childNodes());
        for (Node n : copyOfChildNodeList) {
            n.remove();
            newElement.appendChild(n);
        }
        e.replaceWith(newElement);
        return newElement;
    }
	
    /**
     * Clean comments
     * 
     * @param node
     */
	private void cleanComments(Node node) {
		int i = 0;
        while (i < node.childNodes().size()) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment")) {
            	logger.debug("Cleaning comment tag "+child);
                child.remove();
            }
            else {
            	cleanComments(child);
                i++;
            }
        }
	}

	/**
	 * Removing any null paras
	 * 
	 * @param docToClean
	 * @return
	 */
	private void removeEmptyParas(Document docToClean) {
		Elements paras = docToClean.select("p");
		for(Element para : paras) {
			if(string.isNullOrEmpty(para.text()) && para.childNodes().size() == 0) {
				logger.debug("Null Para found :"+para + "size : "+para.childNodes().size());
				para.remove();
			}
		}
	}
	
	/**
	 * remove those css drop caps where they put the first letter in big text in
	 * the 1st paragraph
	 */
	private void removeDropCaps(Document doc) {
		Elements items = doc.select("span[class~=(dropcap|drop_cap)]");
		logger.debug("Cleaning " + items.size() + " dropcap tags");
		for (Element item : items) {
			TextNode tn = new TextNode(item.text(), doc.baseUri());
			item.replaceWith(tn);
		}
	}

	private void cleanBadTags(Document doc) {
		// only select elements WITHIN the body to avoid removing the body
		// itself
		Elements children = doc.body().children();

		Elements naughtyList = children.select(queryNaughtyIDs);
		logger.debug(naughtyList.size() + " naughty ID elements found");
		for (Element node : naughtyList) {
			logger.debug("Cleaning: Removing node with id: " + node.id());
			removeNode(node);
		}
		Elements naughtyList2 = children.select(queryNaughtyIDs);
		logger.debug(naughtyList2.size()
				+ " naughty ID elements found after removal");

		Elements naughtyList3 = children.select(queryNaughtyClasses);
		logger.debug(naughtyList3.size() + " naughty CLASS elements found");
		for (Element node : naughtyList3) {
			logger.debug("clean: Removing node with class: " + node.className());
			removeNode(node);
		}
		Elements naughtyList4 = children.select(queryNaughtyClasses);
		logger.debug(naughtyList4.size()
				+ " naughty CLASS elements found after removal");

		// starmagazine puts shit on name tags instead of class or id
		Elements naughtyList5 = children.select(queryNaughtyNames);

		logger.debug(naughtyList5.size() + " naughty Name elements found");
		for (Element node : naughtyList5) {
			logger.debug("clean: Removing node with class: "
					+ node.attr("class") + " id: " + node.id() + " name: "
					+ node.attr("name"));
			removeNode(node);
		}
	}

	/**
	 * Apparently jsoup expects the node's parent to not be null and throws if
	 * it is. Let's be safe.
	 * 
	 * @param node
	 *            the node to remove from the doc
	 */
	private void removeNode(Element node) {
		if (node == null || node.parent() == null)
			return;
		logger.debug("Removing Cleaning node : "+node);
		node.remove();
	}

	/**
	 * removes nodes that may have a certain pattern that matches against a
	 * class or id tag
	 * 
	 * @param pattern
	 */
	private void removeNodesViaRegEx(Document doc, Pattern pattern) {
		try {
			Elements naughtyList = doc.getElementsByAttributeValueMatching(
					"id", pattern);
			logger.debug("regExRemoveNodes: " + naughtyList.size()
					+ " ID elements found against pattern: " + pattern);
			for (Element node : naughtyList) {
				removeNode(node);
			}

			Elements naughtyList3 = doc.getElementsByAttributeValueMatching(
					"class", pattern);

			logger.debug("regExRemoveNodes: " + naughtyList3.size()
					+ " CLASS elements found against pattern: " + pattern);
			for (Element node : naughtyList3) {
				removeNode(node);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			logger.error(e.toString());
		}
	}
	
	private void removeScriptsAndStyles(Document doc) {
		logger.debug("Starting to remove script tags");

		Elements scripts = doc.getElementsByTag("script");
		for (Element item : scripts) {
			item.remove();
		}
		logger.debug("Removed: " + scripts.size() + " script tags");
		logger.debug("Removing Style Tags");
		Elements styles = doc.getElementsByTag("style");
		for (Element style : styles) {
			style.remove();
		}
		logger.debug("Removed: " + styles.size() + " style tags");
	}
	
	
	private void convertDivToParagraph(Document docToClean,String tag) {
		logger.debug("Starting to replace bad divs");
		Elements divElements = docToClean.getElementsByTag(tag);
		for(Element divElement :  divElements) {
			boolean hasBlock = false;
			for(Element divChild : divElement.getAllElements()) {
				if(divChild != divElement) {
					if(Patterns.DIV_TO_P_ELEMENTS.contains(divChild.tagName())) {
						hasBlock = true;
					}
				}
			}
			if(!hasBlock) {
				Element newElement = changeElementTag(divElement, "p");
			}else {
				for(int i=0;i<divElement.childNodes().size();i++) {
					Node childNode = divElement.childNodes().get(i);
					if(childNode instanceof TextNode) {
						Element p = docToClean.createElement("p");
                        p.html(((TextNode)childNode).text());
                        childNode.replaceWith(p);
					}
				}
			}
		}
	}
	
	private void convertDivsToParagraphs(Document doc, String domType) {
		logger.debug("Starting to replace bad divs...");

		int divIndex = 0;
		int badDivs = 0;
		int convertedTextNodes = 0;
		Elements divs = doc.getElementsByTag(domType);
		for (Element div : divs) {
			try {
				Matcher divToPElementsMatcher = divToPElementsPattern
						.matcher(div.html().toLowerCase());
				if (divToPElementsMatcher.find() == false) {
					replaceElementsWithPara(doc, div);
					badDivs += 1;
				} else {
					ArrayList<Node> replaceNodes = getReplacementNodes(doc, div);
					for (Element child : div.children()) {
						child.remove();
					}
					for (Node node : replaceNodes) {
						try {
							div.appendChild(node);

						} catch (Exception e) {
							logger.error(e);
						}

					}

				}
			} catch (NullPointerException e) {
				logger.error(e.toString());
			}

			divIndex += 1;

		}

		logger.debug("Found " + divs.size() + " total divs with " + badDivs
				+ " bad divs replaced and " + convertedTextNodes
				+ " textnodes converted inside divs");
	}

	private ArrayList<Node> getReplacementNodes(Document doc, Element div) {
		StringBuilder replacementText = new StringBuilder();
		ArrayList<Node> nodesToReturn = new ArrayList<Node>();
		ArrayList<Node> nodesToRemove = new ArrayList<Node>();

		for (Node kid : div.childNodes()) {
			if (kid.nodeName().equals("p") && replacementText.length() > 0) {
				// flush the buffer of text
				Element newNode = getFlushedBuffer(replacementText, doc);
				nodesToReturn.add(newNode);
				replacementText.setLength(0);
				if (kid instanceof Element) {
					Element kidElement = (Element) kid;
					nodesToReturn.add(kidElement);
				}
			} else if (kid.nodeName().equals("#text")) {
				TextNode kidTextNode = (TextNode) kid;
				String kidText = kidTextNode.attr("text");
				if (string.isNullOrEmpty(kidText))
					continue;

				// clean up text from tabs and newlines
				String replaceText = tabsAndNewLinesReplcesments
						.replaceAll(kidText);
				if (replaceText.trim().length() > 1) {
					Node previousSiblingNode = kidTextNode.previousSibling();
					while (previousSiblingNode != null
							&& previousSiblingNode.nodeName().equals("a")
							&& !previousSiblingNode.attr("grv-usedalready")
									.equals("yes")) {
						replacementText.append(" "
								+ previousSiblingNode.outerHtml() + " ");
						nodesToRemove.add(previousSiblingNode);
						previousSiblingNode.attr("grv-usedalready", "yes");
						if (previousSiblingNode.previousSibling() != null) {
							previousSiblingNode = previousSiblingNode
									.previousSibling();
						} else {
							previousSiblingNode = null;
						}
					}
					// add the text of the node
					replacementText.append(replaceText);
					// check the next set of links that might be after text (see
					// businessinsider2.txt)
					Node nextSiblingNode = kidTextNode.nextSibling();
					while (nextSiblingNode != null
							&& nextSiblingNode.nodeName().equals("a")
							&& !nextSiblingNode.attr("grv-usedalready").equals(
									"yes")) {
						replacementText.append(" "
								+ nextSiblingNode.outerHtml() + " ");
						nodesToRemove.add(nextSiblingNode);
						nextSiblingNode.attr("grv-usedalready", "yes");
						if (nextSiblingNode.nextSibling() != null) {
							nextSiblingNode = nextSiblingNode.nextSibling();
						} else {
							nextSiblingNode = null;
						}
					}
				}

				nodesToRemove.add(kid);
			} else {
				nodesToReturn.add(kid);
			}
		}
		// flush out anything still remaining
		if (replacementText.length() > 0) {
			Element newNode = getFlushedBuffer(replacementText, doc);
			nodesToReturn.add(newNode);
			replacementText.setLength(0);
		}
		for (Node node : nodesToRemove) {
			node.remove();
		}

		return nodesToReturn;

	}

	/**
	 * go through all the div's nodes and clean up dangling text nodes and get
	 * rid of obvious jank
	 */
	private Element getFlushedBuffer(StringBuilder replacementText, Document doc) {
		String bufferedText = replacementText.toString();
		logger.debug("Flushing TextNode Buffer: " + bufferedText.trim());
		Document newDoc = new Document(doc.baseUri());
		Element newPara = newDoc.createElement("p");
		newPara.html(bufferedText);
		return newPara;

	}

	private void replaceElementsWithPara(Document doc, Element div) {
		Document newDoc = new Document(doc.baseUri());
		Element newNode = newDoc.createElement("p");
		newNode.append(div.html());
		div.replaceWith(newNode);

	}

	/**
	 * @param doc
	 * @return
	 * 
	 *         Clean up span tags in paragraphs takes care of the situation
	 *         where you have a span tag nested in a paragraph tag
	 */

	private void cleanUpSpanTagsInParagraphs(Document doc) {
		Elements span = doc.getElementsByTag("span");
		logger.debug("Cleaning " + span.size() + " span tags in paragraph ");

		for (Element item : span) {
			if (item.parent().nodeName().equals("p")) {
				TextNode tn = new TextNode(item.text(), doc.baseUri());
				item.replaceWith(tn);
				logger.debug("Replacing nested span with TextNode: "
						+ item.text());

			}
		}
	}

	/**
	 * replaces <em> tags with textnodes
	 */
	private void cleanEmTags(Document doc) {
		Elements ems = doc.getElementsByTag("em");
		logger.debug("Cleaning " + ems.size() + " EM tags");
		for (Element node : ems) {
			// replace the node with a div node
			Elements images = node.getElementsByTag("img");
			if (images.size() != 0) {
				continue;
			}
			TextNode tn = new TextNode(node.text(), doc.baseUri());
			node.replaceWith(tn);
		}
	}



}