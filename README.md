readabilityBUNDLE
=================

Recent days its really a challenging open issue to find out the main article content from html pages. There are many open source algorithms / implementations are available. What i aim in this project is concise some of the best content extraction algorithm implemented in JAVA.

My focus is mainly on the tuning parameters and customization / modifications of these algorithmic features according to my requirements. 

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

The main challenge which i was facing is to extract the main content by keeping all the images / videos / html tags / and some realated div tags which are used as content / non content identification by most of the algorithms.

readabilityBUNDLE borrows much code and concepts from [Project Goose](http://www.gravity.com/labs/goose/) , [Snacktory](https://github.com/karussell/snacktory) and [Java-Readability](https://github.com/basis-technology-corp/Java-readability). My intension was just fine tune / modify the algorithm to work with my requirements.

Some html pages works very well in a particular algorithm and some not. This is the main reason i put all the available algorithm under a roof . You can choose an algorithm which best suits you.
