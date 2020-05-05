/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * A set of helper methods related to locating executables or checking
 * conditions of a given Java installation.
 *
 * <p>Starting with Java 10 we've stopped adding <code>JAVA_</code>
 * and <code>VERSION_</code> attributes for new major version numbers
 * of the JVM.</p>
 *
 * @since Ant 1.5
 */
public final class JavaEnvUtils {

    /** Are we on a DOS-based system */
    private static final boolean IS_DOS = Os.isFamily("dos");
    /** Are we on Novell NetWare */
    private static final boolean IS_NETWARE = Os.isName("netware");
    /** Are we on AIX */
    private static final boolean IS_AIX = Os.isName("aix");

    /** shortcut for System.getProperty("java.home") */
    private static final String JAVA_HOME = System.getProperty("java.home");

    /** FileUtils instance for path normalization */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** Version of currently running VM. */
    private static String javaVersion;

    /** floating version of the JVM */
    private static int javaVersionNumber;

    /** Version of currently running VM. */
    private static final DeweyDecimal parsedJavaVersion;

    /** Version constant for Java 1.0 */
    public static final String JAVA_1_0 = "1.0";
    /** Number Version constant for Java 1.0 */
    public static final int VERSION_1_0 = 10;

    /** Version constant for Java 1.1 */
    public static final String JAVA_1_1 = "1.1";
    /** Number Version constant for Java 1.1 */
    public static final int VERSION_1_1 = 11;

    /** Version constant for Java 1.2 */
    public static final String JAVA_1_2 = "1.2";
    /** Number Version constant for Java 1.2 */
    public static final int VERSION_1_2 = 12;

    /** Version constant for Java 1.3 */
    public static final String JAVA_1_3 = "1.3";
    /** Number Version constant for Java 1.3 */
    public static final int VERSION_1_3 = 13;

    /** Version constant for Java 1.4 */
    public static final String JAVA_1_4 = "1.4";
    /** Number Version constant for Java 1.4 */
    public static final int VERSION_1_4 = 14;

    /** Version constant for Java 1.5 */
    public static final String JAVA_1_5 = "1.5";
    /** Number Version constant for Java 1.5 */
    public static final int VERSION_1_5 = 15;

    /** Version constant for Java 1.6 */
    public static final String JAVA_1_6 = "1.6";
    /** Number Version constant for Java 1.6 */
    public static final int VERSION_1_6 = 16;

    /** Version constant for Java 1.7 */
    public static final String JAVA_1_7 = "1.7";
    /** Number Version constant for Java 1.7 */
    public static final int VERSION_1_7 = 17;

    /** Version constant for Java 1.8 */
    public static final String JAVA_1_8 = "1.8";
    /** Number Version constant for Java 1.8 */
    public static final int VERSION_1_8 = 18;

    /**
     * Version constant for Java 1.9
     * @deprecated use #JAVA_9 instead
     */
    @Deprecated
    public static final String JAVA_1_9 = "1.9";
    /**
     * Number Version constant for Java 1.9
     * @deprecated use #VERSION_9 instead
     */
    @Deprecated
    public static final int VERSION_1_9 = 19;

    /**
     * Version constant for Java 9
     * @since Ant 1.9.8
     */
    public static final String JAVA_9 = "9";
    /**
     * Number Version constant for Java 9
     * @since Ant 1.9.8
     */
    public static final int VERSION_9 = 90;
    
    /**
     * Version constant for Java 10
     * @since Ant 1.10.7
     */
    public static final String JAVA_10 = "10";
    /**
     * Number Version constant for Java 10
     * @since Ant 1.10.7
     */
    public static final int VERSION_10 = 100;

    /**
     * Version constant for Java 11
     * @since Ant 1.10.7
     */
    public static final String JAVA_11 = "11";
    /**
     * Number Version constant for Java 11
     * @since Ant 1.10.7
     */
    public static final int VERSION_11 = 110;
    
