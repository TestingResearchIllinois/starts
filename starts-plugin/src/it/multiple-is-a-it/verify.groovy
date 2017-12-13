/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.VerifyUtil;

targetDir = new File(basedir, "target");
firstRun = new File(basedir, "first-run.txt");
verifyUtil = new VerifyUtil(new File(basedir, "build.log"));

if (!firstRun.exists()) {
    firstRun.createNewFile();
    verifyUtil.assertCorrectlyAffected("7");
} else {
    verifyUtil.assertCorrectlyAffected("5");
    verifyUtil.deleteFile(firstRun);
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"));
    file = new File(basedir, "src/main/java/first/Fourth.java");
    verifyUtil.replaceAllInFileStatic(file, "long", "short");
    verifyUtil.replaceAllInFileStatic(file, "Long", "Short");
    file = new File(basedir, "src/main/java/first/Second.java");
    verifyUtil.replaceAllInFileStatic(file, "long", "short");
    verifyUtil.replaceAllInFileStatic(file, "Long", "Short");
    verifyUtil.deleteFolder(targetDir);
}
