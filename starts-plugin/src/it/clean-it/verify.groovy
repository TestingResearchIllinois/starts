/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import groovy.io.FileType;

def startsDir = new File(basedir, ".starts")
assert (startsDir.exists() == false)