/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.util;

import org.apache.tools.ant.taskdefs.condition.Os;
import java.io.File;
import java.util.Vector;

/**
 * A set of helper methods related to locating executables or checking
 * conditons of a given Java installation.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 *
 * @since Ant 1.5
 */
public class JavaEnvUtils {

    /** Are we on a DOS-based system */
    private static final boolean isDos = Os.isFamily("dos");
    /** Are we on Novell NetWare */
    private static final boolean isNetware = Os.isName("netware");
    /** Are we on AIX */
    private static final boolean isAix = Os.isName("aix");

    /** shortcut for System.getProperty("java.home") */
    private static final String javaHome = System.getProperty("java.home");

    /** FileUtils instance for path normalization */
    private static final FileUtils fileUtils = FileUtils.newFileUtils();

    /** Version of currently running VM. */
    private static String javaVersion;

    /** floating version of the JVM */
    private static int javaVersionNumber;

    /** Version constant for Java 1.0 */
    public static final String JAVA_1_0 = "1.0";
    /** Version constant for Java 1.1 */
    public static final String JAVA_1_1 = "1.1";
    /** Version constant for Java 1.2 */
    public static final String JAVA_1_2 = "1.2";
    /** Version constant for Java 1.3 */
    public static final String JAVA_1_3 = "1.3";
    /** Version constant for Java 1.4 */
    public static final String JAVA_1_4 = "1.4";

    /** array of packages in the runtime */
    private static Vector jrePackages;


    static {

        // Determine the Java version by looking at available classes
        // java.lang.CharSequence was introduced in JDK 1.4
        // java.lang.StrictMath was introduced in JDK 1.3
        // java.lang.ThreadLocal was introduced in JDK 1.2
        // java.lang.Void was introduced in JDK 1.1
        // Count up version until a NoClassDefFoundError ends the try

        try {
            javaVersion = JAVA_1_0;
            javaVersionNumber=10;
            Class.forName("java.lang.Void");
            javaVersion = JAVA_1_1;
            javaVersionNumber++;
            Class.forName("java.lang.ThreadLocal");
            javaVersion = JAVA_1_2;
            javaVersionNumber++;
            Class.forName("java.lang.StrictMath");
            javaVersion = JAVA_1_3;
            javaVersionNumber++;
            Class.forName("java.lang.CharSequence");
            javaVersion = JAVA_1_4;
            javaVersionNumber++;
        } catch (Throwable t) {
            // swallow as we've hit the max class version that
            // we have
        }
    }

    /**
     * Returns the version of Java this class is running under.
     * @return the version of Java as a String, e.g. "1.1"
     */
    public static String getJavaVersion() {
        return javaVersion;
    }

    /**
     * Compares the current Java version to the passed in String -
     * assumes the argument is one of the constants defined in this
     * class.
     * @return true if the version of Java is the same as the given
     * version.
     * @since Ant 1.5
     */
    public static boolean isJavaVersion(String version) {
        return javaVersion == version;
    }

    /**
     * Finds an executable that is part of a JRE installation based on
     * the java.home system property.
     *
     * <p><code>java</code>, <code>keytool</code>,
     * <code>policytool</code>, <code>orbd</code>, <code>rmid</code>,
     * <code>rmiregistry</code>, <code>servertool</code> and
     * <code>tnameserv</code> are JRE executables on Sun based
     * JRE's.</p>
     *
     * <p>You typically find them in <code>JAVA_HOME/jre/bin</code> if
     * <code>JAVA_HOME</code> points to your JDK installation.  JDK
     * &lt; 1.2 has them in the same directory as the JDK
     * executables.</p>
     *
     * @since Ant 1.5
     */
    public static String getJreExecutable(String command) {
        if (isNetware) {
            // Extrapolating from:
            // "NetWare may have a "java" in that directory, but 99% of
            // the time, you don't want to execute it" -- Jeff Tulley
            // <JTULLEY@novell.com>
            return command;
        }

        File jExecutable = null;

        if (isAix) {
            // On IBM's JDK 1.2 the directory layout is different, 1.3 follows
            // Sun's layout.
            jExecutable = findInDir(javaHome + "/sh", command);
        }

        if (jExecutable == null) {
            jExecutable = findInDir(javaHome + "/bin", command);
        }

        if (jExecutable != null) {
            return jExecutable.getAbsolutePath();
        } else {
            // Unfortunately on Windows java.home doesn't always refer
            // to the correct location, so we need to fall back to
            // assuming java is somewhere on the PATH.
            return addExtension(command);
        }
    }

