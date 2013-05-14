package com.sree.textbytes.readabilityBUNDLE.extractor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.sree.textbytes.readabilityBUNDLE.AddSiblings;
import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.nextpage.AppendNextPage;
import com.sree.textbytes.readabilityBUNDLE.Patterns;
import com.sree.textbytes.readabilityBUNDLE.ScoreInfo;
import com.sree.textbytes.readabilityBUNDLE.WeightMethods;

/**
 * 
 * Exact java porting of Readability content extraction algorithm.
 * 
 * Concept and original codes taken from Java-Readability
 * 
 * source wise customization and code modifications by Sree
 * 
 * modified author : sree
 *
 */

public class ReadabilityExtractor {
	
	public Logger logger = Logger.getLogger(ReadabilityExtractor.class.getName());
	
    private boolean stripUnlikelyCandidates = true;
    
	
    /**
     * Grab the content from the {@link Article} as {@link Element}
     * 
     * @param article
     * @return
     */
	public Element grabArticle(Article article) {
		
		Element extractedContent = null;
		extractedContent = fetchArticleContent(article.getCleanedDocument());

		if(article.getMultiPageStatus()) {
			AppendNextPage appendNextPage = new AppendNextPage();
			Element finalConsolidated = appendNextPage.appendNextPageContent(article, extractedContent, "ReadabilityCore");
			return finalConsolidated;
		}else 
			return extractedContent;
	}
	
	/**
	 * Fetch the article content
	 * 
	 * @param document
	 * @param body
	 * @return
	 */
	public Element fetchArticleContent(Document document) {
		
		String pageCacheHtml = document.html();
		Elements allElements = document.getAllElements();
		
        /*
         * Note: in Javascript, this list would be *live*. If you deleted a node from the tree, it and its
         * children would remove themselves. To get the same effect, we make a linked list and we remove
         * things from it. This won't win prizes for speed, but, then again, the code in Javascript has to be
         * doing something nearly as awful.
         */
		
        LinkedList<Element> allElementsList = new LinkedList<Element>();
        allElementsList.addAll(allElements);
        
        List<Element> nodesToScore = new ArrayList<Element>();
        nodesToScore = nodePrepping(allElementsList,document);
        
        List<Element> candidates = new ArrayList<Element>();
        candidates = calculateContentScore(nodesToScore);
        
        Element topCandidate = findTopCandidate(candidates);
        
        /**
         * If we still have no top candidate, just use the body as a last resort. We also have to copy the
         * body node so it is something we can modify.
         **/
        
        /*if (topCandidate == null || topCandidate == body) {
            topCandidate = document.createElement("div");
            // not efficient but not likely.
            topCandidate.html(document.html());
            document.html("");
            document.appendChild(topCandidate);
            initializeNode(topCandidate);
        }*/
        
        Element articleContent = null;
        if(topCandidate != null) {
        	articleContent = findSiblingElements(topCandidate,document);
        }
        
		return articleContent;
		
	}
	
    /**
     * First, node prepping. Trash nodes that look cruddy (like ones with the class name "comment", etc),
     * and turn divs into P tags where they have been used inappropriately (as in, where they contain no
     * other block level elements.) Note: Assignment from index for performance. See
     * http://www.peachpit.com/articles/article.aspx?p=31567&seqNum=5 TODO: Shouldn't this be a reverse
     * traversal?
     **/

	
	private List<Element> nodePrepping(LinkedList<Element> allElementsList,Document document) {
		List<Element> nodesToScore = new ArrayList<Element>();
        ListIterator<Element> listIterator = allElementsList.listIterator();
        Set<Element> goodAsDead = new HashSet<Element>();
        
        while (listIterator.hasNext()) {
        	Element node = listIterator.next();
        	if (goodAsDead.contains(node)) {
                continue;
            }
        	
        	 /* Remove unlikely candidates */
            if (stripUnlikelyCandidates) {
                String unlikelyMatchString = node.className() + node.id();
                if (Patterns.exists(Patterns.UNLIKELY_CANDIDATES, unlikelyMatchString)
                    && !Patterns.exists(Patterns.OK_MAYBE_ITS_A_CANDIDATE, unlikelyMatchString)
                    && !"body".equals(node.tagName())) {
                    logger.debug("Removing unlikely candidate - " + unlikelyMatchString);
                    List<Element> toRemoveAndBelow = node.getAllElements();
                    listIterator.remove();
                    /*
                     * adding 'node' to that set is harmless and reduces the code complexity here.
                     */
                    goodAsDead.addAll(toRemoveAndBelow);
                    continue;
                }
            }
            
            if ("p".equals(node.tagName()) || "td".equals(node.tagName()) || "pre".equals(node.tagName())) {
                nodesToScore.add(node);
            }
            
            /*
             * Turn all divs that don't have children block level elements into p's
             */
            
            if ("div".equals(node.tagName())) {
                boolean hasBlock = false;
                for (Element divChild : node.getAllElements()) {
                    if (divChild != node) {
                        if (Patterns.DIV_TO_P_ELEMENTS.contains(divChild.tagName())) {
                            hasBlock = true;
                            break;
                        }
                    }
                }
                if (!hasBlock) {
                    Element newElement = changeElementTag(node, "p",document);
                    nodesToScore.remove(node);
                    nodesToScore.add(newElement);
                } else {
                    /* EXPERIMENTAL *//*
                                       * grab just child text and wrap each chunk in a p
                                       */
                    int limit = node.childNodes().size();
                    for (int i = 0; i < limit; i++) {
                        Node childNode = node.childNodes().get(i);
                        if (childNode instanceof TextNode) {
                            Element p = document.createElement("p");
                            p.attr("basisInline", "true");
                            p.html(((TextNode)childNode).text());
                            childNode.replaceWith(p);
                        }
                    }
                }
            }
        }
        
        return nodesToScore;
	}
	
	

