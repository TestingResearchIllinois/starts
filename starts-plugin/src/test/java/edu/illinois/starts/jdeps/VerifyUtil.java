/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import edu.illinois.starts.constants.StartsConstants;

import org.junit.Assert;

/**
 * Util methods for scripts that verify results of integration tests.
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

    public void assertContains(String value) {
        Assert.assertTrue("Log should contains string: " + value, buildLog.contains(value));
    }

    public void assertNotContains(String value) {
        Assert.assertFalse("Log shouldn't contains string: " + value, buildLog.contains(value));
    }

    public void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }
}
