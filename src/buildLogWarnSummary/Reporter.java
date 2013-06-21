/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package buildLogWarnSummary;

import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.HashSet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static buildLogWarnSummary.Messages.*;
import static buildLogWarnSummary.Reporter.TableType.*;

public abstract class Reporter {
    protected enum TableType {
        REF_DELTA_LOCNS("New warnings not found in reference files"),
        LOCN_COUNTS_ALPHA("Warning counts, sorted alphabetically by location"),
        LOCN_COUNTS_FREQ("Warning counts, sorted by frequency of location"),
        LOCN_DIR_COUNTS_ALPHA("Warning counts, sorted alphabetically by location directory"),
        LOCN_DIR_COUNTS_FREQ("Warning counts, sorted by frequency of location directory"),
        LOCN_EXTN_COUNTS_ALPHA("Warning counts, sorted alphabetically by location extension"),
        LOCN_EXTN_COUNTS_FREQ("Warning counts, sorted by frequency of location extension"),
        LOCN_UNKNOWN("Warnings with unrecognized locations"),
        KIND_COUNTS_ALPHA("Warning counts, sorted alphabetically by kind"),
        KIND_COUNTS_FREQ("Warning counts, sorted by frequency of kind"),
        KIND_UNKNOWN("Warnings with unrecognized kinds"),
        TOOL_COUNTS_ALPHA("Warning counts, sorted alphabetically by tool"),
        TOOL_COUNTS_FREQ("Warning counts, sorted by frequency of tool");
        TableType(String title) {
            this.title = title;
        }
        final String title;
    }

    protected Tables tables;
    protected Tables refTables;
    protected Map<String,Collection<String>> locnDeltas;
    protected File outFile;
    protected String title;
    protected boolean showLocations;
    protected boolean showKinds;
    protected boolean showTools;
    protected Tables referenceTables;


