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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Task to compile Java source files. This task can take the following
 * arguments:
 * <ul>
 * <li>sourcedir
 * <li>destdir
 * <li>deprecation
 * <li>classpath
 * <li>bootclasspath
 * <li>extdirs
 * <li>optimize
 * <li>debug
 * <li>target
 * </ul>
 * Of these arguments, the <b>sourcedir</b> and <b>destdir</b> are required.
 * <p>
 * When this task executes, it will recursively scan the sourcedir and
 * destdir looking for Java source files to compile. This task makes its
 * compile decision based on timestamp. Any other file in the
 * sourcedir will be copied to the destdir allowing support files to be
 * located properly in the classpath.
 *
 * @author duncan@x180.com
 */

public class Javac extends Task {

    private File srcDir;
    private File destDir;
    private String compileClasspath;
    private boolean debug = false;
    private boolean optimize = false;
    private boolean deprecation = false;
    private String target;
    private String bootclasspath;
    private String extdirs;

    private Vector compileList = new Vector();
    private Hashtable filecopyList = new Hashtable();

    /**
     * Set the source dir to find the source Java files.
     */
    
    public void setSrcdir(String srcDirName) {
	srcDir = project.resolveFile(srcDirName);
    }

    /**
     * Set the destination directory into which the Java source
     * files should be compiled.
     */
    
    public void setDestdir(String destDirName) {
	destDir = project.resolveFile(destDirName);
    }

    /**
     * Set the classpath to be used for this compilation.
     */
    
    public void setClasspath(String classpath) {
        compileClasspath = Project.translatePath(classpath);
    }

    /**
     * Sets the bootclasspath that will be used to compile the classes
     * against.
     */
    
    public void setBootclasspath(String bootclasspath) {        
        this.bootclasspath = Project.translatePath(bootclasspath);
    }

    /**
     * Sets the extension directories that will be used during the
     * compilation.
     */
    
    public void setExtdirs(String extdirs) {
        this.extdirs = Project.translatePath(extdirs);
    }

    
    /**
     * Set the deprecation flag. Valid strings are "on", "off", "true", and
     * "false".
     */
    
    public void setDeprecation(String deprecation) {
        if (deprecation.equalsIgnoreCase("on") ||
            deprecation.equalsIgnoreCase("true")) {
            this.deprecation = true;
        } else {
            this.deprecation = false;
        }
    }
    
    
    /**
     * Set the debug flag. Valid strings are "on", "off", "true", and "false".
     */
    
    public void setDebug(String debugString) {
	if (debugString.equalsIgnoreCase("on") ||
            debugString.equalsIgnoreCase("true")) {
	    debug = true;
	} else {
            debug = false;
        }
    }

    /**
     * Set the optimize flag. Valid strings are "on", "off", "true", and
     * "false".
     */

     public void setOptimize(String optimizeString) {
	 if (optimizeString.equalsIgnoreCase("on") ||
	     optimizeString.equalsIgnoreCase("true")) {
	     optimize = true;
	 } else {
	     optimize = false;
	 }
     }

    /**
     * Sets the target VM that the classes will be compiled for. Valid
     * strings are "1.1", "1.2", and "1.3".
     */
    
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Executes the task.
     */
    
    public void execute() throws BuildException {

	// first off, make sure that we've got a srcdir and destdir

	if (srcDir == null || destDir == null ) {
	    String msg = "srcDir and destDir attributes must be set!";
	    throw new BuildException(msg);
	}

	// scan source and dest dirs to build up both copy lists and
	// compile lists

	scanDir(srcDir, destDir);
	
	// compile the source files

	String compiler = project.getProperty("build.compiler");
	if (compiler == null) {
	    if (Project.getJavaVersion().startsWith("1.3")) {
		compiler = "modern";
	    } else {
		compiler = "classic";
	    }
	}

	if (compileList.size() > 0) {
            project.log("Compiling " + compileList.size() +
		        " source files to " + destDir);
	    
	    if (compiler.equalsIgnoreCase("classic")) {
		doClassicCompile();
	    } else if (compiler.equalsIgnoreCase("modern")) {
		doModernCompile();
	    } else if (compiler.equalsIgnoreCase("jikes")) {
		doJikesCompile();
	    } else {
		String msg = "Don't know how to use compiler " + compiler;
		throw new BuildException(msg);
	    }
	}
	
	// copy the support files

	if (filecopyList.size() > 0) {
	    project.log("Copying " + filecopyList.size() +
			" support files to " + destDir.getAbsolutePath());
	    Enumeration enum = filecopyList.keys();
	    while (enum.hasMoreElements()) {
		String fromFile = (String)enum.nextElement();
		String toFile = (String)filecopyList.get(fromFile);
		try {
		    copyFile(fromFile, toFile);
		} catch (IOException ioe) {
		    String msg = "Failed to copy " + fromFile + " to " + toFile
			+ " due to " + ioe.getMessage();
		    throw new BuildException(msg);
		}
	    }
	}
    }

