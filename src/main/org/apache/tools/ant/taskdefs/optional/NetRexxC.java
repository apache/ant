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
package org.apache.tools.ant.taskdefs.optional;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import netrexx.lang.Rexx;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;

/**
 * Task to compile NetRexx source files. This task can take the following
 * arguments:
 * <ul>
 * <li>binary</li>
 * <li>classpath</li>
 * <li>comments</li>
 * <li>compile</li>
 * <li>console</li>
 * <li>crossref</li>
 * <li>decimal</li>
 * <li>destdir</li>
 * <li>diag</li>
 * <li>explicit</li>
 * <li>format</li>
 * <li>keep</li>
 * <li>logo</li>
 * <li>replace</li>
 * <li>savelog</li>
 * <li>srcdir</li>
 * <li>sourcedir</li>
 * <li>strictargs</li>
 * <li>strictassign</li>
 * <li>strictcase</li>
 * <li>strictimport</li>
 * <li>symbols</li>
 * <li>time</li>
 * <li>trace</li>
 * <li>utf8</li>
 * <li>verbose</li>
 * </ul>
 * Of these arguments, the <b>srcdir</b> argument is required.
 *
 * <p>When this task executes, it will recursively scan the srcdir
 * looking for NetRexx source files to compile. This task makes its
 * compile decision based on timestamp.
 * <p>Before files are compiled they and any other file in the
 * srcdir will be copied to the destdir allowing support files to be
 * located properly in the classpath. The reason for copying the source files
 * before the compile is that NetRexxC has only two destinations for classfiles:
 * <ol>
 * <li>The current directory, and,</li>
 * <li>The directory the source is in (see sourcedir option)
 * </ol>
 *
 * @author dIon Gillard <a href="mailto:dion@multitask.com.au">dion@multitask.com.au</a>
 */

public class NetRexxC extends MatchingTask {

    // variables to hold arguments
    private boolean binary;
    private String classpath;
    private boolean comments;
    private boolean compact;
    private boolean compile = true;
    private boolean console;
    private boolean crossref;
    private boolean decimal = true;
    private File destDir;
    private boolean diag;
    private boolean explicit;
    private boolean format;
    private boolean java;
    private boolean keep;
    private boolean logo = true;
    private boolean replace;
    private boolean savelog;
    private File srcDir;
    private boolean sourcedir = true; // ?? Should this be the default for ant?
    private boolean strictargs;
    private boolean strictassign;
    private boolean strictcase;
    private boolean strictimport;
    private boolean strictprops;
    private boolean strictsignal;
    private boolean symbols;
    private boolean time;
    private String trace = "trace2";
    private boolean utf8;
    private String verbose = "verbose3";

    // other implementation variables
    private Vector compileList = new Vector();
    private Hashtable filecopyList = new Hashtable();
    private String oldClasspath = System.getProperty("java.class.path");


    /**
     * Set whether literals are treated as binary, rather than NetRexx types
     */
    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    /**
     * Set the classpath used for NetRexx compilation
     */
    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    /**
     * Set whether comments are passed through to the generated java source.
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false
     */
    public void setComments(boolean comments) {
        this.comments = comments;
    }

    /**
     * Set whether error messages come out in compact or verbose format.
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false
     */
    public void setCompact(boolean compact) {
        this.compact = compact;
    }

    /**
     * Set whether the NetRexx compiler should compile the generated java code
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is true.
     * Setting this flag to false, will automatically set the keep flag to true.
     */
    public void setCompile(boolean compile) {
        this.compile = compile;
        if (!this.compile && !this.keep) this.keep = true;
    }

    /**
     * Set whether or not messages should be displayed on the 'console'
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is true.
     */
    public void setConsole(boolean console) {
        this.console = console;
    }

    /**
     * Whether variable cross references are generated
     */
    public void setCrossref(boolean crossref) {
        this.crossref = crossref;
    }

    /**
     * Set whether decimal arithmetic should be used for the netrexx code.
     * Binary arithmetic is used when this flag is turned off.
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is true.
     */
    public void setDecimal(boolean decimal) {
        this.decimal = decimal;
    }

