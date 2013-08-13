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

import java.util.HashMap;

/**
 * The class to hold useful data
 *
 * @author Dan Xu
 */

final class Constant {

    public static enum Repository {
        langtools,
        hotspot,
        corba,
        jaxp,
        jaxws,
        jdk,
        nashorn
    }

    // Create the map from openjdk website, http://openjdk.java.net
    public final static HashMap<String, String> locAreaMap = new HashMap<>();

    static {
        // Core Libraries, Insert based on http://openjdk.java.net/groups/core-libs/
        // --Lang package
        locAreaMap.put("java/lang", "Lang package");

        // --Reflection
        locAreaMap.put("java/lang/reflect", "Reflection");
        locAreaMap.put("sun/reflect", "Reflection");

        // --java.io
        locAreaMap.put("java/io", "I/O");
        locAreaMap.put("sun/io", "I/O");
        // --serialver
        locAreaMap.put("sun/tools/serialver", "I/O");
        // --java.nio
        locAreaMap.put("java/nio", "I/O");
        locAreaMap.put("com/sun/nio", "I/O");
        locAreaMap.put("sun/nio", "I/O");
        locAreaMap.put("sun/nio/ByteBuffered.java", "I/O");
        // --java.nio.channels
        locAreaMap.put("java/nio/channels", "I/O");
        locAreaMap.put("sun/nio/ch", "I/O");
        // --java.nio.charset
        locAreaMap.put("java/nio/charset", "I/O");
        locAreaMap.put("sun/nio/cs", "I/O");
        locAreaMap.put("sun/tools/native2ascii", "I/O");

        // --Util package
        locAreaMap.put("java/util", "Util package");
        locAreaMap.put("sun/util", "Util package");
        locAreaMap.put("sun/util/resources", "Util package");

        // --Concurrency Utilities
        locAreaMap.put("java/util/concurrent", "Concurrency Utilities");

        // --Java Archive (JAR) Files
        locAreaMap.put("sun/net/www/protocol/jar", "Java Archive (JAR) Files");
        locAreaMap.put("com/sun/java/util/jar", "Java Archive (JAR) Files");
        locAreaMap.put("com/sun/tools/extcheck", "Java Archive (JAR) Files");
        locAreaMap.put("java/util/jar", "Java Archive (JAR) Files");
        locAreaMap.put("sun/tools/jar", "Java Archive (JAR) Files");

        // --Remote Method Invocation
        locAreaMap.put("java/rmi", "Remote Method Invocation");
        locAreaMap.put("sun/rmi", "Remote Method Invocation");
        locAreaMap.put("java/rmi/rmic", "Remote Method Invocation");
        locAreaMap.put("com/sun/rmi/rmid", "Remote Method Invocation");
        locAreaMap.put("sun/rmi/registry", "Remote Method Invocation");
        locAreaMap.put("sun/invoke", "Remote Method Invocation");

        // --Java Database Connectivity
        locAreaMap.put("java/sql", "Java Database Connectivity");
        locAreaMap.put("javax/sql", "Java Database Connectivity");
        locAreaMap.put("com/sun/rowset", "Java Database Connectivity");

        // --JNDI
        locAreaMap.put("javax/naming", "JNDI");
        locAreaMap.put("com/sun/naming", "JNDI");
        locAreaMap.put("com/sun/jndi", "JNDI");

        // --Math
        locAreaMap.put("java/math", "Math");
        locAreaMap.put("java/lang/Math.java", "Math");
        locAreaMap.put("java/lang/StrictMath.java", "Math");
        locAreaMap.put("sun/misc/FloatConsts.java", "Math");
        locAreaMap.put("sun/misc/FloatingDecimal.java", "Math");

        // --Annotation
        locAreaMap.put("java/lang/annotation", "Annotation");
        locAreaMap.put("sun/reflect/annotation", "Annotation");

        // --Reference
        locAreaMap.put("java/lang/ref", "Reference");

        // --Java Time (JSR 310)
        locAreaMap.put("java/time", "Java Time");

        // --native.common
        locAreaMap.put("src/share/native/common", "native.common");
        locAreaMap.put("src/windows/native/common", "native.common");
        locAreaMap.put("src/solaris/native/common", "native.common");

        // --Java Script Engine
        locAreaMap.put("javax/script", "Java Script Engine");
        locAreaMap.put("com/sun/tools/script", "Java Script Engine");


        // Security, Insert based on http://openjdk.java.net/groups/security/
        locAreaMap.put("java/security", "Security");
        locAreaMap.put("sun/security", "Security");
        locAreaMap.put("sun/security/pkcs11", "Security");
        locAreaMap.put("sun/security/mscapi", "Security");
        locAreaMap.put("sun/security/ssl", "Security");
        locAreaMap.put("com/sun/security", "Security");
        locAreaMap.put("com/sun/security/cert", "Security");
        locAreaMap.put("java/lang/SecurityException.java", "Security");
        locAreaMap.put("java/lang/SecurityManager.java", "Security");
        locAreaMap.put("javax/smartcardio", "Security");
        locAreaMap.put("javax/xml/crypto", "Security");
        locAreaMap.put("javax/crypto", "Security");
        locAreaMap.put("com/sun/crypto/provider", "Security");
        locAreaMap.put("javax/net/ssl", "Security");
        locAreaMap.put("com/sun/net/ssl", "Security");
        locAreaMap.put("sun/net/www/protocol/https", "Security");
        locAreaMap.put("org/jcp/xml/dsig", "Security");
        locAreaMap.put("com/sun/org/apache/xml/internal/security", "Security");
        locAreaMap.put("org/ietf/jgss", "Security");
        locAreaMap.put("src/share/lib/security", "Security");
        locAreaMap.put("java/lang/SecurityManager.c", "Security");
        locAreaMap.put("javax/security", "Security");


        // Networking, Insert based on http://openjdk.java.net/groups/net/
        locAreaMap.put("java/net", "Networking");
        locAreaMap.put("javax/net", "Networking");
        locAreaMap.put("sun/net", "Networking");
        locAreaMap.put("com/sun/net/httpserver", "Networking");
        locAreaMap.put("com/oracle/net", "Networking");
        locAreaMap.put("com/sun/java/browser/dom", "Networking");


        // Client
        // --AWT
        locAreaMap.put("java/awt", "AWT");
        locAreaMap.put("sun/awt", "AWT");
        locAreaMap.put("sun/awt/resources", "AWT");
        locAreaMap.put("sun/awt/robot", "AWT");
        locAreaMap.put("sun/awt/shell", "AWT");
        locAreaMap.put("sun/xawt", "AWT");
        locAreaMap.put("sun/windows", "AWT");

        // --SWING
        locAreaMap.put("sun/swing", "Swing");
        locAreaMap.put("javax/swing", "Swing");
        locAreaMap.put("com/sun/java/swing", "Swing");
        locAreaMap.put("com/sun/java/swing/plaf", "Swing");
        locAreaMap.put("sun/awt/swing_GTKEngine.c", "Swing");
        locAreaMap.put("sun/awt/swing_GTKStyle.c", "Swing");
        locAreaMap.put("sun/awt/gtk2_interface.h", "Swing");
        locAreaMap.put("sun/awt/gtk2_interface.c", "Swing");
        locAreaMap.put("sun/awt/gtk2_interface.c", "Swing");

        // --Java 2D(tm) Graphics and Imaging
        locAreaMap.put("sun/java2d", "Java 2D(tm) Graphics and Imaging");
        locAreaMap.put("sun/dc", "Java 2D(tm) Graphics and Imaging");
        locAreaMap.put("com/sun/image", "Java 2D(tm) Graphics and Imaging");
        locAreaMap.put("sun/awt/color", "Java 2D(tm) Graphics and Imaging");
        locAreaMap.put("sun/awt/font", "Java 2D(tm) Graphics and Imaging");
        locAreaMap.put("sun/awt/geom", "Java 2D(tm) Graphics and Imaging");
        locAreaMap.put("sun/awt/image", "Java 2D(tm) Graphics and Imaging");
        locAreaMap.put("sun/awt/print", "Java 2D(tm) Graphics and Imaging");

        // --Input Method Framework
        locAreaMap.put("java/text", "Input Method Framework");
        locAreaMap.put("sun/font", "Input Method Framework");
        locAreaMap.put("sun/text", "Input Method Framework");

        // --Sound
        locAreaMap.put("javax/sound", "Sound");
        locAreaMap.put("com/sun/media/sound", "Sound");
        locAreaMap.put("sun/audio", "Sound");

        // --Image I/O
        locAreaMap.put("com/sun/imageio", "Image I/O");
        locAreaMap.put("javax/imageio", "Image I/O");

        // --Print Service
        locAreaMap.put("javax/print", "Print Service");
        locAreaMap.put("sun/print", "Print Service");


        // Serviceability
        // --Instrumentation
        locAreaMap.put("java/lang/instrument", "Instrumentation");
        locAreaMap.put("sun/instrument", "Instrumentation");
        locAreaMap.put("src/share/instrument", "Instrumentation");
        locAreaMap.put("src/solaris/instrument", "Instrumentation");
        locAreaMap.put("src/windows/instrument", "Instrumentation");

        // --Monitoring and Management
        locAreaMap.put("java/lang/management", "Monitoring and Management");
        locAreaMap.put("javax/management", "Monitoring and Management");
        locAreaMap.put("com/sun/jmx", "Monitoring and Management");
        locAreaMap.put("sun/management", "Monitoring and Management");
        locAreaMap.put("com/sun/management", "Monitoring and Management");
        locAreaMap.put("sun/jvmstat", "Monitoring and Management");
        locAreaMap.put("sun/tracing", "Monitoring and Management");

        // --Debugger Architecture
        locAreaMap.put("com/sun/tools/jdi", "Debugger Architecture");
        locAreaMap.put("com/sun/jdi", "Debugger Architecture");

        // --JDK Tools & Utilities
        locAreaMap.put("java/applet", "JDK Tools & Utilities");
        locAreaMap.put("sun/applet", "JDK Tools & Utilities");
        locAreaMap.put("sun/tools", "JDK Tools & Utilities");
        locAreaMap.put("com/sun/tools", "JDK Tools & Utilities");
        locAreaMap.put("sun/launcher", "JDK Tools & Utilities");


        // Accessibility
        locAreaMap.put("javax/accessibility", "Accessibility");

        // JavaBeans(tm)
        locAreaMap.put("java/beans", "JavaBeans(tm)");
        locAreaMap.put("com/sun/beans", "JavaBeans(tm)");
        locAreaMap.put("sun/beans", "JavaBeans(tm)");
        locAreaMap.put("tools/swing-beans", "JavaBeans(tm)");

        // Service Tag
        locAreaMap.put("com/sun/servicetag", "Service Tag");

        // ObjectWeb
        locAreaMap.put("jdk/internal/org/objectweb", "ObjectWeb");

        // JRockit
        locAreaMap.put("oracle/jrockit/jfr", "JRockit");
        locAreaMap.put("com/oracle/jrockit/jfr", "JRockit");
        locAreaMap.put("jdk/jfr", "JRockit");

        // MISC
        locAreaMap.put("sun/misc", "Misc");
        locAreaMap.put("sun/usagetracker", "Misc");
        locAreaMap.put("src/solaris/bin", "Misc");
        locAreaMap.put("src/windows/bin", "Misc");
        locAreaMap.put("src/share/bin", "Misc");
        locAreaMap.put("src/share/transport", "Misc");
        locAreaMap.put("src/solaris/transport", "Misc");
        locAreaMap.put("src/windows/transport", "Misc");
        locAreaMap.put("src/share/back", "Misc");
        locAreaMap.put("src/solaris/back", "Misc");
        locAreaMap.put("src/windows/back", "Misc");

        // Demo
        locAreaMap.put("demo", "Demo");
        // Sample
        locAreaMap.put("sample", "Sample");
    }

