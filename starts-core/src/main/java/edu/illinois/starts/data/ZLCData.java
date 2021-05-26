/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.data;

import static java.lang.String.join;

import java.net.URL;
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
    private Set<Pair<String, Integer>> tests;

    public ZLCData(URL url, String checksum, Set<Pair<String, Integer>> tests, ZLCFormat format) {
        this.format = format;
        this.url = url;
        this.checksum = checksum;
        this.tests = tests;
    }

    @Override
    public String toString() {
        //we track dependencies that are not reached by any test because of *
        String data;
        if (tests.isEmpty()) {
            data = join(WHITE_SPACE, url.toExternalForm(), checksum);
            return data;
        }
        switch (format) {
            case INDEXED:
                data = join(WHITE_SPACE, url.toExternalForm(), checksum, toCSVInt(
                        tests.stream().map(Pair::getValue).collect(Collectors.toSet())
                ));
                break;
            case PLAIN_TEXT:
                data = join(WHITE_SPACE, url.toExternalForm(), checksum, toCSVStr(
                        tests.stream().map(Pair::getKey).collect(Collectors.toSet())
                ));
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

    public void setTests(Set<Pair<String, Integer>> tests) {
        this.tests = tests;
    }
}
