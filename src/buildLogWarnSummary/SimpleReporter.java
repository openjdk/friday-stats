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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;

/**
 *
 * @author jjg
 */
public class SimpleReporter extends Reporter {
    @Override
    protected void startReport() throws IOException {
        if (outFile == null)
            out = new PrintWriter(new OutputStreamWriter(System.out));
        else {
            if (outFile.isDirectory())
                outFile = new File(outFile, "report.txt");
            out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
        }
    }

    @Override
    protected void endReport() throws IOException {
        out.flush();
        if (outFile != null)
            out.close();
    }

    @Override
    protected void writeTableHead(String head) throws IOException {
        out.println(head);
    }

    @Override
    protected void writeTableHead(TableType type) throws IOException {
        out.println(type.title);
    }

//    @Override
//    protected void writeTable(Collection<? extends Map.Entry<?,?>> entries) {
//        if (entries.isEmpty())
//            out.println("(No entries)");
//        else {
//            for (Map.Entry<?,?> e: entries) {
//                out.println(e.getKey() + ": " + e.getValue());
//            }
//        }
//    }

    @Override
    protected void writeTableRows(Collection<? extends Collection<?>> rows) {
        if (rows.isEmpty())
            out.println("(No entries)");
        else {
            for (Collection<?> row: rows) {
                String sep = "";
                for (Object item: row) {
                    out.print(sep);
                    out.print(item);
                    if (sep.isEmpty())
                        sep = ": ";
                    else if (sep.equals(": "))
                        sep = ", ";
                }
                out.println();
            }
        }
    }

    @Override
    protected <T> void writeList(Collection<T> list) throws IOException {
        for (T t: list)
            out.println(t);
    }

    PrintWriter out;

}
