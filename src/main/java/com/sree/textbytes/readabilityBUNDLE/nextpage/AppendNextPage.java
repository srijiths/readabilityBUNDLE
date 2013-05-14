package com.sree.textbytes.readabilityBUNDLE.nextpage;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.ParseWrapper;
import com.sree.textbytes.readabilityBUNDLE.cleaner.DocumentCleaner;
import com.sree.textbytes.readabilityBUNDLE.extractor.GooseExtractor;
import com.sree.textbytes.readabilityBUNDLE.extractor.ReadabilityExtractor;
import com.sree.textbytes.readabilityBUNDLE.extractor.ReadabilitySnack;
import com.sree.textbytes.readabilityBUNDLE.formatter.DocumentFormatter;

/**
 * Append next page extracted content and create a final consolidated
 * 
 * @author sree
 *
 */

public class AppendNextPage {
	
	public Logger logger = Logger.getLogger(AppendNextPage.class.getName());
	
	public List<Integer> contentHashes = new ArrayList<Integer>();
	
	/**
	 * Append next page content 
	 * 
	 * @param article
	 * @param firstPageContent
	 * @param extractionAlgo
	 * @return
	 */
	public Element appendNextPageContent(Article article,Element firstPageContent,String extractionAlgo) {
		int pageNumber = 1;
		
		DocumentFormatter documentFormatter = new DocumentFormatter();
		
		contentHashes.add(firstPageContent.text().hashCode());
		Document document = article.getCleanedDocument();
		document.body().empty();
		
		Element finalConsolidatedContent = document.createElement("div").attr("id", "ace-final-consolidated");
		Element articleContent = document.createElement("div").attr("algo-page-number", Integer.toString(pageNumber)).attr("class", "algo-page-class");
		articleContent.appendChild(documentFormatter.getFormattedElement(firstPageContent));
		
		finalConsolidatedContent.appendChild(articleContent);
		
		ParseWrapper parseWrapper = new ParseWrapper();
		DocumentCleaner documentClearner = new DocumentCleaner();
		
		if(article.getMultiPageStatus()) {
			List<String> nextPageHtmlSource = new ArrayList<String>();
			nextPageHtmlSource = article.getNextPageSources();
			logger.debug("MultiPagesInfo size : "+nextPageHtmlSource.toString());
			
			for(String nextPageHtml : nextPageHtmlSource) {
				logger.debug("Fetching article from next page : ");
				Element nextPageExtractedContent = null;
				Document nextPageDocument = null;
				try {
					nextPageDocument = parseWrapper.parse(nextPageHtml);
					nextPageDocument = documentClearner.clean(nextPageDocument);
				}catch(Exception e) {
					logger.warn("JSOUP PARSE EXCEPTION ",e);
				}
				
				if(extractionAlgo.equalsIgnoreCase("ReadabilitySnack")) {
					ReadabilitySnack readabilitySnack = new ReadabilitySnack();
					nextPageExtractedContent = readabilitySnack.fetchArticleContent(nextPageDocument);

				}else if(extractionAlgo.equalsIgnoreCase("ReadabilityCore")) {
					ReadabilityExtractor readabilityCore = new ReadabilityExtractor();
					nextPageExtractedContent = readabilityCore.fetchArticleContent(nextPageDocument);

				}else if(extractionAlgo.equalsIgnoreCase("ReadabilityGoose")) {
					GooseExtractor readabilityGoose = new GooseExtractor();
					nextPageExtractedContent = readabilityGoose.fetchArticleContent(nextPageDocument);
				}
				
				if(nextPageExtractedContent != null) {
					if(checkDuplicateNextPage(nextPageExtractedContent.text().hashCode())) {
						logger.debug("Duplicate next page content found , skipping");
					}else {
						
						contentHashes.add(nextPageExtractedContent.text().hashCode());
						Element nextPageContent = document.createElement("div").attr("algo-page-number", Integer.toString(pageNumber)).attr("class", "algo-page-class");
						nextPageContent.appendChild(documentFormatter.getFormattedElement(nextPageExtractedContent));
						logger.debug("Next Page Content : "+nextPageExtractedContent);
						if(!checkParagraphDeDupe(finalConsolidatedContent,nextPageContent))
						{
						finalConsolidatedContent.appendChild(nextPageContent);
						pageNumber++;
						}
					}
				}
			}
		}
		
		return finalConsolidatedContent;
	}
	
	
	/**
	 * Paragraph duplicate mechanism. Check whether next page extracted content is duplicate of existing.
	 * 
	 * @param finalConsolidatedContent
	 * @param nextPageContent
	 * @return
	 */
	
	private boolean checkParagraphDeDupe(Element finalConsolidatedContent,Element nextPageContent)
    {

		int pSize = totalTags(nextPageContent);
		if(pSize==0)
		{
		 return true;
		}
		int i = 0, finalPSize = 0;
		Element firstPara = nextPageContent.getElementsByTag("p").get(i);
		if (firstPara.toString().length() < 100) {
			if (pSize > 1) {
				i = 1;
				firstPara = nextPageContent.getElementsByTag("p").get(i);
			}
		}
		Elements finalElements = finalConsolidatedContent
				.getElementsByAttribute("algo-page-number");
		for (Element elt : finalElements) {
			finalPSize = totalTags(elt);
			if (finalPSize > i) {
				Element firstPtag = elt.getElementsByTag("p").get(i);
				if (firstPara.toString().equals(firstPtag.toString())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private int totalTags(Element element)
	{
		return element.getElementsByTag("p").size();
	}
	
	/**
	 * De dupe mechanism using content hash
	 * 
	 * @param contentHash
	 * @return
	 */
	private boolean checkDuplicateNextPage(int contentHash) {
		if(contentHashes.contains(contentHash)) {
			return true;
		}else 
			return false;
	}
}
