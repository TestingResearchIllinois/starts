/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

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
}
