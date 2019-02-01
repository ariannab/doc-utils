package org.docutils.util;

import java.util.ArrayList;

public class MatchInComment {

    private ArrayList<String> signatures;
    private ArrayList<String> sentences;
    private boolean exactEquivalence;
    private int complexity;
    private SystemLocation systemLocation;

    public MatchInComment() {
        this.complexity = 0;
        this.signatures = new ArrayList<>();
        this.sentences = new ArrayList<>();
    }

    public enum SystemLocation{
        C, P, S, U
    }

//    public MatchInComment(boolean exactEquivalence, int complexity) {
//        this.exactEquivalence = exactEquivalence;
//        this.complexity = complexity;
//    }

    public ArrayList<String> getSignatures() {
        return signatures;
    }

    public void addSignature(String signature){
        this.signatures.add(signature);
    }

    public ArrayList<String> getSentences() {
        return sentences;
    }

    public void addSentences(String sentence){
        this.sentences.add(sentence);
    }

    public boolean isExactEquivalence() {
        return exactEquivalence;
    }

    public void setSystemLocation(SystemLocation systemLocation) {
        this.systemLocation = systemLocation;
    }

    public int getComplexity() {
        return complexity;
    }

    public void incrementComplexity(int increment){
        this.complexity += increment;

    }

    public void setExactEquivalence(boolean exactEquivalence) {
        this.exactEquivalence = exactEquivalence;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }
}
