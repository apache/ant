/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Enumeration;

/**
 * Command line entry point into Ant. This class is entered via the
 * cannonical `public static void main` entry point and reads the
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

    /** Our current message output status. Follows Project.MSG_XXX */
    private static int msgOutputLevel = Project.MSG_INFO;

    /** File that we are using for configuration */
    private static File buildFile = new File("build.xml");

    // XXX
    // Change the targets to use a vector or something. I'm not keen
    // on the idea of having an artificial limit, even if it isn't
    // likely that somebody will want to build more than 20 targets.
    
    private static String targets[] = new String[20];
    private static int targetCount=0;

    /** Set of properties that can be used by tasks */
    private static Properties definedProps = new Properties();
    
    /** The Ant security manager */
    private static AntSecurityManager securityManager;

    /**
     * Command line entry point. This method kicks off the building
     * of a project object and executes a build using either a given
     * target or the default target.
     *
     * @param args Command line args.
     */
    
    public static void main(String[] args) {
        
	// cycle through given args
        
	for (int i = 0; i < args.length; i++) {
	    String arg = args[i];
            
	    if (arg.equals("-help") || arg.equals("help")) {
		printUsage();
		return;
	    } else if (arg.equals("-quiet") || arg.equals("-q") ||
		       arg.equals("q")) {
		msgOutputLevel = Project.MSG_WARN;
	    } else if (arg.equals("-verbose") || arg.equals("-v") ||
		       arg.equals("v")) {
		msgOutputLevel = Project.MSG_VERBOSE;
            } else if (arg.equals("-buildfile") || arg.equals("-file") || arg.equals("-f")) {
		try {
		    buildFile = new File(args[i+1]);
		    i++;
		} catch (ArrayIndexOutOfBoundsException aioobe) {
		    String msg = "You must specify a buildfile when " +
			"using the -buildfile argument";
		    System.out.println(msg);
		    return;
		}
	    } else if (arg.startsWith("-D")) {

		/* Interestingly enough, we get to here when a user
		 * uses -Dname=value. However, the JDK goes ahead
		 * and parses this out to args {"-Dname", "value"}
		 * so instead of parsing on "=", we just make the "-D"
		 * characters go away and skip one argument forward.
		 */
		
                String name = arg.substring(2, arg.length());
		String value = args[++i];
                definedProps.put(name, value);
            } else if (arg.startsWith("-")) {
		// we don't have any more args to recognize!
		String msg = "Unknown arg: " + arg;
		System.out.println(msg);
		printUsage();
		return;
	    } else {
		// if it's no other arg, it may be the target
		targets[targetCount]=arg;
		targetCount++;
	    }
	}
        
	// make sure buildfile exists
        
	if (!buildFile.exists()) {
	    System.out.println("Buildfile: " + buildFile + " does not exist!");
	    return;
	}
        
	// make sure it's not a directory (this falls into the ultra
	// paranoid lets check everything catagory
        
	if (buildFile.isDirectory()) {
	    System.out.println("What? Buildfile: " + buildFile + " is a dir!");
	    return;
	}

	// ok, so if we've made it here, let's run the damn build allready
	runBuild();
    
    // se should force the exit() to allow everything to cleanup since
    // there could be leftover threads running around (some stupid AWT code
    // used for image generation does this! grrrr)
    exit(0);
    }

    /**
     * Executes the build.
     */
    
    private static void runBuild() {

        // track when we started
        
	long startTime = System.currentTimeMillis();
	if (msgOutputLevel >= Project.MSG_INFO) {
	    System.out.println("Buildfile: " + buildFile);
	}
        
	Project project = new Project();
	project.setOutputLevel(msgOutputLevel);

	// set user-define properties
        Enumeration e = definedProps.keys();
        while (e.hasMoreElements()) {
            String arg = (String)e.nextElement();
            String value = (String)definedProps.get(arg);
            project.setUserProperty(arg, value);
        }

        // first use the ProjectHelper to create the project object
        // from the given build file.
        
	try {
	    ProjectHelper.configureProject(project, buildFile);
	} catch (BuildException be) {
	    String msg = "BUILD CONFIG ERROR: ";
	    System.out.println(msg + be.getMessage());
	    be.printStackTrace();
	    exit(1);
	}

        // make sure that we have a target to execute
        
	if (targetCount == 0) {
	    String target = project.getDefaultTarget();
	    targets[0]=target;
	    targetCount=1;
	}

        // set the security manager
    securityManager = new AntSecurityManager();
    System.setSecurityManager(securityManager);

        // actually do some work
	try {
	    for(int i=0; i< targetCount; i++) 
		project.executeTarget(targets[i]);
	} catch (BuildException be) {
	    String msg = "BUILD FATAL ERROR: ";
	    System.out.println(msg + be.getMessage());
        if (msgOutputLevel > Project.MSG_INFO) {
            be.printStackTrace();
        }
        exit(1);
	}

        // track our stop time and let the user know how long things
        // took.
        
	long finishTime = System.currentTimeMillis();
	long elapsedTime = finishTime - startTime;
	if (msgOutputLevel >= Project.MSG_INFO) {
	    System.out.println("Completed in " + (elapsedTime/1000)
			       + " seconds");
	}
    }

    /**
     * Prints the usage of how to use this class to System.out
     */
    
    private static void printUsage() {
        String lSep = System.getProperty("line.separator");
        StringBuffer msg = new StringBuffer();
        msg.append("ant [options] [target]" + lSep);
        msg.append("Options: " + lSep);
        msg.append("  -help                  print this message" + lSep);
        msg.append("  -quiet                 be extra quiet" + lSep);
        msg.append("  -verbose               be extra verbose" + lSep);
        msg.append("  -buildfile <file>      use given buildfile" + lSep);
        msg.append("  -D<property>=<value>   use value for given property"
                   + lSep);     
	System.out.println(msg.toString());
    }
    
    private static void exit(int code) {
        securityManager.setExit(true);
        System.exit(code);
    }
}
