package org.docutils.strategies;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.TypedDependency;
import org.docutils.util.StanfordParser;

import java.util.List;

public class DepStrategy implements ParseStrategy {


    /**
     * This method finds Stanford Parser dependencies in the comment in input.
     *
     * @param comment the comment
     * @return whether a dependencies was found
     */
    private static boolean depFound(String comment) {
//      Dep such as:  “advcl”, “aux”, “auxpass”, “vmod”, and “tmod”
        Annotation document =
                new Annotation(comment);
        List<SemanticGraph> graph = StanfordParser.parse(comment);
        for(SemanticGraph sg : graph) {
            for (TypedDependency dep : sg.typedDependencies()) {
                String dependency = dep.reln().toString();
                if (dependency.equals("tmod") || dependency.equals("aux")
                        || dependency.equals("advcl") || dependency.equals("auxpass")
                        || dependency.equals("vmod"))
                    return true;

            }
        }
        return false;
    }
}
