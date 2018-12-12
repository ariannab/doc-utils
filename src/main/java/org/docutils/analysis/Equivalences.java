package org.docutils.analysis;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Equivalences {

    /**
     * This method answers to the question "does the comment express an equivalence?".
     * Basically a man in the middle
     *
     * @param comment comment to parse
     * @return the signature of the (supposedly) equivalent method
     */
    public static String isResultPositive(String comment) {
        //TODO maybe a more comprehensive list (e.g. consider an external dictionary) would be better
        //TODO consider also: behaves (as?), like
        List<String> equivalenceKw = Arrays.asList("equivalent", "similar", "analog",
                "prefer", "alternative", "same as", "as");
        return equivalenceFound(comment, equivalenceKw);
    }

    /**
     * Parses a comment searching for a) presence of one of the keywords b) a method signature (a. and b. in the
     * same sentence).
     *
     * @param comment the comment to parse
     * @param keywords the keywords to search for
     * @return the signature of the (supposedly) equivalent method
     */
    private static String equivalenceFound(String comment, List<String> keywords) {
        String methodRegex = "\\w+(\\(.*?(?<!\\) )\\)|\\.\\w+|#\\w+)+";
        for (String word : keywords) {
            if (Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE).matcher(comment).find()) {
                //I do not only want the comment to contain the keywords, I also want to find a
                //method signature in it - otherwise, what is this method equivalent to?
                java.util.regex.Matcher methodMatch;
                int group = 0;
                if (word.equals("as")) {
                    methodMatch =
                            Pattern.compile(" as (" + methodRegex+")").matcher(comment);
                    group = 1;
                } else {
                    methodMatch =
                            Pattern.compile(methodRegex).matcher(comment);
                }

                if (methodMatch.find()) {
                    return methodMatch.group(group);
                }
            }
        }
        return null;
    }
}
