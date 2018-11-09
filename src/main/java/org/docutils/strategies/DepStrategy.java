package org.docutils.strategies;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.TypedDependency;
import org.docutils.util.StanfordParser;

import java.util.List;

public class DepStrategy {

    /**
     * Finds Stanford Parser dependencies in the comment in input.
     *
     * @param comment the comment
     * @return whether a dependencies was found
     */
    public static boolean depFound(String comment, List<String> dependencies) {
//      Dep such as:  “advcl”, “aux”, “auxpass”, “vmod”, and “tmod”
        Annotation document =
                new Annotation(comment);
        List<SemanticGraph> graph = StanfordParser.parse(comment);
        for (SemanticGraph sg : graph) {
            for (TypedDependency dep : sg.typedDependencies()) {
                String dependency = dep.reln().toString();
                if (dependencies.contains(dependency)) {
                    return true;
                }
            }
        }
        return false;
    }
}
