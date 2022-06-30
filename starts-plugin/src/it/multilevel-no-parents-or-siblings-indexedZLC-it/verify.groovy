/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.VerifyUtil;

firstRun = new File(basedir, "first-run.txt");
verifyUtil = new VerifyUtil(new File(basedir, "build.log"));

if (!firstRun.exists()) {
    firstRun.createNewFile();
    verifyUtil.assertCorrectlyAffected("4");
} else {
    verifyUtil.assertCorrectlyAffected("2");
    verifyUtil.deleteFile(firstRun);
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"));
}
