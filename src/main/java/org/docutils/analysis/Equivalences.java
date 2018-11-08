package org.docutils.analysis;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Equivalences {

    public boolean equivalenceFound(String comment){
        List<String> equivalenceKw = Arrays.asList("equivalent", "similar", "analog",
                "prefer", "alternative", "same as", "as");
        return foundKeywords(comment, equivalenceKw);
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
}
