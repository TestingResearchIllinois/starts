/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.data;

import static java.lang.String.join;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.util.Pair;

/**
 * This class creates objects that represent one row in the .zlc file.
 */
public class ZLCData implements StartsConstants {
    private ZLCFormat format;
    private URL url;
    private String checksum;
    private Set<String> testsStr;
    private Set<Integer> testsIdx;

    public ZLCData(URL url, String checksum, ZLCFormat format, Set<String> testsStr, Set<Integer> testsIdx) {
        this.format = format;
        this.url = url;
        this.checksum = checksum;
        this.testsStr = testsStr;
        this.testsIdx = testsIdx;
    }

    @Override
    public String toString() {
        //we track dependencies that are not reached by any test because of *
        String data;
        switch (format) {
            case INDEXED:
                if (testsIdx.isEmpty()) {
                    data = join(WHITE_SPACE, url.toExternalForm(), checksum);
                } else {
                    data = join(WHITE_SPACE, url.toExternalForm(), checksum, toCSVInt(testsIdx));
                }
                break;
            case PLAIN_TEXT:
                if (testsStr.isEmpty()) {
                    data = join(WHITE_SPACE, url.toExternalForm(), checksum);
                } else {
                    data = join(WHITE_SPACE, url.toExternalForm(), checksum, toCSVStr(testsStr));
                }
                break;
            default:
                throw new RuntimeException("Unexpected ZLCFormat");
        }
        return data;
    }

    private static String toCSVInt(Set<Integer> tests) {
        return tests.stream().map(String::valueOf).collect(Collectors.joining(COMMA));
    }

    private static String toCSVStr(Set<String> tests) {
        return String.join(COMMA, tests);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ZLCData zlcData = (ZLCData) obj;

        if (!url.equals(zlcData.url)) {
            return false;
        }
        return checksum.equals(zlcData.checksum);
    }
}