    /**
     * Loop through all paragraphs, and assign a score to them based on how content-y they look. Then add
     * their score to their parent node. A score is determined by things like number of commas, class
     * names, etc. Maybe eventually link density.
     **/
	
	private List<Element> calculateContentScore(List<Element> nodesToScore) {
		List<Element> candidates = new ArrayList<Element>();
        for (Element nodeToScore : nodesToScore) {
            Element parentNode = nodeToScore.parent();
            if (null == parentNode) { // might be an orphan whose parent was
                // dropped previously.
                continue;
            }
            
            Element grandParentNode = parentNode.parent();
            if (grandParentNode == null) {
                continue; // ditto
            }
            
            String innerText = nodeToScore.text();

            /*
             * If this paragraph is less than 25 characters, don't even count it.
             */
            if (innerText.length() < 25) {
                continue;
            }
            
            /* Initialize readability data for the parent. */
            if ("".equals(parentNode.attr("readability"))) {
                initializeNode(parentNode);
                candidates.add(parentNode);
            }
            
            /* Initialize readability data for the grandparent. */
            /*
             * If the grandparent has no parent, we don't want it as a candidate. It's probably a symptom that
             * we're operating in an orphan.
             */
            if (grandParentNode.parent() != null && "".equals(grandParentNode.attr("readability"))) {
                initializeNode(grandParentNode);
                candidates.add(grandParentNode);
            }
            
            double contentScore = 0;

            /* Add a point for the paragraph itself as a base. */
            contentScore++;

            /* Add points for any commas within this paragraph */
            contentScore += innerText.split(",").length;

            /*
             * For every 100 characters in this paragraph, add another point. Up to 3 points.
             */
            contentScore += Math.min(Math.floor(innerText.length() / 100.0), 3.0);

            /* Add the score to the parent. The grandparent gets half. */
            ScoreInfo.updateContentScore(parentNode, contentScore);

            if (grandParentNode != null) {
            	ScoreInfo.updateContentScore(grandParentNode, contentScore / 2.0);
            }
        	
        }
        
        return candidates;

	}
	
    /**
     * After we've calculated scores, loop through all of the possible candidate nodes we found and find
     * the one with the highest score.
     * 
     **/
	
	private Element findTopCandidate(List<Element> candidates) {
        Element topCandidate = null;
        for (Element candidate : candidates) {
            /**
             * Scale the final candidates score based on link density. Good content should have a relatively
             * small link density (5% or less) and be mostly unaffected by this operation.
             **/
        	double score = ScoreInfo.getContentScore(candidate);
            double newScore = score * (1.0 - getLinkDensity(candidate));
            ScoreInfo.setContentScore(candidate, newScore);
            logger.debug("Candidate [" + candidate.getClass() + "] (" + candidate.className() + ":"
                      + candidate.id() + ") with score " + newScore);

            if (null == topCandidate || newScore > ScoreInfo.getContentScore(topCandidate)) {
                topCandidate = candidate;
            }
        }
        
        return topCandidate;
	}
	
    /**
     * Now that we have the top candidate, look through its siblings for content that might also be
     * related. Things like preambles, content split by ads that we removed, etc.
     **/
	
