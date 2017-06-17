/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.VerifyUtil;

firstRun = new File(basedir, "first-run.txt");
verifyUtil = new VerifyUtil(new File(basedir, "build.log"));

if (!firstRun.exists()) {
    firstRun.createNewFile();
    verifyUtil.assertCorrectlyAffected("1");
} else {
    // Although we change Simple.java between runs, no test is selected because
    // without reflection-awareness, SimpleTest.java does not depend on Simple.java
    verifyUtil.assertCorrectlyAffected("0");
    verifyUtil.deleteFile(firstRun);
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"));
}
