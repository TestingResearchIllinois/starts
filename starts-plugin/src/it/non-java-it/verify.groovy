/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.VerifyUtil;

targetDir = new File(basedir, "target");
firstRun = new File(basedir, "first-run.txt");
verifyUtil = new VerifyUtil(new File(basedir, "build.log"));

if (!firstRun.exists()) {
    firstRun.createNewFile();
	verifyUtil.assertCorrectlyAffected("1");
} else {
    verifyUtil.deleteFile(firstRun);
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"));
    file = new File(basedir, "src/main/resources/books.xml");
    VerifyUtil.replaceAllInFileStatic(file, "boook", "bk");
    verifyUtil.deleteFolder(targetDir);
	verifyUtil.assertCorrectlyAffected("0");
}
