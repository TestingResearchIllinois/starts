/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.data;

import static java.lang.String.join;

import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class creates objects that represent one row in the .zlc file.
 */
public class ZLCData {
    private URL url;
    private String checksum;
    private Set<String> tests;

    public ZLCData(URL url, String checksum, Set<String> tests) {
        this.url = url;
        this.checksum = checksum;
        this.tests = tests;
    }

    @Override
    public String toString() {
        //we track dependencies that are not reached by any test because of *
        String data;
        if (tests.isEmpty()) {
            data = join(" ", url.toExternalForm(), checksum);
        } else {
            data = join(" ", url.toExternalForm(), checksum, toCSV(tests));
        }
        return data;
    }

    private String toCSV(Set<String> tests) {
        return tests.stream().collect(Collectors.joining(","));
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

    public void setTests(Set<String> tests) {
        this.tests = tests;
    }
}
