package com.sree.textbytes.readabilityBUNDLE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;

import com.sree.textbytes.readabilityBUNDLE.image.BestImageGuesser;
import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.cleaner.DocumentCleaner;
import com.sree.textbytes.readabilityBUNDLE.extractor.GooseExtractor;
import com.sree.textbytes.readabilityBUNDLE.extractor.ReadabilityExtractor;
import com.sree.textbytes.readabilityBUNDLE.extractor.ReadabilitySnack;
import com.sree.textbytes.readabilityBUNDLE.formatter.DocumentFormatter;
import com.sree.textbytes.StringHelpers.StringSplitter;
import com.sree.textbytes.StringHelpers.string;

/**
 * Original code from Project Goose
 * 
 * modified author sree
 *
 */

public class ContentExtractor {

	public static Logger logger = Logger.getLogger(ContentExtractor.class.getName());

	public Article extractContent(String rawHtml,String extractionAlgorithm,List<String> htmlSources) {
		return performExtraction(htmlSources,extractionAlgorithm,rawHtml);
	}
	
	public Article extractContent(String rawHtml,String extractionAlgorithm) {
		List<String> htmlSources = new ArrayList<String>();
		htmlSources = null;
		return performExtraction(htmlSources,extractionAlgorithm,rawHtml);
	}

