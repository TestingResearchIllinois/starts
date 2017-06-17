/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import edu.illinois.yasgl.DirectedGraph;
import edu.illinois.yasgl.DirectedGraphBuilder;
import edu.illinois.yasgl.Edge;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class YasglHelperTest {

    public static final String TEST_EMPTY_FILE_PATH = "emptytest.txt";
    public static final String TEST_EDGE_FILE_PATH = "edgetest.txt";
    public static DirectedGraphBuilder builder;
    public static YasglHelper yasgl;
    public static DirectedGraph graph;
    public static File emptyfile;
    public static File edgefile;
    public static Collection<Edge<String>> edges;
    public static Edge<String> edge;

    public static Collection<Edge<String>> addEdgeToGraph(String[] edges) throws Exception {
        for (int i = 0; i < edges.length; i++) {
            yasgl.addEdgeToGraph(builder, edges[i]);
        }
        graph = builder.build();
        return graph.getEdges();
    }

    public static Collection<Edge<String>> addEdgesToBuilder(ArrayList<String> col) {
        Writer writer = new Writer();
        writer.writeToFile(col, TEST_EDGE_FILE_PATH, ".");
        yasgl.addEdgesToBuilder(edgefile, builder);
        graph = builder.build();
        return graph.getEdges();
    }

    @BeforeClass
    public static void setUp() throws IOException {
        builder = new DirectedGraphBuilder();
        yasgl = new YasglHelper();
        emptyfile = new File(TEST_EMPTY_FILE_PATH);
        emptyfile.createNewFile();
        edgefile = new File(TEST_EDGE_FILE_PATH);
        edgefile.createNewFile();
    }

    @AfterClass
    public static void cleanUp() {
        if (emptyfile.exists()) {
            emptyfile.delete();
        }
        if (edgefile.exists()) {
            edgefile.delete();
        }
    }

    @Test
    public void addEdgeToGraphTest() throws Exception {
        String[] edgesToAdd = {"1 2"};
        edges = addEdgeToGraph(edgesToAdd);
        edge = new Edge<String>("1", "2");
        assertTrue(edges.contains(edge));
    }

    @Test
    public void addMultipleEdgesToGraph() throws Exception {
        String [] edgesToAdd = {"x y", "u v"};
        edges = addEdgeToGraph(edgesToAdd);
        edge = new Edge<String>("x", "y");
        assertTrue(edges.contains(edge));
        edge = new Edge<String>("u", "v");
        assertTrue(edges.contains(edge));
    }

    @Test(expected = Exception.class)
    public void addEmptyEdgeTest() throws Exception {
        yasgl.addEdgeToGraph(builder, "");
    }

    @Test
    public void addEdgesToBuilderTest() {
        ArrayList<String> col = new ArrayList<String>();
        col.add("x y");
        edges = addEdgesToBuilder(col);
        edge = new Edge<String>("x", "y");
        assertTrue(edges.contains(edge));
    }

    @Test
    public void addMultipleEdgesToBuilder() {
        ArrayList<String> col = new ArrayList<String>();
        col.add("1 2");
        col.add("3 4");
        edges = addEdgesToBuilder(col);
        edge = new Edge<String>("1", "2");
        assertTrue(edges.contains(edge));
        edge = new Edge<String>("3", "4");
        assertTrue(edges.contains(edge));
    }

    @Test
    public void addEmpytEdgeToBuilderTest() {
        int before = builder.build().getEdges().size();
        yasgl.addEdgesToBuilder(emptyfile, builder);
        int after = builder.build().getEdges().size();
        assertEquals(before, after);
    }

    @Test
    public void addEdgeToBuilderTestNoFile() {
        int before = builder.build().getEdges().size();
        yasgl.addEdgesToBuilder(new File("nofile.txt"), builder);
        int after = builder.build().getEdges().size();
        assertEquals(before, after);
    }
}
