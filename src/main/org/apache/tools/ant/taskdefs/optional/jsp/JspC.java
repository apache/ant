/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.jsp;

import java.io.File;
import java.util.Date;

import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;

import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.optional.jsp.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.optional.jsp.compilers.CompilerAdapterFactory;

import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/** Ant task to run the jsp compiler.
 * <p> This task takes the given jsp files and compiles them into java
 * files. It is then up to the user to compile the java files into classes.
 *
 * <p> The task requires the srcdir and destdir attributes to be
 * set. This Task is a MatchingTask, so the files to be compiled can be
 * specified using includes/excludes attributes or nested include/exclude
 * elements. Optional attributes are verbose (set the verbosity level passed
 * to jasper), package (name of the destination package for generated java
 * classes and classpath (the classpath to use when running the jsp
 * compiler).
 * <p> This task supports the nested elements classpath (A Path) and
 * classpathref (A Reference) which can be used in preference to the
 * attribute classpath, if the jsp compiler is not already in the ant
 * classpath.
 *
 * <p><h4>Notes</h4>
 * <p> At present, this task only supports the jasper compiler. In future,
 other compilers will be supported by setting the jsp.compiler property.
 *
 * <p><h4>Usage</h4>
 * <pre>
 * &lt;jspc srcdir="${basedir}/src/war"
 *       destdir="${basedir}/gensrc"
 *       package="com.i3sp.jsp"
 *       verbose="9"&gt;
 *   &lt;include name="**\/*.jsp" /&gt;
 * &lt;/jspc&gt;
 * </pre>
 *
 * @version $Revision$ $Date$
 * @author <a href="mailto:mattw@i3sp.com">Matthew Watson</a>
 * <p> Large Amount of cutting and pasting from the Javac task...
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */
public class JspC extends MatchingTask
{
    /* ------------------------------------------------------------ */
    private Path classpath;
    private Path src;
    private File destDir;
    private String packageName ;
    private String iepluginid ;
    private boolean mapped ;
    private int verbose = 0;
    protected Vector compileList = new Vector();
    protected boolean failOnError = true;
	
    /**
     *  -uribase <dir>The uri directory compilations should be relative to
     *  (Default is "/")
     */

    private File uribase;

    /**
     *  -uriroot <dir>The root directory that uri files should be resolved
     *  against, 
     */
    private File uriroot;
	
    private final static String FAIL_MSG
        = "Compile failed, messages should have been provided.";
    /* ------------------------------------------------------------ */
    /**
     * Set the source dirs to find the source JSP files.
     */
    public void setSrcdir(Path srcDir) {
        if (src == null) {
            src = srcDir;
        } else {
            src.append(srcDir);
        }
    }
    public Path getSrcDir(){
        return src;
    }
    /* ------------------------------------------------------------ */
    /**
     * Set the destination directory into which the JSP source
     * files should be compiled.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }
    public File getDestdir(){
        return destDir;
    }
    /* ------------------------------------------------------------ */
    /**
     * Set the name of the package the compiled jsp files should be in
     */
    public void setPackage(String pkg){
        this.packageName = pkg;
    }
    public String getPackage(){
        return packageName;
    }
    /* ------------------------------------------------------------ */
    /**
     * Set the verbose level of the compiler
     */
    public void setVerbose(int i){
        verbose = i;
    }
    public int getVerbose(){
        return verbose;
    }
    /* ------------------------------------------------------------ */
    /**
     * Throw a BuildException if compilation fails
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }
    /**
     * Gets the failonerror flag.
     */
    public boolean getFailonerror() {
        return failOnError;
    }
    /* ------------------------------------------------------------ */
    public String getIeplugin()
    {
        return iepluginid;
    }
    /** Set the ieplugin id */
    public void setIeplugin(String iepluginid_)
    {
        iepluginid = iepluginid_;
    }
    /* ------------------------------------------------------------ */
    public boolean isMapped()
    {
        return mapped;
    }
    /** set the mapped flag */
    public void setMapped(boolean mapped_)
    {
        mapped = mapped_;
    }
	
	    /**
     *  -uribase. the uri context of relative URI 
     * references in the JSP pages. If it does not 
     * exist then it is derived from the location of the file
     * relative to the declared or derived value of -uriroot. 
     *
     * @param  uribase  The new Uribase value
     */
    public void setUribase(File uribase) {
        this.uribase = uribase;
    }

	public File getUribase() {
		return uriroot;
	}

