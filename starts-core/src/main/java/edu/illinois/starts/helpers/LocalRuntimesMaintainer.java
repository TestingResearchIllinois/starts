package edu.illinois.starts.helpers;

import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import edu.illinois.starts.helpers.TimeExtractor;

/**
 * Utility methods for maintaining previous runtime logs.
 * TODO should I use this or should I use another method?
 */

public class LocalRuntimesMaintainer {

    private static final int TOTAL_TIMES_PER_TEST = 5;
    private static final String RUNTIME_LOGS_FILE_NAME = "logTimes.txt";

    public static void updateOrCreate(File baseDir, String startsDirectoryPath) {
        String logFileName = baseDir.getAbsolutePath() + startsDirectoryPath + RUNTIME_LOGS_FILE_NAME;
        File logFile = new File(logFileName);
        File statsFile = TimeExtractor.getSurefireStatsFile(baseDir);
        //Get the current hash of the statistics file
        String currHash = FileUtil.getFileHash(statsFile).trim();

        if (logFile.exists()) {
            //Get the checksum of the last statistics file we parsed, so that we don't add duplicate runtimes
            //to our logs
            List<String> oldStats = FileUtil.getFileContents(logFile.toPath());
            String oldHash = oldStats.get(0).trim();

            if (oldHash.equals(currHash)) {
                return;
            } else {
                //Remove the hash value from the list of strings so that we don't try to parse it
                oldStats.remove(0);

                //Get the data that is already recorded
                Map<String, List<Integer>> prevData = getPrevData(oldStats);

                //Get the new timings from the statistics file
                Map<String, Integer> prevRuntimes = TimeExtractor.getPrevTestRunTimeMapFromFile(statsFile);

                Map<String, List<Integer>> newData = updateData(prevData, prevRuntimes);
                writeFile(logFile, currHash, newData);
            }
        } else if (statsFile != null) {
            //Write a log file only if there is a statistics file that already exists
            Map<String, Integer> prevRuntimes = TimeExtractor.getPrevTestRunTimeMapFromFile(statsFile);
            Map<String, List<Integer>> newData = updateData(null, prevRuntimes);
            writeFile(logFile, currHash, newData);
        }
    }

    public static Map<String, List<Integer>> getPrevData(List<String> localStats) {
        if (localStats == null) {
            return null;
        }
        Map<String, List<Integer>> prevData = new HashMap<>();
        for (String test: localStats) {
            String[] testInfo = test.split("|");
            String[] runtimeStrings = testInfo[1].split(",");
            List<Integer> runTimes = new ArrayList<>();
            for (String runtime: runtimeStrings) {
                runTimes.add(Integer.getInteger(runtime));
            }
            prevData.put(testInfo[0], runTimes);
        }
        return prevData;
    }

    public static int getAverageRuntime(File baseDir, String startsDirPath, String testName) {
        int runtimeTotal = 0;
        int entryCount = 0;
        String logFileName = baseDir.getAbsolutePath() + startsDirPath + RUNTIME_LOGS_FILE_NAME;
        File logFile = new File(logFileName);
        if (logFile.exists()) {
            Map<String, List<Integer>> prevData = getPrevData(FileUtil.getFileContents(logFile.toPath()));
            for (Map.Entry<String, List<Integer>> runtime : prevData.entrySet()) {
                if (runtime.getKey().contains(testName)) {
                    for (Integer curr : runtime.getValue()) {
                        if (curr != -1) {
                            runtimeTotal += curr;
                            entryCount++;
                        }
                    }
                }
            }
            if (entryCount == 0) {
                return -1;
            } else {
                return (runtimeTotal / entryCount);
            }
        } else {
            return -1;
        }
    }

    //TODO clean up later?
    public static Map<String, List<Integer>> updateData(Map<String, List<Integer>> prevData,
                                                        Map<String, Integer> prevRuntimes) {
        Map<String, List<Integer>> newData = new HashMap<>();
        for (Map.Entry<String, Integer> runtime : prevRuntimes.entrySet()) {
            String testName = runtime.getKey();
            int currRuntime = runtime.getValue();
            List<Integer> newRuntimeList = new ArrayList<>();
            newRuntimeList.add(currRuntime);

            if (prevData != null) {
                List<Integer> prevRuns = prevData.get(testName);
                for (int i = 0; i < TOTAL_TIMES_PER_TEST - 1; i++) {
                    if (prevRuns != null) {
                        int prevTime = -1;
                        try {
                            prevTime = prevRuns.get(i);
                        } catch (IndexOutOfBoundsException err) {
                            prevTime = -1;
                        }
                        newRuntimeList.add(prevTime);
                    } else {
                        newRuntimeList.add(-1);
                    }
                }
            } else {
                for (int i = 0; i < TOTAL_TIMES_PER_TEST - 1; i++) {
                    newRuntimeList.add(-1);
                }
            }
            newData.put(testName, newRuntimeList);
        }
        return newData;
    }

    public static boolean writeFile(File fileToWrite, String checksum, Map<String, List<Integer>> dataToWrite) {
        if (fileToWrite.exists()) {
            fileToWrite.delete();
        }
        try {
            BufferedWriter fileWriter = Files.newBufferedWriter(fileToWrite.toPath(), StandardCharsets.UTF_8, WRITE);
            fileWriter.write(checksum + System.lineSeparator());
            for (Map.Entry<String, List<Integer>> data : dataToWrite.entrySet()) {
                StringBuilder stringToWrite = new StringBuilder();
                List<Integer> runTimes = data.getValue();
                stringToWrite.append(data.getKey() + "|");
                for (Integer currTime : runTimes) {
                    stringToWrite.append(currTime + ",");
                }
                String finalString = stringToWrite.toString();

                //Remove last comma
                finalString = finalString.substring(0, finalString.length() - 1);
                fileWriter.write(finalString + System.lineSeparator());
            }
        } catch (IOException err) {
            return false;
        }
        return true;
    }
}
