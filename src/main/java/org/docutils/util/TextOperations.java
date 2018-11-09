package org.docutils.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextOperations {

    /**
     * Filter out empty free texts and free texts pointing at inheritDoc.
     *
     * @param freeText the freeText to examine
     * @return true if one of the comment must be filtered out
     */
    public static boolean freeTextToFilter(String freeText) {
        String noBlankFreeText = freeText.trim().replaceAll("\n ", "");
        return noBlankFreeText.isEmpty() || noBlankFreeText.equals("{@inheritDoc}");
    }

    public static String cleanTags(String text) {
        text = text.replaceAll("\\s+", " ");

        final String codePattern1 = "<code>([A-Za-z0-9_]+)</code>";
        text = removeTags(codePattern1, text);

        final String codePattern2 = "\\{@code ([^}]+)\\}";
        text = removeTags(codePattern2, text);

        text = removeTags("\\{@link #?([^}]+)\\}", text);
        text = removeHTMLTags(text);
        text = decodeHTML(text);
        return text.trim();
    }


    /**
     * Removes HTML tags from the comment text.
     */
    private static String removeHTMLTags(String text) {
        String htmlTagPattern = "<([a-zA-Z][a-zA-Z0-9]*)\\b[^>]*>(.*?)</\\1>|(<(.*)/>)|<p>";
        Matcher matcher = Pattern.compile(htmlTagPattern).matcher(text);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                text = text.replace(matcher.group(0), matcher.group(2));
            } else {
                // Match contains self-closing tag
                text = text.replace(matcher.group(0), "");
            }
        }
        return text;
    }

    /**
     * Removes Javadoc inline tags from the comment text preserving the content of the tags.
     *
     * @param pattern a regular expression
     */
    private static String removeTags(String pattern, String text) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        while (matcher.find()) {
            text = text.replace(matcher.group(0), matcher.group(1));
        }
        return text;
    }


    /**
     * Decodes HTML character entities found in comment text with corresponding characters.
     */
    private static String decodeHTML(String text) {
        return text
                .replaceAll("&ge;", ">=")
                .replaceAll("&le;", "<=")
                .replaceAll("&gt;", ">")
                .replaceAll("&lt;", "<")
                .replaceAll("&amp;", "&");
    }

    /**
     * Native splitting of a paragraph into sentences (might be good enough).
     *
     * @param comment is the paragraph to split
     * @return the sentences
     */
    public static String[] splitInSentences(String comment) {
        return comment.split("\\. ");
    }
}
