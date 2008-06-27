/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.javah.JavahAdapter;
import org.apache.tools.ant.taskdefs.optional.javah.JavahAdapterFactory;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.facade.FacadeTaskHelper;
import org.apache.tools.ant.util.facade.ImplementationSpecificArgument;

/**
 * Generates JNI header files using javah.
 *
 * This task can take the following arguments:
 * <ul>
 * <li>classname - the fully-qualified name of a class</li>
 * <li>outputFile - Concatenates the resulting header or source files for all
 *     the classes listed into this file</li>
 * <li>destdir - Sets the directory where javah saves the header files or the
 *     stub files</li>
 * <li>classpath</li>
 * <li>bootclasspath</li>
 * <li>force - Specifies that output files should always be written
       (JDK1.2 only)</li>
 * <li>old - Specifies that old JDK1.0-style header files should be generated
 *     (otherwise output file contain JNI-style native method
 *      function prototypes) (JDK1.2 only)</li>
 * <li>stubs - generate C declarations from the Java object file (used with old)</li>
 * <li>verbose - causes javah to print a message to stdout concerning the status
 *     of the generated files</li>
 * <li>extdirs - Override location of installed extensions</li>
 * </ul>
 * Of these arguments, either <b>outputFile</b> or <b>destdir</b> is required,
 * but not both. More than one classname may be specified, using a comma-separated
 * list or by using <code>&lt;class name="xxx"&gt;</code> elements within the task.
 * <p>
 * When this task executes, it will generate C header and source files that
 * are needed to implement native methods.
 *
 */

public class Javah extends Task {

    private Vector classes = new Vector(2);
    private String cls;
    private File destDir;
    private Path classpath = null;
    private File outputFile = null;
    private boolean verbose = false;
    private boolean force   = false;
    private boolean old     = false;
    private boolean stubs   = false;
    private Path bootclasspath;
    //private Path extdirs;
    private static String lSep = System.getProperty("line.separator");
    private FacadeTaskHelper facade = null;

    /**
     * No arg constructor.
     */
    public Javah() {
        facade = new FacadeTaskHelper(JavahAdapterFactory.getDefault());
    }

    /**
     * the fully-qualified name of the class (or classes, separated by commas).
     * @param cls the classname (or classnames).
     */
    public void setClass(String cls) {
        this.cls = cls;
    }

    /**
     * Adds class to process.
     * @return a <code>ClassArgument</code> to be configured.
     */
    public ClassArgument createClass() {
        ClassArgument ga = new ClassArgument();
        classes.addElement(ga);
        return ga;
    }

    /**
     * A class corresponding the the nested "class" element.
     * It contains a "name" attribute.
     */
    public class ClassArgument {
        private String name;

        /** Constructor for ClassArgument. */
        public ClassArgument() {
        }

        /**
         * Set the name attribute.
         * @param name the name attribute.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Get the name attribute.
         * @return the name attribute.
         */
        public String getName() {
            return name;
        }
    }

    /**
     * Names of the classes to process.
     * @return the array of classes.
     * @since Ant 1.6.3
     */
    public String[] getClasses() {
        ArrayList al = new ArrayList();
        if (cls != null) {
            StringTokenizer tok = new StringTokenizer(cls, ",", false);
            while (tok.hasMoreTokens()) {
                al.add(tok.nextToken().trim());
            }
        }

        Enumeration e = classes.elements();
        while (e.hasMoreElements()) {
            ClassArgument arg = (ClassArgument) e.nextElement();
            al.add(arg.getName());
        }
        return (String[]) al.toArray(new String[al.size()]);
    }

    /**
     * Set the destination directory into which the Java source
     * files should be compiled.
     * @param destDir the destination directory.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * The destination directory, if any.
     * @return the destination directory.
     * @since Ant 1.6.3
     */
    public File getDestdir() {
        return destDir;
    }

    /**
     * the classpath to use.
     * @param src the classpath.
     */
    public void setClasspath(Path src) {
        if (classpath == null) {
            classpath = src;
        } else {
            classpath.append(src);
        }
    }

    /**
     * Path to use for classpath.
     * @return a path to be configured.
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     * @param r a reference to a classpath.
     * @todo this needs to be documented in the HTML docs.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * The classpath to use.
     * @return the classpath.
     * @since Ant 1.6.3
     */
    public Path getClasspath() {
        return classpath;
    }

    /**
     * location of bootstrap class files.
     * @param src the bootstrap classpath.
     */
    public void setBootclasspath(Path src) {
        if (bootclasspath == null) {
            bootclasspath = src;
        } else {
            bootclasspath.append(src);
        }
    }

    /**
     * Adds path to bootstrap class files.
     * @return a path to be configured.
     */
    public Path createBootclasspath() {
        if (bootclasspath == null) {
            bootclasspath = new Path(getProject());
        }
        return bootclasspath.createPath();
    }

    /**
     * To the bootstrap path, this adds a reference to a classpath defined elsewhere.
     * @param r a reference to a classpath
     * @todo this needs to be documented in the HTML.
     */
    public void setBootClasspathRef(Reference r) {
        createBootclasspath().setRefid(r);
    }

