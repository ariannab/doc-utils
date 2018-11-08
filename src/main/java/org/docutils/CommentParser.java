package org.docutils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.docutils.extractor.JavadocExtractor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class CommentParser {

    public static void main(String[] args) throws IOException {
        final JavadocExtractor javadocExtractor = new JavadocExtractor();
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
                FileWriter writer = new FileWriter("Regex_match.csv");
                writer.append("Class");
                writer.append(';');
                writer.append("Method");
                writer.append(';');
                writer.append("Type");
                writer.append(';');
                writer.append("Comment");
                writer.append('\n');
                System.out.println("[INFO] Analyzing " + sourceFolder + " ...");
//                searchKeywords(writer, javadocExtractor, sourceFolder, selectedClassNames);

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
