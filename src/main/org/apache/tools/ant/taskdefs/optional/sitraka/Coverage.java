/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Random;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

/**
 * Runs Sitraka JProbe Coverage.
 *
 * Options are pretty numerous, you'd better check the manual for a full
 * descriptions of options. (not that simple since they differ from the online
 * help, from the usage command line and from the examples...)
 * <p>
 * For additional information, visit <a href="http://www.sitraka.com">www.sitraka.com</a>
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 *
 * @ant.task name="jpcoverage" category="metrics"
 */
public class Coverage extends Task {

    protected File home;

    protected Commandline cmdl = new Commandline();

    protected CommandlineJava cmdlJava = new CommandlineJava();

    protected String function = "coverage";

    protected String seedName;

    protected File inputFile;

    protected File javaExe;

    protected String vm;

    protected boolean applet = false;

    /** this is a somewhat annoying thing, set it to never */
    protected String exitPrompt = "never";

    protected Filters filters = new Filters();

    protected Triggers triggers;

    protected String finalSnapshot = "coverage";

    protected String recordFromStart = "coverage";

    protected File snapshotDir;

    protected File workingDir;

    protected boolean trackNatives = false;

    protected Socket socket;

    protected int warnLevel = 0;

    protected Vector filesets = new Vector();

    //--------- setters used via reflection --

    /**
     * The directory where JProbe is installed.
     */
    public void setHome(File value) {
        home = value;
    }

