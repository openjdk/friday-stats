/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package buildLogWarnAnalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import buildLogWarnAnalyzer.Constant.Repository;

/**
 * Read log files, and find javac warnings and native C warnings.
 *
 * @author Dan Xu
 */
public class Analyzer {

    /**
     *  Total lines of logs which have been analyzed.
     */
    private int totalLines;

    private static enum Status {
        BEFORE,
        START,
        BUILD
    }

    // Regular expressions used to find the start and end of building
    // each repository
    private static final String START_REGEX = initStartRegex();
    private static final String FINISH_SIGN = "## Finished ";
    private static final String BUILD_JOB_REGEX =
            "^Compiling \\d+ files for (BUILD_\\w+)$";

    private static String initStartRegex() {
        StringBuilder temp = new StringBuilder("^## Starting (");
        int repoCount = 0;
        for (Repository repo: Repository.values()) {
            temp.append(repoCount++ == 0 ? repo.name() : "|" + repo.name());
        }

        temp.append(")$");

        if (repoCount == 0) {
            throw new RuntimeException(
                    "No repositories are going to be analyzed.");
        }
        return temp.toString();
    }

    private Status curStatus;
    private Result result;

    public final Result analyze(final Iterable<File> logs) throws IOException {
        result = new Result();
        curStatus = Status.BEFORE;
        totalLines = 0;

        for (File log : logs) {
            result.addLogFile(log.getName());
            read(log);
        }
        return result;
    }

    private void read(final File log) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(log))) {
            String line = null;
            String finishRegex = null;
            while ((line = in.readLine()) != null) {
                switch (curStatus) {
                    case BEFORE:
                        if (line.matches(START_REGEX)) {
                            String repo = line.replaceAll(START_REGEX, "$1");
                            result.addRepo(Repository.valueOf(repo));
                            finishRegex = "^" + FINISH_SIGN + repo
                                    + " \\(build time \\d\\d:\\d\\d:\\d\\d\\)$";
                            curStatus = Status.START;
                        }
                        break;
                    case START:
                        if (line.matches(BUILD_JOB_REGEX)) {
                            String job = line.replaceAll(BUILD_JOB_REGEX, "$1");
                            result.addBuildJob(job);
                            curStatus = Status.BUILD;
                        } else if (line.matches(finishRegex)) {
                            curStatus = Status.BEFORE;
                            result.endCurrentRepo();
                        }
                        break;
                    case BUILD:
                        if (line.matches(finishRegex)) {
                            curStatus = Status.BEFORE;
                            result.endCurrentRepo();
                        } else if (line.matches(BUILD_JOB_REGEX)) {
                            String job = line.replaceAll(BUILD_JOB_REGEX, "$1");
                            result.addBuildJob(job);
                        } else {
                            result.processLine(line);
                        }
                        break;
                    default:
                        break;
                }
                totalLines++;
            }
        }
    }

    public final int getTotalLines() {
        return totalLines;
    }
}
