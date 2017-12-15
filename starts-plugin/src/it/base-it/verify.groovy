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
    verifyUtil.assertCorrectlyAffected("0")
    resetIT()
}

def resetIT() {
    verifyUtil.deleteFile(firstRun)
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"))
    verifyUtil.deleteFolder(new File(basedir, "target"))
}