/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.util;

/**
 * Logger used in all of the STARTS code.
 */
import java.io.PrintStream;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;

public class Logger implements StartsConstants {

    private static final Logger INSTANCE = new Logger();
    private PrintStream out = System.out;
    private Level level = Level.CONFIG;

    public void setLoggingLevel(Level level) {
        this.level = level;
    }

    public Level getLoggingLevel() {
        return this.level;
    }

    public static Logger getGlobal() {
        return Logger.INSTANCE;
    }

    public void log(Level lev, String msg, Throwable thr) {
        if (lev.intValue() < this.level.intValue()) {
            return;
        }
        this.out.println(lev.toString() + COLON + msg);
        this.out.println(thr);
    }

    public void log(Level lev, String msg) {
        if (lev.intValue() < this.level.intValue()) {
            return;
        }
        this.out.println(lev.toString() + COLON + msg);
    }
}