    /**
     * Set the destination directory into which the NetRexx source
     * files should be copied and then compiled.
     */
    public void setDestDir(String destDirName) {
        destDir = project.resolveFile(destDirName);
    }

    /**
     * Whether diagnostic information about the compile is generated
     */
    public void setDiag(boolean diag) {
        this.diag = diag;
    }

    /**
     * Sets whether variables must be declared explicitly before use.
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    /**
     * Whether the generated java code is formatted nicely or left to match NetRexx
     * line numbers for call stack debugging
     */
    public void setFormat(boolean format) {
        this.format = format;
    }

    /**
     * Whether the generated java code is produced
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setJava(boolean java) {
        this.java = java;
    }


    /**
     * Sets whether the generated java source file should be kept after compilation.
     * The generated files will have an extension of .java.keep, <b>not</b> .java
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setKeep(boolean keep) {
        this.keep = keep;
    }

    /**
     * Whether the compiler text logo is displayed when compiling
     */
    public void setLogo(boolean logo) {
        this.logo = logo;
    }

    /**
     * Whether the generated .java file should be replaced when compiling
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    /**
     * Sets whether the compiler messages will be written to NetRexxC.log as
     * well as to the console
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setSavelog(boolean savelog) {
        this.savelog = savelog;
    }

    /**
     * Tells the NetRexx compiler to store the class files in the same directory
     * as the source files. The alternative is the working directory
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is true.
     */
    public void setSourcedir(boolean sourcedir) {
        this.sourcedir = sourcedir;
    }

    /**
     * Set the source dir to find the source Java files.
     */
    public void setSrcDir(String srcDirName) {
        srcDir = project.resolveFile(srcDirName);
    }

    /**
     * Tells the NetRexx compiler that method calls always need parentheses,
     * even if no arguments are needed, e.g. <code>aStringVar.getBytes</code>
     * vs. <code>aStringVar.getBytes()</code>
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setStrictargs(boolean strictargs) {
        this.strictargs = strictargs;
    }

    /**
     * Tells the NetRexx compile that assignments must match exactly on type
     */
    public void setStrictassign(boolean strictassign) {
        this.strictassign = strictassign;
    }

    /**
     * Specifies whether the NetRexx compiler should be case sensitive or not
     */
    public void setStrictcase(boolean strictcase) {
        this.strictcase = strictcase;
    }

    /**
     * Sets whether classes need to be imported explicitly using an
     * <code>import</code> statement. By default the NetRexx compiler will import
     * certain packages automatically
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setStrictimport(boolean strictimport) {
        this.strictimport = strictimport;
    }

    /**
     * Sets whether local properties need to be qualified explicitly using <code>this</code>
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setStrictprops(boolean strictprops) {
        this.strictprops = strictprops;
    }


    /**
     * Whether the compiler should force catching of exceptions by explicitly named types
     */
    public void setStrictsignal(boolean strictsignal) {
        this.strictsignal = strictsignal;
    }

    /**
     * Sets whether debug symbols should be generated into the class file
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setSymbols(boolean symbols) {
        this.symbols = symbols;
    }

    /**
     * Asks the NetRexx compiler to print compilation times to the console
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setTime(boolean time) {
        this.time = time;
    }

    /**
     * Turns on or off tracing and directs the resultant trace output
     * Valid values are: "trace", "trace1", "trace2" and "notrace".
     * "trace" and "trace2"
     */
    public void setTrace(String trace) {
        if (trace.equalsIgnoreCase("trace")
            || trace.equalsIgnoreCase("trace1")
            || trace.equalsIgnoreCase("trace2")
            || trace.equalsIgnoreCase("notrace")) {
            this.trace = trace;
        } else {
            throw new BuildException("Unknown trace value specified: '" + trace + "'");
        }
    }

    /**
     * Tells the NetRexx compiler that the source is in UTF8
     * Valid true values are "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     */
    public void setUtf8(boolean utf8) {
        this.utf8 = utf8;
    }

    /**
     * Whether lots of warnings and error messages should be generated
     */
    public void setVerbose(String verbose) {
        this.verbose = verbose;
    }

