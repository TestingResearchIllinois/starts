/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */
import static org.junit.Assert.assertTrue
 
import edu.illinois.starts.jdeps.VerifyUtil

firstRun = new File(basedir, "first-run.txt")
deps = new File(basedir, ".starts" + File.separator + "deps.zlc")
oldDeps = new File(basedir, ".starts" + File.separator + "deps.zlc.bak")

if (!firstRun.exists()) {
    VerifyUtil.backupDeps(deps, oldDeps)
    firstRun.createNewFile()
} else {
    assertTrue(VerifyUtil.compareDeps(deps, oldDeps) == 2)
    resetIT()
}

def resetIT() {
    verifyUtil = new VerifyUtil(new File(basedir, "build.log"))

    changedFirstFile = new File(basedir, "src/main/java/first/Fourth.java")
    verifyUtil.replaceAllInFileStatic(changedFirstFile, "long", "short")
    verifyUtil.replaceAllInFileStatic(changedFirstFile, "Long", "Short")

    changedSecondFile = new File(basedir, "src/main/java/first/Second.java")
    verifyUtil.replaceAllInFileStatic(changedSecondFile, "long", "short")
    verifyUtil.replaceAllInFileStatic(changedSecondFile, "Long", "Short")

    verifyUtil.deleteFile(firstRun)
    verifyUtil.deleteFile(new File(basedir, ".starts/deps.zlc"))
    verifyUtil.deleteFolder(new File(basedir, "target"))
}