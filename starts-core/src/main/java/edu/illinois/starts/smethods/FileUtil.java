package edu.illinois.starts.smethods;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for manipulating files.
 */
public class FileUtil {

    public static void saveMap(Map<String, Set<String>> mapToStore, String dirName, String fileName) throws Exception {
        File directory = new File(System.getProperty("user.dir") + "/" + dirName);
        if (!directory.exists()) {
            directory.mkdir();
        }

        File txtFile = new File(directory, fileName);
        PrintWriter pw = new PrintWriter(txtFile);

        for (Map.Entry<String, Set<String>> en : mapToStore.entrySet()) {
            String methodName = en.getKey();
            // invokedMethods saved in csv format
            String invokedMethods = String.join(",", mapToStore.get(methodName));
            pw.println(methodName + " " + invokedMethods);
        }
        pw.flush();
        pw.close();
    }
}
