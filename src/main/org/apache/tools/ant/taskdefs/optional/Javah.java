/*
 * Copyright  2000-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.JavaEnvUtils;

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
 * @author Rick Beton <a href="mailto:richard.beton@physics.org">richard.beton@physics.org</a>
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

    /**
     * the fully-qualified name of the class (or classes, separated by commas).
     */
    public void setClass(String cls) {
        this.cls = cls;
    }

    /**
     * Adds class to process.
     */
    public ClassArgument createClass() {
        ClassArgument ga = new ClassArgument();
        classes.addElement(ga);
        return ga;
    }

    public class ClassArgument {
        private String name;

        public ClassArgument() {
        }

        public void setName(String name) {
            this.name = name;
            log("ClassArgument.name=" + name);
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Set the destination directory into which the Java source
     * files should be compiled.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * the classpath to use.
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
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     * @todo this needs to be documented in the HTML docs
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * location of bootstrap class files.
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
     */
    public Path createBootclasspath() {
        if (bootclasspath == null) {
            bootclasspath = new Path(getProject());
        }
        return bootclasspath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     * @todo this needs to be documented in the HTML
     */
    public void setBootClasspathRef(Reference r) {
        createBootclasspath().setRefid(r);
    }

    ///**
    // * Sets the extension directories that will be used during the
    // * compilation.
    // */
    //public void setExtdirs(Path extdirs) {
    //    if (this.extdirs == null) {
    //        this.extdirs = extdirs;
    //    } else {
    //        this.extdirs.append(extdirs);
    //    }
    //}

    ///**
    // * Maybe creates a nested classpath element.
    // */
    //public Path createExtdirs() {
    //    if (extdirs == null) {
    //        extdirs = new Path(project);
    //    }
    //    return extdirs.createPath();
    //}

    /**
     * Concatenates the resulting header or source files for all
     * the classes listed into this file.
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * If true, output files should always be written (JDK1.2 only).
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * If true, specifies that old JDK1.0-style header files should be
     * generated.
     * (otherwise output file contain JNI-style native method function prototypes) (JDK1.2 only)
     */
    public void setOld(boolean old) {
        this.old = old;
    }

    /**
     * If true, generate C declarations from the Java object file (used with old).
     */
    public void setStubs(boolean stubs) {
        this.stubs = stubs;
    }

    /**
     * If true, causes Javah to print a message concerning
     * the status of the generated files.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
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

        String compiler = getProject().getProperty("build.compiler");
        if (compiler == null) {
            if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)
                && !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)) {
                compiler = "modern";
            } else {
                compiler = "classic";
            }
        }

        doClassicCompile();
    }

    // XXX
    // we need a way to not use the current classpath.

    /**
     * Performs a compile using the classic compiler that shipped with
     * JDK 1.1 and 1.2.
     */

    private void doClassicCompile() throws BuildException {
        Commandline cmd = setupJavahCommand();

        // Use reflection to be able to build on all JDKs
        /*
        // provide the compiler a different message sink - namely our own
        sun.tools.javac.Main compiler =
                new sun.tools.javac.Main(new LogOutputStream(this, Project.MSG_WARN), "javac");

        if (!compiler.compile(cmd.getArguments())) {
            throw new BuildException("Compile failed");
        }
        */


        try {
            Class javahMainClass = null;
            try {
                // first search for the "old" javah class in 1.4.2 tools.jar
                javahMainClass = Class.forName("com.sun.tools.javah.oldjavah.Main");
            } catch (ClassNotFoundException cnfe) {
                // assume older than 1.4.2 tools.jar
                javahMainClass = Class.forName("com.sun.tools.javah.Main");
            }

            // now search for the constructor that takes in String[] arguments.
            Class[] strings = new Class[] {String[].class};
            Constructor constructor = javahMainClass.getConstructor(strings);

            // construct the javah Main instance
            Object javahMain = constructor.newInstance(new Object[] {cmd.getArguments()});

            // find the run method
            Method runMethod = javahMainClass.getMethod("run", new Class[0]);

            runMethod.invoke(javahMain, new Object[0]);
        } catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            } else {
                throw new BuildException("Error starting javah: " + ex, ex, getLocation());
            }
        }
    }

    /**
     * Does the command line argument processing common to classic and
     * modern.
     */
    private Commandline setupJavahCommand() {
        Commandline cmd = new Commandline();

        if (destDir != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(destDir);
        }

        if (outputFile != null) {
            cmd.createArgument().setValue("-o");
            cmd.createArgument().setFile(outputFile);
        }

        if (classpath != null) {
            cmd.createArgument().setValue("-classpath");
            cmd.createArgument().setPath(classpath);
        }

        // JDK1.1 is rather simpler than JDK1.2
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            if (verbose) {
                cmd.createArgument().setValue("-v");
            }
        } else {
            if (verbose) {
                cmd.createArgument().setValue("-verbose");
            }
            if (old) {
                cmd.createArgument().setValue("-old");
            }
            if (force) {
                cmd.createArgument().setValue("-force");
            }
        }

        if (stubs) {
            if (!old) {
                throw new BuildException("stubs only available in old mode.", getLocation());
            }
            cmd.createArgument().setValue("-stubs");
        }
        if (bootclasspath != null) {
            cmd.createArgument().setValue("-bootclasspath");
            cmd.createArgument().setPath(bootclasspath);
        }

        logAndAddFilesToCompile(cmd);
        return cmd;
    }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &quot;niceSourceList&quot;
     */
    protected void logAndAddFilesToCompile(Commandline cmd) {
        int n = 0;
        log("Compilation " + cmd.describeArguments(),
            Project.MSG_VERBOSE);

        StringBuffer niceClassList = new StringBuffer();
        if (cls != null) {
            StringTokenizer tok = new StringTokenizer(cls, ",", false);
            while (tok.hasMoreTokens()) {
                String aClass = tok.nextToken().trim();
                cmd.createArgument().setValue(aClass);
                niceClassList.append("    " + aClass + lSep);
                n++;
            }
        }

        Enumeration e = classes.elements();
        while (e.hasMoreElements()) {
            ClassArgument arg = (ClassArgument) e.nextElement();
            String aClass = arg.getName();
            cmd.createArgument().setValue(aClass);
            niceClassList.append("    " + aClass + lSep);
            n++;
        }

        StringBuffer prefix = new StringBuffer("Class");
        if (n > 1) {
            prefix.append("es");
        }
        prefix.append(" to be compiled:");
        prefix.append(lSep);

        log(prefix.toString() + niceClassList.toString(), Project.MSG_VERBOSE);
    }
}

