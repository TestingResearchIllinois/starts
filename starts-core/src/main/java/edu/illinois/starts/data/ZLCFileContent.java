package edu.illinois.starts.data;

import java.util.List;

/**
 * This class creates object that represents the entire .zlc file.
 */
public class ZLCFileContent {
    private ZLCFormat format;
    private List<String> tests;
    private List<ZLCData> zlcData;

    public ZLCFileContent(List<String> tests, List<ZLCData> zlcData, ZLCFormat format) {
        this.tests = tests;
        this.zlcData = zlcData;
        this.format = format;
    }

    public ZLCFormat getFormat() {
        return format;
    }

    public int getTestsCount() {
        return tests.size();
    }

    public List<String> getTests() {
        return tests;
    }

    public List<ZLCData> getZlcData() {
        return zlcData;
    }
}
