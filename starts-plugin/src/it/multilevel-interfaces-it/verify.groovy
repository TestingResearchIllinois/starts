/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.VerifyUtil;

firstRun = new File(basedir, "first-run.txt");
verifyUtil = new VerifyUtil(new File(basedir, "build.log"));

if (!firstRun.exists()) {
    firstRun.createNewFile();
    verifyUtil.assertCorrectlyAffected("3");
    verifyUtil.assertContains("Running inter.ChildTest");
    verifyUtil.assertContains("Running inter.SiblingTest");
    verifyUtil.assertContains("Running inter.GrandChildTest");
} else {
    verifyUtil.assertCorrectlyAffected("2");
    verifyUtil.assertContains("Running inter.ChildTest");
    verifyUtil.assertNotContains("Running inter.SiblingTest");
    verifyUtil.assertContains("Running inter.GrandChildTest");
    verifyUtil.deleteFile(firstRun);
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"));
}
