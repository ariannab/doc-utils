package org.docutils.util;

import com.beust.jcommander.Parameter;

public class Args {

    public enum ANALYSIS {
        TEMPORAL, DETAILED_EQUIVALENCE, SIMPLE_SENTENCE_EQ, EVERYTHING
    }

    public enum STRATEGY {
        DEP, KEYWORD, REGEX
    }

    @Parameter(names = "--analysis", hidden = true)
    private ANALYSIS analysis = null;

    @Parameter(names = "--strategy")
    private STRATEGY strategy = null;

    public ANALYSIS getAnalysis() {
        return analysis;
    }

    public STRATEGY getStrategy() {
        return strategy;
    }
}
