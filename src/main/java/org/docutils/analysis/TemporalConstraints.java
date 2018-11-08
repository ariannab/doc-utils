package org.docutils.analysis;

import org.docutils.strategies.DepStrategy;

import java.util.ArrayList;
import java.util.Collections;

public class TemporalConstraints {

    public boolean temporalConstrFound(String comment){
        DepStrategy depStrategy = new DepStrategy();
        ArrayList<String> depList = new ArrayList<String>();
        Collections.addAll(depList, "tmod", "aux", "advcl", "auxpass", "vmod");
        return depStrategy.depFound(comment, depList);
    }
}
