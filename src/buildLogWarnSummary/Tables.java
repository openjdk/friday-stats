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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static buildLogWarnSummary.Messages.*;

public final class Tables {
    public Tables() { }

    public Tables(Iterable<File> files) throws IOException {
        for (File f: files) {
            this.files.add(f);
            read(f);
        }
    }

    boolean isEmpty() {
        return files.isEmpty();
    }

    Map<String,Integer> getStatistics() {
        Map<String,Integer> stats = new LinkedHashMap<String,Integer>();
        stats.put("total lines read", lines);
        stats.put("total warnings found", warnings);
        stats.put("unique warnings found", uniqueWarnings.size());
        return stats;
    }

    Map<Message.Kind, Collection<Message>> getMessageTable() {
        return messageKindTable;
    }

    public void read(File f) throws IOException {
        System.err.println("read " + f);
        BufferedReader in = new BufferedReader(new FileReader(f));
        try {
            read(in);
        } finally {
            in.close();
        }
    }

    public void read(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            readLine(line);
            if (unmatchedMessages + unmatchedLocations > 100)
                return;
        }
    }

    void readLine(String line) {
        lines++;

        if (!msgs.isWarning(line))
            return;

        warnings++;
        uniqueWarnings.add(line);

        Message m = msgs.getMessage(line);
        if (m == null) {
            System.err.println("unmatched message: " + line);
            unmatchedMessages++;
            m = new Message(Message.Kind.UNKNOWN, line);
        }

        Message.Kind kind = m.kind;
        add(messageKindTable, kind, m);

        if (kind.hasLocation()) {
            if (m.location == null) {
                System.err.println("unmatched location: " + line);
                unmatchedLocations++;
            } else {
                add(pathTable, m.location, m);
            }
        }

        add(toolTable, kind.tool, m);
    }

    <T> void add(Map<T, Collection<Message>> map, T t, Message m) {
        Collection<Message> c = map.get(t);
        if (c == null)
            map.put(t, c = new HashSet<Message>());
        c.add(m);
    }

    int lines;
    int warnings;
    int unmatchedLocations;
    int unmatchedMessages;

    List<File> files = new ArrayList<File>();
    Set<String> uniqueWarnings = new TreeSet<String>();

    Map<Message.Kind, Collection<Message>> messageKindTable =
            new TreeMap<Message.Kind, Collection<Message>>();

    Map<Message.Location, Collection<Message>> pathTable =
            new TreeMap<Message.Location, Collection<Message>>();

    Map<Tool, Collection<Message>> toolTable =
            new TreeMap<Tool, Collection<Message>>();

    Messages msgs = new Messages();
}
