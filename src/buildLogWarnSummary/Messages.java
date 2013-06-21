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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jjg
 */
public class Messages {

    static class Message {
        static class Kind implements Comparable<Kind> {
            Tool tool;
            boolean hasLocation;
            Pattern msgPattern;

            Kind(Tool tool, boolean hasLocation, Pattern msgPattern) {
                this.tool = tool;
                this.hasLocation = hasLocation;
                this.msgPattern = msgPattern;
            }

            boolean hasLocation() {
                return hasLocation;
            }

            boolean matches(String line) {
                return msgPattern.matcher(line).matches();
            }

            public int compareTo(Kind o) {
                int result = tool.name.compareTo(o.tool.name);
                if (result == 0)
                    return msgPattern.pattern().compareTo(o.msgPattern.pattern());
                else
                    return result;
            }

            @Override
            public String toString() {
                String p = msgPattern.pattern();
                int start = (p.startsWith(".*") ? 2 : 0);
                int end = p.length() - (p.endsWith(".*") ? 2 : 0);
                return p.substring(start, end);
            }

            static final Kind UNKNOWN = new Kind(new Tool("unknown"), false, Pattern.compile(""));
        }

        static class Location implements Comparable<Location> {
            String file;
            String path;

            Location(String file, String path) {
                this.file = file;
                this.path = path;
            }

            String getExtension() {
                int dot = file.lastIndexOf(".");
                return (dot == -1 ? "" : file.substring(dot + 1));
            }

            String getPathDirectory() {
                int sep = path.replace("\\", "/").lastIndexOf("/");
                return sep == -1 ? path : path.substring(0, sep);
            }

            @Override
            public int compareTo(Location o) {
                return path.compareTo(o.path);
            }

            @Override
            public String toString() {
                return path;
            }

            static final Location UNKNOWN = new Location("", "");
        }

        Message(Kind kind, String line) {
            this.kind = kind;
            this.line = line;
            String l = line.trim().replace("\\", "/");
            Matcher ddm;
            while ((ddm = dotdot.matcher(l)).matches())
                l = ddm.group(1) + ddm.group(2);

            if (kind.hasLocation) {
                for (Pattern p: kind.tool.locnPatterns) {
                    Matcher m = p.matcher(l);
                    if (m.matches()) {
                        this.location = new Location(m.group(1), m.group(2));
                        return;
                    }
                }
            }
        }

        public String toString() {
            return kind + ": " + line;
        }

        private static Pattern dotdot = Pattern.compile("(.*/)[^/.]+/\\.\\./(.*)");

        String line;
        Location location;
        Kind kind;
    }

    boolean isWarning(String line) {
        if (!warningPattern.matcher(line).matches())
            return false;

        for (Pattern p: notWarningPatterns) {
            if (p.matcher(line).matches())
                return false;
        }

        return true;
    }

    Pattern warningPattern = Pattern.compile("(?i).*\\bwarning\\b.*");
    Pattern[] notWarningPatterns = {
        Pattern.compile(" *(\\[[^ ]+\\])? *[0-9]+ warning(s?|\\(s\\))"),
        Pattern.compile(" *(\\[[^ ]+\\])? *[0-9]+ Warning\\(s\\) detected\\."),
        Pattern.compile(".*warning[^. /\\\\]*\\.(gif|png).*"),
        Pattern.compile("Dialog Warning for language .* built"),
        Pattern.compile(".* - [0-9]+ error\\(s\\), [0-9]+ warning\\(s\\)"),
        Pattern.compile(".*com.sun.java.util.jar.pack.Utils\\$Pack200Logger warning.*")
    };

    Message getMessage(String line) {
        for (Tool t: tools) {
            for (Message.Kind k: t.kinds) {
                if (k.matches(line)) {
                    return new Message(k, line);
                }
            }
        }
        return null;
    }

    static class Tool implements Comparable<Tool> {
        final String name;
        final List<Pattern> locnPatterns = new ArrayList<Pattern>();
        final List<Message.Kind> kinds = new ArrayList<Message.Kind>();

        protected Tool(String name) {
            this.name = name;
        }

        @Override
        public int compareTo(Tool other) {
            return name.compareTo(other.name);
        }

        @Override
        public String toString() {
            return name;
        }

