package org.docutils;

import com.beust.jcommander.JCommander;
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
import org.junit.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CommentAnalyzer {

    public static void main(String[] args) throws IOException {
        Args arguments = new Args();
        String[] argv = {"--analysis", "equivalence"};
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(argv);

        Assert.assertEquals(arguments.getAnalysis(), Args.ANALYSIS.EQUIVALENCE);

        List<String> sourceFolders = FileUtils.readLines(new File(
                CommentAnalyzer.class.getResource("/sources.txt").getPath()));

        int count = 0;
        for (String sourceFolder : sourceFolders) {
            //Collect all sources
            Collection<File> list = FileUtils.listFiles(
                    new File(
                            sourceFolder),
                    new RegexFileFilter("(.*).java"),
                    TrueFileFilter.INSTANCE);
            String[] selectedClassNames = getClassesInFolder(list, sourceFolder);
            FileWriter writer = new FileWriter("Match" + count++ + ".csv");
            writer.append("Class");
            writer.append(';');
            writer.append("Method");
            writer.append(';');
            //writer.append("Type");
            //writer.append(';');
            writer.append("Comment Text");
            writer.append(';');
            writer.append("Sentence");
            writer.append(';');
            writer.append("Complexity");
            if (arguments.getAnalysis().equals(Args.ANALYSIS.EQUIVALENCE)) {
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
            }

            writer.append('\n');
            System.out.println("[INFO] Analyzing " + sourceFolder + " ...");

            final JavadocExtractor javadocExtractor = new JavadocExtractor();

            for (String className : selectedClassNames) {
                DocumentedType documentedType = javadocExtractor.extract(
                        className, sourceFolder);

                if (documentedType == null) continue;
                List<DocumentedExecutable> executables = documentedType.getDocumentedExecutables();
                for (DocumentedExecutable method : executables) {
                    if (!TextOperations.freeTextToFilter(method.getJavadocFreeText())) {
                        String cleanComment = TextOperations.cleanTags(method.getJavadocFreeText());

                        switch (arguments.getAnalysis()) {
                            case TEMPORAL: {
                                boolean anythingFound =
                                        TemporalConstraints.isResultPositive(cleanComment);

                                if (anythingFound) {
                                    writer.append(className);
                                    writer.append(';');
                                    writer.append(method.getSignature());
                                    writer.append(';');
                                    writer.append("Free text");
                                    writer.append(';');
                                    writer.append(cleanComment.replaceAll(";", ","));
                                    writer.append("\n");
                                }
                                break;
                            }

                            case EQUIVALENCE: {
                                MatchInComment methodEquivalent =
                                        Equivalences.getEquivalentOrSimilarMethod(cleanComment);

                                if (methodEquivalent != null) {
                                    int inClass = 0, inPackage = 0, inProject = 0, inUnknown = 0;
                                    String sourcePath = buildPackagePath(className, sourceFolder);
                                    List<String> packageClasses = javadocExtractor.
                                            getClassesInSamePackage(className, sourcePath);
                                    MatchInComment.SystemLocation where = whereIsMethodDeclared(
                                            documentedType, methodEquivalent.getSignatures(), selectedClassNames, packageClasses);
                                    methodEquivalent.setSystemLocation(where);
                                    switch (where) {
                                        case C:
                                            inClass = 1;
                                            break;
                                        case P:
                                            inPackage = 1;
                                            methodEquivalent.incrementComplexity(1);
                                            break;
                                        case S:
                                            inProject = 1;
                                            methodEquivalent.incrementComplexity(2);
                                            break;
                                        case U:
                                            inUnknown = 1;
                                            methodEquivalent.incrementComplexity(3);
                                            break;
                                    }

                                    //Report class name
                                    writer.append(className);
                                    writer.append(';');
                                    //Report method signature
                                    writer.append(method.getSignature());
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
                                    //Report whether method is equivalent or similar
                                    if (methodEquivalent.isExactEquivalence())
                                        writer.append("Equivalent");
                                    else
                                        writer.append("Similar");

                                    writer.append(';');
                                    //Report method signature(s) that is(are) equivalent
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
                                break;
                            }
                        }

                        //TODO same for strategies here

                    }
                }
            }
            writer.flush();
            writer.close();
        }
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
    private static MatchInComment.SystemLocation whereIsMethodDeclared(DocumentedType type, ArrayList<String> signatures, String[] systemClasses, List<String> packageClasses) {
        //TODO look if the method name is smt like Class#method or Class.method and look into packageClasses!
        //TODO Currently there is no parse of the method name so this is all very naive
        if (signatures.size() > 1) {
            return MatchInComment.SystemLocation.U;
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
                        return MatchInComment.SystemLocation.C;
                    }
                }
            }
            if (hashMark != -1) {
                String methodClass = method.substring(0, hashMark);

                if (!packageClasses.isEmpty()) {
                    for (String pclass : packageClasses) {
                        if (pclass.endsWith("." + methodClass)) {
                            return MatchInComment.SystemLocation.P;
                        }
                    }
                    for (String sclass : systemClasses) {
                        if (sclass.endsWith("." + methodClass)) {
                            return MatchInComment.SystemLocation.S;
                        }
                    }
                }
            }
        }
        return MatchInComment.SystemLocation.U;
    }

    /**
     * From a list of files in path, finds the fully qualified names of Java classes.
     *
     * @param list collection of {@code Files}
     * @param path the String path to search in
     * @return fully qualified names found, in an array of Strings
     */
    private static String[] getClassesInFolder(Collection<File> list, String path) {
        String[] selectedClassNames = new String[list.size()];
        int i = 0;
        for (File file : list) {
            String fileName = file.getAbsolutePath();
            String[] unnecessaryPrefix = fileName.split(path);
            String className = unnecessaryPrefix[1].replaceAll("/", ".");
            selectedClassNames[i++] = className.replace(".java", "");
        }
        return selectedClassNames;

    }

}