    public void setOutput(File file) {
        outFile = file;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setShowLocations(boolean b) {
        showLocations = b;
    }

    public void setShowKinds(boolean b) {
        showKinds = b;
    }

    public void setShowTools(boolean b) {
        showTools = b;
    }

    public void setReference(Tables refTables) {
        this.refTables = refTables;
    }

    public void report(Tables t) throws IOException {
        this.tables = t;
//        if (refTables != null) {
//            locnDeltas = getDifference(tables.getLocationMap(), refTables.getLocationMap());
//        }

        startReport();

        writeTableHead("Log files analyzed");
        writeList(tables.files);
        writeTable(tables.getStatistics());
        if (!refTables.isEmpty()) {
            writeTableHead("Reference files analyzed");
            writeList(refTables.files);
            writeTable(refTables.getStatistics());
//            writeTable(REF_DELTA_LOCNS, count(locnDeltas));
        }

        if (showLocations) {
            Map<Message.Location,Integer> locationCountMap = count(t.pathTable);
            writeTable(LOCN_COUNTS_ALPHA, locationCountMap);
            writeTable(LOCN_COUNTS_FREQ, flip(locationCountMap, decreasing));

            Map<String,Integer> locationDirCountMap = count(byDirectory(t.pathTable));
            writeTable(LOCN_DIR_COUNTS_ALPHA, locationDirCountMap);
            writeTable(LOCN_DIR_COUNTS_FREQ, flip(locationDirCountMap, decreasing));

            Map<String,Integer> locationExtnCountMap = count(byExtension(t.pathTable));
            writeTable(LOCN_EXTN_COUNTS_ALPHA, locationExtnCountMap);
            writeTable(LOCN_EXTN_COUNTS_FREQ, flip(locationExtnCountMap, decreasing));

            Collection<Message> unknownLocns = t.pathTable.get(Message.Location.UNKNOWN);
            if (unknownLocns != null)
                writeList(LOCN_UNKNOWN, unknownLocns);
        }

        if (showTools) {
            Map<Tool,Integer> toolCountMap = count(t.toolTable);
            writeTable(TOOL_COUNTS_ALPHA, toolCountMap);
            writeTable(TOOL_COUNTS_FREQ, flip(toolCountMap, decreasing));
        }

        if (showKinds) {
            Map<Message.Kind,Integer> typeCountMap = count(t.messageKindTable);
            writeTableRows(KIND_COUNTS_ALPHA, getKindRows1(typeCountMap.entrySet()));
            writeTableRows(KIND_COUNTS_FREQ, getKindRows2(flip(typeCountMap, decreasing)));

            Collection<Message> unknownTypes = t.messageKindTable.get(Message.Kind.UNKNOWN);
            if (unknownTypes != null)
                writeList(KIND_UNKNOWN, unknownTypes);
        }

        endReport();
    }

    private Collection<? extends Collection<?>> getKindRows1(Collection<? extends Map.Entry<Message.Kind,Integer>> entries) throws IOException {
        List<List<Object>> rows = new ArrayList<List<Object>>();
        for (Map.Entry<Message.Kind,Integer> e: entries) {
            Message.Kind kind = e.getKey();
            Integer count = e.getValue();
            List<Object> row = Arrays.asList((Object)kind.tool.name, trimPattern(kind.msgPattern), count);
            rows.add(row);
        }
        return rows;
    }

    private Collection<? extends Collection<?>> getKindRows2(Collection<? extends Map.Entry<Integer,Message.Kind>> entries) throws IOException {
        List<List<?>> rows = new ArrayList<List<?>>();
        for (Map.Entry<Integer,Message.Kind> e: entries) {
            Integer count = e.getKey();
            Message.Kind kind = e.getValue();
            List<Object> row = Arrays.asList(count, (Object)kind.tool.name, trimPattern(kind.msgPattern));
            rows.add(row);
        }
        return rows;
    }

    String trimPattern(Pattern p) {
        String s = p.pattern();
        int start = (s.startsWith(".*") ? 2 : 0);
        int end = s.length() - (s.endsWith(".*") ? 2 : 0);
        return s.substring(start, end);
    }

    protected void startReport() throws IOException { }

    protected void endReport() throws IOException { }

    protected <T> void writeList(TableType type, Collection<T> list) throws IOException {
        writeTableHead(type);
        writeList(list);
    }

    protected abstract <T> void writeList(Collection<T> list) throws IOException;

    protected void writeTable(TableType type, Map<?,?> table) throws IOException {
        writeTable(type, table.entrySet());
    }

    protected void writeTable(TableType type, Collection<? extends Map.Entry<?,?>> entries) throws IOException {
        writeTableHead(type);
        writeTableRows(getRows(entries));
    }

    protected void writeTableRows(TableType type, Collection<? extends Collection<?>> rows) throws IOException {
        writeTableHead(type);
        writeTableRows(rows);
    }

    protected void writeTable(Map<?,?> table) throws IOException {
        writeTableRows(getRows(table.entrySet()));
    }

    private Collection<? extends Collection<?>> getRows(Collection<? extends Map.Entry<?,?>> entries) throws IOException {
        List<List<?>> rows = new ArrayList<List<?>>();
        for (Map.Entry<?,?> e: entries) {
            rows.add(Arrays.asList(e.getKey(), e.getValue()));
        }
        return rows;
    }

    protected abstract void writeTableHead(String s) throws IOException;
    protected abstract void writeTableHead(TableType type) throws IOException;
//    protected abstract void writeTable(Collection<? extends Map.Entry<?,?>> entries) throws IOException;
    protected abstract void writeTableRows(Collection<? extends Collection<?>> rows) throws IOException;

    Map<String,Collection<String>> getDifference(
            Map<String, Collection<String>> a,
            Map<String, Collection<String>> b) {
        Map<String,Collection<String>> results = new LinkedHashMap<String,Collection<String>>();
        for (Map.Entry<String,Collection<String>> e: a.entrySet()) {
            Collection<String> d = getDifference(e.getValue(), b.get(e.getKey()));
            if (d != null)
                results.put(e.getKey(), d);
        }
        return results;
    }

    /* return strings in a, which are not in b, allowing for differences in coord info */
    Collection<String> getDifference(Collection<String> a, Collection<String> b) {
        if (b == null)
            return a;

        /* create a normalized version of b, with all digits removed. */
        Collection<String> nb = new HashSet<String>();
        for (String s: b)
            nb.add(s.replaceAll("[0-9]+", ""));

        /* build list by comparing normalized strings in a against normalized set b */
        Collection<String> results = null;
        for (String s: a) {
            if (!nb.contains(s.replaceAll("[0-9]+", ""))) {
                if (results == null)
                    results = new ArrayList<String>();
                results.add(s);
            }
        }

        return results;
    }

//    Collection<String> getDifference(Collection<String> a, Collection<String> b) {
//        if (b == null)
//            return a;
//
//        Collection<String> results = null;
//    nextEntry:
//        for (String ae : a) {
//            if (!containsIgnoreLine(b, ae)) {
//                if (results == null)
//                    results = new ArrayList<String>();
//                results.add(ae);
//            }
//        }
//        return results;
//    }
//
//    boolean containsIgnoreLine(Collection<String> c, String s) {
//        if (c.contains(s))
//            return true;
//        String sNoLine = removeLine(s);
//        for (String cs: c) {
//            if (removeLine(cs).equals(sNoLine))
//                return true;
//        }
//        return false;
//    }
//
//    String removeLine(String warning) {
//        Matcher m = ignoreLine.matcher(warning);
//        if (m.matches())
//            return m.group(1) + m.group(2);
//        else
//            return warning;
//    }
//
//    private final Pattern ignoreLine = Pattern.compile("([^:]+:)[0-9:]+:(.*)");

    <T> Map<String,Collection<T>> byExtension(Map<Message.Location,Collection<T>> map) {
        Map<String,Collection<T>> results = new TreeMap<String,Collection<T>>();
        for (Map.Entry<Message.Location,Collection<T>> e: map.entrySet()) {
            String extn = e.getKey().getExtension();
            Collection<T> dest = results.get(extn);
            if (dest == null) {
                dest = new LinkedHashSet<T>();
                results.put(extn, dest);
            }
            dest.addAll(e.getValue());
        }
        return results;
    }

    <T> Map<String,Collection<T>> byDirectory(Map<Message.Location,Collection<T>> map) {
        Map<String,Collection<T>> results = new TreeMap<String,Collection<T>>();
        for (Map.Entry<Message.Location,Collection<T>> e: map.entrySet()) {
            String dir = e.getKey().getPathDirectory();
            Collection<T> dest = results.get(dir);
            if (dest == null) {
                dest = new ArrayList<T>();
                results.put(dir, dest);
            }
            dest.addAll(e.getValue());
        }
        return results;
    }

    <K> Map<K, Integer> count(Map<K,? extends Collection<?>> map) {
        Map<K,Integer> results = new TreeMap<K,Integer>();
        for (Map.Entry<K,? extends Collection<?>> e: map.entrySet())
            results.put(e.getKey(), e.getValue().size());
        return results;
    }

    <K,V> Collection<? extends Map.Entry<V,K>> flip(Map<K,V> map, Comparator<Map.Entry<V,?>> c) {
        List<Map.Entry<V,K>> result = new ArrayList<Map.Entry<V,K>>();
        for (Map.Entry<K,V> e: map.entrySet())
            result.add(new SimpleMapEntry<V,K>(e.getValue(), e.getKey()));
        Collections.sort(result, c);
        return result;
    }

    protected static Comparator<Map.Entry<Integer,?>> decreasing = new Comparator<Map.Entry<Integer,?>>() {
        public int compare(Map.Entry<Integer, ?> o1, Map.Entry<Integer, ?> o2) {
            int i1 = o1.getKey();
            int i2 = o2.getKey();
            return (i1 > i2 ? -1 : i1 == i2 ? 0 : 1);
        }
    };

    protected static class SimpleMapEntry<K,V> implements Map.Entry<K,V> {
        SimpleMapEntry(K k, V v) {
            key = k;
            value = v;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        final K key;
        final V value;
    }
}
