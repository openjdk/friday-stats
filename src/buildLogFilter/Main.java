/*
 * Copyright (c) 2011,2013, Oracle and/or its affiliates. All rights reserved.
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

package buildLogFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;


/**
 * Simple program that filters out the informative/chatty messages from a
 * standard (new)JDK build log, leaving any diagnostic messages that need to be
 * addressed.  A line count summary is provided.
 *
 * Usage:
 *  java -jar BuildLogFilter.jar options logfile
 *
 * Options:
 *  -q --quiet      Do not show the filtered log file; just show the
 *                  line count stats
 *  -xml file       Specify a file for an XML report
 *
 * @author jjg
 */
public class Main {

    /**
     * Command-line entry point.
     * @param args command line args
     */
    public static void main(String[] args) {
        try {
            Main m = new Main();
            boolean ok = m.run(args);
            if (!ok)
                System.exit(1);
        } catch (IOException e) {
            System.err.println("IO error: " + e);
            System.exit(2);
        }
    }

    public void usage(PrintWriter out) {
        out.println(Main.class.getPackage().getName() + ":");
        out.println("  Filters out the informative/chatty messages from a standard" );
        out.println("  (new) JDK build log, leaving any diagnostic messages that need");
        out.println("  to be addressed.");
        out.println();
        out.println("Usage:");
        out.println("  java -jar BuildLogFilter.jar options logfile");
        out.println();
        out.println("Options:");
        out.println("  -h -help --help   Show this message");
        out.println("  -q --quiet        Do not show the filtered log file; just show the ");
        out.println("                    line count stats");
        out.println("  -xml file         Specify file for XML report.");
        out.println();
        out.println("logfile             logfile from JDK build at default log level");
    }

    /**
     * API entry point.
     * @param args command line args
     * @return true if operation completed successfully
     * @throws IOException if an IO error occurs during execution
     */
    public boolean run(String... args) throws IOException {
        PrintWriter out = new PrintWriter(System.out);
        try {
            return run(out, args);
        } finally {
            out.flush();
        }
    }

    /**
     * API entry point.
     * @param args command line args
     * @return true if operation completed successfully
     * @throws IOException if an IO error occurs during execution
     */
    public boolean run(PrintWriter out, String... args) throws IOException {

        processArgs(args);
        if (errors > 0)
            return false;

        if (help) {
            usage(out);
            return true;
        }

        int expected = 0, total = 0;

        try (BufferedReader r = new BufferedReader(new FileReader(inFile))) {
            String line;
            while ((line = r.readLine()) != null) {
                total++;
                if (isExpected(line))
                    expected++;
                else {
                    if (!quiet)
                        out.println(line);
                }
            }
        }

        if (total == 0) {
            out.println("Log is empty");
        } else {
            int expected_percent = Math.round((expected * 100.f) / total);
            out.println(String.format("Expected/info lines   %5d (%3d%%)",
                    expected, expected_percent));
            out.println(String.format("Unexpected/diag lines %5d (%3d%%)",
                    (total - expected), (100 - expected_percent)));
            out.println(String.format("(TOTAL)               %5d",
                    total));
        }

        if (xmlFile != null)
            new XMLReportWriter().write(xmlFile, expected, total);

        return true;
    }

    boolean isExpected(String line) {
        if (line.isEmpty())
            return true;
        for (Pattern p: expectedPatterns) {
            if (p.matcher(line).matches())
                return true;
        }
        return false;
    }

    private static final Pattern[] expectedPatterns = {
        Pattern.compile("^(Assembling|Compiling|Copying|Creating|Generating|Importing|Linking|Making|Running|Updating|Using|Verifying) .*"),
        Pattern.compile("^(Aliases|Cache|Classes|INFO): .*"),
        Pattern.compile("^All done\\..*"),
        Pattern.compile("^(## |---*).*"),
        Pattern.compile("^(Start|End) +[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9][0-9]:[0-9][0-9]:[0-9][0-9]"),
        Pattern.compile("^[0-9][0-9]:[0-9][0-9]:[0-9][0-9] [A-Za-z0-9]+"),
    };

    /**
     * Process command-line arguments.
     * @param args the arguments to be processed
     */
    void processArgs(String... args) {
        if (args.length == 0)
            help = true;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-q") || arg.equals("--quiet"))
                quiet = true;
            else if (arg.equals("-xml") && i + 1 < args.length)
                xmlFile = new File(args[++i]);
            else if (arg.matches("-h|-help|--help"))
                help = true;
            else if (arg.startsWith("-"))
                error("Unrecognized option: " + arg);
            else {
                if (inFile != null)
                    error("Unexpected argument");
                inFile = new File(arg);
            }
        }

        if (help)
            return;

        if (inFile == null)
            error("no build log specified");
        else if (!inFile.exists())
            error("can't find " + inFile);
        else if (!inFile.canRead())
            error("can't read " + inFile);
    }

    boolean help = false;
    boolean quiet = false;
    File inFile;
    File xmlFile;

    /**
     * Record an error message.
     * @param msg the message
     */
    void error(String msg) {
        System.err.println(msg);
        errors++;
    }


    /** The number of errors that have been reported. */
    int errors;

    static class XMLReportWriter {
        void write(File xmlFile, int expected, int total) throws IOException {
            try (PrintWriter out = new PrintWriter(new FileWriter(xmlFile))) {
                out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
                out.println("<buildLogFilter>");
                out.println("<expected>" + expected + "</expected>");
                out.println("<unexpected>" + (total - expected) + "</unexpected>");
                out.println("</buildLogFilter>");
            }
        }
    }
}
