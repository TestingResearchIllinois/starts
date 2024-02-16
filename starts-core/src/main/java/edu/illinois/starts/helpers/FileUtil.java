/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import edu.illinois.starts.changelevel.Macros;

import java.io.File;

/**
 * File handling utility methods.
 */
public class FileUtil {
    public static void delete(File file) {
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                delete(childFile);
            }
        }
        file.delete();
    }

    public static String urlToClassName(String urlExternalForm){
        int i = urlExternalForm.indexOf("target/classes/");
        if (i == -1)
            i = urlExternalForm.indexOf("target/test-classes/") + "target/test-classes/".length();
        else
            i = i + + "target/classes/".length();
        String internalName = urlExternalForm.substring(i, urlExternalForm.length()-6);
        return internalName;
    }

    public static String urlToSerFilePath(String urlExternalForm){
        int index = urlExternalForm.indexOf("target");
        urlExternalForm = urlExternalForm.substring(index).replace(".class", "");
        StringBuffer sb = new StringBuffer();
        String[] array = urlExternalForm.split("/");
        for (int i = 2; i < array.length; i++){
            sb.append(array[i]);
            sb.append(".");
        }
        sb.append("ser");
        return System.getProperty("user.dir") + "/" + Macros.STARTS_ROOT_DIR_NAME + "/" +
            org.ekstazi.Names.CHANGE_TYPES_DIR_NAME + "/" + sb.toString();
    }
}
