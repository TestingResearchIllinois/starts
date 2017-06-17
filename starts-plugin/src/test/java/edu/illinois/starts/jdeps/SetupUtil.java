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

/**
 * Util methods for scripts that set up integration tests.
 */
public class SetupUtil {
    private File zlcFile;

    public SetupUtil(File zlcFile) {
        this.zlcFile = zlcFile;
    }

    public void replaceAllInFile(File file, String before, String after) throws IOException {
        if (zlcFile.exists()) {
            Path path = file.toPath();
            Charset charset = StandardCharsets.UTF_8;
            String content = new String(Files.readAllBytes(path), charset);
            content = content.replaceAll(before, after);
            Files.write(path, content.getBytes(charset));
        }
    }
}