    public final static HashMap<String, String> areaProjectMap = new HashMap<>();

    static {
        // Security
        areaProjectMap.put("Security", "Security");

        // Networking
        areaProjectMap.put("Networking", "Networking");

        // Corelibs
        areaProjectMap.put("Lang package", "Core Libraries");
        areaProjectMap.put("Reflection", "Core Libraries");
        areaProjectMap.put("I/O", "Core Libraries");
        areaProjectMap.put("Util package", "Core Libraries");
        areaProjectMap.put("Concurrency Utilities", "Core Libraries");
        areaProjectMap.put("Java Archive (JAR) Files", "Core Libraries");
        areaProjectMap.put("Remote Method Invocation", "Core Libraries");
        areaProjectMap.put("Java Database Connectivity", "Core Libraries");
        areaProjectMap.put("JNDI", "Core Libraries");
        areaProjectMap.put("Math", "Core Libraries");
        areaProjectMap.put("Annotation", "Core Libraries");
        areaProjectMap.put("Reference", "Core Libraries");
        areaProjectMap.put("Java Script Engine", "Core Libraries");
        areaProjectMap.put("native.common", "Core Libraries");
        areaProjectMap.put("Java Time", "Core Libraries");

        // Client
        areaProjectMap.put("AWT", "Client");
        areaProjectMap.put("Swing", "Client");
        areaProjectMap.put("Java 2D(tm) Graphics and Imaging", "Client");
        areaProjectMap.put("Input Method Framework", "Client");
        areaProjectMap.put("Sound", "Client");
        areaProjectMap.put("Print Service", "Client");
        areaProjectMap.put("Image I/O", "Client");

        // Serviceability
        areaProjectMap.put("Instrumentation", "Serviceability");
        areaProjectMap.put("Monitoring and Management", "Serviceability");
        areaProjectMap.put("Debugger Architecture", "Serviceability");
        areaProjectMap.put("JDK Tools & Utilities", "Serviceability");

        // Accessibility
        areaProjectMap.put("Accessibility", "Accessibility");

        // JavaBeans(tm)
        areaProjectMap.put("JavaBeans(tm)", "JavaBeans(tm)");

        // Service Tag
        areaProjectMap.put("Service Tag", "Service Tag");

        // ObjectWeb
        areaProjectMap.put("ObjectWeb", "ObjectWeb");

        // JRockit
        areaProjectMap.put("JRockit", "JRockit");

        // Demo and Sample
        areaProjectMap.put("Demo", "Demo&Sample");
        areaProjectMap.put("Sample", "Demo&Sample");

        // Misc
        areaProjectMap.put("Misc", "Misc");
    }
}
