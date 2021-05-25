/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.VerifyUtil

firstRun = new File(basedir, "first-run.txt")
verifyUtil = new VerifyUtil(new File(basedir, "build.log"))

if (!firstRun.exists()) {
    verifyUtil.assertCorrectlyAffected("2")
    firstRun.createNewFile()
} else {
    verifyUtil.assertCorrectlyAffected("1")
    resetIT()
}

def resetIT() {
    changedFile = new File(basedir, "src/main/java/first/Second.java")
    verifyUtil.replaceAllInFileStatic(changedFile, "long", "short")
    verifyUtil.replaceAllInFileStatic(changedFile, "Long", "Short")

    verifyUtil.deleteFile(firstRun)
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"))
    verifyUtil.deleteFolder(new File(basedir, "target"))
}