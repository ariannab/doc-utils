package org.docutils;

import com.beust.jcommander.JCommander;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.docutils.analysis.Equivalences;
import org.docutils.analysis.TemporalConstraints;
import org.docutils.extractor.DocumentedExecutable;
import org.docutils.extractor.DocumentedType;
import org.docutils.extractor.JavadocExtractor;
import org.docutils.util.Args;
import org.docutils.util.Cleaner;
import org.junit.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class CommentParser {

    public static void main(String[] args) throws IOException {
        Args arguments = new Args();
        String[] argv = { "--analysis", "temporal"};
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(argv);

        Assert.assertEquals(arguments.getAnalysis(), Args.ANALYSIS.TEMPORAL);

        List<String> sourceFolders = FileUtils.readLines(new File(
                CommentParser.class.getResource("/tmp.txt").getPath()));

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
            writer.append("Type");
            writer.append(';');
            writer.append("Comment");
            writer.append('\n');
            System.out.println("[INFO] Analyzing " + sourceFolder + " ...");

            final JavadocExtractor javadocExtractor = new JavadocExtractor();

            for (String className : selectedClassNames) {
                DocumentedType documentedType = javadocExtractor.extract(
                        className, sourceFolder);

                if (documentedType != null) {
                    List<DocumentedExecutable> executables = documentedType.getDocumentedExecutables();
                    for (DocumentedExecutable method : executables) {
                        if (!Cleaner.freeTextToFilter(method.getJavadocFreeText())) {
                            String cleanComment = Cleaner.cleanTags(method.getJavadocFreeText());

                            boolean anythingFound = false;
                            if(arguments.getAnalysis()!=null) {
                                switch (arguments.getAnalysis()) {
                                    case TEMPORAL:
                                        anythingFound =
                                                TemporalConstraints.isResultPositive(cleanComment);
                                        break;

                                    case EQUIVALENCE:
                                        anythingFound =
                                                Equivalences.isResultPositive(cleanComment);
                                        break;
                                }
                            }
                            //TODO same for strategies here

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
                        }
                    }
                }
            }
            writer.flush();
            writer.close();
        }
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
