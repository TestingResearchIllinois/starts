/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.yasgl.DirectedGraph;
import edu.illinois.yasgl.DirectedGraphBuilder;
import edu.illinois.yasgl.GraphVertexVisitor;

/**
 * Utility methods for interacting with YASGL.
 */
public class YasglHelper implements StartsConstants {
    private List<String> lines = new ArrayList<>();

    public static Set<String> computeReachabilityFromChangedClasses(Set<String> changed, DirectedGraph<String> graph) {
        final Set<String> reachable = graph.acceptForward(changed, new GraphVertexVisitor<String>() {
            @Override
            public void visit(String name) {
            }
        });
        return reachable;
    }

    public static Set<String> reverseReachabilityFromChangedClasses(Set<String> changed, DirectedGraph<String> graph) {
        final Set<String> reachable = graph.acceptBackward(changed, new GraphVertexVisitor<String>() {
            @Override
            public void visit(String name) {
            }
        });
        return reachable;
    }

    public static void readZipToBuilder(File graphFile, DirectedGraphBuilder<String> builder) throws IOException {
        FileInputStream fis = new FileInputStream(graphFile);
        GZIPInputStream gzis = new GZIPInputStream(fis);
        InputStreamReader isr = new InputStreamReader(gzis);
        BufferedReader br = new BufferedReader(isr, 32768);
        try {
            String str;
            while ((str = br.readLine()) != null) {
                addEdgeToGraph(builder, str);
            }
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        } finally {
            fis.close();
        }
    }

    public static void addEdgeToGraph(DirectedGraphBuilder<String> builder, String str) {
        String[] edge = str.split(WHITE_SPACE);
        if (edge.length != 2) {
            throw new IllegalArgumentException("@@@NoEdgeTarget: " + str);
        }
        internAndAddEdge(builder, edge);
    }

    public static void internAndAddEdge(DirectedGraphBuilder<String> builder, String[] edge) {
        // TODO: does it need to "intern" at all, if the "builder" already uses "equals"?!
        if (edge.length != 2) {
            throw new IllegalArgumentException("Edge should have length 2");
        }
        edge[0] = edge[0].intern();
        edge[1] = edge[1].intern();
        builder.addEdge(edge[0], edge[1]);
    }

    @SuppressWarnings("checkstyle:Regexp")
    public DirectedGraphBuilder<String> addEdgesToBuilder(File graphFile, DirectedGraphBuilder<String> builder) {
        boolean noGZ = graphFile.getAbsolutePath().endsWith(".gz") ? false : true;

        if (!graphFile.exists()) {
            return builder;
        }

        try {
            System.out.print(DOT);
            if (noGZ) {
                lines = Files.readAllLines(graphFile.toPath(), Charset.defaultCharset());
                for (String line : lines) {
                    addEdgeToGraph(builder, line);
                }
            } else {
                readZipToBuilder(graphFile, builder);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }

        return builder;
    }
}
