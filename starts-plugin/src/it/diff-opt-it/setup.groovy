/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.SetupUtil

firstRun = new File(basedir, "first-run.txt")

if (firstRun.exists()) {
    changeSrc()
}

def changeSrc() {
    setupUtil = new SetupUtil(new File(basedir, ".starts/deps.zlc"))

    firstFile = new File(basedir, "src/main/java/first/Fourth.java")
    setupUtil.replaceAllInFile(firstFile, "short", "long")
    setupUtil.replaceAllInFile(firstFile, "Short", "Long")

    secondFile = new File(basedir, "src/main/java/first/Second.java")
    setupUtil.replaceAllInFile(secondFile, "short", "long")
    setupUtil.replaceAllInFile(secondFile, "Short", "Long")
}