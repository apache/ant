/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;

/**
 * Command line entry point into Ant. This class is entered via the
 * cannonical "public static void main" entry point and reads the
 * command line arguments. It then assembles and executes an Ant
 * project.
 * <p>
 * If you integrating Ant into some other tool, this is not the class
 * to use as an entry point. Please see the source code of this
 * class to see how it manipulates the Ant project classes.
 *
 * @author duncan@x180.com
 */
public class Main {

    private AntBean ant=new AntBean();
    
    /**
     * Indicates if this ant should be run.
     */
    private boolean readyToRun = false;

    /**
     * Indicates we should only parse and display the project help information
     */
    private boolean projectHelp = false;

    /**
     * Prints the message of the Throwable if it's not null.
     */
    private static void printMessage(Throwable t) {
        String message = t.getMessage();
        if (message != null) {
            System.err.println(message);
        }
    }

    /**
     * Entry point method.
     */
    public static void start(String[] args, Properties additionalUserProperties,
                             ClassLoader coreLoader) {
        Main m = null;

        try {
            m = new Main(args);
        } catch(Throwable exc) {
            printMessage(exc);
            System.exit(1);
        }
        AntBean ant=m.ant;
        ant.setCoreLoader( coreLoader );

        if (additionalUserProperties != null) {
            for (Enumeration e = additionalUserProperties.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                String property = additionalUserProperties.getProperty(key);
                ant.setUserProperty(key, property);
            }
        }
        
        try {
            Project project=ant.getProject();
            
            ant.execute();

            System.exit(0);
        } catch (BuildException be) {
            // ?? What is that, and how should it be implemented
            // XXX if (m.err != System.err) {
            printMessage(be);
            //}
            System.exit(1);
        } catch(Throwable exc) {
            exc.printStackTrace();
            printMessage(exc);
            System.exit(1);
        }
    }
                                 
    
    
    /**
     * Command line entry point. This method kicks off the building
     * of a project object and executes a build using either a given
     * target or the default target.
     *
     * @param args Command line args.
     */
    public static void main(String[] args) {
        start(args, null, null);
    }

