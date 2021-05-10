package edu.illinois.starts.data;

import java.util.List;

/**
 * This class creates object that represents the entire .zlc file.
 */
public class ZLCFile {
    private List<String> tests;
    private List<ZLCData> zlcData;

    public ZLCFile(List<String> tests, List<ZLCData> zlcData) {
        this.tests = tests;
        this.zlcData = zlcData;
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
