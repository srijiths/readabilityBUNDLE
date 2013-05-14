package com.sree.textbytes.readabilityBUNDLE.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.sree.textbytes.readabilityBUNDLE.Patterns;
import com.sree.textbytes.readabilityBUNDLE.ScoreInfo;
import com.sree.textbytes.readabilityBUNDLE.WeightMethods;
import com.sree.textbytes.readabilityBUNDLE.extractor.ReadabilitySnack;
import com.sree.textbytes.StringHelpers.PatternMatcher;
import com.sree.textbytes.StringHelpers.string;

/**
 * Original code from Project Goose , Bunch of customizations by Sree
 * 
 * modified author : Sree
 * 
 * 
 * Prepare the article node for display. Clean out any inline styles,
 * iframes, forms, strip extraneous <p> tags, etc.
 *
 * @param Element
 * @return Element
 **/

public class DocumentFormatter {
	
	public static Logger logger = Logger.getLogger(DocumentFormatter.class.getName()) ;
	
	private static final String regExRemoveNodes;
	private static final String queryNaughtyIDs;
	private static final String queryNaughtyClasses;
	private static final String queryNaughtyNames;
	private static final String regExjunkNotes1;
	private static final String regExjunkNotes2;
	private static List<Integer> lsJunkInfo =new ArrayList<Integer>();
	private static List<String> lsJunkNotes =new ArrayList<String>();
	private List<Integer> htmlHashes = new ArrayList<Integer>();
	static {
		StringBuilder sb = new StringBuilder();
		sb.append("^(small|articleVersion|taglist|memo|insetFullBox|latest_blogs|list-wrapper|comments|block-ec_blogs-ec_blogs_block_bloginfo|twitter-tweet|tpcdiv)$|^ec-messages messages-warning message-region");
		sb.append("|^(headlines assetContainer|mpsharehighlight|glInArticle|block|fourwide ie_leftborder|chrome|jitCmtySignInDialog|jitMktwSignInDialog|jitPremiumsSignInDialog)$");
		sb.append("|^(story_social_toolbar_bottom|sixwide TopBorder|hslice|mobileapps_prom|addtoany_share_save_container)$");
		sb.append("|^(post-meta-data|breadcrumb|nr_fo_bot_of_post|cnbc_share|ibnWdt300|PL10|PT10 b_14)$");
		sb.append("|(comment-\\d+)");
		regExRemoveNodes = sb.toString();
		
		lsJunkInfo.add("(For updates you can share with your friends, follow IBNLive on Facebook, Twitter, Google+ and Pinterest)".hashCode());
		lsJunkInfo.add("RSS feed for comments on this post.".hashCode());
		StringBuilder junkNotes1 =new StringBuilder();
		junkNotes1.append("(?i)(Additional\\s+)?(?i)(Reporting|Writing|Polling|Editing)");
		regExjunkNotes1 = junkNotes1.toString();
	
		StringBuilder junkNotes2 =new StringBuilder();
		junkNotes2.append("(Write to)");
		junkNotes2.append("For more information about");
		regExjunkNotes2 = junkNotes2.toString();
		
		queryNaughtyIDs = "[id~=(" + regExRemoveNodes + ")]";
		queryNaughtyClasses = "[class~=(" + regExRemoveNodes + ")]";
		queryNaughtyNames = "[name~=(" + regExRemoveNodes + ")]";
		
	}
	
	public Element getFormattedElement(Element articleContent) {
		articleContent = cleanupNode(articleContent);
		articleContent = prepareArticle(articleContent);
		articleContent = convertDoubleBrsToP(articleContent);
		articleContent = cleanKnownBadTags(articleContent);
		articleContent = cleanJunks(articleContent);
		articleContent = cleanJunkNotes(articleContent);
		articleContent = removeDuplicateElement(articleContent);
		articleContent = removeBreaksInsideParagraph(articleContent);
		articleContent = normalizeContent(articleContent);
		return articleContent;
	}
	
