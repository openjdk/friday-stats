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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static buildLogWarnAnalyzer.Constant.locAreaMap;
import static buildLogWarnAnalyzer.Constant.areaProjectMap;


/**
 * The information for one javac warning or native C warning
 *
 * @author Dan Xu
 */
final class WarningItem {

    private String msg;
    private String location;
    private String project;
    private String area;
    private String kind;

    public enum Type {
        JAVAC,
        C;
    }

    private WarningItem() {}

    public static WarningItem getWarning(String msg, Pattern pattern) {
        WarningItem instance = null;

        Matcher m = pattern.matcher(msg);
        if (m.find()) {
            instance = new WarningItem();
            instance.msg = msg;
            instance.location = m.group(1);
            if (m.groupCount() > 1) {
                instance.kind = m.group(2);
            }
            if (instance.kind == null) {
                instance.kind = "unknown";
            }
            instance.area = findArea(instance.location);
            instance.project = areaProjectMap.get(instance.area);
        }

        return instance;
    }

    private static String removePrefix(String loc) {
        assert (loc != null);

        String prefixes[] = {"gensrc/", "gensrc_no_srczip/"};
        for (String prefix : prefixes) {
            if (loc.startsWith(prefix)) {
                return loc.substring(prefix.length());
            }
        }

        String platforms = "(?:share|solaris|windows|macosx)";
        Pattern prePattn = Pattern.compile(
                "^(?:src/(\\w+/)?" + platforms + "/(classes|native|demo|sample))/");
        Matcher m = prePattn.matcher(loc);
        if (m.find()) {
            String prefix = m.group();
            // Do nothing if loc starts like "src/share/native/common"
            if (m.group(1) == null && "native".equals(m.group(2))
                    && loc.startsWith(prefix + "common"))
                return loc;
            else if ("demo".equals(m.group(2)))
                return "demo";
            else if ("sample".equals(m.group(2)))
                return "sample";
            else
                return loc.substring(prefix.length());
        }

        return loc;
    }
    private static String findArea(final String loc) {
        String key = removePrefix(loc);
        String area = locAreaMap.get(key);

        while (area == null) {
            int end = key.lastIndexOf("/");
            if (end == -1) {
                break;
            }
            key = key.substring(0, end);
            area = locAreaMap.get(key);
        }
        return area;
    }

    public boolean isCategoriezed() {
        return (area != null && project != null);
    }

    public String getKind() {
        return kind;
    }

    public String getArea() {
        return area;
    }

    public String getProject() {
        return project;
    }

    public String getMessage() {
        return msg;
    }

    @Override
    public String toString() {
        return msg + "\n" + "kind: " + kind + "\n" + "location: " + location
            + "\n" + "area: " + area + "\n" + "project: " + project + "\n";
    }
}
