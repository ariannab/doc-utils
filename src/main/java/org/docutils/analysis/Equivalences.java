package org.docutils.analysis;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Equivalences {

    public static boolean isResultPositive(String comment){
        //TODO maybe a more comprehensive list (e.g. consider an external dictionary) would be better
        List<String> equivalenceKw = Arrays.asList("equivalent", "similar", "analog",
                "prefer", "alternative", "same as", "as");
        return equivalenceFound(comment, equivalenceKw);
    }


    private static boolean equivalenceFound(String comment, List<String> keywords) {
        String methodRegex = "(\\w+(\\(.*\\)\\.?|\\.\\w+|#\\w+))+";
        for(String word : keywords){
            if (Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE).matcher(comment).find()) {
                //I do not only want the comment to contain the keywords, I also want to find a
                //method signature in it! Otherwise, what is this method equivalent to?
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
}
