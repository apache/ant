/*
 * Copyright  2001-2004 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.metamata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Simple Metamata MParse task.
 * Based on the original written by
 * <a href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a>.
 *
 * This version was written for Metamata 2.0 available at
 * <a href="http://www.metamata.com">http://www.metamata.com</a>
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @todo make a subclass of AbstractMetaMataTask
 */
public class MParse extends AbstractMetamataTask {

    private File target = null;
    private boolean verbose = false;
    private boolean debugparser = false;
    private boolean debugscanner = false;
    private boolean cleanup = false;

    /** The .jj file to process; required. */
    public void setTarget(File target) {
        this.target = target;
    }

    /** set verbose mode */
    public void setVerbose(boolean flag) {
        verbose = flag;
    }

    /** set scanner debug mode; optional, default false */
    public void setDebugscanner(boolean flag) {
        debugscanner = flag;
    }

    /** set parser debug mode; optional, default false */
    public void setDebugparser(boolean flag) {
        debugparser = flag;
    }

    /** Remove the intermediate Sun JavaCC file
     * ; optional, default false.
     */
    public void setCleanup(boolean value) {
        cleanup = value;
    }

    public MParse() {
        cmdl.setVm(JavaEnvUtils.getJreExecutable("java"));
        cmdl.setClassname("com.metamata.jj.MParse");
    }


    /** execute the command line */
    public void execute() throws BuildException {
        try {
            setUp();
            ExecuteStreamHandler handler = createStreamHandler();
            _execute(handler);
        } finally {
            cleanUp();
        }
    }

    /** return the default stream handler for this task */
    protected ExecuteStreamHandler createStreamHandler() {
        return new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_INFO);
    }

    /**
     * check the options and build the command line
     */
    protected void setUp() throws BuildException {
        checkOptions();

        // set the classpath as the jar files
        File[] jars = getMetamataLibs();
        final Path classPath = cmdl.createClasspath(getProject());
        for (int i = 0; i < jars.length; i++) {
            classPath.createPathElement().setLocation(jars[i]);
        }

        // set the metamata.home property
        final Commandline.Argument vmArgs = cmdl.createVmArgument();
        vmArgs.setValue("-Dmetamata.home=" + metamataHome.getAbsolutePath());


        // write all the options to a temp file and use it ro run the process
        Vector opts = getOptions();
        String[] options = new String[ opts.size() ];
        opts.copyInto(options);

        optionsFile = createTmpFile();
        generateOptionsFile(optionsFile, options);
        Commandline.Argument args = cmdl.createArgument();
        args.setLine("-arguments " + optionsFile.getAbsolutePath());
    }


    /** execute the process with a specific handler */
    protected void _execute(ExecuteStreamHandler handler) throws BuildException {
        // target has been checked as a .jj, see if there is a matching
        // java file and if it is needed to run to process the grammar
        String pathname = target.getAbsolutePath();
        int pos = pathname.length() - ".jj".length();
        pathname = pathname.substring(0, pos) + ".java";
        File javaFile = new File(pathname);
        if (javaFile.exists() && target.lastModified() < javaFile.lastModified()) {
            getProject().log("Target is already build - skipping (" + target + ")");
            return;
        }

        final Execute process = new Execute(handler);
        log(cmdl.describeCommand(), Project.MSG_VERBOSE);
        process.setCommandline(cmdl.getCommandline());
        try {
            if (process.execute() != 0) {
                throw new BuildException("Metamata task failed.");
            }
        } catch (IOException e) {
            throw new BuildException("Failed to launch Metamata task: ", e);
        }
    }

    /** clean up all the mess that we did with temporary objects */
    protected void cleanUp() {
        if (optionsFile != null) {
            optionsFile.delete();
            optionsFile = null;
        }
        if (cleanup) {
            String name = target.getName();
            int pos = name.length() - ".jj".length();
            name = "__jj" + name.substring(0, pos) + ".sunjj";
            final File sunjj = new File(target.getParent(), name);
            if (sunjj.exists()) {
                getProject().log("Removing stale file: " + sunjj.getName());
                sunjj.delete();
            }
        }
    }

    /**
     * return an array of files containing the path to the needed
     * libraries to run metamata. The file are not checked for
     * existence. You should do this yourself if needed or simply let the
     * forked process do it for you.
     * @return array of jars/zips needed to run metamata.
     */
    protected File[] getMetamataLibs() {
        Vector files = new Vector();
        files.addElement(new File(metamataHome, "lib/metamata.jar"));
        files.addElement(new File(metamataHome, "bin/lib/JavaCC.zip"));

        File[] array = new File[ files.size() ];
        files.copyInto(array);
        return array;
    }


    /**
     * validate options set and resolve files and paths
     * @throws BuildException thrown if an option has an incorrect state.
     */
    protected void checkOptions() throws BuildException {
        // check that the home is ok.
        if (metamataHome == null || !metamataHome.exists()) {
            throw new BuildException("'metamatahome' must point to Metamata home directory.");
        }
        metamataHome = getProject().resolveFile(metamataHome.getPath());

        // check that the needed jar exists.
        File[] jars = getMetamataLibs();
        for (int i = 0; i < jars.length; i++) {
            if (!jars[i].exists()) {
                throw new BuildException(jars[i]
                    + " does not exist. Check your metamata installation.");
            }
        }

        // check that the target is ok and resolve it.
        if (target == null || !target.isFile()
            || !target.getName().endsWith(".jj")) {
            throw new BuildException("Invalid target: " + target);
        }
        target = getProject().resolveFile(target.getPath());
    }

    /**
     * return all options of the command line as string elements
     * @return an array of options corresponding to the setted options.
     */
    protected Vector getOptions() {
        Vector options = new Vector();
        if (verbose) {
            options.addElement("-verbose");
        }
        if (debugscanner) {
            options.addElement("-ds");
        }
        if (debugparser) {
            options.addElement("-dp");
        }
        if (classPath != null) {
            options.addElement("-classpath");
            options.addElement(classPath.toString());
        }
        if (sourcePath != null) {
            options.addElement("-sourcepath");
            options.addElement(sourcePath.toString());
        }
        options.addElement(target.getAbsolutePath());
        return options;
    }

    /**
     * write all options to a file with one option / line
     * @param tofile the file to write the options to.
     * @param options the array of options element to write to the file.
     * @throws BuildException thrown if there is a problem while writing
     * to the file.
     */
    protected void generateOptionsFile(File tofile, String[] options) throws BuildException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(tofile);
            PrintWriter pw = new PrintWriter(fw);
            for (int i = 0; i < options.length; i++) {
                pw.println(options[i]);
            }
            pw.flush();
        } catch (IOException e) {
            throw new BuildException("Error while writing options file " + tofile, e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }
}
