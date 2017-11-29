/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;

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

    public static List<String> getFileContents(Path fileToRead) {
        List<String> fileContents;
        try {
            fileContents = Files.readAllLines(fileToRead, Charset.defaultCharset());
        } catch (IOException error) {
            return null;
        }
        return fileContents;
    }

    public static List<String> getFileContents(Path baseDir, String fileName) {
        return getFileContents(Paths.get(baseDir + fileName));
    }

    public static String getFileHash(File fileToHash) {
        try {
            FileInputStream fileStream = new FileInputStream(fileToHash);
            String hash = org.apache.commons.codec.digest.DigestUtils.md5Hex(fileStream);
            fileStream.close();
            return hash;
        } catch (Exception err) {
            return "";
        }
    }
}
