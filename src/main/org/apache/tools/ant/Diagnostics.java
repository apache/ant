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
package org.apache.tools.ant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.tools.ant.launch.Launcher;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.ProxySetup;
import org.apache.tools.ant.util.java15.ProxyDiagnostics;
import org.xml.sax.XMLReader;

/**
 * A little diagnostic helper that output some information that may help
 * in support. It should quickly give correct information about the
 * jar existing in ant.home/lib and the jar versions...
 *
 * @since Ant 1.5
 */
public final class Diagnostics {

    /**
     * value for which a difference between clock and temp file time triggers
     * a warning.
     * {@value}
     */
    private static final int BIG_DRIFT_LIMIT = 10000;

    /**
     * How big a test file to write.
     * {@value}
     */
    private static final int TEST_FILE_SIZE = 32;
    private static final int KILOBYTE = 1024;
    private static final int SECONDS_PER_MILLISECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;

    /**
     * The error text when a security manager blocks access to a property.
     * {@value}
     */
    protected static final String ERROR_PROPERTY_ACCESS_BLOCKED
            = "Access to this property blocked by a security manager";

    /** utility class */
    private Diagnostics() {
        // hidden constructor
    }

    /**
     * Doesn't do anything.
     * @deprecated Obsolete since Ant 1.8.2
     * @return <code>true</code>
     */
    @Deprecated
    public static boolean isOptionalAvailable() {
        return true;
    }

    /**
     * Doesn't do anything.
     * @deprecated Obsolete since Ant 1.8.2
     */
    @Deprecated
    public static void validateVersion() throws BuildException {
    }

    /**
     * return the list of jar files existing in ANT_HOME/lib
     * and that must have been picked up by Ant script.
     * @return the list of jar files existing in ant.home/lib or
     * <code>null</code> if an error occurs.
     */
    public static File[] listLibraries() {
        String home = System.getProperty(MagicNames.ANT_HOME);
        if (home == null) {
            return null;
        }
        return listJarFiles(new File(home, "lib"));

    }

    /**
     * get a list of all JAR files in a directory
     * @param libDir directory
     * @return array of files (or null for no such directory)
     */
    private static File[] listJarFiles(File libDir) {
        return libDir.listFiles((dir, name) -> name.endsWith(".jar"));
    }

    /**
     * main entry point for command line
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        doReport(System.out);
    }

    /**
     * Helper method to get the implementation version.
     * @param clazz the class to get the information from.
     * @return null if there is no package or implementation version.
     * '?.?' for JDK 1.0 or 1.1.
     */
    private static String getImplementationVersion(Class<?> clazz) {
        return clazz.getPackage().getImplementationVersion();
    }

    /**
     * Helper method to get the location.
     * @param clazz the class to get the information from.
     * @since Ant 1.8.0
     */
    private static URL getClassLocation(Class<?> clazz) {
        if (clazz.getProtectionDomain().getCodeSource() == null) {
            return null;
        }
        return clazz.getProtectionDomain().getCodeSource().getLocation();
    }

    /**
     * what parser are we using.
     * @return the classname of the parser
     */
    private static String getXMLParserName() {
        SAXParser saxParser = getSAXParser();
        if (saxParser == null) {
            return "Could not create an XML Parser";
        }
        // check to what is in the classname
        return saxParser.getClass().getName();
    }

    /**
     * what parser are we using.
     * @return the classname of the parser
     */
    private static String getXSLTProcessorName() {
        Transformer transformer = getXSLTProcessor();
        if (transformer == null) {
            return "Could not create an XSLT Processor";
        }
        // check to what is in the classname
        return transformer.getClass().getName();
    }

