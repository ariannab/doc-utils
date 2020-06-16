package org.docutils;

import com.beust.jcommander.JCommander;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.docutils.analysis.Equivalences;
import org.docutils.util.MatchInComment;
import org.docutils.analysis.TemporalConstraints;
import org.docutils.extractor.DocumentedExecutable;
import org.docutils.extractor.DocumentedType;
import org.docutils.extractor.JavadocExtractor;
import org.docutils.util.Args;
import org.docutils.util.TextOperations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Search for a precise property in method summaries of a Java class.
 */
public class SpecificAnalyzer {

    public static void main(String[] args) throws IOException {
        Args arguments = new Args();
        String[] argv = {"--analysis", "SIMPLE_SENTENCE_EQ"};
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(argv);

//        Assert.assertEquals(arguments.getAnalysis(), Args.ANALYSIS.EQUIVALENCE);

//        List<String> sourceFolders = FileUtils.readLines(new File(
//                CommentAnalyzer.class.getResource("/sources.txt").getPath()));

        Map<String,String> sourceFolders = new HashMap<>();

        // Include the bar at the end of the string paths!

//        sourceFolders.put("coll", "/Users/arianna/toradocu/src/test/resources/commons-collections4-4.1-src/src/main/java/");
//        sourceFolders.put("math", "/Users/arianna/toradocu/src/test/resources/commons-math3-3.6.1-src/src/main/java/");
//        sourceFolders.put("guava", "/Users/arianna/toradocu/src/test/resources/guava-19.0-sources/");
//        sourceFolders.put("vertx", "/Users/arianna/comment-clones/javadoclones/src/resources/src/vertx-core-3.5.0-sources/");
//        sourceFolders.put("elastic", "/Users/arianna/comment-clones/javadoclones/src/resources/src/elasticsearch-6.1.1-sources/");
//        sourceFolders.put("lucene", "/Users/arianna/comment-clones/javadoclones/src/resources/src/lucene-core-7.2.1-sources/");
//        sourceFolders.put("hadoop", "/Users/arianna/comment-clones/javadoclones/src/resources/src/hadoop-2.6.5-src/" +
//                "hadoop-common-project/hadoop-common/src/main/java/");
//        sourceFolders.put("gwt", "/Users/arianna/doc-utils/src/main/resources/gwt-2.5.1-sources/");

//        sourceFolders.put("jdk", "/Users/arianna/toradocu/src/test/resources/jdk8-sources/src/share/classes/");

//        sourceFolders.put("ant", "/Users/arianna/toradocu/src/test/resources/apache-ant-1.10.7-src/src/main/");

//        sourceFolders.put("joda", "/Users/arianna/toradocu/src/test/resources/joda-time-2.10.5-sources/");

//        sourceFolders.put("codec", "/Users/arianna/toradocu/src/test/resources/commons-codec-1.13-sources/");

//        sourceFolders.put("tensor", "/Users/arianna/toradocu/src/test/resources/tensorflow-src/tensorflow/java/src/main/java/");
//        sourceFolders.put("mockito", "/Users/arianna/toradocu/src/test/resources/mockito-all-1.10.19-sources/");

//        sourceFolders.put("weka", "/Users/arianna/toradocu/src/test/resources/weka-stable-3.8.0-sources/");

//        sourceFolders.put("io", "/Users/arianna/toradocu/src/test/resources/commons-io-2.6-sources/");

//        sourceFolders.put("ham", "/Users/arianna/toradocu/src/test/resources/hamcrest-all-1.3-sources/");


//        sourceFolders.put("hibernate", "/Users/arianna/toradocu/src/test/resources/hibernate-core-5.4.2-sources/");
        sourceFolders.put("colt", "/Users/arianna/toradocu/src/test/resources/colt-master/src/");

        int count = 0;
        for (String sourceFolderID : sourceFolders.keySet()) {
            String sourceFolder = sourceFolders.get(sourceFolderID);
            //Collect all sources
            Collection<File> list = FileUtils.listFiles(
                    new File(
                            sourceFolder),
                    new RegexFileFilter("(.*).java"),
                    TrueFileFilter.INSTANCE);
            List<String> selectedClassNames = new ArrayList<>();
            FileWriter writer = new FileWriter("2020/SimpleSentenceEq/Match_SimpleEQONLY_"+sourceFolderID+".csv");
            if (arguments.getAnalysis().equals(Args.ANALYSIS.DETAILED_EQUIVALENCE)){
                selectedClassNames = getClassesInFolder(list, sourceFolder);
                printEquivalenceHeaders(writer);
            }else if(arguments.getAnalysis().equals(Args.ANALYSIS.SIMPLE_SENTENCE_EQ)){
                selectedClassNames = getClassesInFolder(list, sourceFolder);
                printCleanEquivalenceHeaders(writer);
            } if (arguments.getAnalysis().equals(Args.ANALYSIS.EVERYTHING)) {
                // String[] packagesArray = {"base", "collect", "primitives"};
                //String[] packagesArray = {"euclidean", "interpolation", "stat"};
                String[] packagesArray = {};
                if (packagesArray.length > 0) {
                    selectedClassNames = getPackageClassesInFolder(list, sourceFolder, packagesArray);
                }
                else{
                    selectedClassNames = getClassesInFolder(list, sourceFolder);
                }

                printEverythingHeaders(writer);
            }
            System.out.println("[INFO] Analyzing " + sourceFolderID + " ...");

            final JavadocExtractor javadocExtractor = new JavadocExtractor();

            for (String className : selectedClassNames) {
                try {
                    DocumentedType documentedType = javadocExtractor.extractExecutables(
                            className, sourceFolder);


                    Set<String> allSentencesSofar = new LinkedHashSet<>();
                    if (documentedType == null) continue;
                    else {
                        System.out.println("[INFO] " + count++ + " Looking into: " + className);
                    }
                    List<DocumentedExecutable> executables = documentedType.getDocumentedExecutables();
                    for (DocumentedExecutable documentedExec : executables) {
                        if (!TextOperations.freeTextToFilter(documentedExec.getJavadocFreeText())) {
                            String cleanComment = TextOperations.cleanTags(documentedExec.getJavadocFreeText());

                            switch (arguments.getAnalysis()) {
                                case TEMPORAL: {
                                    temporalAnalysisOutput(writer, className, documentedExec, cleanComment);
                                    break;
                                }

                                case DETAILED_EQUIVALENCE: {
                                    eqAnalysisInformativeOutput(sourceFolder, selectedClassNames, writer, javadocExtractor,
                                            className, documentedType, documentedExec, cleanComment);
                                    break;
                                }

                                case SIMPLE_SENTENCE_EQ:{
                                    eqAnalysisSimpleSentenceOutput(className, documentedExec, writer, cleanComment);
                                    break;
                                }

                                case EVERYTHING: {
                                    String[] sentences = cleanComment.split("\\. ");
                                    for (String sentence : sentences) {
                                        if (!allSentencesSofar.contains(sentence)) {
                                            allSentencesSofar.add(sentence);
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
                                    }
                                }
                            }

                            //TODO same for strategies here

                        }
                    }
                }catch (java.lang.NoClassDefFoundError e){
                    continue;
                }
            }
            writer.flush();
            writer.close();
        }
    }

    private static void printCleanEquivalenceHeaders(FileWriter writer) throws IOException {
        writer.append("Class");
        writer.append(';');
        writer.append("Method");
        writer.append(';');
        writer.append("Sentence");
        writer.append(';');

        writer.append('\n');
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

    private static void eqAnalysisSimpleSentenceOutput(String className, DocumentedExecutable documentedExec, FileWriter writer,
                                                       String cleanComment) throws IOException {
        MatchInComment methodEquivalent =
                Equivalences.getEquivalentOrSimilarMethod(cleanComment);

        if (methodEquivalent != null) {
            // Report class
            writer.append(className);
            writer.append(';');
            // Report method signature
            writer.append(documentedExec.getSignature());
            writer.append(';');
            //Report sentence
            writer.append(methodEquivalent.getSentences().get(0)
                    .replaceAll(";", ","));
            writer.append(';');
            // Print 1
            writer.append('1');
            writer.append("\n");
        }
        return;
    }

    private static void eqAnalysisInformativeOutput(String sourceFolder, List<String> selectedClassNames, FileWriter writer, JavadocExtractor javadocExtractor, String className, DocumentedType documentedType, DocumentedExecutable documentedExec, String cleanComment) throws IOException {
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
            int extensionIndex = className.lastIndexOf(".java");
            selectedClassNames.add(className.substring(0, extensionIndex));
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
    private static List<String> getPackageClassesInFolder(Collection<File> list, String path, String[] packages) {
        List<String> allClassNames = new ArrayList<>();
        int i = 0;
        for (File file : list) {
            String fileName = file.getAbsolutePath();
            String[] unnecessaryPrefix = fileName.split(path);
            String className = unnecessaryPrefix[1].replaceAll("/", ".");
            allClassNames.add(className.replace(".java", ""));
        }
        // FIXME ugly. How to do this in Java lambdas?
        List<String> outputList = new ArrayList<>();
        for (String p : packages) {
            for (String className : allClassNames) {
                if (className.contains(p)) {
                    outputList.add(className);
                }
            }
        }
        return outputList;

    }
}