	private Article performExtraction(List<String> htmlSources,String extractionAlgorithm,String rawHtml) {
		
		Article article = new Article();
		try {

			if(!string.isNullOrEmpty(extractionAlgorithm)) {
				logger.debug("Extraction algorithm set : "+extractionAlgorithm);
			}
			
			article.setRawHtml(rawHtml);
			
			Document document = null;
			
			ParseWrapper parseWrapper = new ParseWrapper();
			try {
				document = parseWrapper.parse(rawHtml);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		 
			try {
				article.setPublishDate(extractPublishedDate(document));
			}catch(Exception e) {
				logger.warn("Publish Date extraction failed ");
			}
			
			try {
				article.setTags(extractTags(document));
			}catch(Exception e) {
				logger.warn("Extract tags failed");
			}

			try {
				article.setTitle(getTitle(document));
			} catch (Exception e) {
				logger.warn("Article Set title failed ");
			}
			
			try {
				article.setMetaDescription(getMetaDescription(document));
				article.setMetaKeywords(getMetaKeywords(document));
			} catch (Exception e) {
				logger.warn("Meta Key & Des failed to set ");
			}
			
			/**
			 * Find out the possibility of Next Page in the input,
			 *  
			 */
			if(htmlSources != null) {
				if(htmlSources.size() > 0) {
					logger.debug("There are next pages, true with size : "+htmlSources.size());
					article.setMultiPageStatus(true);
					article.setNextPageHtmlSources(htmlSources);
				}
			}
				
			
			//now perform a nice deep cleansing
			DocumentCleaner documentCleaner = new DocumentCleaner();
			document = documentCleaner.clean(document);
			logger.debug("Cleaned Document:" + document.toString());
			
			article.setCleanedDocument(document);
			
			if(!string.isNullOrEmpty(extractionAlgorithm)) {
				if(extractionAlgorithm.equalsIgnoreCase("ReadabilitySnack")) {
					ReadabilitySnack readabilitySnack = new ReadabilitySnack();
					article.setTopNode(readabilitySnack.grabArticle(article));
				}else if(extractionAlgorithm.equalsIgnoreCase("ReadabilityCore")) {
					ReadabilityExtractor readabilityCore = new ReadabilityExtractor();
					article.setTopNode(readabilityCore.grabArticle(article));
				}else if(extractionAlgorithm.equalsIgnoreCase("ReadabilityGoose")) {
					GooseExtractor gooseExtractor = new GooseExtractor();
					article.setTopNode(gooseExtractor.grabArticle(article));
				}
			}else {
				logger.debug("No ALgorithm set by default , proceeding with Readability Snack");
				ReadabilitySnack readabilitySnack = new ReadabilitySnack();
				article.setTopNode(readabilitySnack.grabArticle(article));
			}
			
			
			
			if(article.getTopNode() != null) {
				logger.debug("Extracted content Before CleanUP : "+article.getTopNode());
				
				/**
				 * Check out another Image Extraction algorithm to find out the best image
				 */
				
				try {
					BestImageGuesser bestImageGuesser = new BestImageGuesser();
					bestImageGuesser.filterBadImages(article.getTopNode());

					Elements imgElements = article.getTopNode()
							.getElementsByTag("img");
					ArrayList<String> imageCandidates = new ArrayList<String>();
					for (Element imgElement : imgElements) {
						imageCandidates.add(imgElement.attr("src"));

					}
					logger.debug("Available size of images in top node : "+ imageCandidates.size());
					
					if(imageCandidates.size() > 0) {
						logger.debug("Top node has images " + imageCandidates.size());
					}else {
						
						logger.debug("Top node may miss image, searching");
						article.setTopImage(bestImageGuesser.getTopImage(article.getTopNode(), document));
						logger.debug("BestImage : "	+ article.getTopImage().getImageSrc());
						
						String bestImage = article.getTopImage().getImageSrc();
						if (!string.isNullOrEmpty(bestImage)) { 
							logger.debug("Best image found : " + bestImage);
							if(!imageCandidates.contains(bestImage)) {
								logger.debug("Top node does not contain the same Best Image");
								try {
									logger.debug("Child Node : "+article.getTopNode().children().size());
									if(article.getTopNode().children().size() > 0) {
										logger.debug("Child Nodes greater than Zero "+article.getTopNode().children().size());
										article.getTopNode().child(0).before("<p><img src=" + bestImage + "></p>");
									} else {
										logger.debug("Top node has 0 childs appending after");
										article.getTopNode().append("<p><img src=" + bestImage + "></p>");
									}

								} catch (Exception e) {
									logger.error(e.toString(), e);
								}
								
							}else {
								logger.debug("Top node already has the Best image found");
							}
						}
					}
				} catch (Exception e) {
					logger.warn("Best Image Guesser failed " + e.toString());

				}
				
		        /**
		         * So we have all of the content that we need. Now we clean it up for presentation.
		         **/
				
				DocumentFormatter documentFormatter = new DocumentFormatter();
				Element node = documentFormatter.getFormattedElement(article.getTopNode());
				
				article.setCleanedArticleText(outputNormalization(node.toString()));
				
				/**
				 * check whether the extracted content lenght less than meta
				 * description
				 */
				logger.debug("Meta des lenght : "+ article.getMetaDescription().length()+ "content lenght : "+ article.getTopNode().text().length());
				if (article.getMetaDescription().trim().length() > article.getTopNode().text().length()) {
					logger.debug("Meta Description greater than extrcated content , swapping");
					article.setCleanedArticleText("<div><p>"+ article.getMetaDescription().trim() + "</p></div>");
				}
				
				logger.debug("After clean up : "+node);
				
			}
			
		}catch(Exception e) {
			logger.error("General Exception occured  " + e.toString(), e);
		}
		
		return article;
	}
	
	/**
	 * Convert single Brs in to double brs
	 * @param text
	 * @return
	 */
	
	private String outputNormalization(String text) {
		return text.replaceAll("<br[^>]*>", "<br /><br />");
	}
	
	
	/**
	 * if the article has meta keywords set in the source, use that
	 */
	private String getMetaKeywords(Document doc) {
		return getMetaContent(doc, "meta[name=keywords]");
	}
	
	/**
	 * if the article has meta description set in the source, use that
	 */
	private String getMetaDescription(Document doc) {
		return getMetaContent(doc, "meta[name=description]");
	}
	
	private String getMetaContent(Document doc, String metaName) {
		Elements meta = doc.select(metaName);
		if (meta.size() > 0) {
			String content = meta.first().attr("content");
			return string.isNullOrEmpty(content) ? string.empty : content.trim();
		}
		return string.empty;
	}
	
	/**
	 * attemps to grab titles from the html pages, lots of sites use different
	 * delimiters for titles so we'll try and do our best guess.
	 * 
	 * 
	 * @param doc
	 * @return
	 */
	private String getTitle(Document doc) {
		String title = string.empty;

		try {
			Elements titleElem = doc.getElementsByTag("title");
			if (titleElem == null || titleElem.isEmpty())
				return string.empty;

			String titleText = titleElem.first().text();
			if (string.isNullOrEmpty(titleText))
				return string.empty;

			boolean usedDelimeter = false;

			if (titleText.contains("|")) {
				titleText = doTitleSplits(titleText, Patterns.PIPE_SPLITTER);
				usedDelimeter = true;
			}

			if (!usedDelimeter && titleText.contains("-")) {
				titleText = doTitleSplits(titleText, Patterns.DASH_SPLITTER);
				usedDelimeter = true;
			}
			if (!usedDelimeter && titleText.contains("»")) {
				titleText = doTitleSplits(titleText, Patterns.ARROWS_SPLITTER);
				usedDelimeter = true;
			}

			if (!usedDelimeter && titleText.contains(":")) {
				titleText = doTitleSplits(titleText, Patterns.COLON_SPLITTER);
			}

			// encode unicode charz
			title = StringEscapeUtils.escapeHtml(titleText);
			title = Patterns.MOTLEY_REPLACEMENT.replaceAll(title);

		} catch (NullPointerException e) {
			logger.error(e.toString());
		}
		return title;
	}
	
	/**
	 * based on a delimeter in the title take the longest piece or do some
	 * custom logic based on the site
	 * 
	 * @param title
	 * @param splitter
	 * @return
	 */
	private String doTitleSplits(String title, StringSplitter splitter) {
		int largetTextLen = 0;
		int largeTextIndex = 0;

		String[] titlePieces = splitter.split(title);

		// take the largest split
		for (int i = 0; i < titlePieces.length; i++) {
			String current = titlePieces[i];
			if (current.length() > largetTextLen) {
				largetTextLen = current.length();
				largeTextIndex = i;
			}
		}

		return Patterns.TITLE_REPLACEMENTS.replaceAll(titlePieces[largeTextIndex])
				.trim();
	}
	
	private Set<String> extractTags(Element node) {
		if (node.children().size() == 0)
			return Patterns.NO_STRINGS;
		Elements elements = Selector.select(Patterns.A_REL_TAG_SELECTOR, node);
		if (elements.size() == 0)
			return Patterns.NO_STRINGS;
		Set<String> tags = new HashSet<String>(elements.size());
		for (Element el : elements) {
			String tag = el.text();
			if (!string.isNullOrEmpty(tag))
				tags.add(tag);
		}
		return tags;
	}
	
	private String extractPublishedDate(Document doc) {
		String pubDateRegex = "(DATE|date|pubdate|Date|REVISION_DATE)";
		return doc.select("meta[name~="+pubDateRegex+"]").attr("content");
	}
	
	
	// used for gawker type ajax sites with pound sites
	private String getUrlToCrawl(String urlToCrawl) {
		String finalURL;
		if (urlToCrawl.contains("#!")) {
			finalURL = Patterns.ESCAPED_FRAGMENT_REPLACEMENT.replaceAll(urlToCrawl);
		} else {
			finalURL = urlToCrawl;
		}
		logger.debug("Extraction: " + finalURL);
		return finalURL;
	}
}
