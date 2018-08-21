/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.data;

import static java.lang.String.join;

import java.net.URL;
import java.util.Set;

import edu.illinois.starts.constants.StartsConstants;

/**
 * This class creates objects that represent one row in the .zlc file.
 */
public class ZLData implements StartsConstants {
    private URL url;
    private String checksum;

    public ZLData(URL url, String checksum) {
        this.url = url;
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        //we track dependencies that are not reached by any test because of *
        String data = join(WHITE_SPACE, url.toExternalForm(), checksum);
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ZLData zlcData = (ZLData) obj;

        if (!url.equals(zlcData.url)) {
            return false;
        }
        return checksum.equals(zlcData.checksum);
    }

}
