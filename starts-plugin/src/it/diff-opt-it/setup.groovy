/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.SetupUtil;

firstRun = new File(basedir, "first-run.txt");

if (firstRun.exists()){
    setupUtil = new SetupUtil(new File(basedir, ".starts/deps.zlc"))
    file = new File(basedir, "src/main/java/first/Fourth.java");
    setupUtil.replaceAllInFile(file, "short", "long");
    setupUtil.replaceAllInFile(file, "Short", "Long");
    file = new File(basedir, "src/main/java/first/Second.java");
    setupUtil.replaceAllInFile(file, "short", "long");
    setupUtil.replaceAllInFile(file, "Short", "Long");
}