    protected Main(String[] args) throws BuildException {

        String searchForThis = null;

        // cycle through given args

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-help")) {
                printUsage();
                return;
            } else if (arg.equals("-version")) {
                printVersion();
                return;
            } else if (arg.equals("-quiet") || arg.equals("-q")) {
                ant.setOutputLevel( Project.MSG_WARN );
            } else if (arg.equals("-verbose") || arg.equals("-v")) {
                printVersion();
                ant.setOutputLevel( Project.MSG_VERBOSE );
            } else if (arg.equals("-debug")) {
                printVersion();
                ant.setOutputLevel( Project.MSG_DEBUG );
            } else if (arg.equals("-logfile") || arg.equals("-l")) {
                try {
                    ant.setLogfile( args[i+1] );
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a log file when " +
                        "using the -log argument";
                    System.out.println(msg);
                    return;
                }
            } else if (arg.equals("-buildfile") || arg.equals("-file") || arg.equals("-f")) {
                try {
                    ant.setBuildfile( args[i+1] );
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a buildfile when " +
                        "using the -buildfile argument";
                    System.out.println(msg);
                    return;
                }
            } else if (arg.equals("-listener")) {
                try {
                    ant.addListener(args[i+1]);
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a classname when " +
                        "using the -listener argument";
                    System.out.println(msg);
                    return;
                }
            } else if (arg.startsWith("-D")) {

                /* Interestingly enough, we get to here when a user
                 * uses -Dname=value. However, in some cases, the JDK
                 * goes ahead * and parses this out to args
                 *   {"-Dname", "value"}
                 * so instead of parsing on "=", we just make the "-D"
                 * characters go away and skip one argument forward.
                 *
                 * I don't know how to predict when the JDK is going
                 * to help or not, so we simply look for the equals sign.
                 */

                String name = arg.substring(2, arg.length());
                String value = null;
                int posEq = name.indexOf("=");
                if (posEq > 0) {
                    value = name.substring(posEq+1);
                    name = name.substring(0, posEq);
                } else if (i < args.length-1) {
                    value = args[++i];
                       }

                ant.setUserProperty(name, value);
                
            } else if (arg.equals("-logger")) {
                try {
                    ant.setLogger( args[++i] );
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    System.out.println("You must specify a classname when " +
                                       "using the -logger argument");
                    return;
                }
            } else if (arg.equals("-emacs")) {
                ant.setEmacs(  true );
            } else if (arg.equals("-projecthelp")) {
                // set the flag to display the targets and quit
                projectHelp = true;
            } else if (arg.equals("-find")) {
                // eat up next arg if present, default to build.xml
                if (i < args.length-1) {
                    ant.setFind(  args[++i] );
                } else {
                    ant.setFind( null );
                }
            } else if (arg.startsWith("-propertyfile")) {
                try {
                    ant.addPropertyfile( args[i+1] );
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a property filename when " +
                        "using the -propertyfile argument";
                    System.out.println(msg);
                    return;
                }
            } else if (arg.startsWith("-")) {
                // we don't have any more args to recognize!
                String msg = "Unknown argument: " + arg;
                System.out.println(msg);
                printUsage();
                return;
            } else {
                // if it's no other arg, it may be the target
                ant.addTarget(arg);
            }
        }

        readyToRun = true;
    }


    protected void addBuildListeners(Project project) {
        ant.addBuildListeners( project );
    }

    /**
     * Prints the usage of how to use this class to System.out
     */
    private static void printUsage() {
        String lSep = System.getProperty("line.separator");
        StringBuffer msg = new StringBuffer();
        msg.append("ant [options] [target [target2 [target3] ...]]" + lSep);
        msg.append("Options: " + lSep);
        msg.append("  -help                  print this message" + lSep);
        msg.append("  -projecthelp           print project help information" + lSep);
        msg.append("  -version               print the version information and exit" + lSep);
        msg.append("  -quiet                 be extra quiet" + lSep);
        msg.append("  -verbose               be extra verbose" + lSep);
        msg.append("  -debug                 print debugging information" + lSep);
        msg.append("  -emacs                 produce logging information without adornments" + lSep);
        msg.append("  -logfile <file>        use given file for log" + lSep);
        msg.append("  -logger <classname>    the class which is to perform logging" + lSep);
        msg.append("  -listener <classname>  add an instance of class as a project listener" + lSep);
        msg.append("  -buildfile <file>      use given buildfile" + lSep);
        msg.append("  -D<property>=<value>   use value for given property" + lSep);
        msg.append("  -propertyfile <name>   load all properties from file with -D" + lSep);
        msg.append("                         properties taking precedence" + lSep);
        msg.append("  -find <file>           search for buildfile towards the root of the" + lSep);
        msg.append("                         filesystem and use it" + lSep);
        System.out.println(msg.toString());
    }

    private static void printVersion() throws BuildException {
        System.out.println(AntBean.getAntVersion());
    }

    public static synchronized String getAntVersion() throws BuildException {
        return AntBean.getAntVersion();
    }

     /**
      * Print the project description, if any
      */
    private static void printDescription(Project project) {
       if (project.getDescription() != null) {
          System.out.println(project.getDescription());
       }
    }

    /**
     * Print out a list of all targets in the current buildfile
     */
    private static void printTargets(Project project, boolean printSubTargets) {
        // find the target with the longest name
        int maxLength = 0;
        Enumeration ptargets = project.getTargets().elements();
        String targetName;
        String targetDescription;
        Target currentTarget;
        // split the targets in top-level and sub-targets depending
        // on the presence of a description
        Vector topNames = new Vector();
        Vector topDescriptions = new Vector();
        Vector subNames = new Vector();

        while (ptargets.hasMoreElements()) {
            currentTarget = (Target)ptargets.nextElement();
            targetName = currentTarget.getName();
            targetDescription = currentTarget.getDescription();
            // maintain a sorted list of targets
            if (targetDescription == null) {
                int pos = findTargetPosition(subNames, targetName);
                subNames.insertElementAt(targetName, pos);
            } else {
                int pos = findTargetPosition(topNames, targetName);
                topNames.insertElementAt(targetName, pos);
                topDescriptions.insertElementAt(targetDescription, pos);
                if (targetName.length() > maxLength) {
                    maxLength = targetName.length();
                }
            }
        }

        printTargets(topNames, topDescriptions, "Main targets:", maxLength);
        
        if( printSubTargets ) {
            printTargets(subNames, null, "Subtargets:", 0);
        }

        String defaultTarget = project.getDefaultTarget();
        if (defaultTarget != null && !"".equals(defaultTarget)) { // shouldn't need to check but...
            System.out.println( "Default target: " + defaultTarget );
        }
    }

    /**
     * Search for the insert position to keep names a sorted list of Strings
     */
    private static int findTargetPosition(Vector names, String name) {
        int res = names.size();
        for (int i=0; i<names.size() && res == names.size(); i++) {
            if (name.compareTo((String)names.elementAt(i)) < 0) {
                res = i;
            }
        }
        return res;
    }

    /**
     * Output a formatted list of target names with an optional description
     */
    private static void printTargets(Vector names, Vector descriptions, String heading, int maxlen) {
        // now, start printing the targets and their descriptions
        String lSep = System.getProperty("line.separator");
        // got a bit annoyed that I couldn't find a pad function
        String spaces = "    ";
        while (spaces.length()<maxlen) {
            spaces += spaces;
        }
        StringBuffer msg = new StringBuffer();
        msg.append(heading + lSep + lSep);
        for (int i=0; i<names.size(); i++) {
            msg.append(" ");
            msg.append(names.elementAt(i));
            if (descriptions != null) {
                msg.append(spaces.substring(0, maxlen - ((String)names.elementAt(i)).length() + 2));
                msg.append(descriptions.elementAt(i));
            }
            msg.append(lSep);
        }
        System.out.println(msg.toString());
    }

    //
    /** The default build file name
     *  @deprecated Use AntBean
     */
    public final static String DEFAULT_BUILD_FILENAME = "build.xml";


}