	private Element cleanJunkNotes(Element node)
	{
		String matchNote = null;
		String regex ="("+regExjunkNotes2+".*?)$|.*?(\\("+regExjunkNotes1+".*?\\)).*?$";
		Element lastElt=node.children().last();
		if(lastElt==null) return node;
		String str=null;
		matchNote =getMatches(regex, lastElt.text(), 1);
		if(matchNote==null)
			matchNote =getMatches(regex, lastElt.text(), 3);
	    if(matchNote!=null) {
	     str=lastElt.text().replace(matchNote, "");
	     if(string.isNullOrEmpty(str))
	    	 removeNode(lastElt);
	     else
	     {
		 lastElt.empty();
		 lastElt.appendText(str);
	     }
		 }
    	return node;
	}
	
	private String getMatches(String regex,String text,int group)
	{
		return PatternMatcher.getMatch(regex, text, group);
	}
	
	private Element cleanJunks(Element node)
	{
		Elements pTag = node.select("p");
		for(Element elt : pTag){
			
			
		   if(!string.isNullOrEmpty(elt.text()) && lsJunkInfo.contains(elt.text().hashCode()))	{
			    removeNode(elt);
			}
		}
	 return node;
	}
	
	/**
	 * Convert double br's in to p tag
	 * 
	 * @param docToClean
	 * @return
	 */
	