        protected void location(String regex) {
            locnPatterns.add(Pattern.compile(regex));
        }

        protected void kind(boolean hasLocation, String regex) {
            kinds.add(new Message.Kind(this, hasLocation,
                    Pattern.compile(".*" + regex + ".*")));
        }
    }

    Tool[] tools = {
        new Tool("adlc") {
            {
                kind(false, "ASSERT is undefined, assertions disabled\\.");
            }
        },

        new Tool("cc") {
            {
                location(".*\"([^\"]+/src/share/native/([^ ]+\\.(?:h|c|cpp)))\".*");
                location(".*\"([^\"]+/src/solaris/native/([^ ]+\\.(?:h|c|cpp)))\".*");
                location(".*\"([^\"]+/src/([^ ]+\\.(?:h|c|cpp)))\".*");
                location("^\"(([^ ]+\\.(?:h|c|cpp)))\".*");

                kind(false, "-xarch=.* is deprecated, use -m64 to create 64-bit programs");
                kind(true, ".* hides the same name in an outer scope.");
                kind(true, ".* hides the virtual function .*\\.");
                kind(true, "Assigning .* to .*\\.");
                kind(true, "Attempt to redefine .* without using #undef.");
                kind(true, "Comparing different enum types \".*\" and \".*\".");
                kind(true, "Conversion of 64 bit type value to .* causes truncation");
                kind(true, "Formal argument .* of type .* in call to .* is being passed .*\\.");
                kind(true, "Formal argument func of type .* is being passed .*\\.");
                kind(true, "Function has no return statement : .*");
                kind(true, "Identifier expected instead of \".*\".");
                kind(true, "Printf conversion specification \".*\" and argument type \".*\" are incompatible.");
                kind(true, "String literal converted to char\\* in assignment.");
                kind(true, "String literal converted to char\\* in formal argument .* in call to .*\\.");
                kind(true, "String literal converted to char\\* in initialization\\.");
                kind(true, "The option .* was seen twice\\.");
                kind(true, "argument .* is incompatible with prototype");
                kind(true, "asm\\(\\) statement disables optimization within function");
                kind(true, "assignment type mismatch");
                kind(true, "constant promoted to unsigned long");
                kind(true, "declaration can not follow a statement");
                kind(true, "empty translation unit");
                kind(true, "end-of-loop code not reached");
                kind(false, "illegal option .*");
                kind(true, "implicit function declaration: .*");
                kind(true, "implicitly declaring function to return int: .*");
                kind(true, "improper pointer/integer combination: .*");
                kind(true, "integer overflow detected: op \".*\"");
                kind(true, "keyword \".*\" is being redefined\\.");
                kind(true, "macro redefined: .*");
                kind(true, "non-constant initializer: .*");
                kind(true, "pointer arithmetic overflow detected");
                kind(true, "statement not reached");
                kind(true, "storage class after type is obsolescent");
                kind(true, "static function called but not defined: .*");
                kind(true, "wvarhidemem: .* hides .*");
            }
        },

        new Tool("cl") {
            {
                kind(false, "ignoring unknown option '.*'");
                kind(false, "option '.*' has been deprecated and will be removed in a future release");
                kind(false, "overriding '.*' with '.*'");
                kind(false, "use 'RTC1' instead of 'GZ'");
            }
        },

        new Tool("createSymbols") {
            {
                kind(false, "package .* does not exist");
            }
        },

        new Tool("gcc") {
            {
                location(".*([^ ]+/src/closed/share/classes/([^ ]+\\.(?:h|c|cpp))):.*");
                location(".*([^ ]+/src/closed/share/native/([^ ]+\\.(?:h|c|cpp))):.*");
                location(".*([^ ]+/src/solaris/([^ ]+\\.(?:h|c|cpp))):.*");
                location(".*([^ ]+/src/share/native/([^ ]+\\.(?:h|c|cpp))):.*");
                location(".*([^ ]+/src/share/demo/([^ ]+\\.(?:h|c|cpp))):.*");
                location(".*([^ ]+/src/share/([^ ]+\\.(?:h|c|cpp))):.*");
                location(".*([^ ]+/src/common/unix/native/([^ ]+\\.(?:h|c|cpp))):.*");
                location(".*([^ ]+/src/([^ ]+\\.(?:h|c|cpp))):.*");
                location("^(([^ ]+\\.(?:h|c|cpp))):.*");

                kind(false, "this is the location of the previous definition");
                kind(true, ".*\\[-Wclobbered\\]");
                kind(true, ".*\\[-Wdeprecated-declarations\\]");
                kind(true, ".*\\[-Wformat\\]");
                kind(true, ".*\\[-Wformat-security\\]");
                kind(true, ".*\\[-Wimplicit-function-declaration\\]");
                kind(true, ".*\\[-Wimplicit-int\\]");
                kind(true, ".*\\[-Wmissing-field-initializers\\]");
                kind(true, ".*\\[-Wpointer-sign\\]");
                kind(true, ".*\\[-Wreorder\\]");
                kind(true, ".*\\[-Wswitch\\]");
                kind(true, ".*\\[-Wuninitialized\\]");
                kind(true, ".*\\[-Wunused-result\\]");
                kind(true, "'.*' is deprecated \\(declared at .*\\)");
                kind(true, "\".*\" redefined");
                kind(true, "'.*' initialized and declared 'extern'");
                kind(true, "'.*' may be used uninitialized in this function");
                kind(true, "'.*' used but never defined");
                kind(true, "'return' with no value, in function returning non-void");
                kind(true, "'static' is not at beginning of declaration");
                kind(true, "\\(near initialization for '.*'\\)");
                kind(true, "\\(perhaps the '.*' macro was used incorrectly\\)");
                kind(true, "\\Q\"/*\"\\E within comment");
                kind(true, "`.*' initialized and declared `extern'");
                kind(true, "`.*' might be used uninitialized in this function");
                kind(true, "`class .*' has virtual functions but non-virtual destructor");
                kind(true, "argument '.*' might be clobbered by 'longjmp' or 'vfork'");
                kind(true, "array subscript has type '.*'");
                kind(true, "assignment discards qualifiers from pointer target type");
                kind(true, "assignment from incompatible pointer type");
                kind(true, "assignment makes integer from pointer without a cast");
                kind(true, "assuming signed overflow does not occur when assuming that \\Q(X + c) < X\\E is always false");
                kind(true, "cast from pointer to integer of different size");
                kind(true, "cast to pointer from integer of different size");
                kind(true, "comparison between pointer and integer");
                kind(true, "comparison between signed and unsigned");
                kind(true, "comparison is always false due to limited range of data type");
                kind(true, "comparison of distinct pointer types lacks a cast");
                kind(true, "comparison of unsigned expression < 0 is always false");
                kind(true, "comparison of unsigned expression >= 0 is always true");
                kind(true, "comparison with string literal results in unspecified behavior");
                kind(true, "conflicting types for built-in function '.*'");
                kind(true, "control reaches end of non-void function");
                kind(true, "deprecated conversion from string constant to 'char\\*'");
                kind(true, "dereferencing type-punned pointer will break strict-aliasing rules");
                kind(true, "enumeration value '.*' not handled in switch");
                kind(true, "extra tokens at end of #endif directive");
                kind(true, "format '.*' expects type '.*', but argument .* has type '.*'");
                kind(true, "ignoring #pragma ident");
                kind(true, "implicit declaration of function '.*'");
                kind(true, "incompatible implicit declaration of built-in function '.*'");
                kind(true, "incompatible implicit declaration of built-in function '.*'");
                kind(true, "initialization makes integer from pointer without a cast");
                kind(true, "integer constant is too large for 'long' type");
                kind(true, "invalid access to non-static data member '.*' of NULL object");
                kind(true, "likely type-punning may break strict-aliasing rules");
                kind(true, "missing braces around initializer");
                kind(true, "missing initializer");
                kind(true, "missing sentinel in function call");
                kind(true, "operation on '.*' may be undefined");
                kind(true, "passing argument .* of '.*' discards qualifiers from pointer target type");
                kind(true, "passing argument .* of '.*' from incompatible pointer type");
                kind(true, "passing argument .* of '.*' makes integer from pointer without a cast");
                kind(true, "passing argument .* of '.*' makes pointer from integer without a cast");
                kind(true, "pointer targets in initialization differ in signedness");
                kind(true, "pointer targets in passing argument .* of '.*' differ in signedness");
                kind(true, "return makes integer from pointer without a cast");
                kind(true, "signed and unsigned type in conditional expression");
                kind(true, "suggest a space before ';' or explicit braces around empty body in 'while' statement");
                kind(true, "suggest braces around empty body in 'do' statement");
                kind(true, "suggest braces around empty body in an 'if' statement");
                kind(true, "type defaults to 'int' in declaration of '.*'");
                kind(true, "universal character names are only valid in C\\+\\+ and C99");
                kind(true, "unmappable character for encoding ASCII");
                kind(true, "unnamed struct/union that defines no instances");
                kind(true, "unsigned int format, long unsigned int arg \\(arg .*\\)");
                kind(true, "variable '.*' might be clobbered by 'longjmp' or 'vfork'");
                kind(true, "incompatible implicit declaration of built-in function ‘.*’ \\[enabled by default\\]");
                kind(true, "passing argument .* of ‘.*’ discards ‘.*’ qualifier from pointer target type \\[enabled by default\\]");
            }
        },

        new Tool("HotSpot") {
            {
                kind(false, "Java HotSpot\\(TM\\) .* VM warning: increase O_BUFLEN in ostream.hpp -- output truncated");
            }
        },

        new Tool("ISCmdBld") {
            {
                kind(false, "The property ALLUSERS is defined in the property table.  This may result is a setup that is always installed per-machine when it has been advertised as a per-user install.");
                kind(false, "One or more of the project's components contain \\.NET properties that require the \\.NET Framework.");
                kind(false, "A condition for feature '.*' may possibly set the InstallLevel for this feature to zero at runtime. If this feature gets enabled on install, you must author similar logic to ensure that it is also enabled in maintenance mode, otherwise in an upgrade the feature will be ignored.");
                kind(false, "The Custom Action .* in the InstallExecuteSequence table is run from an installed file. To run the custom action successfully, you may need to have a condition that checks if the source file is installed locally\\.");

            }
        },

        new Tool("javac") {
            {
                location(".*([^ ]+/src/share/classes/([^ ]+\\.java)):.*");
                location(".*([^ ]+/src/([^ ]+\\.java)):.*");

                kind(false, "cast to .* for a non-varargs call and to suppress this warning");
                kind(false, "\\[options\\] bootstrap class path not set in conjunction with -source .*");
                kind(true, "\\[deprecation\\] .* in .* has been deprecated");
                kind(true, "\\[overrides\\] Class .* overrides equals, but neither it nor any superclass overrides hashCode method");
                kind(true, "\\[serial\\] serializable class .* has no definition of serialVersionUID");
                kind(true, "change obsolete notation for MethodHandle invocations from .* to .*");
                kind(true, "non-varargs call of varargs method with inexact argument type for last parameter");
            }
        },

        new Tool("javadoc") {
            {
                location(".*([^ ]+/impsrc/([^ ]+\\.(?:java|html))):.*");
                location(".*([^ ]+/src/share/classes/([^ ]+\\.java)):.*");
                location(".*([^ ]+/src/([^ ]+\\.java)):.*");

                kind(true, "Tag @link: reference not found: .*");
                kind(true, ".* is an unknown tag.");
                kind(true, "@param argument \".*\" is not a parameter name\\.");
                kind(true, "@see tag has no arguments\\.");
                kind(true, "Tag @link: can't find .* in .*");
                kind(true, "Tag @link: missing '#': .*");
                kind(true, "Tag @linkplain: reference not found: .*");
                kind(true, "Tag @return cannot be used in constructor documentation.");
                kind(true, "Tag @return cannot be used in field documentation.");
                kind(true, "Tag @see: can't find .* in .*");
                kind(true, "Tag @see: missing '.*': \".*\"");
                kind(true, "Tag @see: reference not found: .*");
                kind(true, "\\} missing for possible See Tag in comment string:.*");
            }
        },

        new Tool("javazic") {
            {
                kind(false, "found last rules for .* inconsistent");
            }
        },

        new Tool("jdwpgen") {
            {
                kind(false, "Generated jvmti file does not exist: ");
            }
        },

        new Tool("junit") {
            {
                kind(false, "\\[junit\\] +class .* ignored in headless mode\\.");
            }
        },

        new Tool("jvmtiEnvFill") {
            {

                kind(false, "function .*: filled and stub arguments differ");

            }
        },

        new Tool("ld") {
            {
                kind(false, "option .* appears more than once, first setting taken");
                kind(false, "`.*' does not appear in file `.*'");
                kind(false, "section `.*' does not appear in any input file");
                kind(false, "symbol `.*' has differing types");
            }
        },

        new Tool("link") {
            {
                kind(false, "all references to '.*' discarded by /OPT:REF");
                kind(false, "defaultlib '.*' conflicts with use of other libs; use /NODEFAULTLIB:library");
                kind(false, "export '.*' specified multiple times; using first specification");
                kind(false, "exported symbol '.*' should not be assigned an ordinal");
                kind(false, "no public symbols found; archive member will be inaccessible");
                kind(false, "unrecognized option '.*'; ignored");
                kind(false, "object specified more than once; extras ignored");
                kind(false, "ignoring '/EDITANDCONTINUE' due to '/INCREMENTAL:NO' specification");
                kind(false, ".* directive in .* differs from output filename '.*'; ignoring directive");
                kind(false, ".* is no longer supported;  ignored");
                kind(false, "/MACHINE not specified; defaulting to X86");
                kind(false, "# statement not supported for the target platform; ignored");
                kind(false, "locally defined symbol .* imported in function .*");
                kind(false, "/DELAYLOAD:.* ignored; no imports found from .*");
            }
        },

        new Tool("make") {
            {
                location(".*([^ ]+(make/[^ ]+(?:\\.gmk|Makefile))):.*");
                location("[./]*(([^ ]*(?:\\.gmk|Makefile))):.*");

                kind(false, "File was not built with a mapfile: .*");
                kind(false, "LicenseeSourceScan found .* patterns, see .*");
                kind(false, "The file jvmti.h is not the same interface as the VM version.");
                kind(false, "The official builds on windows use .*. You appear to be using .*");
                kind(false, "The version of ant being used is older than");
                kind(false, "The windows compiler is not version .*");
                kind(false, "This build does not include running javadoc.");
                kind(false, "This machine appears to only have .* of physical memory");
                kind(false, "To build Java 2 SDK .* you need :");
                kind(false, "You are not building the DEPLOY sources\\.");
                kind(false, "junit.jar is not found");
                kind(false, "awk:.*\\Qescape sequence `\\%' treated as plain `%'\\E");
                kind(false, "zip warning.*name not matched: .*");
                kind(true, "Value of .* cannot be empty, check or set .*");
                kind(true, "ignoring old commands for target `.*'");
                kind(true, "overriding commands for target `.*'");
                kind(false, "Value of .* cannot be empty, will use '.*'");
                kind(false, "The combo jre installer is not built since the 64-bit Installer (?:path )?is not (?:defined|found)");
                kind(false, "-jN forced in submake: disabling jobserver mode.");
            }
        },

        new Tool("msival2") {
            {
                kind(false, "Row '.*' in table '.*' has bits set in the '.*' column that are reserved. They should be 0 to ensure compat[ia]bility with future installer versions.");
            }
        },

        new Tool("pack200") {
            {
                kind(false, "skipping .* bytes of StackMapTable attribute in .*");
            }
        },

        new Tool("rc") {
            {
                kind(false, "'.*' : redefinition");
            }
        },

        new Tool("tar") {
            {
                kind(false, "file \".*\": Warning! File larger than 2 gigabytes will be successfully archived; but other de-archivers may have problems reading the resulting archive.");
            }
        },

        new Tool("unknown") {
            {
                kind(false, "<.*> entry is missing!!!");
                kind(false, "Java HotSpot\\(TM\\) Server VM warning: Performance bug: SystemDictionary lookup_count=.* lookup_length=.* average=.* load=.*");
                kind(false, "Option .* passed to ld, if ld is invoked, ignored otherwise");
                kind(false, "Path does not exist as file or directory:");
                kind(false, "Possible HotSpot VM interface conflict.");
                kind(false, "parameter .* set to \".*\"");
                kind(false, "the use of `.*' is dangerous, better use `.*'");
                kind(false, "file .* has not been placed into a bundle");
                kind(false, ".* does not end with a trailing slash.  This build instance will add the slash as it is required to allow proper evaluation of the .*\\.");
            }
        },

        new Tool("vs") {
            {
                location("(?:2>)?(.*(/VC/INCLUDE/.*\\.(?:h|hpp|c|cpp|inl)))\\([0-9]+\\).*");
                location("(?:2>)?(.*/src/windows/native/(.*\\.(?:h|hpp|c|cpp|inl)))\\([0-9]+\\).*");
                location("(?:2>)?(.*/src/windows/(.*\\.(?:h|hpp|c|cpp|inl)))\\([0-9]+\\).*");
                location("(?:2>)?(.*/src/closed/share/native/(.*\\.(?:h|hpp|c|cpp|inl)))\\([0-9]+\\).*");
                location("(?:2>)?(.*/src/(.*\\.(?:h|hpp|c|cpp|inl)))\\([0-9]+\\).*");
                location("(?:2>)?(.*/jdk7/(.*\\.(?:h|c|cpp|hpp|inl)))\\([0-9]+\\).*");
                location("(?:2>)?((.*\\.(?:h|c|cpp|hpp|inl)))\\([0-9]+\\).*");
                kind(true, "'.*' : conversion from '.*' to '.*', possible loss of data");
                kind(true, "'.*': name was marked as #pragma deprecated");
                kind(true, "'.*' : different 'const' qualifiers");
                kind(true, "'.*' : unsafe mix of type 'BOOL' and type 'bool' in operation");
                kind(true, "'.*' : inconsistent dll linkage");
                kind(true, "'.*' : not all control paths return a value");
                kind(true, "'.*' : pointer mismatch for actual parameter .*");
                kind(true, "'.*' : signed/unsigned mismatch");
                kind(true, "'.*' : unrecognized character escape sequence");
                kind(true, "'.*' : unreferenced label");
                kind(true, "'.*' : unreferenced local variable");
                kind(true, "'.*' differs in levels of indirection from '.*'");
                kind(true, "'.*' undefined; assuming extern returning int");
                kind(true, "'.*': .* has been superseded by .* and .*");
                kind(true, "'.*': .* has been superseded by .*");
                kind(true, "'.*': This function or variable has been superceded by newer library or operating system functionality\\.");
                kind(true, "'.*': This function or variable may be unsafe. Consider using .* instead\\.");
                kind(true, "'.*': identifier in type library '.*' is already a macro; use the 'rename' qualifier");
                kind(true, "'.*' was declared deprecated");
                kind(true, "#ident ignored;");
                kind(true, "C\\+\\+ exception handler used, but unwind semantics are not enabled\\.");
                kind(true, "The POSIX name for this item is deprecated. Instead, use the ISO C\\+\\+ conformant name: .*\\.");
                kind(true, "_STATIC_CPPLIB is deprecated");
                kind(true, "automatically excluding '.*' while importing type library '.*'");
                kind(true, "benign redefinition of type");
                kind(true, "different 'volatile' qualifiers");
                kind(true, "different types for formal and actual parameter .*");
                kind(true, "forcing value to bool 'true' or 'false' \\(performance warning\\)");
                kind(true, "incompatible types - from '.*' to '.*'");
                kind(true, "local variable '.*' used without having been initialized");
                kind(true, "macro redefinition");
                kind(true, "nonstandard extension used: '.*' uses .* and '.*' has destructor");
                kind(true, "nonstandard extension used: enum '.*' used in qualified name");
                kind(true, "obsolete declaration style: please use '.*' instead");
                kind(true, "returning address of local variable or temporary");
                kind(true, "shift count negative or too big, undefined behavior");
                kind(true, "too many actual parameters for macro '.*'");
                kind(true, "truncation from '.*' to '.*'");
                kind(true, "unary minus operator applied to unsigned type, result still unsigned");
                kind(true, "unexpected tokens following preprocessor directive - expected a newline");
                kind(true, "uninitialized local variable '.*' used");
                kind(true, "'.*': .* has been changed to conform with the ISO C standard, adding an extra character count parameter.");
                kind(true, "'ocscpy': ocscpy is not safe. Intead, use ocscpy_s");
                kind(true, "'type cast' : conversion from '.*' to '.*' of greater size");
            }
        }
    };
}
