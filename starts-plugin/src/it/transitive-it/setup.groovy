/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import edu.illinois.starts.jdeps.SetupUtil;

setupUtil = new SetupUtil(new File(basedir, ".starts/deps.zlc"))
file = new File(basedir, "src/main/java/transitive/Base.java");
setupUtil.replaceAllInFile(file, "LinkedHashSet", "HashSet")
