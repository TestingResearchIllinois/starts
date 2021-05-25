/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.VerifyUtil

firstRun = new File(basedir, "first-run.txt")
verifyUtil = new VerifyUtil(new File(basedir, "build.log"))

if (!firstRun.exists()) {
    verifyUtil.assertCorrectlyAffected("1")
    firstRun.createNewFile()
} else {
    // Although we change Simple.java between runs, no test is selected because
    // without reflection-awareness, SimpleTest.java does not depend on Simple.java
    verifyUtil.assertCorrectlyAffected("0")
    resetIT()
}

def resetIT() {
    changedFile = new File(basedir, "src/main/java/base/Simple.java")
    VerifyUtil.replaceAllInFileStatic(changedFile, "MissedDependency", "MissingDependency")

    verifyUtil.deleteFile(firstRun)
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"))
    verifyUtil.deleteFolder(new File(basedir, "target"))
}