    /**
     * Executes the task, i.e. does the actual compiler call
     */
    public void execute() throws BuildException {

        // first off, make sure that we've got a srcdir and destdir
        if (srcDir == null || destDir == null ) {
            throw new BuildException("srcDir and destDir attributes must be set!");
        }

        // scan source and dest dirs to build up both copy lists and
        // compile lists
        //        scanDir(srcDir, destDir);
        DirectoryScanner ds = getDirectoryScanner(srcDir);

        String[] files = ds.getIncludedFiles();

        scanDir(srcDir, destDir, files);

        // copy the source and support files
        copyFilesToDestination();

        // compile the source files
        if (compileList.size() > 0) {
            log("Compiling " + compileList.size() + " source file"
                + (compileList.size() == 1 ? "" : "s")
                + " to " + destDir);
            doNetRexxCompile();
        }
    }

    /**
     * Scans the directory looking for source files to be compiled and
     * support files to be copied.
     */
    private void scanDir(File srcDir, File destDir, String[] files) {
        for (int i = 0; i < files.length; i++) {
            File srcFile = new File(srcDir, files[i]);
            File destFile = new File(destDir, files[i]);
            String filename = files[i];
            // if it's a non source file, copy it if a later date than the
            // dest
            // if it's a source file, see if the destination class file
            // needs to be recreated via compilation
            if (filename.toLowerCase().endsWith(".nrx")) {
                File classFile = 
                    new File(destDir, 
                             filename.substring(0, filename.lastIndexOf('.')) + ".class");

                if (!compile || srcFile.lastModified() > classFile.lastModified()) {
                    filecopyList.put(srcFile.getAbsolutePath(), destFile.getAbsolutePath());
                    compileList.addElement(destFile.getAbsolutePath());
                }
            } else {
                if (srcFile.lastModified() > destFile.lastModified()) {
                    filecopyList.put(srcFile.getAbsolutePath(), destFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Copy eligible files from the srcDir to destDir
     */
    private void copyFilesToDestination() {
        if (filecopyList.size() > 0) {
            log("Copying " + filecopyList.size() + " file"
                + (filecopyList.size() == 1 ? "" : "s")
                + " to " + destDir.getAbsolutePath());
            Enumeration enum = filecopyList.keys();
            while (enum.hasMoreElements()) {
                String fromFile = (String)enum.nextElement();
                String toFile = (String)filecopyList.get(fromFile);
                try {
                    project.copyFile(fromFile, toFile);
                } catch (IOException ioe) {
                    String msg = "Failed to copy " + fromFile + " to " + toFile
                        + " due to " + ioe.getMessage();
                    throw new BuildException(msg, ioe);
                }
            }
        }
    }

    /**
     * Peforms a copmile using the NetRexx 1.1.x compiler
     */
    private void doNetRexxCompile() throws BuildException {
        log("Using NetRexx compiler", Project.MSG_VERBOSE);
        String classpath = getCompileClasspath();
        StringBuffer compileOptions = new StringBuffer();
        StringBuffer fileList = new StringBuffer();

        // create an array of strings for input to the compiler: one array
        // comes from the compile options, the other from the compileList
        String[] compileOptionsArray = getCompileOptionsAsArray();
        String[] fileListArray = new String[compileList.size()];
        Enumeration e = compileList.elements();
        int j = 0;
        while (e.hasMoreElements()) {
            fileListArray[j] = (String)e.nextElement();
            j++;
        }
        // create a single array of arguments for the compiler
        String compileArgs[] = new String[compileOptionsArray.length + fileListArray.length];
        for (int i = 0; i < compileOptionsArray.length; i++) {
            compileArgs[i] = compileOptionsArray[i];
        }
        for (int i = 0; i < fileListArray.length; i++) {
            compileArgs[i+compileOptionsArray.length] = fileListArray[i];
        }

        // print nice output about what we are doing for the log
        compileOptions.append("Compilation args: ");
        for (int i = 0; i < compileOptionsArray.length; i++) {
            compileOptions.append(compileOptionsArray[i]);
            compileOptions.append(" ");
        }
        log(compileOptions.toString(), Project.MSG_VERBOSE);

        String eol = System.getProperty("line.separator");
        StringBuffer niceSourceList = new StringBuffer("Files to be compiled:" + eol);

        for (int i = 0; i < compileList.size(); i++) {
            niceSourceList.append("    ");
            niceSourceList.append(compileList.elementAt(i).toString());
            niceSourceList.append(eol);
        }

        log(niceSourceList.toString(), Project.MSG_VERBOSE);

        // need to set java.class.path property and restore it later
        // since the NetRexx compiler has no option for the classpath
        String currentClassPath = System.getProperty("java.class.path");
        Properties currentProperties = System.getProperties();
        currentProperties.put("java.class.path", classpath);

        try {
            StringWriter out = new StringWriter(); 
            int rc = COM.ibm.netrexx.process.NetRexxC.
                main(new Rexx(compileArgs), new PrintWriter(out));

            if (rc > 1) { // 1 is warnings from real NetRexxC
                log(out.toString(), Project.MSG_ERR);
                String msg = "Compile failed, messages should have been provided.";
                throw new BuildException(msg);
            }
            else if (rc == 1) {
                log(out.toString(), Project.MSG_WARN);
            }
            else {
                log(out.toString(), Project.MSG_INFO);
            }        
        } finally {
            // need to reset java.class.path property
            // since the NetRexx compiler has no option for the classpath
            currentProperties = System.getProperties();
            currentProperties.put("java.class.path", currentClassPath);
        }
    }

    /**
     * Builds the compilation classpath.
     */
    private String getCompileClasspath() {
        StringBuffer classpath = new StringBuffer();

        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath
        classpath.append(destDir.getAbsolutePath());

        // add our classpath to the mix
        if (this.classpath != null) {
            addExistingToClasspath(classpath, this.classpath);
        }

        // add the system classpath
        // addExistingToClasspath(classpath,System.getProperty("java.class.path"));
        return classpath.toString();
    }

    /**
     * This
     */
    private String[] getCompileOptionsAsArray() {
        Vector options = new Vector();
        options.addElement(binary ? "-binary" : "-nobinary");
        options.addElement(comments ? "-comments" : "-nocomments");
        options.addElement(compile ? "-compile" : "-nocompile");
        options.addElement(compact ? "-compact" : "-nocompact");
        options.addElement(console ? "-console" : "-noconsole");
        options.addElement(crossref ? "-crossref" : "-nocrossref");
        options.addElement(decimal ? "-decimal" : "-nodecimal");
        options.addElement(diag ? "-diag" : "-nodiag");
        options.addElement(explicit ? "-explicit": "-noexplicit");
        options.addElement(format ? "-format" : "-noformat");
        options.addElement(keep ? "-keep" : "-nokeep");
        options.addElement(logo ? "-logo" : "-nologo");
        options.addElement(replace ? "-replace" : "-noreplace");
        options.addElement(savelog ? "-savelog" : "-nosavelog");
        options.addElement(sourcedir ? "-sourcedir" : "-nosourcedir");
        options.addElement(strictargs ? "-strictargs" : "-nostrictargs");
        options.addElement(strictassign ? "-strictassign" : "-nostrictassign");
        options.addElement(strictcase ? "-strictcase": "-nostrictcase");
        options.addElement(strictimport ? "-strictimport" : "-nostrictimport");
        options.addElement(strictprops ? "-strictprops" : "-nostrictprops");
        options.addElement(strictsignal ? "-strictsignal" : "-nostrictsignal");
        options.addElement(symbols ? "-symbols" : "-nosymbols");
        options.addElement(time ? "-time" : "-notime");
        options.addElement("-" + trace);
        options.addElement(utf8 ? "-utf8" : "-noutf8");
        options.addElement("-" + verbose);
        String[] results = new String[options.size()];
        options.copyInto(results);
        return results;
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
                log("Dropping from classpath: "+
                    f.getAbsolutePath(), Project.MSG_VERBOSE);
            }
        }

    }
}
