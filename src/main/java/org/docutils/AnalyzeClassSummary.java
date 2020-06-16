package org.docutils;

import com.beust.jcommander.JCommander;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.docutils.extractor.DocumentedType;
import org.docutils.extractor.JavadocExtractor;
import org.docutils.util.Args;
import org.docutils.util.TextOperations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyzeClassSummary {
    private static int MAX_CLASSES = 0;

    private final static String OUTPUT_FILE_NAME = "Class_summaries_june_";

    public static void main(String[] args) throws IOException {
        Args arguments = new Args();
        String[] argv = {"--analysis", "class_summary"};
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(argv);

        Map<String, String> sourceFolders = getSourceFoldersMap();

        for (String sourceFolderID : sourceFolders.keySet()) {
            String sourceFolder = sourceFolders.get(sourceFolderID);
            //Collect all sources
            Collection<File> list = FileUtils.listFiles(
                    new File(
                            sourceFolder),
                    new RegexFileFilter("(.*).java"),
                    TrueFileFilter.INSTANCE);
            List<String> selectedClassNames = new ArrayList<>();
            FileWriter writer = new FileWriter(OUTPUT_FILE_NAME+sourceFolderID+".txt");
            if (arguments.getAnalysis().equals(Args.ANALYSIS.CLASS_SUMMARY)) {
                selectedClassNames = getClassesInFolder(list, sourceFolder);
                printEverythingHeaders(writer);
            }

            MAX_CLASSES = selectedClassNames.size() - selectedClassNames.size()*75/100;
            System.out.println("\n[INFO] Analyzing " + sourceFolderID + " ...");

            final JavadocExtractor javadocExtractor = new JavadocExtractor();
            List<String> alreadyExtractedClasses = new ArrayList<>();
            String className;
            int classSummaryCount = 0;
            int count = 0;
            while(classSummaryCount < MAX_CLASSES){
                // FIXME what if we want to extract a fixed number of classes for EACH package
                do {
                    // 1. RANDOMLY EXTRACT ONE CLASS FROM THE POOL
                    int random = (int) (Math.random() * selectedClassNames.size());
                    className = selectedClassNames.get(random);
                }while(alreadyExtractedClasses.contains(className));

                alreadyExtractedClasses.add(className);
                try {
                    DocumentedType documentedType = javadocExtractor.extractClassOnly(
                            className, sourceFolder);

                    if (documentedType == null) continue;
                    else {
                        System.out.println("[INFO] " + count++ +"  Looking into: " + className + ", sentences:" + classSummaryCount);
                    }

                    String cleanComment = TextOperations.cleanSummary(documentedType.getClassSummary());
                    //Report class name
                    writer.append(className);
                    writer.append(';');
                    writer.append("\n");
                    // Report class summary
                    writer.append(cleanComment.replaceAll(";", ","));
                    writer.append("\n");
                    writer.append("--------------------------------------");
                    writer.append("\n");
                    classSummaryCount++;

                }catch (NoClassDefFoundError e){
                }
            }
            writer.flush();
            writer.close();
        }
    }

    private static Map<String, String> getSourceFoldersMap() {
        Map<String,String> sourceFolders = new HashMap<>();
        sourceFolders.put("math", "/Users/arianna/toradocu/src/test/resources/commons-math3-3.6.1-src/src/main/java/");
//        sourceFolders.put("coll","/Users/arianna/toradocu/src/test/resources/commons-collections4-4.1-src/src/main/java/");
//        sourceFolders.put("guava", "/Users/arianna/toradocu/src/test/resources/guava-19.0-sources/");
//        sourceFolders.put("lucene","/Users/arianna/comment-clones/javadoclones/src/resources/src/lucene-core-7.2.1-sources/");
//        sourceFolders.put("hadoop","/Users/arianna/comment-clones/javadoclones/src/resources/src/hadoop-2.6.5-src/" +
//                "hadoop-common-project/hadoop-common/src/main/java/");
//        sourceFolders.put("gwt", "/Users/arianna/doc-utils/src/main/resources/gwt-2.5.1-sources/");

//        Map<String, List<String>> packages = new HashMap<>();
//        packages.put("math", Arrays.asList("fitting", "interpolation", "euclidean", "transform", "solvers", "regression"));
//        packages.put("coll", Arrays.asList("collections4"));
//        packages.put("guava", Arrays.asList("collect", "primitives", "base"));
//        packages.put("lucene", Arrays.asList("util", "codecs"));
//        packages.put("hadoop", Arrays.asList("util", "fs", "conf"));
//        packages.put("gwt", Arrays.asList("canvas.dom.client.", "safecss.shared", "user.client",
//                "web.bindery.requestfactory.shared", "server.rpc", "client.ui", "safehtml.shared"));
        return sourceFolders;
    }


    private static void printEverythingHeaders(FileWriter writer) throws IOException {
        writer.append("Class");
        writer.append(';');
        writer.append("Summary");

        writer.append('\n');
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
