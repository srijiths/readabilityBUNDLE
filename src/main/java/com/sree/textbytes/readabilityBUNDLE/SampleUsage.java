package com.sree.textbytes.readabilityBUNDLE;

import com.sree.textbytes.network.HtmlFetcher;


public class SampleUsage {
	public static void main(String[] args) throws Exception {
		Article article = new Article();
		ContentExtractor ce = new ContentExtractor();
		HtmlFetcher htmlFetcher = new HtmlFetcher();
		String html = htmlFetcher.getHtml("http://blogmaverick.com/2012/11/19/what-i-really-think-about-facebook/?utm_source=feedburner&utm_medium=feed&utm_campaign=Feed%3A+Counterparties+%28Counterparties%29", 0);
		
		//System.out.println("Html : "+html);
		article = ce.extractContent(html, "ReadabilitySnack");
		
		System.out.println("Content : "+article.getCleanedArticleText());
		
		
	}

}
