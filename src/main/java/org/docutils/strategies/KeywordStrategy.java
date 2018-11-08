package org.docutils.strategies;

import org.docutils.extractor.DocumentedExecutable;
import org.docutils.extractor.DocumentedType;
import org.docutils.extractor.JavadocExtractor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeywordStrategy implements ParseStrategy {

    /**
     * Search for keywords and stores them.
     *
     * @param writer             {@code FileWriter} where to store the clones
     * @param javadocExtractor   the {@code JavadocExtractor} that extracts the Javadocs
     * @param sourcesFolder      folder containing the Java sources to analyze
     * @param selectedClassNames fully qualified names of the Java classes to be analyzed
     * @throws IOException            if there are problems with the file
     */
    private static void searchKeywords(FileWriter writer, JavadocExtractor javadocExtractor, String sourcesFolder,
                                       String[] selectedClassNames) throws IOException {
        for (String className : selectedClassNames) {
            DocumentedType documentedType = javadocExtractor.extract(
                    className, sourcesFolder);

            if (documentedType != null) {
                List<DocumentedExecutable> executables = documentedType.getDocumentedExecutables();
                for (int i = 0; i < executables.size(); i++) {
                    DocumentedExecutable first = executables.get(i);
                    if (!freeTextToFilter(first.getJavadocFreeText())) {
                        String cleanFirst = cleanTags(first.getJavadocFreeText());
//                            String regex =
//                                    ".*(calling )?(this method|it) (is|behaves)( \\w+)? equivalent(ly)? to .*";
                        List<String> keywords = Arrays.asList("equivalent", "similar", "analog",
                                "prefer", "alternative", "same as", "as");
                        if (foundKeywords(cleanFirst, keywords)) {
                            writer.append(className);
                            writer.append(';');
                            writer.append(first.getSignature());
                            writer.append(';');
                            writer.append("Free text");
                            writer.append(';');
                            writer.append(cleanFirst.replaceAll(";", ","));
                            writer.append("\n");
                        }
                    }
                }
            }
        }
    }


    private static boolean foundKeywords(String comment, List<String> keywords) {
        String methodRegex = "(\\w+(\\(.*\\)\\.?|\\.\\w+|#\\w+))+";
        for(String word : keywords){
            if (Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE).matcher(comment).find()) {
                if (word.equals("as")){
                    if(comment.matches(".* as " + methodRegex + " .*")){
                        return true;
                    }
                } else if (comment.matches(".* " + methodRegex + " .*")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Filter out empty free texts and free texts pointing at inheritDoc.
     *
     * @param freeText the freeText to examine
     * @return true if one of the comment must be filtered out
     */
    private static boolean freeTextToFilter(String freeText) {
        String noBlankFreeText = freeText.trim().replaceAll("\n ", "");
        return noBlankFreeText.isEmpty() || noBlankFreeText.equals("{@inheritDoc}");
    }


    private static String cleanTags(String text) {
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


    /** Removes HTML tags from the comment text. */
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


    /** Decodes HTML character entities found in comment text with corresponding characters. */
    private static String decodeHTML(String text) {
        return text
                .replaceAll("&ge;", ">=")
                .replaceAll("&le;", "<=")
                .replaceAll("&gt;", ">")
                .replaceAll("&lt;", "<")
                .replaceAll("&amp;", "&");
    }
}
