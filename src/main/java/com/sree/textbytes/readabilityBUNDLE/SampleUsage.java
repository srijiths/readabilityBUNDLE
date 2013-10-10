package com.sree.textbytes.readabilityBUNDLE;

import com.sree.textbytes.network.HtmlFetcher;


public class SampleUsage {
	public static void main(String[] args) throws Exception {
		Article article = new Article();
		ContentExtractor ce = new ContentExtractor();
		HtmlFetcher htmlFetcher = new HtmlFetcher();
		String html = htmlFetcher.getHtml("http://www.firstpost.com/tech/htc-eyes-15-indian-smartphone-market-to-open-7000-outlets-1164045.html", 0);
		
		//System.out.println("Html : "+html);
		article = ce.extractContent(html, "ReadabilitySnack");
		
		System.out.println("Content : "+article.getCleanedArticleText());
		
		
	}

}
