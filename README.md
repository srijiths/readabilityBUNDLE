readabilityBUNDLE
=================

Main Content Extraction from html written in Java. It will extract the article text with out the around clutters.

Recent days its really a challenging open issue to extract the main article content from html pages. There are many open source algorithms / implementations available. What i aim in this project is concise some of the best content extraction algorithm implemented in JAVA.

My focus is mainly on the tuning parameters and customization / modifications of these algorithmic features according to my requirements.

readabilityBUNDLE will perform equally what other algorithms does plus below listed extras. 

Whats extra in readabilityBUNDLE
================================

* Preserve the html tags in the extracted content.
* Keep all the possible images in the content instead of finding best image.
* Keep all the available videos.
* Better extraction of li,ul,ol tags
* Content normalization of extracted content.
* Incorporated 3 best popular extraction algorithm , you can choose based on your requirement.
* Provision to append next pages extracted content and create a consolidated output
* Many cleaner / formatter measures added.
* Some core changes in algorithms.

The main challenge which i was facing to extract the main content by keeping all the images / videos / html tags / and some realated div tags which are used as content / non content identification by most of the algorithms.

readabilityBUNDLE borrows much code and concepts from [Project Goose](https://github.com/GravityLabs/goose) , [Snacktory](https://github.com/karussell/snacktory) and [Java-Readability](https://github.com/basis-technology-corp/Java-readability). My intension was just fine tune / modify the algorithm to work with my requirements.

Some html pages works very well in a particular algorithm and some not. This is the main reason i put all the available algorithm under a roof . You can choose an algorithm which best suits you.

You can see all author citations in each java file itself.

Dependency Projects
===================
* [StringHelpers](https://github.com/srijiths/StringHelpers)
* [Network](https://github.com/srijiths/Network)
* [NextPageFinder] (https://github.com/srijiths/NextPageFinder)

Usage
=====
You need to say which extraction algorithm to use. The 3 extraction algorithms are ReadabilitySnack,ReadabilityCore and ReadabilityGoose. By default its ReadabilitySnack.

* With out next page finding

Sample Usage

	Article article = new Article();
	ContentExtractor ce = new ContentExtractor();
	HtmlFetcher htmlFetcher = new HtmlFetcher();
	String html = htmlFetcher.getHtml("http://blogmaverick.com/2012/11/19/what-i-really-think-about-facebook/?utm_source=feedburner&utm_medium=feed&utm_campaign=Feed%3A+Counterparties+%28Counterparties%29", 0);

	article = ce.extractContent(html, "ReadabilitySnack");

	System.out.println("Content : "+article.getCleanedArticleText());

* With next page html sources

If you need to extract and append content from next pages also then,

* You can use [NextPageFinder] (https://github.com/srijiths/NextPageFinder) to find out all the next pages links.
* Get the html of each next pages as a List of String using [Network](https://github.com/srijiths/Network)
* Pass it to the content extractor like

	article = ce.extractContent(firstPageHtml,extractionAlgorithm,nextPagesHtmlSources)

Build
=====

Using Maven , mvn clean package

License
=======
Apache License 2 - http://www.apache.org/licenses/LICENSE-2.0.html