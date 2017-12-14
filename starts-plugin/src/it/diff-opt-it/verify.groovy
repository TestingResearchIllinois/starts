/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */
import static org.junit.Assert.assertTrue;
 
import edu.illinois.starts.jdeps.VerifyUtil;
import groovy.io.FileType;
import java.io.File;

firstRun = new File(basedir, "first-run.txt");
deps = new File(basedir, ".starts" + File.separator + "deps.zlc");
oldDeps = new File(basedir, ".starts" + File.separator + "deps.zlc.bak");

if (!firstRun.exists()) {
    VerifyUtil.backupDeps(deps, oldDeps);
    firstRun.createNewFile();
} else {
    derp = VerifyUtil.compareDeps(deps, oldDeps);
    assertTrue(derp == 2);
    verifyUtil = new VerifyUtil(new File(basedir, "build.log"));
    verifyUtil.deleteFile(firstRun);
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"));
    file = new File(basedir, "src/main/java/first/Fourth.java");
    verifyUtil.replaceAllInFileStatic(file, "long", "short");
    verifyUtil.replaceAllInFileStatic(file, "Long", "Short");
    file = new File(basedir, "src/main/java/first/Second.java");
    verifyUtil.replaceAllInFileStatic(file, "long", "short");
    verifyUtil.replaceAllInFileStatic(file, "Long", "Short");
    targetDir = new File(basedir, "target");
    verifyUtil.deleteFolder(targetDir);
}