    /** seed name for snapshot file. Can be null, default to snap */
    public void setSeedname(String value) {
        seedName = value;
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setInputfile(File value) {
        inputFile = value;
    }

    /**
     * Path to the java executable.
     */
    public void setJavaexe(File value) {
        javaExe = value;
    }

    public static class Javavm extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{"java2", "jdk118", "jdk117"};
        }
    }

    /**
     * Indicates which virtual machine to run: "jdk117", "jdk118" or "java2".
     * Can be null, default to "java2". */
    public void setVm(Javavm value) {
        vm = value.getValue();
    }

    /**
     * If true, run an applet.
     */
    public void setApplet(boolean value) {
        applet = value;
    }

    /**
     * Toggles display of the console prompt: always, error, never
     */
    public void setExitprompt(String value) {
        exitPrompt = value;
    }

    /**
     * Defines class/method filters based on pattern matching.
     * The syntax is filters is similar to a fileset.
     */
    public Filters createFilters() {
        return filters;
    }

    /**
     * Defines events to use for interacting with the
     * collection of data performed during coverage.
     *
     * For example you may run a whole application but only decide
     * to collect data once it reaches a certain method and once it
     * exits another one.
     */
    public Triggers createTriggers() {
        if (triggers == null) {
            triggers = new Triggers();
        }
        return triggers;
    }

    /**
     * Define a host and port to connect to if you want to do
     * remote viewing.
     */
    public Socket createSocket() {
        if (socket == null) {
            socket = new Socket();
        }
        return socket;
    }

    public static class Finalsnapshot extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{"coverage", "none", "all"};
        }
    }

    /**
     * Type of snapshot to send at program termination: none, coverage, all.
     * Can be null, default to none
     */
    public void setFinalsnapshot(String value) {
        finalSnapshot = value;
    }

    public static class Recordfromstart extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{"coverage", "none", "all"};
        }
    }

    /**
     * "all", "coverage",  or "none".
     */
    public void setRecordfromstart(Recordfromstart value) {
        recordFromStart = value.getValue();
    }

    /**
     * Set warning level (0-3, where 0 is the least amount of warnings).
     */
    public void setWarnlevel(Integer value) {
        warnLevel = value.intValue();
    }

    /**
     * The path to the directory where snapshot files are stored.
     * Choose a directory that is reachable by both the remote
     * and local computers, and enter the same path on the command-line
     * and in the viewer.
     */
    public void setSnapshotdir(File value) {
        snapshotDir = value;
    }

    /**
     * The physical path to the working directory for the VM.
     */
    public void setWorkingdir(File value) {
        workingDir = value;
    }

    /**
     * If true, track native methods.
     */
    public void setTracknatives(boolean value) {
        trackNatives = value;
    }

    //

    /**
     * Adds a JVM argument.
     */
    public Commandline.Argument createJvmarg() {
        return cmdlJava.createVmArgument();
    }

    /**
     * Adds a command argument.
     */
    public Commandline.Argument createArg() {
        return cmdlJava.createArgument();
    }

    /**
     * classpath to run the files.
     */
    public Path createClasspath() {
        return cmdlJava.createClasspath(project).createPath();
    }

    /**
     * classname to run as standalone or runner for filesets.
     */
    public void setClassname(String value) {
        cmdlJava.setClassname(value);
    }

    /**
     * the classnames to execute.
     */
    public void addFileset(FileSet fs) {
        filesets.addElement(fs);
    }


    //---------------- the tedious job begins here

    public Coverage() {
    }

    /** execute the jplauncher by providing a parameter file */
    public void execute() throws BuildException {
        File paramfile = null;
        // if an input file is used, all other options are ignored...
        if (inputFile == null) {
            checkOptions();
            paramfile = createParamFile();
        } else {
            paramfile = inputFile;
        }
        try {
            // we need to run Coverage from his directory due to dll/jar issues
            cmdl.setExecutable(new File(home, "jplauncher").getAbsolutePath());
            cmdl.createArgument().setValue("-jp_input=" + paramfile.getAbsolutePath());

            // use the custom handler for stdin issues
            LogStreamHandler handler = new CoverageStreamHandler(this);
            Execute exec = new Execute(handler);
            log(cmdl.describeCommand(), Project.MSG_VERBOSE);
            exec.setCommandline(cmdl.getCommandline());
            int exitValue = exec.execute();
            if (exitValue != 0) {
                throw new BuildException("JProbe Coverage failed (" + exitValue + ")");
            }
        } catch (IOException e) {
            throw new BuildException("Failed to execute JProbe Coverage.", e);
        } finally {
            //@todo should be removed once switched to JDK1.2
            if (inputFile == null && paramfile != null) {
                paramfile.delete();
            }
        }
    }

    /** wheck what is necessary to check, Coverage will do the job for us */
    protected void checkOptions() throws BuildException {
        // check coverage home
        if (home == null || !home.isDirectory()) {
            throw new BuildException("Invalid home directory. Must point to JProbe home directory");
        }
        home = new File(home, "coverage");
        File jar = new File(home, "coverage.jar");
        if (!jar.exists()) {
            throw new BuildException("Cannot find Coverage directory: " + home);
        }

        // make sure snapshot dir exists and is resolved
        if (snapshotDir == null) {
            snapshotDir = new File(".");
        }
        snapshotDir = project.resolveFile(snapshotDir.getPath());
        if (!snapshotDir.isDirectory() || !snapshotDir.exists()) {
            throw new BuildException("Snapshot directory does not exists :" + snapshotDir);
        }
        if (workingDir == null) {
            workingDir = new File(".");
        }
        workingDir = project.resolveFile(workingDir.getPath());

        // check for info, do your best to select the java executable.
        // JProbe 3.0 fails if there is no javaexe option. So
        if (javaExe == null && (vm == null || "java2".equals(vm))) {
            String version = System.getProperty("java.version");
            // make we are using 1.2+, if it is, then do your best to
            // get a javaexe
            if (!version.startsWith("1.1")) {
                if (vm == null) {
                    vm = "java2";
                }
                // if we are here obviously it is java2
                String home = System.getProperty("java.home");
                boolean isUnix = File.separatorChar == '/';
                javaExe = isUnix ? new File(home, "bin/java") : new File(home, "/bin/java.exe");
            }
        }
    }

    /**
     * return the command line parameters. Parameters can either be passed
     * to the command line and stored to a file (then use the -jp_input=&lt;filename&gt;)
     * if they are too numerous.
     */
    protected String[] getParameters() {
        Vector params = new Vector();
        params.addElement("-jp_function=" + function);
        if (vm != null) {
            params.addElement("-jp_vm=" + vm);
        }
        if (javaExe != null) {
            params.addElement("-jp_java_exe=" + project.resolveFile(javaExe.getPath()));
        }
        params.addElement("-jp_working_dir=" + workingDir.getPath());
        params.addElement("-jp_snapshot_dir=" + snapshotDir.getPath());
        params.addElement("-jp_record_from_start=" + recordFromStart);
        params.addElement("-jp_warn=" + warnLevel);
        if (seedName != null) {
            params.addElement("-jp_output_file=" + seedName);
        }
        params.addElement("-jp_filter=" + filters.toString());
        if (triggers != null) {
            params.addElement("-jp_trigger=" + triggers.toString());
        }
        if (finalSnapshot != null) {
            params.addElement("-jp_final_snapshot=" + finalSnapshot);
        }
        params.addElement("-jp_exit_prompt=" + exitPrompt);
        //params.addElement("-jp_append=" + append);
        params.addElement("-jp_track_natives=" + trackNatives);
        //.... now the jvm
        // arguments
        String[] vmargs = cmdlJava.getVmCommand().getArguments();
        for (int i = 0; i < vmargs.length; i++) {
            params.addElement(vmargs[i]);
        }
        // classpath
        Path classpath = cmdlJava.getClasspath();
        if (classpath != null && classpath.size() > 0) {
            params.addElement("-classpath " + classpath.toString());
        }
        // classname (runner or standalone)
        if (cmdlJava.getClassname() != null) {
            params.addElement(cmdlJava.getClassname());
        }
        // arguments for classname
        String[] args = cmdlJava.getJavaCommand().getArguments();
        for (int i = 0; i < args.length; i++) {
            params.addElement(args[i]);
        }

        String[] array = new String[params.size()];
        params.copyInto(array);
        return array;
    }


    /**
     * create the parameter file from the given options. The file is
     * created with a random name in the current directory.
     * @return the file object where are written the configuration to run
     * JProbe Coverage
     * @throws BuildException thrown if something bad happens while writing
     * the arguments to the file.
     */
    protected File createParamFile() throws BuildException {
        //@todo change this when switching to JDK 1.2 and use File.createTmpFile()
        File file = createTmpFile();
        log("Creating parameter file: " + file, Project.MSG_VERBOSE);

        // options need to be one per line in the parameter file
        // so write them all in a single string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] params = getParameters();
        for (int i = 0; i < params.length; i++) {
            pw.println(params[i]);
        }
        pw.flush();
        log("JProbe Coverage parameters:\n" + sw.toString(), Project.MSG_VERBOSE);

        // now write them to the file
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            fw.write(sw.toString());
            fw.flush();
        } catch (IOException e) {
            throw new BuildException("Could not write parameter file " + file, e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ignored) {
                }
            }
        }
        return file;
    }

    /** create a temporary file in the current dir (For JDK1.1 support) */
    protected File createTmpFile() {
        final long rand = (new Random(System.currentTimeMillis())).nextLong();
        File file = new File("jpcoverage" + rand + ".tmp");
        return file;
    }

    /** specific pumper to avoid those nasty stdin issues */
    static class CoverageStreamHandler extends LogStreamHandler {
        CoverageStreamHandler(Task task) {
            super(task, Project.MSG_INFO, Project.MSG_WARN);
        }

        /**
         * there are some issues concerning all JProbe executable
         * In our case a 'Press ENTER to close this window..." will
         * be displayed in the current window waiting for enter.
         * So I'm closing the stream right away to avoid problems.
         */
        public void setProcessInputStream(OutputStream os) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
        }
    }

}
