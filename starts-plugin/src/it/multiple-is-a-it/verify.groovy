/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.VerifyUtil

firstRun = new File(basedir, "first-run.txt")
verifyUtil = new VerifyUtil(new File(basedir, "build.log"))

if (!firstRun.exists()) {
    verifyUtil.assertCorrectlyAffected("7")
    firstRun.createNewFile()
} else {
    verifyUtil.assertCorrectlyAffected("5")
    resetIT()
}

def resetIT() {
    firstChangedFile = new File(basedir, "src/main/java/first/Fourth.java")
    verifyUtil.replaceAllInFileStatic(firstChangedFile, "long", "short")
    verifyUtil.replaceAllInFileStatic(firstChangedFile, "Long", "Short")

    secondChangedFile = new File(basedir, "src/main/java/first/Second.java")
    verifyUtil.replaceAllInFileStatic(secondChangedFile, "long", "short")
    verifyUtil.replaceAllInFileStatic(secondChangedFile, "Long", "Short")

    verifyUtil.deleteFile(firstRun)
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"))
    verifyUtil.deleteFolder(new File(basedir, "target"))
}