    /**
     * Scans the directory looking for source files to be compiled and
     * support files to be copied.
     */
    
    private void scanDir(File srcDir, File destDir) {

	String[] list = srcDir.list(new DesirableFilter());
	int len = (list==null ? 0 : list.length);
	for (int i = 0; i < len; i++) {
	    String filename = list[i];
	    File srcFile = new File(srcDir, filename);
	    File destFile = new File(destDir, filename);
	    if (srcFile.isDirectory()) {
		// it's a dir, scan that recursively
		scanDir(srcFile, destFile);
	    } else {
		// it's a file, see if we compile it or just copy it
		if (filename.endsWith(".java")) {
		    File classFile =
			new File(destDir,
				 filename.substring(0,
						    filename.indexOf(".java"))
						    + ".class");
		    if (srcFile.lastModified() > classFile.lastModified()) {
			compileList.addElement(srcFile.getAbsolutePath());
		    }
		} else {
		    if (srcFile.lastModified() > destFile.lastModified()) {
			filecopyList.put(srcFile.getAbsolutePath(),
					 destFile.getAbsolutePath());
		    }
		}
	    }
	}
    }

    /**
     * Builds the compilation classpath.
     */

    // XXX
    // we need a way to not use the current classpath.
    
    private String getCompileClasspath() {
	StringBuffer classpath = new StringBuffer();

	// add dest dir to classpath so that previously compiled and
	// untouched classes are on classpath

	//classpath.append(sourceDir.getAbsolutePath());
	//classpath.append(File.pathSeparator);
	classpath.append(destDir.getAbsolutePath());

	// add our classpath to the mix

	if (compileClasspath != null) {
            addExistingToClasspath(classpath,compileClasspath);
	}

	// add the system classpath

        addExistingToClasspath(classpath,System.getProperty("java.class.path"));
	return classpath.toString();
    }
    

     /**
     * Takes a classpath-like string, and adds each element of
     * this string to a new classpath, if the components exist.
     * Components that don't exist, aren't added.
     * We do this, because jikes issues warnings for non-existant
     * files/dirs in his classpath, and these warnings are pretty
     * annoying.
     * @param target - target classpath
     * @param source - source classpath
     * to get file objects.
     */
    private void addExistingToClasspath(StringBuffer target,String source) {
       StringTokenizer tok = new StringTokenizer(source,
                             System.getProperty("path.separator"), false);
       while (tok.hasMoreTokens()) {
           File f = project.resolveFile(tok.nextToken());

           if (f.exists()) {
               target.append(File.pathSeparator);
               target.append(f.getAbsolutePath());
           } else {
               project.log("Dropping from classpath: "+
                   f.getAbsolutePath(),project.MSG_VERBOSE);
           }
       }

    }


    /**
     * Peforms a copmile using the classic compiler that shipped with
     * JDK 1.1 and 1.2.
     */
    
    private void doClassicCompile() throws BuildException {
	project.log("Using classic compiler", project.MSG_VERBOSE);
	String classpath = getCompileClasspath();
	Vector argList = new Vector();

        if (deprecation == true)
            argList.addElement("-deprecation");
        
	argList.addElement("-d");
	argList.addElement(destDir.getAbsolutePath());
	argList.addElement("-classpath");
	// Just add "sourcepath" to classpath ( for JDK1.1 )
	if (Project.getJavaVersion().startsWith("1.1")) {
	    argList.addElement(classpath + File.pathSeparator +
                               srcDir.getAbsolutePath());
	} else {
	    argList.addElement(classpath);
	    argList.addElement("-sourcepath");
	    argList.addElement(srcDir.getAbsolutePath());
            if (target != null) {
                argList.addElement("-target");
                argList.addElement(target);
            }
	}
	if (debug) {
	    argList.addElement("-g");
	}
	if (optimize) {
	    argList.addElement("-O");
	}
	if (bootclasspath != null) {
	    argList.addElement("-bootclasspath");
	    argList.addElement(bootclasspath);
	}
	if (extdirs != null) {
	    argList.addElement("-extdirs");
	    argList.addElement(extdirs);
	}

	project.log("Compilation args: " + argList.toString(),
		    project.MSG_VERBOSE);
	
	String[] args = new String[argList.size() + compileList.size()];
	int counter = 0; 
	
	for (int i = 0; i < argList.size(); i++) {
	    args[i] = (String)argList.elementAt(i);
	    counter++;
	}

	// XXX
	// should be using system independent line feed!
	
	StringBuffer niceSourceList = new StringBuffer("Files to be compiled:"
						       + "\r\n");

	Enumeration enum = compileList.elements();
	while (enum.hasMoreElements()) {
	    args[counter] = (String)enum.nextElement();
	    niceSourceList.append("    " + args[counter] + "\r\n");
	    counter++;
	}

	project.log(niceSourceList.toString(), project.MSG_VERBOSE);

	// XXX
	// provide the compiler a different message sink - namely our own

        JavacOutputStream jos = new JavacOutputStream(project);
        
	sun.tools.javac.Main compiler =
	    new sun.tools.javac.Main(jos, "javac");
	compiler.compile(args);
        if (jos.getErrorFlag()) {
            String msg = "Compile failed, messages should have been provided.";
            throw new BuildException(msg);
        }
    } 