    /**
     * Version constant for Java 12
     * @since Ant 1.10.7
     */
    
    public static final String JAVA_12 = "12";
    /**
     * Number Version constant for Java 12
     * @since Ant 1.10.7
     */
    public static final int VERSION_12 = 120;
    
    

    /** Whether this is the Kaffe VM */
    private static boolean kaffeDetected;

    /** Whether this is a GNU Classpath based VM */
    private static boolean classpathDetected;

    /** Whether this is the GNU VM (gcj/gij) */
    private static boolean gijDetected;

    /** Whether this is Apache Harmony */
    private static boolean harmonyDetected;

    /** array of packages in the runtime */
    private static Vector<String> jrePackages;

    private JavaEnvUtils() {
    }

    static {

        try {
        	// only java 1.8 up supported.
            javaVersion = JAVA_1_8;
            javaVersionNumber = VERSION_1_8;
            Class.forName("java.lang.module.ModuleDescriptor");
            // at least Java9 and this should properly support the purely numeric version property
            String v = System.getProperty("java.specification.version");
            DeweyDecimal pv = new DeweyDecimal(v);
            javaVersionNumber = pv.get(0) * 10;
            if (pv.getSize() > 1) {
                javaVersionNumber += pv.get(1);
            }
            javaVersion = pv.toString();
        } catch (Throwable t) {
            // swallow as we've hit the max class version that
            // we have
        }
        parsedJavaVersion = new DeweyDecimal(javaVersion);
        kaffeDetected = false;
        try {
            Class.forName("kaffe.util.NotImplemented");
            kaffeDetected = true;
        } catch (Throwable t) {
            // swallow as this simply doesn't seem to be Kaffe
        }
        classpathDetected = false;
        try {
            Class.forName("gnu.classpath.Configuration");
            classpathDetected = true;
        } catch (Throwable t) {
            // swallow as this simply doesn't seem to be GNU classpath based.
        }
        gijDetected = false;
        try {
            Class.forName("gnu.gcj.Core");
            gijDetected = true;
        } catch (Throwable t) {
            // swallow as this simply doesn't seem to be gcj/gij
        }
        harmonyDetected = false;
        try {
            Class.forName("org.apache.harmony.luni.util.Base64");
            harmonyDetected = true;
        } catch (Throwable t) {
            // swallow as this simply doesn't seem to be Apache Harmony
        }
    }

    /**
     * Returns the version of Java this class is running under.
     *
     * <p>Up until Java 8 Java version numbers were 1.VERSION -
     * e.g. 1.8.x for Java 8, starting with Java 9 it became 9.x.</p>
     *
     * @return the version of Java as a String, e.g. "1.6" or "9"
     */
    public static String getJavaVersion() {
        return javaVersion;
    }


    /**
     * Returns the version of Java this class is running under.
     * <p>This number can be used for comparisons.</p>
     * @return the version of Java as a number 10x the major/minor,
     * e.g Java1.5 has a value of 15 and Java9 the value 90 - major
     * will be 1 for all versions of Java prior to Java 9, minor will
     * be 0 for all versions of Java starting with Java 9.
     * @deprecated use #getParsedJavaVersion instead
     */
    @Deprecated
    public static int getJavaVersionNumber() {
        return javaVersionNumber;
    }

    /**
     * Returns the version of Java this class is running under.
     * <p>This number can be used for comparisons.</p>
     * @return the version of Java as major.minor, e.g Java1.5 has a
     * value of 1.5 and Java9 the value 9 - major will be 1 for all
     * versions of Java prior to Java 9, minor will be 0 for all
     * versions of Java starting with Java 9.
     */
    public static DeweyDecimal getParsedJavaVersion() {
        return parsedJavaVersion;
    }

