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

package org.apache.tools.ant.taskdefs.optional.jsp;

import java.io.File;
import java.time.Instant;
import java.util.Vector;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.optional.jsp.compilers.JspCompilerAdapter;
import org.apache.tools.ant.taskdefs.optional.jsp.compilers.JspCompilerAdapterFactory;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Runs a JSP compiler.
 *
 * <p>This task takes the given jsp files and compiles them into java
 * files. It is then up to the user to compile the java files into classes.</p>
 *
 * <p>The task requires the srcdir and destdir attributes to be
 * set. This Task is a MatchingTask, so the files to be compiled can be
 * specified using includes/excludes attributes or nested include/exclude
 * elements. Optional attributes are verbose (set the verbosity level passed
 * to jasper), package (name of the destination package for generated java
 * classes and classpath (the classpath to use when running the jsp
 * compiler).</p>
 * <p>This task supports the nested elements classpath (a Path) and
 * classpathref (a Reference) which can be used in preference to the
 * attribute classpath, if the jsp compiler is not already in the ant
 * classpath.</p>
 *
 * <h2>Usage</h2>
 * <pre>
 * &lt;jspc srcdir="${basedir}/src/war"
 *       destdir="${basedir}/gensrc"
 *       package="com.i3sp.jsp"
 *       verbose="9"&gt;
 *   &lt;include name="**\/*.jsp" /&gt;
 * &lt;/jspc&gt;
 * </pre>
 *
 * <p>Large amount of cutting and pasting from the Javac task...</p>
 * @since 1.5
 */
public class JspC extends MatchingTask {
    private Path classpath;
    private Path compilerClasspath;
    private Path src;
    private File destDir;
    private String packageName;
    /** name of the compiler to use */
    private String compilerName = "jasper";

    /**
     *  -ieplugin &lt;clsid&gt; Java Plugin classid for Internet Explorer
     */
    private String iepluginid;
    private boolean mapped;
    private int verbose = 0;
    // CheckStyle:VisibilityModifier OFF - bc
    protected Vector<String> compileList = new Vector<>();
    Vector<File> javaFiles = new Vector<>();

    /**
     *  flag to control action on execution trouble
     */
    protected boolean failOnError = true;

    /**
     *  -uriroot &lt;dir&gt; The root directory that uri files should be resolved
     *  against,
     */
    private File uriroot;

    /**
     *  -webinc &lt;file&gt; Creates partial servlet mappings for the -webapp option
     */
    private File webinc;

    /**
     *  -webxml &lt;file&gt; Creates a complete web.xml when using the -webapp option.
     */

    private File webxml;

    /**
     *  web apps
     */
    protected WebAppParameter webApp;



    private static final String FAIL_MSG
        = "Compile failed, messages should have been provided.";

    // CheckStyle:VisibilityModifier ON

    /**
     * Set the path for source JSP files.
     * @param srcDir the source path.
     */
    public void setSrcDir(Path srcDir) {
        if (src == null) {
            src = srcDir;
        } else {
            src.append(srcDir);
        }
    }

    /**
     * Get the source dir.
     * @return the source path.
     */
    public Path getSrcDir() {
        return src;
    }

    /**
     * Set the destination directory into which the JSP source
     * files should be compiled.
     * @param destDir the destination directory.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Get the destination directory.
     * @return the directory.
     */
    public File getDestdir() {
        return destDir;
    }

    /**
     * Set the name of the package the compiled jsp files should be in.
     * @param pkg the name of the package.
     */
    public void setPackage(String pkg) {
        this.packageName = pkg;
    }

    /**
     * Get the name of the package.
     * @return the package.
     */
    public String getPackage() {
        return packageName;
    }

    /**
     * Set the verbose level of the compiler
     * @param i the verbose level to use.
     */
    public void setVerbose(int i) {
        verbose = i;
    }

    /**
     * Get the verbose level.
     * @return the level.
     */
    public int getVerbose() {
        return verbose;
    }

    /**
     * Whether or not the build should halt if compilation fails.
     * Defaults to <code>true</code>.
     * @param fail a <code>boolean</code> value.
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }
    /**
     * Gets the failonerror flag.
     * @return the flag.
     */
    public boolean getFailonerror() {
        return failOnError;
    }

    /**
     * Get the IE CLASSID value.
     * @return the value.
     */
    public String getIeplugin() {
        return iepluginid;
    }

    /**
     * Java Plugin CLASSID for Internet Explorer
     * @param iepluginid the id to use.
     */
    public void setIeplugin(String iepluginid) {
        this.iepluginid = iepluginid;
    }

    /**
     * If true, generate separate write() calls for each HTML line
     * in the JSP.
     * @return mapping status
     */
    public boolean isMapped() {
        return mapped;
    }

    /**
     * If true, generate separate write() calls for each HTML line
     * in the JSP.
     * @param mapped a <code>boolean</code> value.
     */
    public void setMapped(boolean mapped) {
        this.mapped = mapped;
    }

    /**
     * The URI context of relative URI references in the JSP pages.
     * If it does not exist then it is derived from the location
     * of the file relative to the declared or derived value of uriroot.
     *
     * @param  uribase  The new Uribase value
     */
    public void setUribase(File uribase) {
        log("Uribase is currently an unused parameter", Project.MSG_WARN);
    }

    /**
     * Get the uri base value.
     * @return the value.
     */
    public File getUribase() {
        return uriroot;
    }

    /**
     *  The root directory that uri files should be resolved
     *  against. (Default is the directory jspc is invoked from)
     *
     * @param  uriroot  The new Uribase value
     */
    public void setUriroot(File uriroot) {
        this.uriroot = uriroot;
    }

    /**
     * Get the uri root value.
     * @return the value.
     */
    public File getUriroot() {
        return uriroot;
    }

    /**
     * Set the classpath to be used for this compilation.
     * @param cp the path to be used.
     */
    public void setClasspath(Path cp) {
        if (classpath == null) {
            classpath = cp;
        } else {
            classpath.append(cp);
        }
    }

    /**
     * Adds a path to the classpath.
     * @return a path to be configured.
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere
     * @param r a reference to a classpath.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Get the classpath.
     * @return the classpath.
     */
    public Path getClasspath() {
        return classpath;
    }

    /**
     * Set the classpath to be used to find this compiler adapter
     * @param cp the compiler classpath.
     */
    public void setCompilerclasspath(Path cp) {
        if (compilerClasspath == null) {
            compilerClasspath = cp;
        } else {
            compilerClasspath.append(cp);
        }
    }

    /**
     * get the classpath used to find the compiler adapter
     * @return the compiler classpath.
     */
    public Path getCompilerclasspath() {
        return compilerClasspath;
    }

    /**
     * Support nested compiler classpath, used to locate compiler adapter
     * @return a path to be configured.
     */
    public Path createCompilerclasspath() {
        if (compilerClasspath == null) {
            compilerClasspath = new Path(getProject());
        }
        return compilerClasspath.createPath();
    }

    /**
     *  Filename for web.xml.
     *
     * @param  webxml  The new Webxml value
     */
    public void setWebxml(File webxml) {
        this.webxml = webxml;
    }

    /**
     * Filename for web.xml.
     * @return The filename for web.xml.
     */
    public File getWebxml() {
        return this.webxml;
    }

    /**
     *  output filename for the fraction of web.xml that lists
     *  servlets.
     * @param  webinc  The new Webinc value
     */
    public void setWebinc(File webinc) {
        this.webinc = webinc;
    }

    /**
     * Get the webinc attribute.
     * @return the webinc attribute.
     */
    public File getWebinc() {
        return this.webinc;
    }

    /**
     * Adds a single webapp.
     *
     * @param  webappParam  add a web app parameter
     * @throws BuildException if more than one webapp is specified.
     */
    public void addWebApp(WebAppParameter webappParam)
        throws BuildException {
        //demand create vector of filesets
        if (webApp == null) {
            webApp = webappParam;
        } else {
            throw new BuildException("Only one webapp can be specified");
        }
    }

    /**
     * Get the web app.
     * @return the web app attribute.
     */
    public WebAppParameter getWebApp() {
        return webApp;
    }

    /**
     * Class name of a JSP compiler adapter.
     * @param compiler the compiler class name.
     */
    public void setCompiler(String compiler) {
        this.compilerName = compiler;
    }

    /**
     * get the list of files to compile
     * @return the list of files.
     */
    public Vector<String> getCompileList() {
        return compileList;
    }

    /**
     * execute by building up a list of files that
     * have changed and hand them off to a jsp compiler
     * @throws BuildException on error.
     */
    @Override
    public void execute()
        throws BuildException {

        // make sure that we've got a destdir
        if (destDir == null) {
            throw new BuildException("destdir attribute must be set!",
                                     getLocation());
        }

        if (!destDir.isDirectory()) {
            throw new BuildException("destination directory \"" + destDir
                                     + "\" does not exist or is not a directory",
                                     getLocation());
        }

        File dest = getActualDestDir();

        try (AntClassLoader al = getProject().createClassLoader(compilerClasspath)) {
            //bind to a compiler
            JspCompilerAdapter compiler =
                JspCompilerAdapterFactory.getCompiler(compilerName, this, al);

            //if we are a webapp, hand off to the compiler, which had
            //better handle it
            if (webApp != null) {
                doCompilation(compiler);
                return;
            }

            // make sure that we've got a srcdir
            if (src == null) {
                throw new BuildException("srcdir attribute must be set!",
                                         getLocation());
            }
            String[] list = src.list();
            if (list.length == 0) {
                throw new BuildException("srcdir attribute must be set!",
                                         getLocation());
            }


            // if the compiler does its own dependency stuff, we just
            // call it right now
            if (compiler.implementsOwnDependencyChecking()) {
                doCompilation(compiler);
                return;
            }

            //the remainder of this method is only for compilers that
            //need their dependency work done
            JspMangler mangler = compiler.createMangler();

            // scan source directories and dest directory to build up both copy
            // lists and compile lists
            resetFileLists();
            int filecount = 0;
            for (String fileName : list) {
                File srcDir = getProject().resolveFile(fileName);
                if (!srcDir.exists()) {
                    throw new BuildException("srcdir \"" + srcDir.getPath()
                                             + "\" does not exist!",
                                             getLocation());
                }
                DirectoryScanner ds = this.getDirectoryScanner(srcDir);
                String[] files = ds.getIncludedFiles();
                filecount = files.length;
                scanDir(srcDir, dest, mangler, files);
            }

            // compile the source files

            log("compiling " + compileList.size() + " files",
                Project.MSG_VERBOSE);

            if (!compileList.isEmpty()) {

                log("Compiling " + compileList.size() + " source file"
                    + (compileList.size() == 1 ? "" : "s")
                    + " to "
                    + dest);
                doCompilation(compiler);

            } else if (filecount == 0) {
                log("there were no files to compile", Project.MSG_INFO);
            } else {
                log("all files are up to date", Project.MSG_VERBOSE);
            }
        }
    }

    /**
     * calculate where the files will end up:
     * this is destDir or it id destDir + the package name
     */
    private File getActualDestDir() {
        if (packageName == null) {
            return destDir;
        }
        return new File(destDir.getPath() + File.separatorChar
            + packageName.replace('.', File.separatorChar));
    }

    /**
     * do the compile
     */
    private void doCompilation(JspCompilerAdapter compiler)
        throws BuildException {
        // now we need to populate the compiler adapter
        compiler.setJspc(this);

        // finally, lets execute the compiler!!
        if (!compiler.execute()) {
            if (failOnError) {
                throw new BuildException(FAIL_MSG, getLocation());
            }
            log(FAIL_MSG, Project.MSG_ERR);
        }
    }

    /**
     * Clear the list of files to be compiled and copied..
     */
    protected void resetFileLists() {
        compileList.removeAllElements();
    }

    /**
     * Scans the directory looking for source files to be compiled.
     * The results are returned in the class variable compileList
     * @param srcDir the source directory.
     * @param dest   the destination directory.
     * @param mangler the jsp filename mangler.
     * @param files   the file names to mangle.
     */
    protected void scanDir(File srcDir, File dest, JspMangler mangler,
                           String[] files) {

        long now = Instant.now().toEpochMilli();

        for (String filename : files) {
            File srcFile = new File(srcDir, filename);
            File javaFile = mapToJavaFile(mangler, srcFile, srcDir, dest);
            if (javaFile == null) {
                continue;
            }
            if (srcFile.lastModified() > now) {
                log("Warning: file modified in the future: " + filename,
                    Project.MSG_WARN);
            }
            if (isCompileNeeded(srcFile, javaFile)) {
                compileList.addElement(srcFile.getAbsolutePath());
                javaFiles.addElement(javaFile);
            }
        }
    }

    /**
     * Test whether or not compilation is needed. A return value of
     * <code>true<code> means yes, <code>false</code> means
     * our tests do not indicate this, but as the TLDs are
     * not used for dependency checking this is not guaranteed.
     * The current tests are
     * <ol>
     * <li>no dest file
     * <li>dest file out of date w.r.t source
     * <li>dest file zero bytes long
     * </ol>
     * @param srcFile JSP source file
     * @param javaFile JSP dest file
     * @return true if a compile is definitely needed.
     *
     */
    private boolean isCompileNeeded(File srcFile, File javaFile) {
        boolean shouldCompile = false;
        if (!javaFile.exists()) {
            shouldCompile = true;
            log("Compiling " + srcFile.getPath()
                + " because java file " + javaFile.getPath()
                + " does not exist", Project.MSG_VERBOSE);
        } else if (srcFile.lastModified() > javaFile.lastModified()) {
            shouldCompile = true;
            log("Compiling " + srcFile.getPath()
                + " because it is out of date with respect to "
                + javaFile.getPath(),
                Project.MSG_VERBOSE);
        } else if (javaFile.length() == 0) {
            shouldCompile = true;
            log("Compiling " + srcFile.getPath()
                + " because java file " + javaFile.getPath()
                + " is empty", Project.MSG_VERBOSE);
        }
        return shouldCompile;
    }

    /**
     * get a filename from our jsp file.
     * @param mangler the jsp filename manager.
     * @param srcFile the source file.
     * @param srcDir  the source directory.
     * @param dest    the destination directory.
     * @return the filename.
     * @todo support packages and subdirs
     */
    protected File mapToJavaFile(JspMangler mangler, File srcFile, File srcDir,
                                 File dest) {
        if (!srcFile.getName().endsWith(".jsp")) {
            return null;
        }
        String javaFileName = mangler.mapJspToJavaName(srcFile);
        return new File(dest, javaFileName);
    }

    /**
     * delete any java output files that are empty
     * this is to get around a little defect in jasper: when it
     * fails, it leaves incomplete files around.
     */
    public void deleteEmptyJavaFiles() {
        if (javaFiles != null) {
            for (File file : javaFiles) {
                if (file.exists() && file.length() == 0) {
                    log("deleting empty output file " + file);
                    file.delete();
                }
            }
        }
    }

    /**
     * static inner class used as a parameter element
     */
    public static class WebAppParameter {

        /**
         * the sole option
         */
        private File directory;

        /**
         * query current directory
         * @return the directory.
         */
        public File getDirectory() {
            return directory;
        }

        /**
         * set directory; alternate syntax
         * @param directory the base dir.
         */
        public void setBaseDir(File directory) {
            this.directory = directory;
        }
    }

}
