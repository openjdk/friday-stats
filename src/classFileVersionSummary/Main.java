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

package classFileVersionSummary;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jsqrdg
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
        out.println("  Report on the various class file versions found in class files.");
        out.println();
        out.println("Usage:");
        out.println("  java -jar ClassFileVersionSummary.jar options ... files...");
        out.println();
        out.println("Options:");
        out.println("  -htmlDir dir       Specifies directory for HTML report (no HTML if not provided)");
        out.println("  -xml file          Specifies file for XML report (no XML if not provided)");
        out.println("  -title string      Plain text title for report");
        out.println("  -h -help --help    Show this message");
        out.println();
        out.println("files                One or more jar files, directories or .class files");
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

    public boolean run(PrintWriter out, String... args) throws IOException {
        processArgs(args);
        if (errors > 0)
            return false;

        if (help) {
            usage(out);
            return true;
        }

        Table t = new Table();

        for (File f: files) {
            t.scan(f);
        }

        int total = 0;
        for (Version v: Version.values())
            total += t.map.get(v).size();

        if (total == 0)
            out.println("no classes found");
        else {
            for (Version v: Version.values()) {
                List<Main.Entry> list = t.map.get(v);
                out.println(String.format("%-4s %-6s %6d %3d%%",
                        v.name().replace("V", "").replace("_", "."),
                        v.jdk,
                        list.size(),
                        Math.round(list.size() * 100 / total)));
            }
            out.println(String.format("total       %6d", total));
        }

        if (htmlDir != null)
            new HTMLReportWriter().write(htmlDir, t, title, files);

        if (xmlFile != null)
            new XMLReportWriter().write(xmlFile, t);

        return (errors == 0);
    }

    /**
     * Process command-line arguments.
     * @param args the arguments to be processed
     */
    void processArgs(String... args) {
        if (args.length == 0)
            help = true;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-htmlDir") && i + 1 < args.length)
                htmlDir = new File(args[++i]);
            else if (arg.equals("-xml") && i + 1 < args.length)
                xmlFile = new File(args[++i]);
            else if (arg.equals("-title") && i + 1 < args.length)
                title = args[++i];
            else if (arg.matches("-h|-help|--help"))
                help = true;
            else if (arg.startsWith("-"))
                error("Unrecognized option: " + arg);
            else {
                files.add(new File(arg));
            }
        }

        if (help)
            return;

        if (files.isEmpty())
            error("no files specified");
    }

    boolean help = false;
    List<File> files = new ArrayList<>();
    File htmlDir;
    File xmlFile;
    String title;

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

    class Table {
        final Map<Version, List<Entry>> map;

        Table() {
            map = new HashMap<>();
            for (Version v: Version.values())
                map.put(v, new ArrayList<Entry>());
        }

        void scan(File file) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File f: files)
                        scan(f);
                }
            } else if (file.getName().endsWith(".class")) {
                try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
                    addEntry(new FileEntry(file), in);
                } catch (IOException e) {
                    error("problem reading " + file + ": " + e);
                }
            } else if (file.getName().endsWith(".jar")) {
                try (JarFile jf = new JarFile(file)) {
                    Enumeration<JarEntry> entries = jf.entries();
                    while (entries .hasMoreElements()) {
                        JarEntry je = entries .nextElement();
                        if (je.getName().endsWith(".class")) {
                            try (DataInputStream in = new DataInputStream(jf.getInputStream(je))) {
                                addEntry(new JarFileEntry(file, je.getName()), in);
                            } catch (IOException e) {
                                error("problem reading " + file + "!" + je.getName() + ": " + e);
                            }
                        }

                    }
                } catch (IOException e) {
                    error("problem reading " + file + ": " + e);
                }
            }

        }

        void addEntry(Entry e, InputStream in) throws IOException {
            DataInputStream data = new DataInputStream(in);
            int magic = data.readInt();
            if (magic == 0xcafebabe) {
                int minor = data.readUnsignedShort();
                int major = data.readUnsignedShort();
                Version v = Version.of(major, minor);
                if (v != null) {
                    map.get(v).add(e);
                } else {
                    error("unknown version for " + e + ": " + major + "." + minor);
                }
            } else {
                error("file does not start with 0xcafebabe: " + e + " " + Integer.toHexString(magic));
            }
        }
    }

    enum Version {
        V45_3("1.1"),
        V46("1.2"),
        V47("1.3"),
        V48("1.4"),
        V49("1.5"),
        V50("1.6"),
        V51("1.7"),
        V52("1.8"),
        V53("1.9");

        final String jdk;

        Version(String jdk) {
            this.jdk = jdk;
        }

        static Version of(int major, int minor) {
            if (minor != 0)
                return (major == 45) ? V45_3 : null;
            else switch (major) {
                case 46: return V46;
                case 47: return V47;
                case 48: return V48;
                case 49: return V49;
                case 50: return V50;
                case 51: return V51;
                case 52: return V52;
                case 53: return V53;
                default:  return null;
            }
        }
    }

    static abstract class Entry {
        abstract File getFile();
    }

    static class FileEntry extends Entry {
        final File file;
        FileEntry(File file) {
            this.file = file;
        }

        File getFile() {
            return file;
        }

        @Override
        public String toString() {
            return file.toString();
        }
    }

    static class JarFileEntry extends Entry {
        File jarFile;
        String entryName;
        JarFileEntry(File jarFile, String entryName) {
            this.jarFile = jarFile;
            this.entryName = entryName;
        }

        File getFile() {
            return jarFile;
        }

        @Override
        public String toString() {
            return jarFile + "!" + entryName;
        }
    }

    static class HTMLReportWriter {
        static final String DOCTYPE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
                + "\"http://www.w3.org/TR/html4/loose.dtd\">";

        void write(File htmlDir, Table t, String title, List<File> files) throws IOException {
            File index = new File(htmlDir, "index.html");
            try (PrintWriter out = new PrintWriter(new FileWriter(index))) {
                writeHead(out, "Class file version summary" + (title == null ? "" : ": " + title));

                writeFiles(out, files);
                writeMainSummaryTable(out, htmlDir, t);
                writeFileSummaryTable(out, t);

                writeTail(out);
            }
        }

        void writeMainSummaryTable(PrintWriter out, File htmlDir, Table t) throws IOException {
            int total = 0;
            for (Version v: Version.values())
                total += t.map.get(v).size();

            out.println("<table class=\"summary\" summary=\"number of classes for each class file version\">");
            out.println("<thead>");
            out.println("<tr><th>Class file version<th>JDK version<th>Number of classes<th>% of total</tr>");
            out.println("</thead>");
            out.println("<tbody>");
            int row = 0;
            for (Version v: Version.values()) {
                List<Main.Entry> list = t.map.get(v);
                out.print("<tr class=\"" + (++row % 2 == 0 ? "even" : "odd") + "\">"
                        + "<td>" + v.name().replace("V", "").replace("_", ".")
                        + "<td>" + escape(v.jdk)
                        + "<td>");
                if (list.isEmpty())
                    out.print("0");
                else {
                    File f = writeList(htmlDir, t, v);
                    out.println("<a href=\"" + f.getName() + "\">" + list.size() + "</a>");
                }
                out.println("<td>" + Math.round(list.size() * 100 / total));
            }
            out.println("<tr class=\"total\"><th colspan=\"2\">Total<td>" + total + "<td>");
            out.println("</tbody>");
            out.println("</table>");
        }

        void writeFiles(PrintWriter out, List<File> files) {
            final int MAX_FILES_TO_SHOW = 50;
            out.print("<p><b>Files:</b><span style=\"font-family:monospace\"> ");
            out.println("(" + files.size() + ")");
            int count = 0;
            for (File f: files) {
                out.print(escape(f.getPath()));
                out.print(" ");
                if (++count == MAX_FILES_TO_SHOW && count < files.size()) {
                    out.print("<i> ... and " + (files.size() - count) + " more</i>");
                    break;
                }
            }
        }

        void writeFileSummaryTable(PrintWriter out, Table t) throws IOException {
            Map<File, Map<Version,Integer>> imap = invert(t.map);
            out.println("<table class=\"vrules\" summary=\"summary of class file versions in each file\">");
            out.println("<thead>");
            out.println("<tr><th>File");
            for (Version v: Version.values())
                out.println("<th>" + v.name().replace("V", "").replace("_", "."));
            out.println("</thead>");
            out.println("<tbody>");
            int row = 0;
            for (Map.Entry<File, Map<Version,Integer>> e: imap.entrySet()) {
                out.print("<tr class=\"" + (++row % 2 == 0 ? "even" : "odd") + "\">"
                        + "<td class=\"name\">" + e.getKey());
                Map<Version,Integer> values = e.getValue();
                for (Version v: Version.values()) {
                    Integer val = values.get(v);
                    out.print("<td>");
                    if (val != null) out.print(val);
                }
            }
            out.println("</tbody>");
            out.println("</table>");
        }

        Map<File, Map<Version, Integer>> invert(Map<Version, List<Entry>> map) {
            Map<File, Map<Version, Integer>> imap = new TreeMap<>();
            for (Map.Entry<Version, List<Entry>> e: map.entrySet()) {
                Version v = e.getKey();
                for (Entry item: e.getValue()) {
                    File f = item.getFile();
                    Map<Version,Integer> m = imap.get(f);
                    if (m == null)
                        imap.put(f, m = new EnumMap<>(Version.class));
                    Integer n = m.get(v);
                    m.put(v, n == null ? 1 : n + 1);
                }
            }
            return imap;
        }

        File writeList(File htmlDir, Table t, Version v) throws IOException {
            Pattern p = Pattern.compile("(.*[\\\\/])(.+)");
            Map<String, Set<String>> map = new TreeMap<>();
            for (Main.Entry e: t.map.get(v)) {
                String full = e.toString();
                Matcher m = p.matcher(full);
                String dir, base;
                if (m.matches()) {
                    dir = m.group(1);
                    base = m.group(2);
                } else {
                    dir = "";
                    base = full;
                }
                Set<String> set = map.get(dir);
                if (set == null)
                    map.put(dir, set = new TreeSet<>());
                set.add(base);
            }

            File f = new File(htmlDir, v.name().toLowerCase() + ".html");
            String title = "Classes using version " + v.name().toLowerCase().replace("_", ".") + " (JDK " + v.jdk + ")";
            try (PrintWriter out = new PrintWriter(new FileWriter(f))) {
                writeHead(out, title);

                out.println("<table summary=\"table of classes grouped by directory name\">");
                out.println("<thead>");
                out.println("<tr><th>directory<th>name</tr>");
                out.println("</thead>");
                out.println("<tbody>");
                int row = 0;
                for (Map.Entry<String, Set<String>> e: map.entrySet()) {

                    out.println("<tr class=\"" + (++row % 2 == 0 ? "even" : "odd") + "\"><td>");
                    String dir = e.getKey();
                    int jarSep = dir.indexOf(".jar!");
                    if (jarSep == -1)
                        out.println(escape(dir));
                    else {
                        out.println(escape(dir.substring(0, jarSep + 5)));
                        out.println("<br><i>" + escape(dir.substring(jarSep + 5)));
                    }
                    String sep = "<td>";
                    for (String s: e.getValue()) {
                        out.print(sep + escape(s));
                        sep = ", ";
                    }
                    out.println();
                }
                out.println("</tbody>");
                out.println("</table>");

                writeTail(out);
            }
            return f;
        }

        void writeHead(PrintWriter out, String title) {
            out.println(DOCTYPE);
            out.println("<html>");
            out.println("<head>");
            out.println("<title>" + escape(title) + "</title>");
            out.println("<style type=\"text/css\">");
            out.println("h1 { font-size: 18pt }");
            out.println("table { border: 1px solid black; border-collapse:collapse; margin-top:18px; }");
            out.println("table.summary td { text-align: right }");
            out.println("table.summary td.name { text-align: left }");
            out.println("table.vrules td { border-left: 1px solid black }");
            out.println("td { font-size: 10pt }");
            out.println("td, th { padding: 3px 6px }");
            out.println("th { background-color: lightgray; border: 1px solid black; }");
            out.println("tr.odd { background-color:white }");
            out.println("tr.even { background-color:#f0f0f0 }");
            out.println("tr.total { background-color:white; border-top: 1px solid black }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>" + escape(title) + "</h1>");

        }

        void writeTail(PrintWriter out) {
            out.println("</body>");
            out.println("</html>");
        }

        String escape(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&lt;");
        }
    }

    static class XMLReportWriter {
        void write(File xmlFile, Table t) throws IOException {
            try (PrintWriter out = new PrintWriter(new FileWriter(xmlFile))) {
                out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
                out.println("<classFileVersionSummary>");
                for (Version v: Version.values()) {
                    List<Main.Entry> list = t.map.get(v);
                    out.println("<version name=\"" + v.name().replace("V", "").replace("_", ".") + "\">"
                            + list.size() + "</version>");
                }
                out.println("</classFileversionSummary>");
            }
        }
    }
}
