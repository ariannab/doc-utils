package org.docutils.util;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a method to get the semantic graph of a sentence produced by the Stanford
 * parser. To optimize execution time, the Stanford parser is initialized once in the static block
 * to ensure that its initialization phase is done only once.
 */
public class StanfordParser {

  private static final LexicalizedParser LEXICALIZED_PARSER;
  private static final GrammaticalStructureFactory GSF;
  private static final Logger log = LoggerFactory.getLogger(StanfordParser.class);

  static {
    LEXICALIZED_PARSER = LexicalizedParser.loadModel();
    // tlp is the PennTreebankLanguagePack for English.
    TreebankLanguagePack tlp = LEXICALIZED_PARSER.treebankLanguagePack();
    if (!tlp.supportsGrammaticalStructures()) {
      throw new RuntimeException(
          "Error in the Stanford Parser configuration. Are models available?");
    }
    GSF = tlp.grammaticalStructureFactory();
  }

  static List<List<HasWord>> tokenize(String comment) {
    final DocumentPreprocessor sentences = new DocumentPreprocessor(new StringReader(comment));
    ArrayList<List<HasWord>> result = new ArrayList<>();
    sentences.forEach(result::add);
    return result;
  }

  /**
   * Parses the given {@code words} producing a SemanticGraph. Before asking the Stanford Parser to
   * produce the semantic graph, this method (POS-)tags code elements as NN and inequalities
   * placeholders as JJ.
   *
   *
   * @return the semantic graph of the input sentence produced by the Stanford Parser
   */
  public static List<SemanticGraph> parse(String sentence) {
    List<SemanticGraph> semanticGraphs = new ArrayList<SemanticGraph>();
    List<List<HasWord>> boh = tokenize(sentence);
    Tree tree;
    // Parse the sentence.
    for(List<HasWord> b : boh) {
      if(b.size()<50) {
        tree = LEXICALIZED_PARSER.parse(b);
        GrammaticalStructure gs = GSF.newGrammaticalStructure(tree);
        // Build the semantic graph.
        semanticGraphs.add(new SemanticGraph(gs.typedDependenciesCCprocessed()));
        System.out.println("[INFO] SG generated success");
      }
    }

    return  semanticGraphs;
  }

  public static List<CoreLabel> lemmatize(String text) {
    return LEXICALIZED_PARSER.lemmatize(text);
  }
}
