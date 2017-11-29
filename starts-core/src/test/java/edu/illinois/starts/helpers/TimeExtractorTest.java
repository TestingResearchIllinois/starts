package edu.illinois.starts.helpers;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TimeExtractorTest {

    public static final String TEST_XML_FILE_PATH = "TEST-testTimeExtractor.xml";
    public static final String TEST_SUREFIRE_FILE_PATH = ".surefire-ABRACADABRA";
    public static final String ARTIFACTDIR = ".";
    public static final String testName = "testTimeExtractor";
    public static final int totalRuntime = 12307;
    public static File xmlFile;
    public static File surefireFile;
    public static File artifactDir;
    public static BufferedWriter XMLFileWriter;
    public static BufferedWriter surefireFileWriter;
    public static final String newLine = System.lineSeparator();
    private static final Logger LOGGER = Logger.getGlobal();

    @Before
    public void setupOnce() throws Exception {
        xmlFile = new File(TEST_XML_FILE_PATH);
        surefireFile = new File(TEST_SUREFIRE_FILE_PATH);
        XMLFileWriter = Files.newBufferedWriter(xmlFile.toPath(), StandardCharsets.UTF_8);
        surefireFileWriter = Files.newBufferedWriter(surefireFile.toPath(), StandardCharsets.UTF_8);
        artifactDir = new File(ARTIFACTDIR);
        writeTestXMLFile();
        writeTestSurefireFile();
    }

    public static void writeTestXMLFile() throws Exception {
        Double runtimeInS = (Double)(totalRuntime / 1000.0);
        XMLFileWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newLine);
        XMLFileWriter.write("<testsuite xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:schemaLocation=\"https://maven.apache.org/surefire/maven-surefire-plugin/xsd/"
            + "surefire-test-report.xsd\" name=\"testTimeExtractor\" time=\"" + runtimeInS
            + "\" tests=\"5\" errors=\"0\" skipped=\"0\" failures=\"0\">" + newLine);
        XMLFileWriter.write("  <properties>" + newLine);
        XMLFileWriter.write("    <property name=\"java.vendor\" value=\"Oracle Corporation\"/>" + newLine);
        XMLFileWriter.write("  </properties>" + newLine);
        XMLFileWriter.write("  <testcase name=\"test1\" classname=\"test1\" time=\"2.501\"/>" + newLine);
        XMLFileWriter.write("  <testcase name=\"test2\" classname=\"test2\" time=\"1.501\"/>" + newLine);
        XMLFileWriter.write("  <testcase name=\"test3\" classname=\"test3\" time=\"2.102\"/>" + newLine);
        XMLFileWriter.write("  <testcase name=\"test4\" classname=\"test4\" time=\"3.101\"/>" + newLine);
        XMLFileWriter.write("  <testcase name=\"test5\" classname=\"test5\" time=\"3.102\"/>" + newLine);
        XMLFileWriter.write("</testsuite>");
        XMLFileWriter.flush();
    }

    public static void writeTestSurefireFile() throws Exception {
        surefireFileWriter.write("1,2501,test1(testTimeExtractor)" + newLine);
        surefireFileWriter.write("1,1501,test2(testTimeExtractor)" + newLine);
        surefireFileWriter.write("1,2102,test3(testTimeExtractor)" + newLine);
        surefireFileWriter.write("1,3101,test4(testTimeExtractor)" + newLine);
        surefireFileWriter.write("1,3102,test5(testTimeExtractor)");
        surefireFileWriter.flush();
    }

    @Test
    public void readFromXMLFiles() throws Exception {
        int runtime = TimeExtractor.getPrevTestRuntimeFromXML(artifactDir, testName);
        assertEquals(totalRuntime, runtime);
    }

    @Test
    public void readFromStatisticsFile() throws Exception {
        deleteXMLFile();
        Map testMap = TimeExtractor.getPrevTestRunTimeMapFromFile(surefireFile);
        int runtime = TimeExtractor.getPrevTestRuntimeFromStatsMap(testMap, testName);
        assertEquals(runtime, totalRuntime);
        writeTestXMLFile();
    }

    @Test
    public void testRuntimePairSummation() throws Exception {
        List<Pair> testPairList = new ArrayList<>();
        testPairList.add(new Pair("test1", 1));
        testPairList.add(new Pair("test2", 1232));
        testPairList.add(new Pair("test3", 655));
        testPairList.add(new Pair("test4", 94552));
        testPairList.add(new Pair("test5", 33));
        int totalTime = TimeExtractor.getTotalRuntime(testPairList);
        assertEquals(96473, totalTime);
    }

    @Test
    public void readFromXMLIfStatsFileNonexistent() throws Exception {
        deleteSurefireFile();
        assertEquals(false, surefireFile.exists());
        Set<String> testNames = new HashSet<>();
        testNames.add(testName);
        List runtimes = TimeExtractor.getEstimatedRuntimes(testNames, artifactDir, artifactDir, "");
        int runtime = TimeExtractor.getTotalRuntime(runtimes);
        assertEquals(totalRuntime, runtime);
        writeTestSurefireFile();
    }

    private static void deleteXMLFile() throws IOException {
        if (xmlFile.exists()) {
            xmlFile.delete();
        }
    }

    private static void deleteSurefireFile() throws IOException {
        if (surefireFile.exists()) {
            surefireFile.delete();
        }
    }

    @After
    public void cleanUp() throws IOException {
        deleteXMLFile();
        deleteSurefireFile();
    }
}
