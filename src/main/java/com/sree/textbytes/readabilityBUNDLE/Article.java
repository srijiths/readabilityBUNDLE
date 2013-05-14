package com.sree.textbytes.readabilityBUNDLE;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.sree.textbytes.readabilityBUNDLE.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Original code from Project Goose
 * 
 * modified author : Sree
 * 
 * This class represents the extraction of an Article from a website
 * It also contains all the meta data's extracted from the web page.
 */
public class Article {
	private static final Logger logger = Logger.getLogger(Article.class.getName());
	
	/**
	 * Cleaned document for extraction
	 */
	private Document cleanedDocument;

	/**
	 * Holds the title of the webpage
	 */
	private String title;
	
	/**
	 * publish date
	 */
	private String publishDate;
	
	/**
	 * If any top image from the web page
	 */
	Image topImage;
	
	/**
	 * holds the metadescription meta tag in the html doc
	 */
	private String metaDescription;
	/**
	 * holds the clean text after we do strip out everything but the text and
	 * wrap it up in a nice package this is the guy you probably want, just pure
	 * text
	 */
	private String cleanedArticleText;

	/**
	 * holds the original unmodified HTML retrieved from the URL
	 */
	private String rawHtml;

	/**
	 * holds the meta keywords that would in the meta tag of the html doc
	 */
	private String metaKeywords;

	/**
	 * holds the meta data canonical link that may be place in the meta tags of
	 * the html doc
	 */
	private String canonicalLink;

	/**
	 * this represents the jSoup element that we think is the big content dude
	 * of this page we can use this node to start grabbing text, images, etc..
	 * around the content
	 */
	private Element topNode;

	/**
	 * holds a list of tags extracted from the article
	 */
	private Set<String> tags;
	
	private List<String> nextPageHtmlSources = new ArrayList<String>();

	/**
	 * Set the next page html sources
	 * @param nextPageSources
	 */
	public void setNextPageHtmlSources(List<String> nextPageSources) {
		this.nextPageHtmlSources = nextPageSources;
	}
	
	/**
	 * Get the next page html sources
	 * @return
	 */
	
	public List<String> getNextPageSources() {
		return this.nextPageHtmlSources;
	}


	
	public void setCleanedDocument(Document document) {
		this.cleanedDocument = document;
	}
	
	public Document getCleanedDocument() {
		return cleanedDocument;
	}
	
	/**
	 * Its true of the document has next pages.
	 */
	private boolean isMultiPage = false;
	
	public void setMultiPageStatus(boolean status) {
		this.isMultiPage = status;
	}
	
	public boolean getMultiPageStatus() {
		return isMultiPage;
	}
	
	/**
	 * returns the title of the webpage
	 * 
	 * @return
	 */

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMetaDescription() {
		return metaDescription;
	}

	public void setMetaDescription(String metaDescription) {
		this.metaDescription = metaDescription;
	}

	public String getMetaKeywords() {
		return metaKeywords;
	}

	public void setMetaKeywords(String metaKeywords) {
		this.metaKeywords = metaKeywords;
	}

	public String getCanonicalLink() {
		return canonicalLink;
	}

	public void setCanonicalLink(String canonicalLink) {
		this.canonicalLink = canonicalLink;
	}

	public Element getTopNode() {
		return topNode;
	}

	public void setTopNode(Element topNode) {
		this.topNode = topNode;
	}

	/**
	 * The unique set of tags that matched: "a[rel=tag], a[href*=/tag/]"
	 * 
	 * @return the unique set of TAGs extracted from this {@link Article}
	 */
	public Set<String> getTags() {
		if (tags == null) {
			tags = new HashSet<String>();
		}
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public String getCleanedArticleText() {
		return cleanedArticleText;
	}

	public void setCleanedArticleText(String cleanedArticleText) {
		this.cleanedArticleText = cleanedArticleText;
	}

	public String getRawHtml() {
		return rawHtml;
	}

	public void setRawHtml(String rawHtml) {
		this.rawHtml = rawHtml;
	}

	public void setPublishDate(String date) {
		publishDate = date;
	}
	
	public String getPublishDate() {
		return publishDate;
	}
	
	public Image getTopImage() {
		return topImage;
	}

	public void setTopImage(Image topImage) {
		this.topImage = topImage;
	}
	
}