    /**
     * Performs a compile using the newer compiler that ships with JDK 1.3
     */
    
    private void doModernCompile() throws BuildException {
	project.log("Performing a Modern Compile");
    }

    /**
     * Performs a compile using the Jikes compiler from IBM..
     * Mostly of this code is identical to doClassicCompile()
     * However, it does not support all options like
     * bootclasspath, extdirs, deprecation and so on, because
     * there is no option in jikes and I don't understand
     * what they should do.
     *
     * It has been successfully tested with jikes 1.10
     *
     * @author skanthak@muehlheim.de
     */
    
    private void doJikesCompile() throws BuildException {
	project.log("Using jikes compiler",project.MSG_VERBOSE);
	String classpath = getCompileClasspath();
	Vector argList = new Vector();

	if (deprecation == true)
	    argList.addElement("-deprecation");
	
	// We want all output on stdout to make
	// parsing easier
	argList.addElement("-Xstdout");
	
	argList.addElement("-d");
	argList.addElement(destDir.getAbsolutePath());
	argList.addElement("-classpath");
	// Jikes has no option for source-path so we
	// will add it to classpath.
	// XXX is this correct?
	argList.addElement(classpath+File.pathSeparator +
			   srcDir.getAbsolutePath());
	if (debug) {
	    argList.addElement("-g");
	}
	if (optimize) {
	    argList.addElement("-O");
	}

       /**
        * XXX
        * Perhaps we shouldn't use properties for these
        * two options (emacs mode and warnings),
        * but include it in the javac directive?
        */

       /**
        * Jikes has the nice feature to print error
        * messages in a form readable by emacs, so
        * that emcas can directly set the cursor
        * to the place, where the error occured.
        */
       boolean emacsMode = false;
       String emacsProperty = project.getProperty("build.compiler.emacs");
       if (emacsProperty != null &&
           (emacsProperty.equalsIgnoreCase("on") ||
            emacsProperty.equalsIgnoreCase("true"))
           ) {
           emacsMode = true;
       }

       /**
        * Jikes issues more warnings that javac, for
        * example, when you have files in your classpath
        * that don't exist. As this is often the case, these
        * warning can be pretty annoying.
        */
       boolean warnings = true;
       String warningsProperty = project.getProperty("build.compiler.warnings");
       if (warningsProperty != null &&
           (warningsProperty.equalsIgnoreCase("off") ||
            warningsProperty.equalsIgnoreCase("false"))
           ) {
           warnings = false;
       }

       if (emacsMode)
           argList.addElement("+E");

       if (!warnings)
           argList.addElement("-nowarn");
	
	project.log("Compilation args: " + argList.toString(),
		    project.MSG_VERBOSE);
	
	String[] args = new String[argList.size() + compileList.size()];
	int counter = 0; 
	
	for (int i = 0; i < argList.size(); i++) {
	    args[i] = (String)argList.elementAt(i);
	    counter++;
	}

	// XXX
	// should be using system independent line feed!
	
	StringBuffer niceSourceList = new StringBuffer("Files to be compiled:"
						       + "\r\n");

	Enumeration enum = compileList.elements();
	while (enum.hasMoreElements()) {
	    args[counter] = (String)enum.nextElement();
	    niceSourceList.append("    " + args[counter] + "\r\n");
	    counter++;
	}

	project.log(niceSourceList.toString(), project.MSG_VERBOSE);

	// XXX
	// provide the compiler a different message sink - namely our own
	
	JikesOutputParser jop = new JikesOutputParser(project,emacsMode);
	
	Jikes compiler = new Jikes(jop,"jikes");
	compiler.compile(args);
	if (jop.getErrorFlag()) {
	    String msg = "Compile failed, messages should have been provided.";
	    throw new BuildException(msg);
	}
    }
}