    /**
     * Compares the current Java version to the passed in String -
     * assumes the argument is one of the constants defined in this
     * class.
     * Note that Ant now requires JDK 1.8+ so {@link #JAVA_1_0} through
     * {@link #JAVA_1_7} need no longer be tested for.
     * @param version the version to check against the current version.
     * @return true if the version of Java is the same as the given version.
     * @since Ant 1.5
     */
    public static boolean isJavaVersion(String version) {
        return javaVersion.equals(version)
            || (javaVersion.equals(JAVA_9) && JAVA_1_9.equals(version));
    }

    /**
     * Compares the current Java version to the passed in String -
     * assumes the argument is one of the constants defined in this
     * class.
     * Note that Ant now requires JDK 1.8+ so {@link #JAVA_1_0} through
     * {@link #JAVA_1_7} need no longer be tested for.
     * @param version the version to check against the current version.
     * @return true if the version of Java is the same or higher than the
     * given version.
     * @since Ant 1.7
     */
    public static boolean isAtLeastJavaVersion(String version) {
        return parsedJavaVersion.compareTo(new DeweyDecimal(version)) >= 0;
    }

    /**
     * Checks whether the current Java VM is Kaffe.
     * @return true if the current Java VM is Kaffe.
     * @since Ant 1.6.3
     * @see <a href="https://github.com/kaffe/kaffe">https://github.com/kaffe/kaffe</a>
     */
    public static boolean isKaffe() {
        return kaffeDetected;
    }

    /**
     * Checks whether the current Java VM is GNU Classpath
     * @since Ant 1.9.1
     * @return true if the version of Java is GNU Classpath
     */
    public static boolean isClasspathBased() {
        return classpathDetected;
    }

    /**
     * Checks whether the current Java VM is the GNU interpreter gij
     * or we are running in a gcj precompiled binary.
     * @since Ant 1.8.2
     * @return true if the current Java VM is gcj/gij.
     */
    public static boolean isGij() {
        return gijDetected;
    }

