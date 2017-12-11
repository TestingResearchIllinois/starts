/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.VerifyUtil;

targetDir = new File(basedir, "target");
firstRun = new File(basedir, "first-run.txt");
verifyUtil = new VerifyUtil(new File(basedir, "build.log"));
verifyUtil.assertCorrectlyAffected("1");

if (!firstRun.exists()) {
    firstRun.createNewFile();
} else {
    verifyUtil.deleteFile(firstRun);
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"));
    file = new File(basedir, "src/main/java/first/First.java");
    VerifyUtil.replaceAllInFileStatic(file, "Set g", "Set<Integer> g");
    verifyUtil.deleteFolder(targetDir);
}
