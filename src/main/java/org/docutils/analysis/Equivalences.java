package org.docutils.analysis;

import org.apache.commons.lang3.StringUtils;
import org.docutils.util.KeywordsSet;
import org.docutils.util.MatchInComment;
import org.docutils.util.TextOperations;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Equivalences {

    /**
     * This method answers to the question "does the comment express one or more equivalences?".
     *
     * @param comment comment to parse
     * @return the equivalence match found in comment
     */
    public static MatchInComment getEquivalentOrSimilarMethod(String comment) {
        //TODO maybe a more comprehensive list (e.g. consider an external dictionary) would be better
        //TODO consider also: behaves (as?), like
        KeywordsSet equivalenceKw = new KeywordsSet(Arrays.asList(
                "equivalent", "similar", "analog", "identical", "behaves", "equal to", "redundant", "same", "as", "like"
//                "equivalent", "similar"
        ),
                KeywordsSet.Category.EQUIVALENCE);

        KeywordsSet similarityKw = new KeywordsSet(Arrays.asList(
                "prefer", "alternative", "replacement for"
                 ),
                KeywordsSet.Category.SIMILARITY);

        MatchInComment matchInComment = new MatchInComment();
        String[] sentences = TextOperations.splitInSentences(comment);
        boolean foundMatch = false;
        for (String sentence : sentences) {
            foundMatch = getSignatureInMatchingComment(matchInComment, sentence, equivalenceKw);
            foundMatch = getSignatureInMatchingComment(matchInComment, sentence, similarityKw) || foundMatch;
        }
        if(foundMatch)
         return matchInComment;
        else
            return null;
    }

    /**
     * Parses a sentence searching for a) presence of one of the keywords b) a method signature (a. and b. in the
     * same sentence).
     *
     *
     * @param matchInComment match found in comment to update if needed
     * @param sentence     the sentence to parse
     * @param keywordsSet the keywords to search for
     * @return the equivalence match found in sentence
     */
    private static boolean getSignatureInMatchingComment(MatchInComment matchInComment,
                                                         String sentence, KeywordsSet keywordsSet) {
        String methodRegex = "\\w+(\\(.*?(?<!\\) )\\)|\\.\\w+|#\\w+)+";
        int complexity = 0;
        boolean found = false;

        for (String word : keywordsSet.getKw()) {
            Matcher kwMatcher = Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE).matcher(sentence);
            if (kwMatcher.find()) {
                //complexity++;
                //I do not only want the sentence to contain the keywords, I also want to find a
                //method signature in it - otherwise, what is this method equivalent to?
                java.util.regex.Matcher methodMatch;
                int group = 0;

                if (word.equals("as")) {
                    methodMatch =
                            Pattern.compile(" as (" + methodRegex + ")").matcher(sentence);
                    group = 1;
                } else {
                    methodMatch =
                            Pattern.compile(methodRegex).matcher(sentence);
                }

                while (methodMatch.find()) {
                    complexity += nestedSignatures(methodMatch, methodRegex);
                    if(!doRangesOverlap(kwMatcher, methodMatch)) {
                        //TODO check if there is an "if" or "when" or "except" - more?
                        if (keywordsSet.getCategory().equals(KeywordsSet.Category.SIMILARITY) ||
                                Pattern.compile("\\b" + "if" + "\\b", Pattern.CASE_INSENSITIVE).matcher(sentence).find() ||
                                Pattern.compile("\\b" + "when" + "\\b", Pattern.CASE_INSENSITIVE).matcher(sentence).find() ||
                                Pattern.compile("\\b" + "except" + "\\b", Pattern.CASE_INSENSITIVE).matcher(sentence).find()) {
                            matchInComment.setExactEquivalence(false);
                            matchInComment.setComplexity(++complexity);
                            matchInComment.addSignature(methodMatch.group(group));
                            matchInComment.addSentences(sentence);
                            found = true;
                        } else {
                            matchInComment.setExactEquivalence(true);
                            matchInComment.setComplexity(complexity);
                            matchInComment.addSignature(methodMatch.group(group));
                            matchInComment.addSentences(sentence);
                            found = true;
                        }
                    }
                }

            }
        }

        return found;
    }

    private static int nestedSignatures(Matcher methodMatch, String methodRegex) {
        String method = methodMatch.group(0);
        return StringUtils.countMatches(method, "(");
        /*
        if(parenthesis.charAt(0) == '(') {
            //FIXME sta cosa è pessima va fixata la regex!
            parenthesis = parenthesis.substring(1);
        }
        if(parenthesis!=null) {
            Matcher insideMethod =
                    Pattern.compile(methodRegex).matcher(parenthesis);

            return insideMethod.find();
        }
        return false;
        */


    }

    private static boolean doRangesOverlap(Matcher matcher, Matcher methodMatch) {
        // We do NOT want that (x1 <= y2 && y1 <= x2)
        return matcher.start() <= methodMatch.end() && methodMatch.start() <= matcher.end();
    }
}