    /**
     * The bootclasspath to use.
     * @return the bootclass path.
     * @since Ant 1.6.3
     */
    public Path getBootclasspath() {
        return bootclasspath;
    }

    /**
     * Concatenates the resulting header or source files for all
     * the classes listed into this file.
     * @param outputFile the output file.
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * The destination file, if any.
     * @return the destination file.
     * @since Ant 1.6.3
     */
    public File getOutputfile() {
        return outputFile;
    }

    /**
     * If true, output files should always be written (JDK1.2 only).
     * @param force the value to use.
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * Whether output files should always be written.
     * @return the force attribute.
     * @since Ant 1.6.3
     */
    public boolean getForce() {
        return force;
    }

    /**
     * If true, specifies that old JDK1.0-style header files should be
     * generated.
     * (otherwise output file contain JNI-style native method function
     *  prototypes) (JDK1.2 only).
     * @param old if true use old 1.0 style header files.
     */
    public void setOld(boolean old) {
        this.old = old;
    }

    /**
     * Whether old JDK1.0-style header files should be generated.
     * @return the old attribute.
     * @since Ant 1.6.3
     */
    public boolean getOld() {
        return old;
    }

    /**
     * If true, generate C declarations from the Java object file (used with old).
     * @param stubs if true, generated C declarations.
     */
    public void setStubs(boolean stubs) {
        this.stubs = stubs;
    }

    /**
     * Whether C declarations from the Java object file should be generated.
     * @return the stubs attribute.
     * @since Ant 1.6.3
     */
    public boolean getStubs() {
        return stubs;
    }

    /**
     * If true, causes Javah to print a message concerning
     * the status of the generated files.
     * @param verbose if true, do verbose printing.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Whether verbose output should get generated.
     * @return the verbose attribute.
     * @since Ant 1.6.3
     */
    public boolean getVerbose() {
        return verbose;
    }

    /**
     * Choose the implementation for this particular task.
     * @param impl the name of the implemenation.
     * @since Ant 1.6.3
     */
    public void setImplementation(String impl) {
        if ("default".equals(impl)) {
            facade.setImplementation(JavahAdapterFactory.getDefault());
        } else {
            facade.setImplementation(impl);
        }
    }

    /**
     * Adds an implementation specific command-line argument.
     * @return a ImplementationSpecificArgument to be configured.
     *
     * @since Ant 1.6.3
     */
    public ImplementationSpecificArgument createArg() {
        ImplementationSpecificArgument arg =
            new ImplementationSpecificArgument();
        facade.addImplementationArgument(arg);
        return arg;
    }

    /**
     * Returns the (implementation specific) settings given as nested
     * arg elements.
     * @return the arguments.
     * @since Ant 1.6.3
     */
    public String[] getCurrentArgs() {
        return facade.getArgs();
    }

    /**
     * Execute the task
     *
     * @throws BuildException is there is a problem in the task execution.
     */
    public void execute() throws BuildException {
        // first off, make sure that we've got a srcdir

        if ((cls == null) && (classes.size() == 0)) {
            throw new BuildException("class attribute must be set!",
                getLocation());
        }

        if ((cls != null) && (classes.size() > 0)) {
            throw new BuildException("set class attribute or class element, "
                + "not both.", getLocation());
        }

        if (destDir != null) {
            if (!destDir.isDirectory()) {
                throw new BuildException("destination directory \"" + destDir
                    + "\" does not exist or is not a directory", getLocation());
            }
            if (outputFile != null) {
                throw new BuildException("destdir and outputFile are mutually "
                    + "exclusive", getLocation());
            }
        }

        if (classpath == null) {
            classpath = (new Path(getProject())).concatSystemClasspath("last");
        } else {
            classpath = classpath.concatSystemClasspath("ignore");
        }

        JavahAdapter ad =
            JavahAdapterFactory.getAdapter(facade.getImplementation(),
                                           this);
        if (!ad.compile(this)) {
            throw new BuildException("compilation failed");
        }
    }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &quot;niceSourceList&quot;
     * @param cmd the command line.
     */
    public void logAndAddFiles(Commandline cmd) {
        logAndAddFilesToCompile(cmd);
    }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &quot;niceSourceList&quot;
     * @param cmd the command line to add parameters to.
     */
    protected void logAndAddFilesToCompile(Commandline cmd) {
        log("Compilation " + cmd.describeArguments(),
            Project.MSG_VERBOSE);

        StringBuffer niceClassList = new StringBuffer();
        String[] c = getClasses();
        for (int i = 0; i < c.length; i++) {
            cmd.createArgument().setValue(c[i]);
            niceClassList.append("    ");
            niceClassList.append(c[i]);
            niceClassList.append(lSep);
        }

        StringBuffer prefix = new StringBuffer("Class");
        if (c.length > 1) {
            prefix.append("es");
        }
        prefix.append(" to be compiled:");
        prefix.append(lSep);

        log(prefix.toString() + niceClassList.toString(), Project.MSG_VERBOSE);
    }
}