    /**
     * Checks whether the current VM is Apache Harmony.
     * @since Ant 1.8.2
     * @return true if the current VM is Apache Harmony.
     */
    public static boolean isApacheHarmony() {
        return harmonyDetected;
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
     * @param command the java executable to find.
     * @return the path to the command.
     * @since Ant 1.5
     */
    public static String getJreExecutable(String command) {
        if (IS_NETWARE) {
            // Extrapolating from:
            // "NetWare may have a "java" in that directory, but 99% of
            // the time, you don't want to execute it" -- Jeff Tulley
            // <JTULLEY@novell.com>
            return command;
        }

        File jExecutable = null;

        if (IS_AIX) {
            // On IBM's JDK 1.2 the directory layout is different, 1.3 follows
            // Sun's layout.
            jExecutable = findInDir(JAVA_HOME + "/sh", command);
        }

        if (jExecutable == null) {
            jExecutable = findInDir(JAVA_HOME + "/bin", command);
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
     * @param command the java executable to find.
     * @return the path to the command.
     * @since Ant 1.5
     */
    public static String getJdkExecutable(String command) {
        if (IS_NETWARE) {
            // Extrapolating from:
            // "NetWare may have a "java" in that directory, but 99% of
            // the time, you don't want to execute it" -- Jeff Tulley
            // <JTULLEY@novell.com>
            return command;
        }

        File jExecutable = null;

        if (IS_AIX) {
            // On IBM's JDK 1.2 the directory layout is different, 1.3 follows
            // Sun's layout.
            jExecutable = findInDir(JAVA_HOME + "/../sh", command);
        }

        if (jExecutable == null) {
            jExecutable = findInDir(JAVA_HOME + "/../bin", command);
        }

        if (jExecutable != null) {
            return jExecutable.getAbsolutePath();
        } else {
            // fall back to JRE bin directory, also catches JDK 1.0 and 1.1
            // where java.home points to the root of the JDK and Mac OS X where
            // the whole directory layout is different from Sun's
            // and also catches JDK 9 (and probably later) which
            // merged JDK and JRE dirs
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
        return command + (IS_DOS ? ".exe" : "");
    }

    /**
     * Look for an executable in a given directory.
     *
     * @return null if the executable cannot be found.
     */
    private static File findInDir(String dirName, String commandName) {
        File dir = FILE_UTILS.normalize(dirName);
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
     * When you add a new package, add a new test below.
     */

    private static void buildJrePackages() {
    	// do we still need this? As we only support java 1.8 upwards this would be the fixed list
		// is this list correct for java 9 and above?
		jrePackages = new Vector<>();
		jrePackages.addElement("sun");
		jrePackages.addElement("java");
		jrePackages.addElement("javax");
		jrePackages.addElement("com.sun.java");
		jrePackages.addElement("com.sun.image");
		jrePackages.addElement("org.omg");
		jrePackages.addElement("com.sun.corba");
		jrePackages.addElement("com.sun.jndi");
		jrePackages.addElement("com.sun.media");
		jrePackages.addElement("com.sun.naming");
		jrePackages.addElement("com.sun.org.omg");
		jrePackages.addElement("com.sun.rmi");
		jrePackages.addElement("sunw.io");
		jrePackages.addElement("sunw.util");
		jrePackages.addElement("org.ietf.jgss");
		jrePackages.addElement("org.w3c.dom");
		jrePackages.addElement("org.xml.sax");
		jrePackages.addElement("com.sun.org.apache");
		jrePackages.addElement("jdk");
	}

    /**
     * Testing helper method; kept here for unification of changes.
     * @return a list of test classes depending on the java version.
     */
    public static Vector<String> getJrePackageTestCases() {
    	// do we still need this? As we only support java 1.8 upwards this would be the fixed list
		// is this list correct for java 9 and above?
        Vector<String> tests = new Vector<>();
		tests.addElement("java.lang.Object");
		tests.addElement("sun.reflect.SerializationConstructorAccessorImpl");
		tests.addElement("sun.net.www.http.HttpClient");
		tests.addElement("sun.audio.AudioPlayer");
		tests.addElement("javax.accessibility.Accessible");
		tests.addElement("sun.misc.BASE64Encoder");
		tests.addElement("com.sun.image.codec.jpeg.JPEGCodec");
		tests.addElement("org.omg.CORBA.Any");
		tests.addElement("com.sun.corba.se.internal.corba.AnyImpl");
		tests.addElement("com.sun.jndi.ldap.LdapURL");
		tests.addElement("com.sun.media.sound.Printer");
		tests.addElement("com.sun.naming.internal.VersionHelper");
		tests.addElement("com.sun.org.omg.CORBA.Initializer");
		tests.addElement("sunw.io.Serializable");
		tests.addElement("sunw.util.EventListener");
		tests.addElement("sun.audio.AudioPlayer");
		tests.addElement("org.ietf.jgss.Oid");
		tests.addElement("org.w3c.dom.Attr");
		tests.addElement("org.xml.sax.XMLReader");
		tests.addElement("com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl");
		tests.addElement("jdk.net.Sockets");
		return tests;
    }
    /**
     * get a vector of strings of packages built into
     * that platforms runtime jar(s)
     * @return list of packages.
     */
    public static Vector<String> getJrePackages() {
        if (jrePackages == null) {
            buildJrePackages();
        }
        return jrePackages;
    }

    /**
     * Writes the command into a temporary DCL script and returns the
     * corresponding File object.
     * It is the job of the caller to delete the file on exit.
     * @param cmds the command.
     * @return the file containing the command.
     * @throws IOException if there is an error writing to the file.
     */
    public static File createVmsJavaOptionFile(String[] cmds)
            throws IOException {
        File script = FILE_UTILS.createTempFile(null, "ANT", ".JAVA_OPTS", null, false, true);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(script))) {
            for (String cmd : cmds) {
                out.write(cmd);
                out.newLine();
            }
        }
        return script;
    }

    /**
     * Return the value of ${java.home}
     * @return the java home value.
     */
    public static String getJavaHome() {
        return JAVA_HOME;
    }
}