    /**
     * Create a JAXP SAXParser
     * @return parser or null for trouble
     */
    private static SAXParser getSAXParser() {
        SAXParserFactory saxParserFactory = null;
        try {
            saxParserFactory = SAXParserFactory.newInstance();
        } catch (Exception e) {
            // ignore
            ignoreThrowable(e);
            return null;
        }
        SAXParser saxParser = null;
        try {
            saxParser = saxParserFactory.newSAXParser();
        } catch (Exception e) {
            // ignore
            ignoreThrowable(e);
        }
        return saxParser;
    }

    /**
     * Create a JAXP XSLT Transformer
     * @return parser or null for trouble
     */
    private static Transformer getXSLTProcessor() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        if (transformerFactory != null) {
            try {
                return transformerFactory.newTransformer();
            } catch (Exception e) {
                // ignore
                ignoreThrowable(e);
            }
        }
        return null;
    }

    /**
     * get the location of the parser
     * @return path or null for trouble in tracking it down
     */
    private static String getXMLParserLocation() {
        SAXParser saxParser = getSAXParser();
        if (saxParser == null) {
            return null;
        }
        URL location = getClassLocation(saxParser.getClass());
        return location != null ? location.toString() : null;
    }

    private static String getNamespaceParserName() {
        try {
            XMLReader reader = JAXPUtils.getNamespaceXMLReader();
            return reader.getClass().getName();
        } catch (BuildException e) {
            //ignore
            ignoreThrowable(e);
            return null;
        }
    }

    private static String getNamespaceParserLocation() {
        try {
            XMLReader reader = JAXPUtils.getNamespaceXMLReader();
            URL location = getClassLocation(reader.getClass());
            return location != null ? location.toString() : null;
        } catch (BuildException e) {
            //ignore
            ignoreThrowable(e);
            return null;
        }
    }

    /**
     * get the location of the parser
     * @return path or null for trouble in tracking it down
     */
    private static String getXSLTProcessorLocation() {
        Transformer transformer = getXSLTProcessor();
        if (transformer == null) {
            return null;
        }
        URL location = getClassLocation(transformer.getClass());
        return location != null ? location.toString() : null;
    }

    /**
     * ignore exceptions. This is to allow future
     * implementations to log at a verbose level
     * @param thrown a Throwable to ignore
     */
    private static void ignoreThrowable(Throwable thrown) {
    }


    /**
     * Print a report to the given stream.
     * @param out the stream to print the report to.
     */
    public static void doReport(PrintStream out) {
        doReport(out, Project.MSG_INFO);
    }

    /**
     * Print a report to the given stream.
     * @param out the stream to print the report to.
     * @param logLevel denotes the level of detail requested as one of
     * Project's MSG_* constants.
     */
    public static void doReport(PrintStream out, int logLevel) {
        out.println("------- Ant diagnostics report -------");
        out.println(Main.getAntVersion());
        header(out, "Implementation Version");

        out.println("core tasks     : " + getImplementationVersion(Main.class)
                    + " in " + getClassLocation(Main.class));

        header(out, "ANT PROPERTIES");
        doReportAntProperties(out);

        header(out, "ANT_HOME/lib jar listing");
        doReportAntHomeLibraries(out);

        header(out, "USER_HOME/.ant/lib jar listing");
        doReportUserHomeLibraries(out);

        header(out, "Tasks availability");
        doReportTasksAvailability(out);

        header(out, "org.apache.env.Which diagnostics");
        doReportWhich(out);

        header(out, "XML Parser information");
        doReportParserInfo(out);

        header(out, "XSLT Processor information");
        doReportXSLTProcessorInfo(out);

        header(out, "System properties");
        doReportSystemProperties(out);

        header(out, "Temp dir");
        doReportTempDir(out);

        header(out, "Locale information");
        doReportLocale(out);

        header(out, "Proxy information");
        doReportProxy(out);

        out.println();
    }

    private static void header(PrintStream out, String section) {
        out.println();
        out.println("-------------------------------------------");
        out.print(" ");
        out.println(section);
        out.println("-------------------------------------------");
    }

    /**
     * Report a listing of system properties existing in the current vm.
     * @param out the stream to print the properties to.
     */
    private static void doReportSystemProperties(PrintStream out) {
        Properties sysprops = null;
        try {
            sysprops = System.getProperties();
        } catch (SecurityException  e) {
            ignoreThrowable(e);
            out.println("Access to System.getProperties() blocked " + "by a security manager");
            return;
        }
        sysprops.stringPropertyNames().stream()
                .map(key -> key + " : " + getProperty(key)).forEach(out::println);
    }

    /**
     * Get the value of a system property. If a security manager
     * blocks access to a property it fills the result in with an error
     * @param key a property key
     * @return the system property's value or error text
     * @see #ERROR_PROPERTY_ACCESS_BLOCKED
     */
    private static String getProperty(String key) {
        String value;
        try {
            value = System.getProperty(key);
        } catch (SecurityException e) {
            value = ERROR_PROPERTY_ACCESS_BLOCKED;
        }
        return value;
    }

    /**
     * Report the content of ANT_HOME/lib directory
     * @param out the stream to print the content to
     */
    private static void doReportAntProperties(PrintStream out) {
        Project p = new Project();
        p.initProperties();
        out.println(MagicNames.ANT_VERSION + ": " + p.getProperty(MagicNames.ANT_VERSION));
        out.println(MagicNames.ANT_JAVA_VERSION + ": "
                + p.getProperty(MagicNames.ANT_JAVA_VERSION));
        out.println("Is this the Apache Harmony VM? "
                    + (JavaEnvUtils.isApacheHarmony() ? "yes" : "no"));
        out.println("Is this the Kaffe VM? "
                    + (JavaEnvUtils.isKaffe() ? "yes" : "no"));
        out.println("Is this gij/gcj? "
                    + (JavaEnvUtils.isGij() ? "yes" : "no"));
        out.println(MagicNames.ANT_LIB + ": " + p.getProperty(MagicNames.ANT_LIB));
        out.println(MagicNames.ANT_HOME + ": " + p.getProperty(MagicNames.ANT_HOME));
    }

    /**
     * Report the content of ANT_HOME/lib directory
     * @param out the stream to print the content to
     */
    private static void doReportAntHomeLibraries(PrintStream out) {
        out.println(MagicNames.ANT_HOME + ": " + System.getProperty(MagicNames.ANT_HOME));
        printLibraries(listLibraries(), out);
    }

    /**
     * Report the content of ~/.ant/lib directory
     *
     * @param out the stream to print the content to
     */
    private static void doReportUserHomeLibraries(PrintStream out) {
        String home = System.getProperty(Launcher.USER_HOMEDIR);
        out.println("user.home: " + home);
        File libDir = new File(home, Launcher.USER_LIBDIR);
        printLibraries(listJarFiles(libDir), out);
    }

    /**
     * list the libraries
     * @param libs array of libraries (can be null)
     * @param out output stream
     */
    private static void printLibraries(File[] libs, PrintStream out) {
        if (libs == null) {
            out.println("No such directory.");
            return;
        }
        for (File lib : libs) {
            out.println(lib.getName() + " (" + lib.length() + " bytes)");
        }
    }


    /**
     * Call org.apache.env.Which if available
     *
     * @param out the stream to print the content to.
     */
    private static void doReportWhich(PrintStream out) {
        Throwable error = null;
        try {
            Class<?> which = Class.forName("org.apache.env.Which");
            Method method = which.getMethod(
                "main", String[].class);
            method.invoke(null, new Object[]{new String[]{}});
        } catch (ClassNotFoundException e) {
            out.println("Not available.");
            out.println("Download it at https://xml.apache.org/commons/");
        } catch (InvocationTargetException e) {
            error = e.getTargetException() == null ? e : e.getTargetException();
        } catch (Throwable e) {
            error = e;
        }
        // report error if something weird happens...this is diagnostic.
        if (error != null) {
            out.println("Error while running org.apache.env.Which");
            error.printStackTrace(out); //NOSONAR
        }
    }

    /**
     * Create a report about non-available tasks that are defined in the
     * mapping but could not be found via lookup. It might generally happen
     * because Ant requires multiple libraries to compile and one of them
     * was missing when compiling Ant.
     * @param out the stream to print the tasks report to
     * <code>null</code> for a missing stream (ie mapping).
     */
    private static void doReportTasksAvailability(PrintStream out) {
        InputStream is = Main.class.getResourceAsStream(
                MagicNames.TASKDEF_PROPERTIES_RESOURCE);
        if (is == null) {
            out.println("None available");
        } else {
            Properties props = new Properties();
            try {
                props.load(is);
                for (String key : props.stringPropertyNames()) {
                    String classname = props.getProperty(key);
                    try {
                        Class.forName(classname);
                        props.remove(key);
                    } catch (ClassNotFoundException e) {
                        out.println(key + " : Not Available "
                                + "(the implementation class is not present)");
                    } catch (NoClassDefFoundError e) {
                        String pkg = e.getMessage().replace('/', '.');
                        out.println(key + " : Missing dependency " + pkg);
                    } catch (LinkageError e) {
                        out.println(key + " : Initialization error");
                    }
                }
                if (props.size() == 0) {
                    out.println("All defined tasks are available");
                } else {
                    out.println("A task being missing/unavailable should only "
                            + "matter if you are trying to use it");
                }
            } catch (IOException e) {
                out.println(e.getMessage());
            }
        }
    }

    /**
     * tell the user about the XML parser
     * @param out a PrintStream
     */
    private static void doReportParserInfo(PrintStream out) {
        String parserName = getXMLParserName();
        String parserLocation = getXMLParserLocation();
        printParserInfo(out, "XML Parser", parserName, parserLocation);
        printParserInfo(out, "Namespace-aware parser", getNamespaceParserName(),
                getNamespaceParserLocation());
    }

    /**
     * tell the user about the XSLT processor
     * @param out a PrintStream
     */
    private static void doReportXSLTProcessorInfo(PrintStream out) {
        String processorName = getXSLTProcessorName();
        String processorLocation = getXSLTProcessorLocation();
        printParserInfo(out, "XSLT Processor", processorName, processorLocation);
    }

    private static void printParserInfo(PrintStream out, String parserType, String parserName,
            String parserLocation) {
        if (parserName == null) {
            parserName = "unknown";
        }
        if (parserLocation == null) {
            parserLocation = "unknown";
        }
        out.println(parserType + " : " + parserName);
        out.println(parserType + " Location: " + parserLocation);
    }

    /**
     * try and create a temp file in our temp dir; this
     * checks that it has space and access.
     * We also do some clock reporting.
     * @param out a PrintStream
     */
    private static void doReportTempDir(PrintStream out) {
        String tempdir = System.getProperty("java.io.tmpdir");
        if (tempdir == null) {
            out.println("Warning: java.io.tmpdir is undefined");
            return;
        }
        out.println("Temp dir is " + tempdir);
        File tempDirectory = new File(tempdir);
        if (!tempDirectory.exists()) {
            out.println("Warning, java.io.tmpdir directory does not exist: " + tempdir);
            return;
        }
        //create the file
        long now = System.currentTimeMillis();
        File tempFile = null;
        OutputStream fileout = null;
        InputStream filein = null;
        try {
            tempFile = File.createTempFile("diag", "txt", tempDirectory);
            //do some writing to it
            fileout = Files.newOutputStream(tempFile.toPath());
            byte[] buffer = new byte[KILOBYTE];
            for (int i = 0; i < TEST_FILE_SIZE; i++) {
                fileout.write(buffer);
            }
            fileout.close();
            fileout = null;

            // read to make sure the file has been written completely
            Thread.sleep(1000);
            filein = Files.newInputStream(tempFile.toPath());
            int total = 0;
            int read = 0;
            while ((read = filein.read(buffer, 0, KILOBYTE)) > 0) {
                total += read;
            }
            filein.close();
            filein = null;

            long filetime = tempFile.lastModified();
            long drift = filetime - now;
            tempFile.delete();

            out.print("Temp dir is writeable");
            if (total != TEST_FILE_SIZE * KILOBYTE) {
                out.println(", but seems to be full.  Wrote "
                            + (TEST_FILE_SIZE * KILOBYTE)
                            + "but could only read " + total + " bytes.");
            } else {
                out.println();
            }

            out.println("Temp dir alignment with system clock is " + drift + " ms");
            if (Math.abs(drift) > BIG_DRIFT_LIMIT) {
                out.println("Warning: big clock drift -maybe a network filesystem");
            }
        } catch (IOException e) {
            ignoreThrowable(e);
            out.println("Failed to create a temporary file in the temp dir " + tempdir);
            out.println("File  " + tempFile + " could not be created/written to");
        } catch (InterruptedException e) {
            ignoreThrowable(e);
            out.println("Failed to check whether tempdir is writable");
        } finally {
            FileUtils.close(fileout);
            FileUtils.close(filein);
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Report locale information
     * @param out stream to print to
     */
    private static void doReportLocale(PrintStream out) {
        //calendar stuff.
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        out.println("Timezone "
                + tz.getDisplayName()
                + " offset="
                + tz.getOffset(cal.get(Calendar.ERA), cal.get(Calendar.YEAR), cal
                        .get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), cal
                        .get(Calendar.DAY_OF_WEEK), ((cal.get(Calendar.HOUR_OF_DAY)
                        * MINUTES_PER_HOUR + cal.get(Calendar.MINUTE))
                        * SECONDS_PER_MINUTE + cal.get(Calendar.SECOND))
                        * SECONDS_PER_MILLISECOND + cal.get(Calendar.MILLISECOND)));
    }

    /**
     * print a property name="value" pair if the property is set;
     * print nothing if it is null
     * @param out stream to print on
     * @param key property name
     */
    private static void printProperty(PrintStream out, String key) {
        String value = getProperty(key);
        if (value != null) {
            out.print(key);
            out.print(" = ");
            out.print('"');
            out.print(value);
            out.println('"');
        }
    }

    /**
     * Report proxy information
     *
     * @param out stream to print to
     * @since Ant1.7
     */
    private static void doReportProxy(PrintStream out) {
        printProperty(out, ProxySetup.HTTP_PROXY_HOST);
        printProperty(out, ProxySetup.HTTP_PROXY_PORT);
        printProperty(out, ProxySetup.HTTP_PROXY_USERNAME);
        printProperty(out, ProxySetup.HTTP_PROXY_PASSWORD);
        printProperty(out, ProxySetup.HTTP_NON_PROXY_HOSTS);
        printProperty(out, ProxySetup.HTTPS_PROXY_HOST);
        printProperty(out, ProxySetup.HTTPS_PROXY_PORT);
        printProperty(out, ProxySetup.HTTPS_NON_PROXY_HOSTS);
        printProperty(out, ProxySetup.FTP_PROXY_HOST);
        printProperty(out, ProxySetup.FTP_PROXY_PORT);
        printProperty(out, ProxySetup.FTP_NON_PROXY_HOSTS);
        printProperty(out, ProxySetup.SOCKS_PROXY_HOST);
        printProperty(out, ProxySetup.SOCKS_PROXY_PORT);
        printProperty(out, ProxySetup.SOCKS_PROXY_USERNAME);
        printProperty(out, ProxySetup.SOCKS_PROXY_PASSWORD);

        printProperty(out, ProxySetup.USE_SYSTEM_PROXIES);
        ProxyDiagnostics proxyDiag = new ProxyDiagnostics();
        out.println("Java1.5+ proxy settings:");
        out.println(proxyDiag.toString());
    }

}
