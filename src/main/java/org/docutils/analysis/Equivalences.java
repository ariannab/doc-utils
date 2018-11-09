package org.docutils.analysis;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Equivalences {

    public static String isResultPositive(String comment){
        //TODO maybe a more comprehensive list (e.g. consider an external dictionary) would be better
        List<String> equivalenceKw = Arrays.asList("equivalent", "similar", "analog",
                "prefer", "alternative", "same", "behaves", "like", "as");
        return equivalenceFound(comment, equivalenceKw);
    }

    private static String equivalenceFound(String comment, List<String> keywords) {
        String methodRegex = "((?![0-9])\\w+(\\(\\S*\\)\\.?|\\.\\w+|#\\w+)+)";
        for(String word : keywords){
            if (Pattern.compile("\\b"+word+"\\b", Pattern.CASE_INSENSITIVE).matcher(comment).find()) {
                //I do not only want the comment to contain the keywords, I also want to find a
                //method signature in it - otherwise, what is this method equivalent to?
                final java.util.regex.Matcher methodMatch =
                        Pattern.compile(".* " + methodRegex + " .*").matcher(comment);

                if(methodMatch.matches()) {
                    return methodMatch.group(1);
                }
            }
        }
        return null;
    }
}
