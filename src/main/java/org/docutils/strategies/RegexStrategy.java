package org.docutils.strategies;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexStrategy implements ParseStrategy {


    /**
     * Returns whether string matches regex case insensitively.
     *
     * @param string the string
     * @param regex the regex
     * @return whether string matches regex
     */
    private static boolean matchesRegex(String string, String regex) {
        Pattern mypattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher mymatcher = mypattern.matcher(string);
        return mymatcher.matches();
    }
}
