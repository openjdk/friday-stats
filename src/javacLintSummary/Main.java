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

package javacLintSummary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.tools.doclint.DocLint;
import com.sun.tools.doclint.Messages;
import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.util.JCDiagnostic;

/**
 * Simple program that prints a summary of how many of each type of warning
 * are generated in files and packages.
 *
 * Usage:
 *  java -jar javacLintSummary.jar options files-or-packages
 *
 * Options:
 *  -htmlDir dir        specifies directory for HTML (no HTML if not provided)
 *  -xml file           specifies XML output file (no XML if not provided)
 *  -title string       plain text title for report
 *  -bootclasspath, -classpath, -sourcepath
 *                      all as for javac
 *  -Xlint -Xlint:opts -Xdoclint and -Xdoclint:opts
 *                      all as for javac; default is -Xlint
 *
 * Files-or-packages
 *  Files are recognized by ending in .java
 *  Packages can be given as p.q or p.q.**.  They are expanded using the
 *  value of -sourcepath. p.q expands to the set of compilation units in
 *  package p.q; p.q.** expands to the set of compilation units in package
 *  p.q and all subpackages.  Take care to quote the wildcard form when
 *  necessary, e.g. on a shell command line.
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
        out.println("  Prints a summary of how many of each type of lint or doclint");
        out.println("  message are generated in the given files and packages.");
        out.println();
        out.println("Usage:");
        out.println("  java -jar javacLintSummary.jar options files-or-packages");
        out.println();
        out.println("Options:");
        out.println("  -bootclasspath, -classpath, -sourcepath");
        out.println("                     all as for javac");
        out.println("  -Xlint -Xlint:opts -Xdoclint and -Xdoclint:opts ");
        out.println("                     all as for javac; default is -Xlint");
        out.println("  -htmlDir dir       specifies directory for HTML report (no HTML if not provided)");
        out.println("  -xml file          specifies file for XML report (no XML if not provided)");
        out.println("  -title string      plain text title for report");
        out.println();
        out.println("Files-or-packages");
        out.println("  Files are recognized by ending in .java");
        out.println("  Packages can be given as p.q or p.q.**.  They are expanded using the");
        out.println("  value of -sourcepath. p.q expands to the set of compilation units in");
        out.println("  package p.q; p.q.** expands to the set of compilation units in package");
        out.println("  p.q and all subpackages.  Take care to quote the wildcard form when");
        out.println("  necessary, e.g. on a shell command line.");
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

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null);
        if (bootclasspath != null)
            fm.setLocation(StandardLocation.PLATFORM_CLASS_PATH, pathToFiles(bootclasspath));
        if (classpath != null)
            fm.setLocation(StandardLocation.CLASS_PATH, pathToFiles(classpath));
        if (sourcepath != null)
            fm.setLocation(StandardLocation.SOURCE_PATH, pathToFiles(sourcepath));
        List<? extends JavaFileObject> files = getFiles(fm, items);
//        fm.setLocation(StandardLocation.SOURCE_PATH, null);
        List<String> opts = new ArrayList<>();
        if (lintOpts.isEmpty())
            opts.add("-Xlint:all");
        else
            opts.addAll(lintOpts);
        opts.addAll(Arrays.asList("-Xmaxerrs", "9999"));
        opts.addAll(Arrays.asList("-Xmaxwarns", "9999"));
        Table t =  new Table(fm, files);
        out.println("Analyzing " + files.size() + " files");
        JavacTask task = (JavacTask) javac.getTask(null, fm, t, opts, null, files);
        fixupDocLint(task, opts);
        task.analyze();

        new SimpleTableWriter().write(title, t, out);
        if (htmlDir != null)
            new HtmlTableWriter().write(title, t, htmlDir, args);
        if (xmlFile != null)
            new XMLTableWriter().write(title, t, xmlFile, args);
        return true;
    }

    void fixupDocLint(JavacTask t, List<String> javacOpts) {
        boolean doclint = false;
        List<String> docLintOpts = new ArrayList<>();
        for (String opt: javacOpts) {
            if (opt.startsWith("-Xdoclint")) {
                if (!opt.equals("-Xdoclint:none"))
                    doclint = true;
                docLintOpts.add(opt.replace("-Xdoclint", "-Xmsgs"));
            }
        }

        if (doclint) {
            // standard doclet normally generates H1, H2
            docLintOpts.add(DocLint.XIMPLICIT_HEADERS + "2");
            DocLint l = new DocLint();
            l.init(t, docLintOpts.toArray(new String[docLintOpts.size()]), true);
        }
    }

    /**
     * Expand the set of command line items into a list of JavaFileObjects.
     * @param fm the file manager to use
     * @param items a list of command line items to be expanded
     * @return a list of JavaFileObjects
     * @throws IOException if an error occurs
     */
    List<JavaFileObject> getFiles(StandardJavaFileManager fm, List<String> items)
            throws IOException {
        List<JavaFileObject> files = new ArrayList<>();
        for (String item: items) {
            if (item.endsWith(".java"))
                addAll(files, fm.getJavaFileObjects(item));
            else {
                boolean recursive = false;
                if (item.endsWith(".**")) {
                    item = item.substring(0, item.length() - 3);
                    recursive=true;
                }
                addAll(files, fm.list(StandardLocation.SOURCE_PATH,
                        item,
                        EnumSet.of(JavaFileObject.Kind.SOURCE),
                        recursive));
            }
        }
        //System.err.println("files: " + files.size() + " " + files);
        return files;
    }

    /**
     * Utility method to add all members of an iterable to a Collection.
     * @param <T> The type of the each item
     * @param dest The collection to which to add the items
     * @param items The source of items to be added to the collection
     */
    static <T> void addAll(Collection<T> dest, Iterable<? extends T> items) {
        for (T item: items)
            dest.add(item);
    }

    /**
     * Convert a path option to a list of files, ignoring entries
     * which do not exist or cannot be read.
     * @param path the path value to be split
     * @return a list of files
     */
    List<File> pathToFiles(String path) {
        List<File> files = new ArrayList<>();
        for (String p: path.split(File.pathSeparator)) {
            File f = new File(p);
            if (f.canRead())
                files.add(f);
        }
        return files;
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
            if (arg.equals("-bootclasspath") && i + 1 < args.length)
                bootclasspath = args[++i];
            else if (arg.equals("-classpath") && i + 1 < args.length)
                classpath = args[++i];
            else if (arg.equals("-sourcepath") && i + 1 < args.length)
                sourcepath = args[++i];
            else if (arg.startsWith("-Xlint") || arg.startsWith("-Xdoclint"))
                lintOpts.add(arg);
            else if (arg.equals("-html") && i + 1 < args.length)
                htmlDir = new File(args[++i]);
            else if (arg.equals("-xml") && i + 1 < args.length)
                xmlFile = new File(args[++i]);
            else if (arg.equals("-title") && i + 1 < args.length)
                title = args[++i];
            else if (arg.matches("-h|-help|--help"))
                help = true;
            else if (arg.startsWith("-"))
                error("Unrecognized option: " + arg);
            else if (arg.endsWith(".java") && new File(arg).exists())
                items.add(arg);
            else if (SourceVersion.isName(arg)
                    || arg.endsWith(".**") && SourceVersion.isName(arg.substring(0, arg.length() - 3)))
                items.add(arg);
            else
                error("Unrecognized argument: " + arg);
        }

        if (htmlDir != null) {
            if (htmlDir.exists()) {
                if (!(htmlDir.isDirectory() && htmlDir.canWrite()))
                    error("bad HTML directory");
            } else {
                if (!htmlDir.mkdirs())
                    error("Could not create HTML output directory");
            }
        }

        if (title == null)
            title = "Lint Report";
    }

    /**
     * Record an error message.
     * @param msg the message
     */
    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    boolean help;
    String bootclasspath;
    String classpath;
    String sourcepath;
    Set<String> lintOpts = new LinkedHashSet<>();
    List<String> items = new ArrayList<>();
    File htmlDir;
    File xmlFile;
    String title;

    /** The number of errors that have been reported. */
    int errors;

    /**
     * Main data structure recording the number of each type of warning
     * encountered in each package.
     */
    static class Table implements DiagnosticListener<JavaFileObject> {

        static class Row {
            Map<String, Cell> counts = new HashMap<>();
            void inc(String group, JCDiagnostic d) {
                Cell c = counts.get(group);
                if (c == null) counts.put(group, c = new Cell());
                c.count++;
                if (d != null) {
                    if (c.diags == null) c.diags = new ArrayList<>();
                    c.diags.add(d);
                }
            }
            int getCount(String group) {
                Cell c = counts.get(group);
                return c == null ? 0 : c.count;
            }
            List<JCDiagnostic> getDiags(String group) {
                Cell c = counts.get(group);
                return c == null ? null : c.diags;
            }
        }

        static class Cell {
            int count;
            List<JCDiagnostic> diags;
        }

        JavaFileManager fm;
        Set<JavaFileObject> files;
        Set<String> headings = new TreeSet<>();
        Map<String, Row> map = new TreeMap<>();
        Row totals = new Row();
        Map<Diagnostic.Kind, Integer> kindCounts
                = new EnumMap<>(Diagnostic.Kind.class);
        DocLintManager docLintManager = new DocLintManager();

        Table(JavaFileManager fm, Collection<? extends JavaFileObject> files) {
            this.fm = fm;
            this.files = new HashSet<>(files);
        }

        void inc(String pkg, String group, JCDiagnostic d) {
            Row row = map.get(pkg);
            if (row == null)
                map.put(pkg, row = new Row());
            headings.add(group);
            row.inc(group, d);
            totals.inc(group, d);
        }

        @Override
        public void report(Diagnostic<? extends JavaFileObject> d) {
            if (d.getCode().equals("compiler.warn.sun.proprietary"))
                return;

            if (!files.contains(d.getSource()))
                return;

            Integer kc = kindCounts.get(d.getKind());
            kindCounts.put(d.getKind(), kc == null ? 1 : kc + 1);

            JCDiagnostic jd = ((ClientCodeWrapper.DiagnosticSourceUnwrapper) d).d;
            String group;
            if (jd.getLintCategory() != null) {
                group = jd.getLintCategory().toString().toLowerCase();
            } else if (docLintManager.isDocLintMessage(jd)) {
                group = docLintManager.getGroup(jd);
            } else {
                switch (d.getKind()) {
                    case ERROR:
                        System.err.println(d.getSource().getName() + ": " + d.getMessage(null));
                        return;
                    case NOTE:
                        return;
                }
                group = "default";
            }

            JavaFileObject f = d.getSource();
            String binaryName = fm.inferBinaryName(StandardLocation.SOURCE_PATH, f);
            int lastDot = binaryName.lastIndexOf(".");
            String pkgName = (lastDot == -1) ? "" : binaryName.substring(0, lastDot);
            //System.err.println(d.getCode() + " " + binaryName + " " + pkgName + " " + d.getMessage(null));
            inc(pkgName, group, jd);
        }
    }

    /**
     * Write out results in plain text, for easy reporting to the console.
     */
    static class SimpleTableWriter {
        Table t;

        void write(String title, Table table, PrintWriter out) {
            t = table;

            if (title != null)
                System.err.println(title);

            if (t.map.isEmpty()) {
                System.err.println("No warnings found");
                return;
            }

            writeHeadings(out);
            for (Map.Entry<String, Table.Row> e: t.map.entrySet()) {
                writeRow(out, e.getKey(), e.getValue());
            }
            writeRow(out, "(total)", t.totals);

            out.println();
            String sep = "";
            for (Diagnostic.Kind k: Diagnostic.Kind.values()) {
                Integer count = t.kindCounts.get(k);
                if (count != null) {
                    out.print(sep + k.toString().toLowerCase() + ": " + count);
                    sep = ", ";
                }
            }
            out.println();
        }

        void writeHeadings(PrintWriter out) {
            int col1w = 0;
            for (String pkg: t.map.keySet())
                col1w = Math.max(col1w, pkg.length());
            col1f = "%-" + col1w + "s";
            out.print(String.format(col1f, ""));
            for (String w: t.headings) {
                String head = w.length() <= 8 ? w : w.substring(0, 8);
                out.print(String.format(" %8s", head));
            }
            out.print(String.format(" %8s", "(total)"));
            out.println();
        }

        void writeRow(PrintWriter out, String pkg, Table.Row row) {
            int total = 0;
            out.print(String.format(col1f, pkg));
            for (String w: t.headings) {
                int v = row.getCount(w);
                out.print(String.format(" %8d", v));
                total += v;
            }
            out.print(String.format(" %8d", total));
            out.println();
        }

        String col1f;
    }

    /**
     * Write out results in HTML, for easy browsing.
     */
    static class HtmlTableWriter {
        File htmlDir;
        Table table;

        void write(String title, Table table, File htmlDir, String[] args) throws IOException {
            this.table = table;
            this.htmlDir = htmlDir;

            File indexFile = new File(htmlDir, "index.html");
            try (PrintWriter index = new PrintWriter(new FileWriter(indexFile))) {
                writeFile(title, index, args);
            }
        }

        void writeFile(String title, PrintWriter out, String[] args) {
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>" + escape(title) + "</title>");
            out.println("<style type=\"text/css\">");
            out.println("table { border: 1px solid black; border-collapse:collapse }");
            out.println("td { font-family: monospace; padding: 3px 6px }");
            out.println("td.num { text-align:right }");
            out.println("td.pkg { border-right: solid black 1px }");
            out.println("td.total { text-align:right; border-left: 1px solid black }");
            out.println("th { background-color: lightgray; border: 1px solid black; padding: 3px 6px }");
            out.println("tr.odd { background-color:white }");
            out.println("tr.even { background-color:#f0f0f0 }");
            out.println("tr.total { background-color:white; border-top: 1px solid black }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>" + escape(title) + "</h1>");
            out.print("<p><b>Args:</b><span style=\"font-family:monospace\"> ");
            for (int i = 0; i < Math.min(args.length, 32); i++) {
                out.print(escape(args[i]));
                out.print(" ");
            }
            if (args.length > 32)
                out.print("...");
            out.println();
            out.println("</span></p>");
            if (table.map.isEmpty()) {
                out.println("<span style=\"padding:3px; background-color: palegreen; color: green; font-size:larger\">");
                out.println("No warnings found");
                out.println("</span>");
            } else {
                out.println("<table>");
                writeHeadings(out);
                for (Map.Entry<String, Table.Row> e: table.map.entrySet()) {
                    writeRow(out, e.getKey(), e.getValue());
                }
                writeRow(out, "(total)", table.totals);
                out.println("</table>");
                out.println("<p>");
                String sep = "";
                for (Diagnostic.Kind k: Diagnostic.Kind.values()) {
                    Integer count = table.kindCounts.get(k);
                    if (count != null) {
                        out.print(sep + k.toString().toLowerCase() + ": " + count);
                        sep = ", ";
                    }
                }
                out.println();
                out.println("</p>");
            }
            out.println("</body>");
            out.println("</html>");

        }

        void writeHeadings(PrintWriter out) {
            out.print("<tr class=\"head\">");
            out.print("<th></th>");
            for (String w: table.headings) {
                out.print("<th>" + w + "</th>");
            }
            out.print("<th style=\"border-left: 1px solid black\">(total)</th>");
            out.println("</tr>");
        }

        void writeRow(PrintWriter out, String pkg, Table.Row row) {
            String c = pkg.equals("(total)") ? "total" : (rowNo++ % 2 == 0) ? "even" : "odd";
            out.print("<tr class=\"" + c + "\">");
            int total = 0;
            out.print("<td class=\"pkg\">" + pkg + "</td>");
            for (String g: table.headings) {
                int v = row.getCount(g);
                out.print("<td class=\"num\">");
                if (v != 0) {
                    File list = writeDiags(pkg, g, row.getDiags(g));
                    if (list == null)
                        out.print(v);
                    else
                        out.print("<a href=\"" + list.getName() + "\">" + v + "</a>");
                }
                out.print("</td>");
                total += v;
            }
            out.print("<td class=\"total\">" + total + "</td>");
            out.println();
            out.println("</tr>");
        }

        int rowNo = 0;

        File writeDiags(String pkg, String group, List<JCDiagnostic> diags) {
            if (diags == null)
                return null;

            File txt = new File(htmlDir, pkg.replaceAll("[^A-Za-z0-9_$]+", "_") + "-" + group + ".txt");
            try (PrintWriter out = new PrintWriter(new FileWriter(txt))) {
                for (JCDiagnostic d : diags)
                    out.println(d);
            } catch (IOException e) {
                return null;
            }
            return txt;
        }

        String escape(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&lt;");
        }
    }

    /**
     * Write out results in XML, for use by Hudson/Jenkins Plot Plugin.
     */
    static class XMLTableWriter {
        void write(String title, Table table, File xmlFile, String[] args) throws IOException {
            try (PrintWriter out = new PrintWriter(new FileWriter(xmlFile))) {
                out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
                out.println("<javacLintSummary>");
                out.println("<title>" + escape(title) + "</table>");
                out.println("<args>");
                for (String arg: args) {
                    out.println("<arg>" + escape(arg) + "</arg>");
                }
                out.println("</args>");
                for (Map.Entry<String, Table.Row> e: table.map.entrySet()) {
                    String pkg = e.getKey();
                    Table.Row row = e.getValue();
                    out.println("<package name=\"" + pkg + "\">");
                    for (String g: table.headings) {
                        out.println("<group name=\"" + g + "\">" + row.getCount(g) + "</group>");
                    }
                    out.println("</package>");
                }
                out.println("</javacLintSummary>");
            }
        }

        String escape(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    /**
     * This class reverse engineers the doclint message group from the message.
     * You cannot (currently) get at the original unlocalized info, so we have
     * to pattern match against the messages in the resource bundles. Uugh.
     */
    static class DocLintManager {
        private static final String ACCESSIBILITY = Messages.Group.ACCESSIBILITY.name().toLowerCase();
        private static final String HTML = Messages.Group.HTML.name().toLowerCase();
        private static final String MISSING = Messages.Group.MISSING.name().toLowerCase();
        private static final String REFERENCE = Messages.Group.REFERENCE.name().toLowerCase();
        private static final String SYNTAX = Messages.Group.SYNTAX.name().toLowerCase();

        private static final String DOCLINT_MAIN = "doclint:main"; // should not happen

        final Map<String,String> strings = new HashMap<>();
        final Map<Pattern,String> patterns = new HashMap<>();

        DocLintManager() {
            // Currently, it is not possible to easily identify the group from
            // the name of the diag key, so for now, we use manually constructed tables.

            ResourceBundle rb = ResourceBundle.getBundle("com.sun.tools.doclint.resources.doclint");
            if (rb == null) {
                System.err.println("Warning: Cannot locate doclint resource bundle");
            } else {
                add(rb, "dc.anchor.already.defined", HTML);
                add(rb, "dc.anchor.value.missing", HTML);
                add(rb, "dc.attr.lacks.value", HTML);
                add(rb, "dc.attr.not.number", HTML);
                add(rb, "dc.attr.obsolete", ACCESSIBILITY);
                add(rb, "dc.attr.obsolete.use.css", ACCESSIBILITY);
                add(rb, "dc.attr.repeated", HTML);
                add(rb, "dc.attr.unknown", HTML);
                add(rb, "dc.bad.option", DOCLINT_MAIN);
                add(rb, "dc.bad.value.for.option", DOCLINT_MAIN);
                add(rb, "dc.empty", SYNTAX);
                add(rb, "dc.entity.invalid", HTML);
                add(rb, "dc.exception.not.thrown", REFERENCE);
                add(rb, "dc.invalid.anchor", HTML);
                add(rb, "dc.invalid.param", REFERENCE);
                add(rb, "dc.invalid.return", REFERENCE);
                add(rb, "dc.invalid.throws", REFERENCE);
                add(rb, "dc.invalid.uri", HTML);
                add(rb, "dc.no.alt.attr.for.image", ACCESSIBILITY);
                add(rb, "dc.no.summary.or.caption.for.table", ACCESSIBILITY);
                add(rb, "dc.main.ioerror", DOCLINT_MAIN);
                add(rb, "dc.main.no.files.given", DOCLINT_MAIN);
                add(rb, "dc.main.usage", DOCLINT_MAIN);
                add(rb, "dc.missing.comment", MISSING);
                add(rb, "dc.missing.param", MISSING);
                add(rb, "dc.missing.return", MISSING);
                add(rb, "dc.missing.throws", MISSING);
                add(rb, "dc.param.name.not.found", REFERENCE);
                add(rb, "dc.ref.not.found", REFERENCE);
                add(rb, "dc.tag.code.within.code", HTML);
                add(rb, "dc.tag.empty", HTML);
                add(rb, "dc.tag.end.not.permitted", HTML);
                add(rb, "dc.tag.end.unexpected", HTML);
                add(rb, "dc.tag.header.sequence.1", ACCESSIBILITY);
                add(rb, "dc.tag.header.sequence.2", ACCESSIBILITY);
                add(rb, "dc.tag.nested.not.allowed", HTML);
                add(rb, "dc.tag.not.allowed.here", HTML);
                add(rb, "dc.tag.not.allowed", HTML);
                add(rb, "dc.tag.not.allowed.inline.element", HTML);
                add(rb, "dc.tag.not.allowed.inline.tag", HTML);
                add(rb, "dc.tag.not.allowed.inline.other", HTML);
                add(rb, "dc.tag.not.closed", HTML);
                add(rb, "dc.tag.p.in.pre", HTML);
                add(rb, "dc.tag.self.closing", HTML);
                add(rb, "dc.tag.start.unmatched", HTML);
                add(rb, "dc.tag.unknown", HTML);
                add(rb, "dc.text.not.allowed", HTML);
            }


            rb = ResourceBundle.getBundle("com.sun.tools.javac.resources.compiler");
            if (rb == null) {
                System.err.println("Warning: Cannot locate javac resource bundle");
            } else {
                add(rb, "compiler.err.dc.bad.entity", SYNTAX);
                add(rb, "compiler.err.dc.bad.gt", SYNTAX);
                add(rb, "compiler.err.dc.bad.inline.tag", SYNTAX);
                add(rb, "compiler.err.dc.identifier.expected", SYNTAX);
                add(rb, "compiler.err.dc.malformed.html", SYNTAX);
                add(rb, "compiler.err.dc.missing.semicolon", SYNTAX);
                add(rb, "compiler.err.dc.no.content", SYNTAX);
                add(rb, "compiler.err.dc.no.tag.name", SYNTAX);
                add(rb, "compiler.err.dc.gt.expected", SYNTAX);
                add(rb, "compiler.err.dc.ref.bad.parens", SYNTAX);
                add(rb, "compiler.err.dc.ref.syntax.error", SYNTAX);
                add(rb, "compiler.err.dc.ref.unexpected.input", SYNTAX);
                add(rb, "compiler.err.dc.unexpected.content", SYNTAX);
                add(rb, "compiler.err.dc.unterminated.inline.tag", SYNTAX);
                add(rb, "compiler.err.dc.unterminated.signature", SYNTAX);
                add(rb, "compiler.err.dc.unterminated.string", SYNTAX);
            }
        }

        private void add(ResourceBundle rb, String code, String group) {
            try {
                String msg = rb.getString(code).replace("''", "'");
                if (msg.matches(".*\\{[0-9]\\}.*"))
                    patterns.put(Pattern.compile(msg.replaceAll("\\{[0-9]\\}", ".*")), group);
                else
                    strings.put(msg, group);
            } catch (MissingResourceException e) {
                System.err.println("Warning: cannot find doclint message " + code);
            }
        }

        boolean isDocLintMessage(JCDiagnostic d) {
            return d.getCode().endsWith(".proc.messager");
        }

        String getGroup(JCDiagnostic d) {
            Object[] args = d.getArgs();
            if (args.length == 1 && args[0] instanceof String) {
                String arg = (String) args[0];
                String g = strings.get(arg);
                if (g != null)
                    return g;
                for (Map.Entry<Pattern,String> e: patterns.entrySet()) {
                    if (e.getKey().matcher(arg).matches())
                        return e.getValue();
                }
            }
            System.err.println("can't analyze " + d);
            return "unknown";
        }
    }
}
