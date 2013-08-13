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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import buildLogWarnAnalyzer.WarningItem.Type;

/**
 * A collection of warnings from a build job of a jdk repository
 *
 * @author Dan Xu
 */
final class WarningInfo {

    private boolean debug = false;
    /**
     * This is the javac warning count shown in the build log.
     * It is used for verification purpose.
     */
    private int javaWarningCnt = 0;

    private List<WarningItem> javaWarnings;
    private List<WarningItem> cWarnings;

    private Map<String, Integer> kindStatistic;
    private Map<String, Integer> areaStatistic;
    private Map<String, Integer> projectStatistic;

    public WarningInfo() {
        javaWarnings = new ArrayList<>();
        cWarnings = new ArrayList<>();
    }

    public void addWarning(WarningItem.Type type, WarningItem item) {
        switch (type) {
            case JAVAC:
                javaWarnings.add(item);
                break;
            case C:
                cWarnings.add(item);
                break;
            default:
                break;
        }
    }

    public List<WarningItem> getWarnings(Type type) {
        switch (type) {
            case JAVAC:
                return javaWarnings;
            case C:
                return cWarnings;
            default:
                return null;
        }
    }

    public void setJavaWarnCount(int cnt) {
        javaWarningCnt = cnt;
    }

    public int getJavaWarnCount() {
        return javaWarningCnt;
    }

    private void getStatistic() {
        kindStatistic = new TreeMap<>();
        areaStatistic = new TreeMap<>();
        projectStatistic = new TreeMap<>();

        final String UNKNOWN = "unknown";

        for (WarningItem warning : javaWarnings) {
            // Kind
            String kind = warning.getKind();
            if (kind == null || kind.isEmpty() || kind.equals("unknown")) {
                kind = UNKNOWN;
                if (debug) {
                    System.err.println(warning.toString());
                    System.err.println("Missing Kind information");
                }
            }
            Integer curKind = kindStatistic.get(kind);
            int kindCnt = (curKind == null) ? 1 : curKind.intValue() + 1;
            kindStatistic.put(kind, Integer.valueOf(kindCnt));


            // Area
            String area = warning.getArea();
            if (area == null || area.isEmpty()) {
                area = UNKNOWN;
                if (debug) {
                    System.err.println(warning.toString());
                    System.err.println("Missing Area information");
                }
            }
            Integer curArea = areaStatistic.get(area);
            int areaCnt = (curArea == null) ? 1 : curArea.intValue() + 1;
            areaStatistic.put(area, Integer.valueOf(areaCnt));

            // Project
            String project = warning.getProject();
            if (project == null || project.isEmpty()) {
                project = UNKNOWN;
                if (debug) {
                    System.err.println(warning.toString());
                    System.err.println("Missing Project information");
                }
            }
            Integer curProject = projectStatistic.get(project);
            int projectCnt = (curProject == null)
                             ? 1
                             : curProject.intValue() + 1;
            projectStatistic.put(project, Integer.valueOf(projectCnt));
        }
    }

    public Map<String, Integer> getKindStatistic() {
        if (kindStatistic == null)
            getStatistic();
        return kindStatistic;
    }

    public Map<String, Integer> getAreaStatistic() {
        if (areaStatistic == null)
            getStatistic();
        return areaStatistic;
    }

    public Map<String, Integer> getProjectStatistic() {
        if (projectStatistic == null)
            getStatistic();
        return projectStatistic;
    }

    public String getStatisticInfo(boolean showKind, boolean showArea,
                                   boolean showProject)
    {
        StringBuilder str = new StringBuilder();

        if (showKind) {
            // Print Kind statistic
            str.append("**********Kind Statistic\n");
            for (Map.Entry<String, Integer> entry :
                    getKindStatistic().entrySet())
            {
                str.append("------------>");
                str.append(entry.getKey()).append(": ");
                str.append(entry.getValue()).append("\n");
            }
        }

        if (showArea) {
            // Print Area statistic
            str.append("**********Area Statistic\n");
            for (Map.Entry<String, Integer> entry :
                    getAreaStatistic().entrySet())
            {
                str.append("------------>");
                str.append(entry.getKey()).append(": ");
                str.append(entry.getValue()).append("\n");
            }
        }

        if (showProject) {
            // Print Area statistic
            str.append("**********Project Statistic\n");
            for (Map.Entry<String, Integer> entry :
                    getProjectStatistic().entrySet())
            {
                str.append("------------>");
                str.append(entry.getKey()).append(": ");
                str.append(entry.getValue()).append("\n");
            }
        }

        return str.toString();
    }
}
