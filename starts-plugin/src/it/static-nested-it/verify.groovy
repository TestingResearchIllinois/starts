/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.VerifyUtil

firstRun = new File(basedir, "first-run.txt")
verifyUtil = new VerifyUtil(new File(basedir, "build.log"))
verifyUtil.assertCorrectlyAffected("1")

if (!firstRun.exists()) {
    firstRun.createNewFile()
} else {
    resetIT()
}

def resetIT() {
    changedFile = new File(basedir, "src/main/java/first/First.java")
    VerifyUtil.replaceAllInFileStatic(changedFile, "Long", "Short")
    VerifyUtil.replaceAllInFileStatic(changedFile, "long", "short")

    verifyUtil.deleteFile(firstRun)
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"))
    verifyUtil.deleteFolder(new File(basedir, "target"))
}