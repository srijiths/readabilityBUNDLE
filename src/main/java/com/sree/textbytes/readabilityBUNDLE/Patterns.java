package com.sree.textbytes.readabilityBUNDLE;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.sree.textbytes.StringHelpers.ReplaceSequence;
import com.sree.textbytes.StringHelpers.StringReplacement;
import com.sree.textbytes.StringHelpers.StringSplitter;
import com.sree.textbytes.StringHelpers.string;

/**
 * 
 * Basic patterns used in the project
 *
 */
public class Patterns {

	public static Pattern DROP_CAP = compilePattern("dropcap|drop_cap|dropc");
    public static Pattern PAGE_NUMBER_LIKE = compilePattern("((_|-)?p[a-z]*|(_|-))[0-9]{1,2}$");
    public static Pattern PAGE_AND_NUMBER = compilePattern("p(a|g|ag)?(e|ing|ination)?(=|/)[0-9]{1,2}");
    public static Pattern PAGE_OR_PAGING = compilePattern("(page|paging)");
    public static Pattern EXTRANEOUS = compilePattern("print|archive|comment|discuss|e[\\-]?mail|share|reply|all|login|sign|single");
    public static Pattern NEXT_LINK = compilePattern("(story-paging|next|weiter|continue|>([^\\|]|$)|»([^\\|]|$))");
                    // Match: next, continue, >, >>, » but not >|, »| as those usually mean last."
    public static Pattern PAGINATION = compilePattern("pag(e|ing|inat)");
    public static Pattern FIRST_OR_LAST = compilePattern("(first|last)");
    public static Pattern NEGATIVE = compilePattern("(combx|comment|com-|contact|foot|footer|footnote|masthead|media|meta|outbrain|promo|related|scroll|shoutbox|sidebar|sponsor|shopping|tags|tool|widget)");
    public static Pattern PREV_LINK = compilePattern("(prev|earl|old|new|<|«)");
    public static Pattern POSITIVE = compilePattern("(article|body|content|entry|hentry|main|page|pagination|post|text|blog|story)");
    public static Pattern REPLACE_BRS = compilePattern("(<br[^>]*>[ \n\r\t]*){2,}");
    public static Pattern BR = compilePattern("<br[^>]*>");

    public static Pattern UNLIKELY_CANDIDATES = compilePattern("combx|comment|community|disqus|extra|foot|header|menu|remark|rss|shoutbox|sidebar|sponsor|ad-break|agegate|pagination|pager|popup|tweet|twitter");
    public static Pattern OK_MAYBE_ITS_A_CANDIDATE = compilePattern("and|article|body|column|main|shadow");
    public static Pattern ENDS_WITH_DOT = Pattern.compile("\\.( |$)");
    public static Pattern DIGIT = Pattern.compile("\\d");
    public static Pattern BAR_DASH = Pattern.compile(" [\\|\\-] ");
    public static Pattern VIDEOS = Pattern.compile("http:\\/\\/(www\\.)?(youtube|vimeo|player\\.vimeo)\\.com");
    
	public static final StringReplacement MOTLEY_REPLACEMENT = StringReplacement.compile("&#65533;", string.empty);
	public static final StringReplacement ESCAPED_FRAGMENT_REPLACEMENT = StringReplacement.compile("#!", "?_escaped_fragment_=");
	public static final ReplaceSequence TITLE_REPLACEMENTS = ReplaceSequence.create("&raquo;").append("»");
	public static final StringSplitter PIPE_SPLITTER = new StringSplitter("\\|");
	public static final StringSplitter DASH_SPLITTER = new StringSplitter(" - ");
	public static final StringSplitter ARROWS_SPLITTER = new StringSplitter("»");
	public static final StringSplitter COLON_SPLITTER = new StringSplitter(":");
	public static final StringSplitter SPACE_SPLITTER = new StringSplitter(" ");

	public static final Set<String> NO_STRINGS = new HashSet<String>(0);
	public static final String A_REL_TAG_SELECTOR = "a[rel=tag], a[href*=/tag/]";
    
    
    //public static Pattern DIV_TO_P_ELEMENTS = compilePattern("<(a|blockquote|dl|div|img|ol|p|pre|table|ul)");
    public static final Set<String> DIV_TO_P_ELEMENTS;
    
    static {
        DIV_TO_P_ELEMENTS = new HashSet<String>();
        DIV_TO_P_ELEMENTS.add("a");
        DIV_TO_P_ELEMENTS.add("blockquote");
        DIV_TO_P_ELEMENTS.add("dl");
        DIV_TO_P_ELEMENTS.add("div");
        DIV_TO_P_ELEMENTS.add("img");
        DIV_TO_P_ELEMENTS.add("ol");
        DIV_TO_P_ELEMENTS.add("p");
        DIV_TO_P_ELEMENTS.add("pre");
        DIV_TO_P_ELEMENTS.add("table");
        DIV_TO_P_ELEMENTS.add("ul");
    }

    private Patterns() {
        //
    }

    public static boolean match(Pattern pattern, String string) {
        return pattern.matcher(string).matches();
    }

    public static boolean exists(Pattern pattern, String string) {
        return pattern.matcher(string).find();
    }

    private static Pattern compilePattern(String patternString) {
        return Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
    }

}
