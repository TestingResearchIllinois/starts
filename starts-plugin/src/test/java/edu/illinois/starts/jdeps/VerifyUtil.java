/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.io.File;
import java.io.IOException;
import java.lang.Throwable;
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

    public static void backupDeps(File depsFile, File oldDepsFile) {
        Path source = depsFile.toPath();
        Path target = oldDepsFile.toPath();
        System.out.println("source: " + source.toString());
        System.out.println("target: " + target.toString());
        try {
            Files.copy(source, target);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static int compareDeps(File depsFile, File oldDepsFile) {
        List<String> oldDeps = null;
        List<String> newDeps = null;
        try {
            oldDeps = Files.readAllLines(oldDepsFile.toPath(), Charset.defaultCharset());
            newDeps = Files.readAllLines(depsFile.toPath(), Charset.defaultCharset());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (oldDeps == null) {
            System.out.println("oldDeps is null");
        }
        if (oldDeps != null && newDeps != null  && (oldDeps.size() == newDeps.size())) {
            oldDeps.removeAll(newDeps);
        }
        return oldDeps.size();
    }
}
