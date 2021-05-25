/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import edu.illinois.starts.constants.StartsConstants;

import org.junit.Assert;

/**
 * Util methods for scripts that verify results of integration tests and reset src code to original state.
 */
public class VerifyUtil implements StartsConstants {
    private List<String> buildLog;

    public VerifyUtil(File buildLog) throws IOException {
        this.buildLog = Files.readAllLines(buildLog.toPath(), Charset.defaultCharset());
    }

    public void assertCorrectlyAffected(String value) throws IOException {
        for (String line : buildLog) {
            if (line.contains(STARTS_AFFECTED_TESTS)) {
                String[] affectedTests = line.split(COLON);
                Assert.assertTrue("Number of affected tests expected: " + value, affectedTests[2].equals(value));
            }
        }
    }

    public static void replaceAllInFileStatic(File file, String before, String after) throws IOException {
        Path path = file.toPath();
        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll(before, after);
        Files.write(path, content.getBytes(charset));
    }

    public void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    public void deleteFolder(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteFolder(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