	private Element findSiblingElements(Element topCandidate,Document document) {
		/*Element articleContent = document.createElement("div");
		 if (isPaging) {
	           articleContent.attr("id", "readability-content");
	     }*/
		 
		 double siblingScoreThreshold = Math.max(10, ScoreInfo.getContentScore(topCandidate) * 0.2);
		 List<Element> siblingNodes = topCandidate.siblingElements();
		 int insertedSibling = 0;
		 if(siblingNodes.size() > 0) {
			 for (Element siblingNode : siblingNodes) {
					boolean scored = ScoreInfo.isElementScored(siblingNode);

		            boolean append = false;

		            logger.debug("Looking at sibling node: [" + siblingNode.getClass() + "] (" + siblingNode.className()
		                      + ":" + siblingNode.id() + ")");
		            if (scored) {
		            	logger.debug("Sibling has score " + ScoreInfo.getContentScore(siblingNode));
		            } else {
		            	logger.debug("Sibling has score unknown");
		            }

		            if (siblingNode == topCandidate) {
		                append = true;
		            }
		            double contentBonus = 0;
		            /*
		             * Give a bonus if sibling nodes and top candidates have the example same classname
		             */
		            if (siblingNode.className().equals(topCandidate.className())
		                && !"".equals(topCandidate.className())) {
		            	contentBonus += ScoreInfo.getContentScore(topCandidate) * 0.2;
		            }

		            if (scored && (ScoreInfo.getContentScore(siblingNode) + contentBonus >= siblingScoreThreshold)) {
		                append = true;
		            }

		            if ("p".equals(siblingNode.tagName())) {
		                double linkDensity = getLinkDensity(siblingNode);
		                String nodeContent = siblingNode.text();
		                int nodeLength = nodeContent.length();

		                if (nodeLength > 80 && linkDensity < 0.25) {
		                    append = true;
		                } else if (nodeLength < 80 && linkDensity == 0&& Patterns.exists(Patterns.ENDS_WITH_DOT, nodeContent)) {
		                    append = true;
		                }
		            }
		            if (append) {
		            	logger.debug("Appending node: [" + siblingNode.getClass() + "]");

		                Element nodeToAppend = null;
		                if (!"div".equals(siblingNode.tagName()) && !"p".equals(siblingNode.tagName())) {
		                    /*
		                     * We have a node that isn't a common block level element, like a form or td tag. Turn it
		                     * into a div so it doesn't get filtered out later by accident.
		                     */

		                	logger.debug("Altering siblingNode of " + siblingNode.tagName() + " to div.");
		                    nodeToAppend = changeElementTag(siblingNode, "div",document);
		                } else {
		                    nodeToAppend = siblingNode;
		                }

		                /*
		                 * To ensure a node does not interfere with readability styles, remove its classnames
		                 */
		                nodeToAppend.removeAttr("class");
		                /*
		                 * Append sibling and subtract from our list because it removes the node when you append to
		                 * another node
		                 */
		                /*articleContent.appendChild(nodeToAppend);
		                 * 
		                 *
		            }else {
		            	articleContent.appendChild(topCandidate);
		            }*/
		                
		                if(insertedSibling == 0) {
		                	topCandidate.child(insertedSibling).before(nodeToAppend.html());
		                	insertedSibling ++;
		                }else {
		                	topCandidate.appendElement("p").html(nodeToAppend.html());
		                	
		                }
				}else {
					topCandidate = AddSiblings.addSiblings(topCandidate);
				}
			 
		 }/*else {
			 articleContent.appendChild(topCandidate);*/
		 }
            //return articleContent;
			 
			 return topCandidate;
	}
	
	/**
	 * Change {@link Element} tag in to a newsTag in {@link Document}
	 * 
	 * @param element
	 * @param newTag
	 * @param document
	 * @return
	 */
	
	private Element changeElementTag(Element element, String newTag,Document document) {
	    Element newElement = document.createElement(newTag);
	    /* JSoup gives us the live child list, so we need to make a copy. */
	    List<Node> copyOfChildNodeList = new ArrayList<Node>();
	    copyOfChildNodeList.addAll(element.childNodes());
	    for (Node n : copyOfChildNodeList) {
	        n.remove();
	        newElement.appendChild(n);
	    }
	    element.replaceWith(newElement);
	    return newElement;
	}
	
	/**
	 * Initialize scroring for each {@link Elements}
	 * 
	 * @param node
	 */
    private void initializeNode(Element node) {
        node.attr("readability", "true");
        String tagName = node.tagName();
        if ("div".equals(tagName)) {
        	ScoreInfo.updateContentScore(node, 5);
        } else if ("pre".equals(tagName) || "td".equals(tagName) || "blockquote".equals(tagName)) {
        	ScoreInfo.updateContentScore(node, 3);
        } else if ("address".equals(tagName) || "ol".equals(tagName) || "ul".equals(tagName)
                   || "dl".equals(tagName) || "dd".equals(tagName) || "dt".equals(tagName)
                   || "li".equals(tagName) || "form".equals(tagName)) {
        	ScoreInfo.updateContentScore(node, -3);
        } else if (tagName.matches("h[1-6]") || "th".equals(tagName)) {
        	ScoreInfo.updateContentScore(node, -5);
        }
        ScoreInfo.updateContentScore(node, WeightMethods.getClassWeight(node));
    }
    
    /**
     * Get link density of a {@link Element}
     * 
     * @param element
     * @return
     */
    private double getLinkDensity(Element element) {
        Elements links = element.getElementsByTag("a");
        double textLength = element.text().length();
        double linkLength = 0;
        for (Element link : links) {
            linkLength += link.text().length();
        }

        return linkLength / textLength;
    }
}