    /**
     *  -uriroot <dir>The root directory that uri files should be resolved
     *  against, (Default is the directory jspc is invoked from)
     *
     * @param  uriroot  The new Uribase value
     */
    public void setUriroot(File uriroot) {
        this.uriroot = uriroot;
    }

	public File getUriroot() {
		return uriroot;
	}
	
	
    /* ------------------------------------------------------------ */
    /** Set the classpath to be used for this compilation */
    public void setClasspath(Path cp) {
        if (classpath == null)
            classpath = cp;
        else
            classpath.append(cp);
    }
    /** Maybe creates a nested classpath element. */
    public Path createClasspath() {
        if (classpath == null)
            classpath = new Path(project);
        return classpath.createPath();
    }
    /** Adds a reference to a CLASSPATH defined elsewhere */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }
    public Path getClasspath(){
        return classpath;
    }
    /* ------------------------------------------------------------ */
    public Vector getCompileList(){
        return compileList;
    }
    /* ------------------------------------------------------------ */
    public void execute()
        throws BuildException
    {
        // first off, make sure that we've got a srcdir
        if (src == null) {
            throw new BuildException("srcdir attribute must be set!",
                                     location);
        }
        String [] list = src.list();
        if (list.length == 0) {
            throw new BuildException("srcdir attribute must be set!",
                                     location);
        }

        if (destDir != null && !destDir.isDirectory()) {
            throw new
                BuildException("destination directory \"" + destDir +
                               "\" does not exist or is not a directory",
                               location);
        }

        // calculate where the files will end up:
        File dest = null;
        if (packageName == null)
            dest = destDir;
        else {
            String path = destDir.getPath() + File.separatorChar +
                packageName.replace('.', File.separatorChar);
            dest = new File(path);
        }

        // scan source directories and dest directory to build up both copy
        // lists and compile lists
        resetFileLists();
        for (int i = 0; i < list.length; i++) {
            File srcDir = (File)project.resolveFile(list[i]);
            if (!srcDir.exists()) {
                throw new BuildException("srcdir \"" + srcDir.getPath() +
                                         "\" does not exist!", location);
            }

            DirectoryScanner ds = this.getDirectoryScanner(srcDir);

            String[] files = ds.getIncludedFiles();

            scanDir(srcDir, dest, files);
        }

        // compile the source files

        String compiler = project.getProperty("jsp.compiler");
        if (compiler == null) {
            compiler = "jasper";
        }

        if (compileList.size() > 0) {

            CompilerAdapter adapter =
                CompilerAdapterFactory.getCompiler(compiler, this);
            log("Compiling " + compileList.size() +
                " source file"
                + (compileList.size() == 1 ? "" : "s")
                + (destDir != null ? " to " + destDir : ""));

            // now we need to populate the compiler adapter
            adapter.setJspc( this );

            // finally, lets execute the compiler!!
            if (!adapter.execute()) {
                if (failOnError) {
                    throw new BuildException(FAIL_MSG, location);
                }
                else {
                    log(FAIL_MSG, Project.MSG_ERR);
                }
            }
        }
    }
    /* ------------------------------------------------------------ */
    /**
     * Clear the list of files to be compiled and copied..
     */
    protected void resetFileLists() {
        compileList.removeAllElements();
    }
    /* ------------------------------------------------------------ */
    /**
     * Scans the directory looking for source files to be compiled.
     * The results are returned in the class variable compileList
     */
    protected void scanDir(File srcDir, File destDir, String files[]) {

        long now = (new Date()).getTime();

        for (int i = 0; i < files.length; i++) {
            File srcFile = new File(srcDir, files[i]);
            if (files[i].endsWith(".jsp")) {
                // drop leading path (if any)
                int fileStart =
                    files[i].lastIndexOf(File.separatorChar) + 1;
                File javaFile = new File(destDir, files[i].substring(fileStart,
                                                                     files[i].indexOf(".jsp")) + ".java");

                if (srcFile.lastModified() > now) {
                    log("Warning: file modified in the future: " +
                        files[i], Project.MSG_WARN);
                }

                if (!javaFile.exists() ||
                    srcFile.lastModified() > javaFile.lastModified())
                {
                    if (!javaFile.exists()) {
                        log("Compiling " + srcFile.getPath() +
                            " because java file "
                            + javaFile.getPath() + " does not exist",
                            Project.MSG_DEBUG);
                    } else {
                        log("Compiling " + srcFile.getPath() +
                            " because it is out of date with respect to "
                            + javaFile.getPath(), Project.MSG_DEBUG);
                    }
                    compileList.addElement(srcFile.getAbsolutePath());
                }
            }
        }
    }
    /* ------------------------------------------------------------ */
}
