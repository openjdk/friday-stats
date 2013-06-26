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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Utility to analyze the warnings and other diagnostics generated during a
 * JDK build.
 *
 * @author jjg
 */
public class Main {
    static abstract class Option implements Comparable<Option> {
        static void processAll(Option[] options, String... args) throws Fault {
            for (Iterator<String> iter = Arrays.asList(args).iterator(); iter.hasNext(); ) {
                String arg = iter.next();
                process(options, arg, iter);
            }
        }

        private static void process(Option[] options, String arg, Iterator<String> rest) throws Fault {
            for (Option o: options) {
                if (o.matches(arg)) {
                    if (o.hasArg) {
                        if (rest.hasNext())
                            o.process(arg, rest);
                        else
                            throw new Fault("no value given for " + arg);
                    } else
                        o.process(arg, rest);
                    return;
                }
            }
            throw new Fault("unrecognized option " + arg);
        }
        Option(String name, boolean hasArg, String help) {
            this.name = name;
            this.hasArg = hasArg;
            this.help = help;
        }

        boolean matches(String opt) {
            return name.equals(opt);
        }

        abstract void process(String opt, Iterator<String> args) throws Fault;

        @Override
        public int compareTo(Option other) {
            return name.compareTo(other.name);
        }

        @Override
        public String toString() {
            return "Option[" + name +"]";
        }

        final String name;
        final boolean hasArg;
        final String help;
    }

    Option[] options = {
        new Option("-help", false, "show this help message") {
            @Override
            void process(String opt, Iterator<String> args) {
                Main.this.help = true;
            }
        },
        new Option("-usage", false, "show this help message") {
            @Override
            void process(String opt, Iterator<String> args) {
                Main.this.help = true;
            }
        },
        new Option("-h", false, "generate HTML report (defaults from output file extn") {
            @Override
            void process(String opt, Iterator<String> args) {
                format = Format.HTML;
            }
        },
        new Option("-l", false, "show warnings categorized by location") {
            @Override
            void process(String opt, Iterator<String> args) {
                showLocations = true;
            }
        },
        new Option("-o", true, "output file") {
            @Override
            void process(String opt, Iterator<String> args) {
                outFile = new File(args.next());
            }
        },
        new Option("-k", false, "show warnings categorized by kind") {
            @Override
            void process(String opt, Iterator<String> args) {
                showKinds = true;
            }
        },
        new Option("-a", false, "show warnings categorized by area") {
            @Override
            void process(String opt, Iterator<String> args) {
                showAreas = true;
            }
        },
        new Option("-t", false, "show warnings categorized by tool") {
            @Override
            void process(String opt, Iterator<String> args) {
                showTools = true;
            }
        },
        new Option("-title", true, "title for report") {
            @Override
            void process(String opt, Iterator<String> args) {
                title = args.next();
            }
        },
        new Option("-r", true, "reference log file(s)") {
            @Override
            void process(String opt, Iterator<String> args) {
                refFiles.add(new File(args.next()));
            }
        },
        new Option("<files>", false, "log files to be analyzed") {
            @Override
            boolean matches(String opt) {
                return (!opt.startsWith("-"));
            }
            @Override
            void process(String opt, Iterator<String> args) {
                inFiles.add(new File(opt));
                while (args.hasNext())
                    inFiles.add(new File(args.next()));
            }
        }
    };

    enum Format { HTML, SIMPLE };

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new Main().run(args);
        } catch (Fault e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace(System.err);
        }
    }

    /**
     * API entry point.
     * @param args command line args
     * @return true if operation completed successfully
     * @throws IOException if an IO error occurs during execution
     */
    public void run(String... args) throws IOException, Fault {
        PrintWriter out = new PrintWriter(System.out);
        try {
            run(out, args);
        } finally {
            out.flush();
        }
    }

    public void run(PrintWriter out, String... args) throws IOException, Fault {
        Option.processAll(options, args);

        if (help) {
            showHelp(out);
            if (inFiles.isEmpty())
                return;
        }

        if (!showLocations && !showKinds && !showTools && refFiles.isEmpty()) {
            showKinds = true;
            showLocations = true;
            showTools = true;
            showAreas = true;
        }

        Tables ref = new Tables(refFiles);
        Tables t = new Tables(inFiles);


        Reporter r = createReporter();
        if (outFile != null)
            r.setOutput(outFile);
        if (title != null)
            r.setTitle(title);
        r.setShowLocations(showLocations);
        r.setShowKinds(showKinds);
        r.setShowTools(showTools);
        r.setShowAreas(showAreas);
        r.setReference(ref);
        r.report(t);
    }

    Reporter createReporter() {
        Format f = format;
        if (f == null && outFile != null) {
            String fileName = outFile.getName();
            int lastDot = fileName.lastIndexOf('.');
            String extn = fileName.substring(lastDot);
            if (extn.equals(".html"))
                f = Format.HTML;
        }
        if (f == null)
            f = Format.SIMPLE;

        switch (f) {
            case HTML:
                return new HTMLReporter();
            default:
                return new SimpleReporter();
        }
    }

    void showHelp(PrintWriter out) {
        out.println(Main.class.getPackage().getName() + ":");
        out.println("  Analyze the warnings and other diagnostics generated during a JDK build.");
        out.println();
        out.println("Usage:");
        out.println("  java -jar " + findJar(Main.class).getName() + " [options...] files...");
        out.println();
        out.println("Options:");
        List<Option> opts = new ArrayList<>(Arrays.asList(options));
        Collections.sort(opts);
        for (Option o: opts) {
            String s = (o.hasArg ? o.name + " <arg>" : o.name);
            out.println(String.format("  %-18s %s", s, o.help));
        }
    }

    private File findJar(Class<?> c) {
        try {
            String className = c.getName().replace(".", "/") + ".class";
            // use URI to avoid encoding issues, e.g. Program%20Files
            URI uri = getClass().getClassLoader().getResource(className).toURI();
            if (uri.getScheme().equals("jar")) {
                String ssp = uri.getRawSchemeSpecificPart();
                int sep = ssp.lastIndexOf("!");
                uri = new URI(ssp.substring(0, sep));
                if (uri.getScheme().equals("file"))
                    return new File(uri.getPath());
            }
        } catch (URISyntaxException ignore) {
            ignore.printStackTrace(System.err);
        }

        return null;
    }

    boolean help;
    File outFile;
    Format format;
    String title;
    boolean showLocations;
    boolean showKinds;
    boolean showTools;
    boolean showAreas;
    List<File> inFiles = new ArrayList<>();
    List<File> refFiles = new ArrayList<>();
}