    /**
     * Finds an executable that is part of a JDK installation based on
     * the java.home system property.
     *
     * <p>You typically find them in <code>JAVA_HOME/bin</code> if
     * <code>JAVA_HOME</code> points to your JDK installation.</p>
     *
     * @since Ant 1.5
     */
    public static String getJdkExecutable(String command) {
        if (isNetware) {
            // Extrapolating from:
            // "NetWare may have a "java" in that directory, but 99% of
            // the time, you don't want to execute it" -- Jeff Tulley
            // <JTULLEY@novell.com>
            return command;
        }

        File jExecutable = null;

        if (isAix) {
            // On IBM's JDK 1.2 the directory layout is different, 1.3 follows
            // Sun's layout.
            jExecutable = findInDir(javaHome + "/../sh", command);
        }

        if (jExecutable == null) {
            jExecutable = findInDir(javaHome + "/../bin", command);
        }

        if (jExecutable != null) {
            return jExecutable.getAbsolutePath();
        } else {
            // fall back to JRE bin directory, also catches JDK 1.0 and 1.1
            // where java.home points to the root of the JDK and Mac OS X where
            // the whole directory layout is different from Sun's
            return getJreExecutable(command);
        }
    }

    /**
     * Adds a system specific extension to the name of an executable.
     *
     * @since Ant 1.5
     */
    private static String addExtension(String command) {
        // This is the most common extension case - exe for windows and OS/2,
        // nothing for *nix.
        return command + (isDos ? ".exe" : "");
    }

    /**
     * Look for an executable in a given directory.
     *
     * @return null if the executable cannot be found.
     */
    private static File findInDir(String dirName, String commandName) {
        File dir = fileUtils.normalize(dirName);
        File executable = null;
        if (dir.exists()) {
            executable = new File(dir, addExtension(commandName));
            if (!executable.exists()) {
                executable = null;
            }
        }
        return executable;
    }

    /**
     * demand creation of the package list.
     * When you add a new package, add a new test below
     */

    private static void buildJrePackages() {
        jrePackages=new Vector();
        switch(javaVersionNumber) {
            case 14:
                jrePackages.addElement("org.apache.crimson");
                jrePackages.addElement("org.apache.xalan");
                jrePackages.addElement("org.apache.xml");
                jrePackages.addElement("org.apache.xpath");
                jrePackages.addElement("org.ietf.jgss");
                jrePackages.addElement("org.w3c.dom");
                jrePackages.addElement("org.xml.sax");
                // fall through
            case 13:
                jrePackages.addElement("org.omg");
                jrePackages.addElement("com.sun.corba");
                jrePackages.addElement("com.sun.jndi");
                jrePackages.addElement("com.sun.media");
                jrePackages.addElement("com.sun.naming");
                jrePackages.addElement("com.sun.org.omg");
                jrePackages.addElement("com.sun.rmi");
                jrePackages.addElement("sunw.io");
                jrePackages.addElement("sunw.util");
                // fall through
            case 12:
                jrePackages.addElement("com.sun.java");
                jrePackages.addElement("com.sun.image");
                // are there any here that we forgot?
                // fall through
            case 11:
            default:
                //things like sun.reflection, sun.misc, sun.net
                jrePackages.addElement("sun");
                jrePackages.addElement("java");
                jrePackages.addElement("javax");
                break;
        }
    }

    /**
     * Testing helper method; kept here for unification of changes.
     */
    public static Vector getJrePackageTestCases() {
        Vector tests=new Vector();
        tests.addElement("java.lang.Object");
        switch(javaVersionNumber) {
            case 14:
                tests.addElement("sun.audio.AudioPlayer");
                tests.addElement("org.apache.crimson.parser.ContentModel");
                tests.addElement("org.apache.xalan.processor.ProcessorImport");
                tests.addElement("org.apache.xml.utils.URI");
                tests.addElement("org.apache.xpath.XPathFactory");
                tests.addElement("org.ietf.jgss.Oid");
                tests.addElement("org.w3c.dom.Attr");
                tests.addElement("org.xml.sax.XMLReader");
                // fall through
            case 13:
                tests.addElement("org.omg.CORBA.Any");
                tests.addElement("com.sun.corba.se.internal.corba.AnyImpl");
                tests.addElement("com.sun.jndi.ldap.LdapURL");
                tests.addElement("com.sun.media.sound.Printer");
                tests.addElement("com.sun.naming.internal.VersionHelper");
                tests.addElement("com.sun.org.omg.CORBA.Initializer");
                tests.addElement("sunw.io.Serializable");
                tests.addElement("sunw.util.EventListener");
                // fall through
            case 12:
                tests.addElement("javax.accessibility.Accessible");
                tests.addElement("sun.misc.BASE64Encoder");
                tests.addElement("com.sun.image.codec.jpeg.JPEGCodec");
                // fall through
            case 11:
            default:
                //things like sun.reflection, sun.misc, sun.net
                tests.addElement("sun.reflect.SerializationConstructorAccessorImpl");
                tests.addElement("sun.net.www.http.HttpClient");
                tests.addElement("sun.audio.AudioPlayer");
                break;
        }
        return tests;
    }
    /**
     * get a vector of strings of packages built into
     * that platforms runtime jar(s)
     * @return list of packages
     */
    public static Vector getJrePackages() {
        if(jrePackages==null) {
            buildJrePackages();
        }
        return jrePackages;
    }
}
