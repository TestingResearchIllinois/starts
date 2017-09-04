/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.yasgl.DirectedGraph;
import edu.illinois.yasgl.DirectedGraphBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WriterTest {

    public static final String TEST_FILE_PATH = "writerTest.txt";
    public static final String ARTIFACTDIR = ".";
    public static File file;
    public static BufferedReader reader;
    public static Path path;
    public static Path sfpath;
    public static List<String> lines;
    public static Charset charset;
    private static Writer writer;

    public static void writeToGraph(String[] edges) {
        DirectedGraphBuilder<String> builder = new DirectedGraphBuilder<String>();
        for (int i = 0; i < edges.length; i++) {
            String[] nodes = edges[i].split(",");
            builder.addEdge(nodes[0], nodes[1]);
        }
        DirectedGraph<String> graph = builder.build();
        writer.writeGraph(graph, ARTIFACTDIR, true, TEST_FILE_PATH);
    }

    @Before
    public void setupOnce() throws Exception {
        file = new File(TEST_FILE_PATH);
        file.createNewFile();
        writer = new Writer();
        path = Paths.get(TEST_FILE_PATH);
        sfpath = Paths.get("sf-classpath");
        reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        charset = Charset.forName("UTF-8");
    }

    @Test
    public void testWriteToFile() throws IOException {
        ArrayList collection = new ArrayList<>();
        collection.add("hello");
        writer.writeToFile(collection, TEST_FILE_PATH, ARTIFACTDIR);
        lines = Files.readAllLines(path, charset);
        assertTrue(lines.contains("hello"));
        assertEquals(1, lines.size());
    }

    @Test
    public void testWriteTCSimple() throws IOException {
        Map<String, Set<String>> testDeps = new HashMap<String, Set<String>>();
        Set<String> input1 = new HashSet<String>();
        input1.add("hi");
        input1.add("how");
        testDeps.put("first", input1);
        writer.writeTCSimple(testDeps, ARTIFACTDIR, TEST_FILE_PATH);
        lines = Files.readAllLines(path, charset);
        String line = lines.get(0);
        String[] graph = line.split(" ");
        assertEquals("first", graph[0]);
        assertTrue(graph[1].contains("first") && graph[1].contains("hi") && graph[1].contains("how"));
    }

    @Test
    public void testWriteGraph() throws IOException {
        String[] edges = {"A,B"};
        writeToGraph(edges);
        lines = Files.readAllLines(path, charset);
        assertTrue(lines.contains("A B"));
        assertEquals(1, lines.size());
    }

    @Test
    public void testWriteGrpahWithMultipleEdges() throws IOException {
        String[] edges = {"A,B", "C,D"};
        writeToGraph(edges);
        lines = Files.readAllLines(path, charset);
        String line = lines.get(0);
        assertEquals("A B", line);
        line = lines.get(1);
        assertEquals("C D", line);
        assertEquals(2, lines.size());
    }

    @Test
    public void testWriteGraphWithRepeatedEdges() throws IOException {
        String[] edges = {"A,B", "A,B"};
        writeToGraph(edges);
        lines = Files.readAllLines(path, charset);
        assertTrue(lines.contains("A B"));
        assertEquals(1, lines.size());
    }

    @Test
    public void testWriteClassPath() throws IOException {
        String classPath = "a.jar";
        writer.writeClassPath(classPath, ARTIFACTDIR);
        lines = Files.readAllLines(sfpath, charset);
        assertTrue(lines.contains("a.jar"));
        assertEquals(1, lines.size());
    }

    @Test
    public void testMultipleClassPath() throws IOException {
        String classPath = "1.jar" + File.pathSeparator + "2.jar";
        writer.writeClassPath(classPath, ARTIFACTDIR);
        lines = Files.readAllLines(sfpath, charset);
        assertTrue(lines.contains(classPath));
        assertEquals(1, lines.size());
    }

    @Test
    public void testPathToString() {
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        String result = "a" + File.pathSeparator + "b";
        assertEquals(result, writer.pathToString(list));
    }

    @Test
    public void testEmptyPathToString() {
        List<String> list = new ArrayList<String>();
        list.add("");
        assertEquals("", writer.pathToString(list));
    }

    @Test
    public void testSinglePathToString() {
        List<String> list = new ArrayList<String>();
        list.add("a");
        assertEquals("a", writer.pathToString(list));
    }

    @After
    public void cleanUp() throws IOException {
        if (file.exists()) {
            file.delete();
        }
        file = new File("sf-classpath");
        if (file.exists()) {
            file.delete();
        }
        writer = null;
        reader.close();
    }
}
