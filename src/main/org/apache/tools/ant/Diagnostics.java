/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * A little diagnostic helper that output some information that may help
 * in support. It should quickly give correct information about the
 * jar existing in ant.home/lib and the jar versions...
 *
 * @since Ant 1.5
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public final class Diagnostics {

    /** utility class */
    private Diagnostics(){
    }

    /**
     * Check if optional tasks are available. Not that it does not check
     * for implementation version. Use <tt>validateVersion()</tt> for this.
     * @return <tt>true</tt> if optional tasks are available.
     */
    public static boolean isOptionalAvailable() {
        try {
            Class.forName("org.apache.tools.ant.taskdefs.optional.Test");
        } catch (ClassNotFoundException e){
            return false;
        }
        return true;
    }

    /**
     * Check if core and optional implementation version do match.
     * @throws BuildException if the implementation version of optional tasks
     * does not match the core implementation version.
     */
    public static void validateVersion() throws BuildException {
        try {
            Class optional = Class.forName("org.apache.tools.ant.taskdefs.optional.Test");
            String coreVersion = getImplementationVersion(Main.class);
            String optionalVersion = getImplementationVersion(optional);
            
            if (coreVersion != null && !coreVersion.equals(optionalVersion) ){
                throw new BuildException(
                        "Invalid implementation version between Ant core and Ant optional tasks.\n" +
                        " core    : " + coreVersion + "\n" +
                        " optional: " + optionalVersion);
            }
        } catch (ClassNotFoundException e){
        }
    }

    /**
     * return the list of jar files existing in ANT_HOME/lib
     * and that must have been picked up by Ant script.
     * @return the list of jar files existing in ant.home/lib or
     * <tt>null</tt> if an error occurs.
     */
    public static File[] listLibraries() {
        String home = System.getProperty("ant.home");
        if (home == null) {
            return null;
        }
        File libDir = new File(home, "lib");
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        };
        // listFiles is JDK 1.2+ method...
        String[] filenames = libDir.list(filter);
        if (filenames == null) {
            return null;
        }
        File[] files = new File[filenames.length];
        for (int i = 0; i < filenames.length; i++){
            files[i] = new File(libDir, filenames[i]);
        }
        return files;
    }

    /**
     * main entry point for command line
     * @param args command line arguments.
     */
    public static void main(String[] args){
        doReport(System.out);
    }


    /**
     * Helper method to get the implementation version.
     * @param clazz the class to get the information from.
     * @return null if there is no package or implementation version.
     * '?.?' for JDK 1.0 or 1.1.
     */
    private static String getImplementationVersion(Class clazz){
        try {
          // Package pkg = clazz.getPackage();        
          Method method = Class.class.getMethod("getPackage", new Class[0]);
          Object pkg = method.invoke(clazz, null);
          if (pkg != null) {
              // pkg.getImplementationVersion();
              method = pkg.getClass().getMethod("getImplementationVersion", new Class[0]);
              Object version = method.invoke(pkg, null);          
              return (String)version;
          }
        } catch (Exception e){
          // JDK < 1.2 should land here because the methods above don't exist.
          return "?.?";
        }
        return null;
    }

    /**
     * Print a report to the given stream.
     * @param out the stream to print the report to.
     */
    public static void doReport(PrintStream out){
        out.println("------- Ant diagnostics report -------");
        out.println(Main.getAntVersion());
        out.println();
        out.println("-------------------------------------------");
        out.println(" Implementation Version (JDK1.2+ only)");
        out.println("-------------------------------------------");
        out.println("core tasks     : " + getImplementationVersion(Main.class));

        Class optional = null;
        try {
            optional = Class.forName(
                    "org.apache.tools.ant.taskdefs.optional.Test");
            out.println("optional tasks : " + getImplementationVersion(optional));
        } catch (ClassNotFoundException e){
            out.println("optional tasks : not available");
        }

        out.println();
        out.println("-------------------------------------------");
        out.println(" ANT_HOME/lib jar listing");
        out.println("-------------------------------------------");
        doReportLibraries(out);

        out.println();
        out.println("-------------------------------------------");
        out.println(" Tasks availability");
        out.println("-------------------------------------------");
        doReportTasksAvailability(out);

        out.println();
        out.println("-------------------------------------------");
        out.println(" org.apache.env.Which diagnostics");
        out.println("-------------------------------------------");
        doReportWhich(out);

        out.println();
        out.println("-------------------------------------------");
        out.println(" System properties");
        out.println("-------------------------------------------");
        doReportSystemProperties(out);

        out.println();
    }

    /**
     * Report a listing of system properties existing in the current vm.
     * @param out the stream to print the properties to.
     */
    private static void doReportSystemProperties(PrintStream out){
        for( Enumeration keys = System.getProperties().keys();
            keys.hasMoreElements(); ){
            String key = (String)keys.nextElement();
            out.println(key + " : " + System.getProperty(key));
        }
    }


    /**
     * Report the content of ANT_HOME/lib directory
     * @param out the stream to print the content to
     */
    private static void doReportLibraries(PrintStream out){
        out.println("ant.home: " + System.getProperty("ant.home"));
        File[] libs = listLibraries();
        if (libs == null) {
            out.println("Unable to list libraries.");
            return;
        }
        for (int i = 0; i < libs.length; i++){
            out.println(libs[i].getName()
                    + " (" + libs[i].length() + " bytes)");
        }
    }


    /**
     * Call org.apache.env.Which if available
     * @param out the stream to print the content to.
     */
    private static void doReportWhich(PrintStream out){
        Throwable error = null;
        try {
            Class which = Class.forName("org.apache.env.Which");
            Method method = which.getMethod("main", new Class[]{ String[].class });
            method.invoke(null, new Object[]{new String[]{}});
        } catch (ClassNotFoundException e) {
            out.println("Not available.");
            out.println("Download it at http://xml.apache.org/commons/");
        } catch (InvocationTargetException e) {
            error = e.getTargetException() == null ? e : e.getTargetException();
        } catch (Throwable e) {
            error = e;
        }
        // report error if something weird happens...this is diagnostic.
        if (error != null) {
            out.println("Error while running org.apache.env.Which");
            error.printStackTrace();
        }
    }

    /**
     * Create a report about non-available tasks that are defined in the
     * mapping but could not be found via lookup. It might generally happen
     * because Ant requires multiple libraries to compile and one of them
     * was missing when compiling Ant.
     * @param out the stream to print the tasks report to
     * <tt>null</tt> for a missing stream (ie mapping).
     */
    private static void doReportTasksAvailability(PrintStream out){
        InputStream is = Main.class.getResourceAsStream(
                "/org/apache/tools/ant/taskdefs/defaults.properties");
        if (is == null) {
            out.println("None available");
        } else {
            Properties props = new Properties();
            try {
                props.load(is);
                for (Enumeration keys = props.keys(); keys.hasMoreElements();){
                    String key = (String)keys.nextElement();
                    String classname = props.getProperty(key);
                    try {
                        Class.forName(classname);
                        props.remove(key);
                    } catch (ClassNotFoundException e){
                        out.println(key + " : Not Available");
                    } catch (NoClassDefFoundError e) {
                        String pkg = e.getMessage().replace('/', '.');
                        out.println(key + " : Missing dependency " + pkg );
                    } catch (Error e) {
                        out.println(key + " : Initialization error");
                    }
                }
                if (props.size() == 0){
                    out.println("All defined tasks are available");
                }
            } catch (IOException e){
                out.println(e.getMessage());
            }
        }
    }

}
