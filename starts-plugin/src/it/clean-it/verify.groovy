/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

import groovy.io.FileType;

def list = []
def dir = new File(basedir, ".starts")
assert (dir.exists() == false)