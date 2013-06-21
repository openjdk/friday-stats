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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;

import static buildLogWarnSummary.HTMLWriter.*;
import static buildLogWarnSummary.Reporter.TableType.*;
import static buildLogWarnSummary.Messages.*;

/**
 *
 * @author jjg
 */
public class HTMLReporter extends Reporter {
    @Override
    protected void startReport() throws IOException {
        if (outFile == null)
            outFile = new File(System.getProperty("user.dir"));
        if (outFile.isDirectory())
            outFile = new File(outFile, "report.html");
        Writer w = new BufferedWriter(new FileWriter(outFile));
        out = new HTMLWriter(w);
        out.startTag(HTML);
        out.startTag(HEAD);
        out.startTag(TITLE);
        out.write("JDK Build Warnings");
        out.endTag(TITLE);
        out.startTag(STYLE);
        out.writeAttr("type", "text/css");
        out.write("");
        out.newLine();
        out.write("table { border: 1px solid grey; }");
        out.newLine();
        out.write("tr.odd { background-color: white; }");
        out.newLine();
        out.write("tr.even { background-color: #f0f0f0; }");
        out.newLine();
        out.endTag(STYLE);
        out.endTag(HEAD);
        out.startTag(BODY);
        out.startTag(H1);
        out.write("JDK Build Warnings");
        out.endTag(H1);
        if (title != null) {
            out.startTag(H2);
            out.write(title);
            out.endTag(H2);
        }
//        if (tables.firstLine != null) {
//            out.startTag(H3);
//            out.write(tables.firstLine);
//            out.endTag(H3);
//        }
        out.startTag(HR);
        out.writeAttr(ALIGN, LEFT);
        out.writeAttr(WIDTH, "50%");
        writeIndex();
        out.startTag(HR);
        out.writeAttr(ALIGN, LEFT);
        out.writeAttr(WIDTH, "50%");
    }

    @Override
    protected void endReport() throws IOException {
        out.startTag(HR);
        out.startTag(SPAN);
        out.writeStyleAttr("font-size: smaller");
        out.write("Generated on " + (new Date()));
        out.endTag(SPAN);
        out.endTag(BODY);
        out.endTag(HTML);
        out.close();
    }

    void writeIndex() throws IOException {
        out.startTag(UL);
        if (!refTables.isEmpty()) {
            writeIndexEntryStart("Comparison against reference results");
            out.startTag(UL);
            writeIndexEntry(REF_DELTA_LOCNS);
            out.endTag(UL);
            writeIndexEntryEnd();
        }

        if (showLocations) {
            writeIndexEntryStart("Warnings categorized by location");
            out.startTag(UL);
            writeIndexEntry(LOCN_COUNTS_ALPHA);
            writeIndexEntry(LOCN_COUNTS_FREQ);
            writeIndexEntry(LOCN_DIR_COUNTS_ALPHA);
            writeIndexEntry(LOCN_DIR_COUNTS_FREQ);
            writeIndexEntry(LOCN_EXTN_COUNTS_ALPHA);
            writeIndexEntry(LOCN_EXTN_COUNTS_FREQ);
            Collection<Message> unknownLocns = tables.pathTable.get(Message.Location.UNKNOWN);
            if (unknownLocns != null)
                writeIndexEntry(LOCN_UNKNOWN);
            out.endTag(UL);
            writeIndexEntryEnd();
        }

        if (showTools) {
            writeIndexEntryStart("Warnings categorized by tool");
            out.startTag(UL);
            writeIndexEntry(TOOL_COUNTS_ALPHA);
            writeIndexEntry(TOOL_COUNTS_FREQ);
            out.endTag(UL);
            writeIndexEntryEnd();
        }

        if (showKinds) {
            writeIndexEntryStart("Warnings categorized by kind");
            out.startTag(UL);
            writeIndexEntry(KIND_COUNTS_ALPHA);
            writeIndexEntry(KIND_COUNTS_FREQ);
            Collection<Message> unknownKinds = tables.messageKindTable.get(Message.Kind.UNKNOWN);
            if (unknownKinds != null)
                writeIndexEntry(KIND_UNKNOWN);
            out.endTag(UL);
            writeIndexEntryEnd();
        }
        out.endTag(UL);
    }

    void writeIndexEntry(String text) throws IOException {
        writeIndexEntryStart(text);
        writeIndexEntryEnd();
    }

    void writeIndexEntryStart(String text) throws IOException {
        out.startTag(LI);
        out.write(text);
    }

    void writeIndexEntry(TableType type) throws IOException {
        writeIndexEntryStart(type);
        writeIndexEntryEnd();
    }

    void writeIndexEntryStart(TableType type) throws IOException {
        out.startTag(LI);
        out.startTag(A);
        out.writeAttr(HREF, "#" + type);
        out.write(type.title);
        out.endTag(A);
    }

    void writeIndexEntryEnd() throws IOException {
        out.endTag(LI);
    }

//    @Override
//    protected void writeTable(Collection<? extends Map.Entry<?,?>> entries) throws IOException {
//        if (entries.isEmpty()) {
//            out.write("No entries");
//        } else {
//            out.startTag(TABLE);
//            int count = 0;
//            for (Map.Entry<?,?> e: entries) {
//                out.startTag(TR);
//                out.writeAttr("class", ((count++ % 2 == 0) ? "even" : "odd"));
//                writeTableCell(e.getKey());
//                writeTableCell(e.getValue());
//                out.endTag(TR);
//            }
//            out.endTag(TABLE);
//        }
//    }

    @Override
    protected void writeTableRows(Collection<? extends Collection<?>> rows) throws IOException {
        if (rows.isEmpty()) {
            out.write("No entries");
        } else {
            out.startTag(TABLE);
            int count = 0;
            for (Collection<?> row: rows) {
                out.startTag(TR);
                out.writeAttr("class", ((count++ % 2 == 0) ? "even" : "odd"));
                for (Object item: row)
                    writeTableCell(item);
                out.endTag(TR);
            }
            out.endTag(TABLE);
        }
    }

    void writeTableCell(Object o) throws IOException {
        out.startTag(TD);
        if (o instanceof Number)
            out.writeAttr(ALIGN, RIGHT);
        out.write(String.valueOf(o));
        out.endTag(TD);
    }

    @Override
    protected void writeTableHead(String head) throws IOException {
        out.startTag(H3);
        out.write(head);
        out.endTag(H3);
    }

    @Override
    protected void writeTableHead(TableType type) throws IOException {
        out.startTag(H3);
        out.startTag(A);
        out.writeAttr(NAME, type.toString());
        out.endTag(A);
        out.write(type.title);
        out.endTag(H3);
    }

    @Override
    protected <T> void writeList(Collection<T> list) throws IOException {
        out.startTag(UL);
        for (T t: list) {
            out.startTag(LI);
            out.write(String.valueOf(t));
            out.endTag(LI);
        }
        out.endTag(UL);
    }

    HTMLWriter out;
}
