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

import java.util.Map;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import buildLogWarnAnalyzer.Constant.Repository;

/**
 * Represent a warning result of a complete jdk build log.
 *
 * @author Dan Xu
 */
public class Result {

    private boolean debug = false;
    /**
     * Current repository.
     */
    private Repository curRepo;
    /**
     * Current build job.
     */
    private String curBuildJob;
    /**
     * The map from repositories to underline build jobs.
     */
    private Map<Repository, Map<String, WarningInfo>> repoMap;
    private Set<String> files;
    private int parsedLines;

    /**
     * Regular expression for javac warnings.
     */
    private static final String JAVAC_REGEX =
        "/((?:(?:\\w|-)+/)+(?:\\w|-)+\\.java):\\d+: warning: (?:\\[((\\w|-)+)\\] )?";
    /**
     * Regular expression for C warnings.
     */
    private static final String C_REGEX =
        "/((?:\\w+/)+\\w+\\.c(?:pp)?)(?:\\d|\\(|\\)|:| )+ warning( C\\d{4})?: ";

    private Pattern javacPatt, cPatt;

    public Result() {
        repoMap = new EnumMap<>(Repository.class);
        files = new LinkedHashSet<>();
        parsedLines = 0;
    }

    public void addLogFile(final String fileName) {
        files.add(fileName);
    }

    public Set<String> getFiles() {
        return files;
    }

    public void addRepo(final Repository repo) {
        if (repoMap.containsKey(repo)) {
            throw new RuntimeException("Repository, " + repo
                + ", is already processed.");
        }

        Map<String, WarningInfo> buildJobs = new LinkedHashMap<>();
        repoMap.put(repo, buildJobs);
        curRepo = repo;

        javacPatt = Pattern.compile(curRepo + JAVAC_REGEX);
        cPatt = Pattern.compile(curRepo + C_REGEX);
    }

    public void addBuildJob(final String buildJob) {
        if (curRepo == null) {
            throw new RuntimeException("No repository is being processed.");
        }

        if (curBuildJob != null) {
            verifyBuildJob();
        }

        Map<String, WarningInfo> buildJobs = repoMap.get(curRepo);
        if (buildJobs.containsKey(buildJob)) {
            throw new RuntimeException("Build job, " + buildJob
                + ", is already processed");
        }

        buildJobs.put(buildJob, new WarningInfo());
        curBuildJob = buildJob;
    }

    public void verifyBuildJob() {
        WarningInfo info = repoMap.get(curRepo).get(curBuildJob);
        int cnt = info.getJavaWarnCount();
        int jWarnSize = info.getWarnings(WarningItem.Type.JAVAC).size();
        assert (jWarnSize == cnt);
    }

    public void endCurrentRepo() {
        curRepo = null;
        curBuildJob = null;
    }

    public int getParsedLines() {
        return parsedLines;
    }

    private static final String WARN_COUNT_REGEX = "^(\\d+) warning(s)?";

    public void processLine(final String line) {
        if (curRepo == null) {
            throw new RuntimeException("No repository is being processed.");
        }

        if (curBuildJob == null) {
            throw new RuntimeException("No build job is being processed.");
        }

        WarningInfo info = repoMap.get(curRepo).get(curBuildJob);

        if (line.matches(WARN_COUNT_REGEX)) {
            String number = line.replaceAll(WARN_COUNT_REGEX, "$1");
            info.setJavaWarnCount(Integer.parseInt(number));
        } else {
            parse(info, line);
        }
        parsedLines++;
    }

    private static final Pattern WARN_PATTERN = Pattern
            .compile("\\.(j|c)(?:\\w|\\d|\\(|\\)|:| )+: warning");

    private void parse(final WarningInfo info, final String line) {
        Matcher m = WARN_PATTERN.matcher(line);
        if (m.find()) {
            char c = m.group(1).charAt(0);
            String msg = line.trim().replace("\\", "/");

            WarningItem.Type type;
            Pattern pattern;
            int ext;

            if ('j' == c) {
                type = WarningItem.Type.JAVAC;
                pattern = javacPatt;
                ext = msg.indexOf(".j");
            } else {
                type = WarningItem.Type.C;
                pattern = cPatt;
                ext = msg.indexOf(".c");
            }

            int start = msg.lastIndexOf(curRepo + "/src", ext);
            if (start == -1) {
                start = msg.lastIndexOf(curRepo + "/gensrc", ext);
            }
            if (start > 0) {
                msg = msg.substring(start);
            }

            WarningItem item = WarningItem.getWarning(msg, pattern);
            if (item != null) {
                if (curRepo == Repository.jdk
                    && curBuildJob.equals("BUILD_JDK")
                    && !item.isCategoriezed()) {
                    System.err.println("Uncategorised warning:\n" + line);
                } else {
                    info.addWarning(type, item);
                }
            } else if (debug) {
                System.err.println("Repository: " + curRepo);
                System.err.println("Build job: " + curBuildJob);
                System.err.println("Unknown " + type + " warning:\n" + line);
                System.err.println("Warning message: " + msg);
                System.err.println("Warning pattern: " + pattern);
            }
        }
    }

    Map<String, WarningInfo> getRepoWarnInfo(Repository repo) {
        Map<String, WarningInfo> buildJobs = repoMap.get(curRepo);
        return buildJobs;
    }

    WarningInfo getBuildJobWarnInfo(Repository repo, String job) {
        Map<String, WarningInfo> buildJobs = repoMap.get(curRepo);
        return buildJobs == null ? null : buildJobs.get(job);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (Map.Entry<Repository, Map<String, WarningInfo>> entry
                : repoMap.entrySet()) {
            Repository repo = entry.getKey();
            str.append(repo).append("\n");
            Map<String, WarningInfo> buildJobs = entry.getValue();
            for (Map.Entry<String, WarningInfo> job : buildJobs.entrySet()) {
                String curJob = job.getKey();
                str.append("----> ").append(curJob).append("\n");
                WarningInfo info = job.getValue();
                str.append("--------> Java Warning: ")
                   .append(info.getJavaWarnCount())
                   .append("\n");
                if (repo == Repository.jdk && curJob.equals("BUILD_JDK")) {
                    str.append(info.getStatisticInfo(true, true, true));
                } else {
                    str.append(info.getStatisticInfo(true, false, false));
                }
                str.append("--------> C Warning: ")
                   .append(info.getWarnings(WarningItem.Type.C).size())
                   .append("\n");
            }
        }

        return str.toString();
    }
}
