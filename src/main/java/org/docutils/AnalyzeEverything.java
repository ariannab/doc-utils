package org.docutils;

import com.beust.jcommander.JCommander;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.docutils.analysis.Equivalences;
import org.docutils.analysis.TemporalConstraints;
import org.docutils.extractor.DocumentedExecutable;
import org.docutils.extractor.DocumentedType;
import org.docutils.extractor.JavadocExtractor;
import org.docutils.util.Args;
import org.docutils.util.MatchInComment;
import org.docutils.util.TextOperations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnalyzeEverything {

    final static int MAX_SENTENCES = 500;

    public static void main(String[] args) throws IOException {
        Args arguments = new Args();
        String[] argv = {"--analysis", "everything"};
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(argv);

        //Assert.assertEquals(arguments.getAnalysis(), Args.ANALYSIS.EQUIVALENCE);

//        List<String> sourceFolders = FileUtils.readLines(new File(
//                AnalyzeRandomly.class.getResource("/sources.txt").getPath()));

        Map<String,String> sourceFolders = new HashMap<>();
        sourceFolders.put("math", "/Users/arianna/toradocu/src/test/resources/commons-math3-3.6.1-src/src/main/java/");
//        sourceFolders.put("coll","/Users/arianna/toradocu/src/test/resources/commons-collections4-4.1-src/src/main/java/");
//        sourceFolders.put("guava", "/Users/arianna/toradocu/src/test/resources/guava-19.0-sources/");
//        sourceFolders.put("lucene","/Users/arianna/comment-clones/javadoclones/src/resources/src/lucene-core-7.2.1-sources/");
//        sourceFolders.put("hadoop","/Users/arianna/comment-clones/javadoclones/src/resources/src/hadoop-2.6.5-src/" +
//                "hadoop-common-project/hadoop-common/src/main/java/");
//        sourceFolders.put("gwt", "/Users/arianna/doc-utils/src/main/resources/gwt-2.5.1-sources/");

        Map<String, List<String>> packages = new HashMap<>();
        packages.put("math", Arrays.asList("fitting", "interpolation", "euclidean", "transform", "solvers", "regression"));
//        packages.put("coll", Arrays.asList("collections4"));
//        packages.put("guava", Arrays.asList("collect", "primitives", "base"));
//        packages.put("lucene", Arrays.asList("util", "codecs"));
//        packages.put("hadoop", Arrays.asList("util", "fs", "conf"));
//        packages.put("gwt", Arrays.asList("canvas.dom.client.", "safecss.shared", "user.client",
//                "web.bindery.requestfactory.shared", "server.rpc", "client.ui", "safehtml.shared"));

        for (String sourceFolderID : sourceFolders.keySet()) {
            String sourceFolder = sourceFolders.get(sourceFolderID);
            //Collect all sources
            Collection<File> list = FileUtils.listFiles(
                    new File(
                            sourceFolder),
                    new RegexFileFilter("(.*).java"),
                    TrueFileFilter.INSTANCE);
            List<String> selectedClassNames = new ArrayList<>();
            FileWriter writer = new FileWriter("July_new_mixed_examples/Match_"+sourceFolderID+"_pack.csv");
            if (arguments.getAnalysis().equals(Args.ANALYSIS.DETAILED_EQUIVALENCE)) {
                selectedClassNames = getClassesInFolder(list, sourceFolder);
                printEquivalenceHeaders(writer);
            } else if (arguments.getAnalysis().equals(Args.ANALYSIS.EVERYTHING)) {
                List<String> packageList = packages.get(sourceFolderID);
                if (packageList.size() > 0) {
                    selectedClassNames = getPackageClassesInFolder(list, sourceFolder, packageList);
                }

                printEverythingHeaders(writer);
            }
            System.out.println("\n[INFO] Analyzing " + sourceFolderID + " ...");

            final JavadocExtractor javadocExtractor = new JavadocExtractor();
            List<String> alreadyExtractedClasses = new ArrayList<>();
            //for(String className : selectedClassNames){
            String className;
            int sentencesCount = 0;
            int count = 0;
            while(sentencesCount < MAX_SENTENCES){
                do {
                    // 1. RANDOMLY EXTRACT ONE CLASS FROM THE POOL
                    int random = (int) (Math.random() * selectedClassNames.size());
                    className = selectedClassNames.get(random);
                }while(alreadyExtractedClasses.contains(className));

                alreadyExtractedClasses.add(className);
                try {
                    DocumentedType documentedType = javadocExtractor.extract(
                            className, sourceFolder);

                    // IN THE SAME CLASS THERE COULD BE DUPLICATED SENTENCES, WE DO NOT WANT DUPLICATES
                    Set<String> allSentencesSoFar = new LinkedHashSet<>();
                    if (documentedType == null) continue;
                    else {
                        System.out.println("[INFO] " + count++ +"  Looking into: " + className + ", sentences:" + sentencesCount);
                    }
                    List<DocumentedExecutable> executables = documentedType.getDocumentedExecutables();
                    Collections.shuffle(executables);
                    for (DocumentedExecutable documentedExec : executables) {
                        if (!TextOperations.freeTextToFilter(documentedExec.getJavadocFreeText())) {
                            String cleanComment = TextOperations.cleanTags(documentedExec.getJavadocFreeText());
                                    String[] sentences = cleanComment.split("\\. ");
                                    for (String sentence : sentences) {
                                        if (!allSentencesSoFar.contains(sentence)) {
                                            allSentencesSoFar.add(sentence);
                                        } else {
                                            continue;
                                        }
                                        //Report class name
                                        writer.append(className);
                                        writer.append(';');
                                        //Report documentedExec signature
                                        writer.append(documentedExec.getExecutable().toGenericString());
                                        writer.append(';');
                                        //Report comment text
                                        writer.append(cleanComment.replaceAll(";", ","));
                                        writer.append(';');
                                        //Report sentence
                                        writer.append(sentence.replaceAll(";", ","));
                                        writer.append(';');
                                        //Report equivalence or not
                                        writer.append(String.valueOf(0));

                                        writer.append("\n");
                                        sentencesCount++;
                                    }
                                }
                            //TODO same for strategies here

                        }
                  //  }
                }catch (NoClassDefFoundError e){
                }
            }
            writer.flush();
            writer.close();
        }
    }

    private static void printEquivalenceHeaders(FileWriter writer) throws IOException {
        writer.append("Class");
        writer.append(';');
        writer.append("Method 1");
        writer.append(';');
        writer.append("Method 2");
        writer.append(';');
        writer.append("Compatible");
        writer.append(';');
        //writer.append("Type");
        //writer.append(';');
        writer.append("Comment Text");
        writer.append(';');
        writer.append("Sentence");
        writer.append(';');
        writer.append("Complexity");
        writer.append(';');
        writer.append("Equiv/Simil");
        writer.append(';');
        writer.append("Signature(s)");
        writer.append(';');
        //Report whether it is in same class...
        writer.append("C");
        writer.append(';');
        //Report whether it is in same package...
        writer.append("P");
        writer.append(';');
        //Report whether it is inside project...
        writer.append("S");
        writer.append(';');
        //Report whether it is unknown...
        writer.append("U");

        writer.append('\n');
    }

    private static void printEverythingHeaders(FileWriter writer) throws IOException {
        writer.append("Class");
        writer.append(';');
        writer.append("Method");
        writer.append(';');
        writer.append("Comment Text");
        writer.append(';');
        writer.append("Sentence");
        writer.append(';');
        writer.append("Eq");

        writer.append('\n');
    }

    private static void temporalAnalysisOutput(FileWriter writer, String className, DocumentedExecutable documentedExec, String cleanComment) throws IOException {
        boolean anythingFound =
                TemporalConstraints.isResultPositive(cleanComment);

        if (anythingFound) {
            writer.append(className);
            writer.append(';');
            writer.append(documentedExec.getSignature());
            writer.append(';');
            writer.append("Free text");
            writer.append(';');
            writer.append(cleanComment.replaceAll(";", ","));
            writer.append("\n");
        }
        return;
    }

    private static void equivalenceAnalysisOutput(String sourceFolder, List<String> selectedClassNames, FileWriter writer, JavadocExtractor javadocExtractor, String className, DocumentedType documentedType, DocumentedExecutable documentedExec, String cleanComment) throws IOException {
        MatchInComment methodEquivalent =
                Equivalences.getEquivalentOrSimilarMethod(cleanComment);

        if (methodEquivalent != null) {
            int inClass = 0, inPackage = 0, inProject = 0, inUnknown = 0;
            String sourcePath = buildPackagePath(className, sourceFolder);
            List<String> packageClasses = javadocExtractor.
                    getClassesInSamePackage(className, sourcePath);
            Pair<DocumentedExecutable, MatchInComment.SystemLocation> theEquivalent = whereIsMethodDeclared(
                    documentedType, methodEquivalent.getSignatures(), selectedClassNames, packageClasses);

            DocumentedExecutable equivalentExec = theEquivalent.getKey();
            boolean areExecsCompatible = areReturnTypesEquals(documentedExec, equivalentExec);
            MatchInComment.SystemLocation where = theEquivalent.getValue();
            methodEquivalent.setSystemLocation(where);
            switch (where) {
                case C:
                    inClass = 1;
                    break;
                case P:
                    inPackage = 1;
                    methodEquivalent.incrementComplexity(2);
                    break;
                case S:
                    inProject = 1;
                    methodEquivalent.incrementComplexity(3);
                    break;
                case U:
                    inUnknown = 1;
                    methodEquivalent.incrementComplexity(4);
                    break;
            }

            //Report class name
            writer.append(className);
            writer.append(';');
            //Report documentedExec signature
            writer.append(documentedExec.getExecutable().toGenericString());
            writer.append(';');
            //Report documentedExec signature
            if (equivalentExec != null) {
                writer.append(equivalentExec.getExecutable().toGenericString());
            } else {
                writer.append("");
            }
            writer.append(';');
            // Return compatible?
            writer.append(String.valueOf(areExecsCompatible));
            writer.append(';');

            //Report type of comment
            //writer.append("Free text");
            //writer.append(';');

            //Report comment text
            writer.append(cleanComment.replaceAll(";", ","));
            writer.append(';');
            //Report sentence
            writer.append(methodEquivalent.getSentences().toString()
                    .replaceAll(";", ","));
            writer.append(';');
            //Report complexity
            writer.append(String.valueOf(methodEquivalent.getComplexity()));
            writer.append(';');
            //Report whether documentedExec is equivalent or similar
            if (methodEquivalent.isExactEquivalence())
                writer.append("Equivalent");
            else
                writer.append("Similar");

            writer.append(';');
            //Report documentedExec signature(s) that is(are) equivalent
            writer.append(methodEquivalent.getSignatures().toString());
            writer.append(';');
            //Report whether it is in same class...
            writer.append(String.valueOf(inClass));
            writer.append(';');
            //Report whether it is in same package...
            writer.append(String.valueOf(inPackage));
            writer.append(';');
            //Report whether it is inside project...
            writer.append(String.valueOf(inProject));
            writer.append(';');
            //Report whether it is unknown...
            writer.append(String.valueOf(inUnknown));

            writer.append("\n");
        }
        return;
    }

    private static boolean areReturnTypesEquals(
            DocumentedExecutable documentedExec, DocumentedExecutable equivalentExec) {
        if (equivalentExec != null) {
            Type docType = documentedExec.getExecutable().getAnnotatedReturnType().getType();
            Type equivType = equivalentExec.getExecutable().getAnnotatedReturnType().getType();
            return docType.equals(equivType) && !docType.equals(Void.TYPE);
        }
        //FIXME all equivalents that are not in same class are currently null, so this is not a reliable result
        return false;
    }

    /**
     * @param className
     * @param sourceFolder
     * @return
     */
    private static String buildPackagePath(String className, String sourceFolder) {
        String classPath = className.substring(0, className.lastIndexOf("."));
        classPath = classPath.replaceAll("\\.", "/");
        String result = sourceFolder + classPath;
        if (result.charAt((result.length() - 1)) != '/') {
            result += "/";
        }
        System.out.println(result);
        return result;
    }


    /**
     * Tells whether the signatures are located in the class (C), package (P), system (S).
     * "U" means Unknown and typically means the method is in another system.
     *
     * @param type           the class
     * @param signatures     the signatures
     * @param systemClasses  all the class names in the system
     * @param packageClasses the classes in the same package
     * @return a string representing the location (C, P, S, U)
     */
    private static Pair<DocumentedExecutable, MatchInComment.SystemLocation>
    whereIsMethodDeclared(DocumentedType type, ArrayList<String> signatures, List<String> systemClasses, List<String> packageClasses) {
        //TODO look if the method name is smt like Class#method or Class.method and look into packageClasses!
        //TODO Currently there is no parse of the method name so this is all very naive
        if (signatures.size() > 1) {
            return new Pair<>(null, MatchInComment.SystemLocation.U);
        } else {
            String method = signatures.get(0).trim();
            List<DocumentedExecutable> executables = type.getDocumentedExecutables();
            int parenthesis = method.indexOf("(");
            int hashMark = method.indexOf("#");
            String methodSimpleName;

            if (parenthesis != -1 && hashMark == -1) {
                methodSimpleName = method.substring(0, parenthesis);
                for (DocumentedExecutable ex : executables) {
                    if (ex.getSignature().contains(methodSimpleName)) {
                        return new Pair<>(ex, MatchInComment.SystemLocation.C);
                    }
                }
            }
            if (hashMark != -1) {
                String methodClass = method.substring(0, hashMark);

                if (!packageClasses.isEmpty()) {
                    for (String pclass : packageClasses) {
                        if (pclass.endsWith("." + methodClass)) {
                            //FIXME don't like null here
                            return new Pair<>(null, MatchInComment.SystemLocation.P);
                        }
                    }
                    for (String sclass : systemClasses) {
                        if (sclass.endsWith("." + methodClass)) {
                            //FIXME don't like null here
                            return new Pair<>(null, MatchInComment.SystemLocation.S);
                        }
                    }
                }
            }
        }
        return new Pair<>(null, MatchInComment.SystemLocation.U);
    }

    /**
     * From a list of files in path, finds the fully qualified names of Java classes.
     *
     * @param list collection of {@code Files}
     * @param path the String path to search in
     * @return fully qualified names found, in an array of Strings
     */
    private static List<String> getClassesInFolder(Collection<File> list, String path) {
        List<String> selectedClassNames = new ArrayList<>();
        int i = 0;
        for (File file : list) {
            String fileName = file.getAbsolutePath();
            String[] unnecessaryPrefix = fileName.split(path);
            String className = unnecessaryPrefix[1].replaceAll("/", ".");
            selectedClassNames.add(className.replace(".java", ""));
        }
        return selectedClassNames;

    }

    /**
     * From a list of files in path, finds the fully qualified names of Java classes,
     * but only belonging to specific packages.
     *
     * @param list     collection of {@code Files}
     * @param path     the String path to search in
     * @param packages the packages we want the classes from
     * @return fully qualified names found, in an array of Strings
     */
    private static List<String> getPackageClassesInFolder(Collection<File> list, String path, List<String> packages) {
        List<String> allClassNames = new ArrayList<>();
        for (File file : list) {
            String fileName = file.getAbsolutePath();
            String[] unnecessaryPrefix = fileName.split(path);
            String className = unnecessaryPrefix[1].replaceAll("/", ".");
            allClassNames.add(className);
        }
        // FIXME ugly. How to do this in Java lambdas?
        List<String> outputList = new ArrayList<>();
        for (String p : packages) {
            for (String className : allClassNames) {
                if (className.contains(p) && className.matches(".*"+p+"\\..*\\.java")) {
                    outputList.add(className.replace(".java", ""));
                }
            }
        }
        return outputList;

    }
}