	private Element convertDoubleBrsToP(Element node) {
		 Elements doubleBrs = node.select("br + br");
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
	            inner = Patterns.REPLACE_BRS.matcher(inner).replaceAll(" ");
	            parent.html(inner);
	        }
	        return node;
	}

	
	/**
	 * Clean known bad tags - source specific
	 * 
	 * @param node
	 * @returnappend
	 */
	private Element cleanKnownBadTags(Element node) {
		Elements naughtyIDs = node.select(queryNaughtyIDs);
		logger.debug("Found "+naughtyIDs.size() + " naughty IDs in the top node ");
		for(Element naughtyElement : naughtyIDs) {
			removeNode(naughtyElement);
		}
		
		Elements naughtyClass = node.select(queryNaughtyClasses);
		logger.debug("Found "+naughtyClass.size() + " naughty classes in the top node ");
		for(Element naughtyElement : naughtyClass) {
			removeNode(naughtyElement);
		}
		
		Elements naughtyNames = node.select(queryNaughtyNames);
		logger.debug("Found "+naughtyNames.size() + " naughty names in the top node ");
		for(Element naughtyElement : naughtyNames) {
			removeNode(naughtyElement);
			
		}
		
		return node;
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
	 * remove any divs that looks like non-content, clusters of links, or paras
	 * with no gusto
	 * 
	 * @param node
	 * @return
	 */
	private Element cleanupNode(Element node) {
		logger.debug("Starting clean up Node");
		Elements nodes = node.children();
	    String dropCapText = null;
	    boolean isDropCap = false;

		for(Element e : nodes) {
			if(e.tagName().equals("p")) {
				continue;
			}
			

			logger.debug("CLEANUP  NODE: " + e.id() + " class: " + e.attr("class") + "Node : "+e);
		      // now check for word density
		      // grab all the paragraphs in the children and remove ones that are too small to matter
		      Elements subParagraphs = e.getElementsByTag("p");
		      for (Element p : subParagraphs) {
		        if (p.text().length() < 15) {
		        	Elements imgElements = p.getElementsByTag("img");
		        	Elements iframeElements = p.getElementsByTag("iframe");
		        	Elements objectElements = p.getElementsByTag("object");
		        	if(imgElements.size() > 0) {
		        		continue;
		        	} if (iframeElements.size() > 0 || objectElements.size() > 0){
		        		for(Element iframe : iframeElements) {
		        			if(iframe.tagName().equals("iframe")) {
								String attributes = getIFrameAttributes(iframe);
								logger.debug("IFRAME attributes : "+attributes);
								if(Patterns.exists(Patterns.VIDEOS, attributes)) {
									logger.debug("valid video in iframe"+iframe);
									continue;
								}else {
				        			logger.debug("Removing IFRAME , not a valid video");
				        			iframe.remove();
				        		}
		        			}else if(iframe.tagName().equals("object")) {
		        				Elements embedElements = iframe.getElementsByTag("embed");
		        				for(Element embedElement : embedElements) {
		        					String srcAttribute = embedElement.attr("src");
		        					logger.debug("Object Attribute : "+srcAttribute);
		        					if(Patterns.exists(Patterns.VIDEOS, srcAttribute)) {
		        						logger.debug("Valid video found in Object embed");
		        						continue;
		        					}else {
		        						logger.debug("Removing Object , not a valid video");
		        						iframe.remove();
		        					}
		        				}
		        			}
		        		}
		        		
		        	}else {
		        		logger.debug("Clean Up text less than critical and not images"+p);
		        		p.remove();
		        	}
		          
		        }
		      }
		      
		      //if this node has a decent enough gravityScore we should keep it as well, might be content
				
			  if(e.tagName().equals("ul") || e.tagName().equals("ol")) {
				  logger.debug("Tag is ul or ol, skipping");
				  continue;
			  }else if(e.tagName().equals("div") || e.tagName().equals("a")) {
				  
				  Elements tableElements = e.getElementsByTag("table");
				  
				  double topNodeScore = ScoreInfo.getContentScore(node);
			      double currentNodeScore = ScoreInfo.getContentScore(e);
			      
			      
			      Elements imgElements = e.getElementsByTag("img");
			      Elements iframeElements = e.getElementsByTag("iframe");
			      logger.debug("Image size : "+imgElements.size());
			      float thresholdScore = (float) (topNodeScore * .08);
			      logger.debug("topNodeScore: " + topNodeScore + " currentNodeScore: " + currentNodeScore + " threshold: " + thresholdScore);
			      if (currentNodeScore < thresholdScore && imgElements.size() == 0 && iframeElements.size() == 0) {
			        if (!e.tagName().equals("td") && !e.tagName().equals("table") && tableElements.size() == 0) {
			            logger.debug("Removing node due to low threshold score : node "+e);
			            e.remove();
			        } else {
			            logger.debug("Not removing TD node");
			        }

			        continue;
			      }
				  
			  }else {
				  
			      String className = e.attr("class");
			      isDropCap = Patterns.exists(Patterns.DROP_CAP, className);
			      if(isDropCap) {
			    	  dropCapText = e.html();
			    	  logger.debug("DropCap Text :"+dropCapText);
			      }
			      
			      logger.debug("Drop Cap is : "+isDropCap);

				  
			  }
		      
		}
		
		if(dropCapText != null) {
			for(Element e : node.children()) {
				String className = e.attr("class");
				if(Patterns.exists(Patterns.DROP_CAP, className)) {
					logger.debug("Drop Cap Text element : "+e);
					String html = dropCapText + e.nextElementSibling().html();
					e.nextElementSibling().html(html);
					e.remove();
					break;
				}
				
			}
		}

		return node;

	}
	
	/**
	 * Get the IFRAME attributes to check its a valid video / not
	 * 
	 * @param iframe
	 * @return
	 */
	
	private String getIFrameAttributes(Element iframe) {
		Attributes iframeAttributes = iframe.attributes();
		String attributes = "";
		for(Attribute attrib : iframeAttributes) {
			if(!string.isNullOrEmpty(attrib.getValue())) 
				attributes += attrib.getValue() + "|";
		}
	return attributes.substring(0, attributes.lastIndexOf("|"));
}
	
    /**
     * Prepare the article node for display. Clean out any inline styles, iframes, forms, strip extraneous
     * <p>
     * tags, etc. This takes an element in, but returns a string.
     * 
     * @param Element
     * @return void
     **/
	
	private Element prepareArticle(Element articleContent) {
		cleanStyles(articleContent);
		
		cleanConditionally(articleContent, "form");
        clean(articleContent, "object");
        clean(articleContent, "h1");
      
        /**
         * If there is only one h2, they are probably using it as a header and not a subheader, so remove it
         * since we already have a header.
         ***/
        if (articleContent.getElementsByTag("h2").size() == 1) {
            clean(articleContent, "h2");
        }
        clean(articleContent, "iframe");
        
        /*
         * Do these last as the previous stuff may have removed junk that will affect these
         */
        cleanConditionally(articleContent, "table");
        cleanConditionally(articleContent, "ul");
        cleanConditionally(articleContent, "div");
        
       
        
        removeExtraParas(articleContent);
        removeParasWithPreceedingBreak(articleContent);
		
		return articleContent;
	}

	/**
	 * we want to clear off the style attributes in case they influence something else.
	 * 
	 * @param articleContent
	 */
    private Element cleanStyles(Element articleContent) {
        for (Element e : articleContent.getAllElements()) {
            e.removeAttr("style");
        }
        return articleContent;
    }
    
	/**
	 * Clean an element of all tags of type "tag" if they look fishy. "Fishy" is
	 * an algorithm based on content length, classnames, link density, number of
	 * images & embeds, etc.
	 * 
	 * @return void
	 **/

	private Element cleanConditionally(Element element, String tag) {
		Elements tagList = element.getElementsByTag(tag);
		ReadabilitySnack readability = new ReadabilitySnack();
		for (Element tagElement : tagList) {
			//double weight = WeightMethods.getClassWeight(tagElement);
			double weight = readability.getElementScore(tagElement);
			double contentScore = ScoreInfo.getContentScore(tagElement);
			if ((weight + contentScore) < 0) {
				logger.debug("Cleaning tag wiight < 0 weight : " + weight + " content score : " + contentScore + "  tag "+ tagElement);
				if(tagElement.tagName().equals("ul") || tagElement.tagName().equals("ol")) {
					double linkDensity = WeightMethods.getLinkDensity(tagElement);
					logger.debug("Link density of ul node : "+linkDensity);
					if(linkDensity > 0.3) {
						logger.debug("Removing list node due to high link density");
						tagElement.remove();
					}
				}else {
					tagElement.remove();
				}
				
			} else if (WeightMethods.getCharCount(tagElement, ',') < 10) {
				/**
				 * If there are not very many commas, and the number of
				 * non-paragraph elements is more than paragraphs or other
				 * ominous signs, remove the element.
				 **/
				int p = tagElement.select("p").size();
				int img = tagElement.select("img").size();
				int li = tagElement.select("li").size() - 100;
				int input = tagElement.select("input").size();

				double linkDensity = WeightMethods.getLinkDensity(tagElement);
				int contentLength = tagElement.text().length();
				boolean toRemove = false;

				if (img > p)
					toRemove = true;
				else if (li > p && !tag.equals("ul") && !tag.equals("ol"))
					toRemove = true;
				else if (input > Math.floor(p / 3))
					toRemove = true;
				else if (contentLength < 25 && (img == 0 || img > 2))
					toRemove = true;
				else if (weight < 25 && linkDensity > 0.2)
					toRemove = true;
				else if (weight >= 25 && linkDensity > 0.5)
					toRemove = true;
				else
					toRemove = false;
				if (toRemove) {
					logger.debug("Removing node toRemove is true : " + tagElement);
					int imageSize = tagElement.select("img").size();
					if(imageSize > 0) {
						logger.debug("Not removing since it contains some images");
					}else {
						tagElement.remove();
					}
					
				}

			}

		}

		return element;
	}
	
	
    /**
     * Clean a node of all elements of type "tag".
     * 
     * @param Element
     * @param string tag to clean
     **/
    private Element clean(Element element, String tag) {
        Elements targetList = element.getElementsByTag(tag);
        if(tag.equals("iframe")) {
            for(Element target : targetList) {
            	String attributes = getIFrameAttributes(target);
            	Pattern pattern = Pattern.compile("http:\\/\\/(www\\.)?(youtube|vimeo)\\.com");
				Matcher matcher = pattern.matcher(attributes);
				if(matcher.find()) {
					logger.debug("Ifarme match found");
					continue;
				}else {
					target.remove();
				}
            }
        }
        else if (tag.equals("h1"))
        {
            if(targetList.size()>0)
        	{
        	 targetList.get(0).remove();
             }
        	
        }
        
        else {
        	targetList.remove();
        }
        return element;
    }
    
    /**
     * Clean out spurious headers from an Element. Checks things like classnames and link density.
     * 
     * @param Element
     * @return void
     **/
    private void cleanHeaders(Element element) {
        for (int headerIndex = 1; headerIndex < 3; headerIndex++) {
            Elements headers = element.getElementsByTag("h" + headerIndex);
            for (int i = headers.size() - 1; i >= 0; i--) {
                if (WeightMethods.getClassWeight(headers.get(i)) < 0 || WeightMethods.getLinkDensity(headers.get(i)) > 0.33) {
                    headers.get(i).remove();
                }
            }
        }
    }
    
    /**
     * Removing any extra paragraphs which dont have a decent content
     * 
     * @param articleContent
     */
    
    private Element removeExtraParas(Element articleContent) {
        /* Remove extra paragraphs */
        Elements articleParagraphs = articleContent.getElementsByTag("p");
        for (Element para : articleParagraphs) {
            int imgCount = para.getElementsByTag("img").size();
            int embedCount = para.getElementsByTag("embed").size();
            int objectCount = para.getElementsByTag("object").size();
            int iframeCount = para.getElementsByTag("iframe").size();

            if (iframeCount == 0 && imgCount == 0 && embedCount == 0 && objectCount == 0 && para.text().matches("\\s*")) {
                para.remove();
            }
        }
        
        return articleContent;
    }
    
    private Element removeParasWithPreceedingBreak(Element articleContent) {
    	 Elements parasWithPreceedingBreaks = articleContent.getElementsByTag("br + p");
         for (Element pe : parasWithPreceedingBreaks) {
             Element brElement = pe.previousElementSibling();
             brElement.remove();
         }
         
         return articleContent;
    }
    
    /**
     * Anti-duplicate mechanism on extracted output
     * 
     * @param articleContent
     * @return
     */
    private Element removeDuplicateElement(Element articleContent) {
    	Elements elements = articleContent.getAllElements();
    	if(elements.size() > 0) {
    		for(Element element : elements) {
    			if(element.hasText()) {
        			if(checkHtmlHashDuplicates(element.hashCode())) {
        				logger.debug("Duplicate Html hash tag found in extracted output");
        				removeNode(element);
        			}else 
        				htmlHashes.add(element.html().hashCode());
    			}
    		}
    	}
    	
    	return articleContent;
    }
    

    /**
     * Check duplciate html hashes
     * @param htmlHash
     * @return
     */
    private boolean checkHtmlHashDuplicates(int htmlHash) {
    	if(htmlHashes.contains(htmlHash))
    		return true;
    	else
    		return false;
    }
    
    private Element normalizeContent(Element node)
    {      		
    		Elements nodes = node.children();
    	    String text=null;
    		for(Element e : nodes) {
    			if(e.tagName().equals("ul") || e.tagName().equals("ol"))
    			{
    				Elements nodeChildren = e.children();
    				for(Element eNode : nodeChildren) {
    		           text = eNode.html();
    		           if(!checkForSentenceSplit(text))
    		           {
    			         text = text + ".";
    		     	     eNode.html(text);
    		           }
    			 	}
    			 }
    		}
    		
    	return node;
    }
    
    /**
     * Check for sentence split at the end of the sentence
     * @param text
     * @return
     */
    private boolean checkForSentenceSplit(String text) {
    	if(text.trim().endsWith("."))
    		return true;
    	else
    		return false;
    }
    
    /**
     * Remove breaks inside the paragraph tag , possible misleading break for sentence detector
     * 
     * @param element
     * @return
     */
    private Element removeBreaksInsideParagraph(Element element) {
    	Elements paraElements = element.getElementsByTag("p");
    	for(Element paraElement : paraElements) {
    		String innerHtml = paraElement.html();
    		innerHtml = Patterns.BR.matcher(innerHtml).replaceAll(" ");
    		paraElement.html(innerHtml);
    	}
    	
    	return element;
    }
    
    }
