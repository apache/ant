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

package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.StreamPumper;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;


/**
 * Create a CAB archive.
 *
 * @author Roger Vaughn <a href="mailto:rvaughn@seaconinc.com">rvaughn@seaconinc.com</a>
 * @author Jesse Stockall
 */

public class Cab extends MatchingTask {

    private File cabFile;
    private File baseDir;
    private Vector filesets = new Vector();
    private boolean doCompress = true;
    private boolean doVerbose = false;
    private String cmdOptions;

    protected String archiveType = "cab";

    private FileUtils fileUtils = FileUtils.newFileUtils();

    /**
     * The name/location of where to create the .cab file.
     */
    public void setCabfile(File cabFile) {
        this.cabFile = cabFile;
    }

    /**
     * Base directory to look in for files to CAB.
     */
    public void setBasedir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * If true, compress the files otherwise only store them.
     */
    public void setCompress(boolean compress) {
        doCompress = compress;
    }

    /**
     * If true, display cabarc output.
     */
    public void setVerbose(boolean verbose) {
        doVerbose = verbose;
    }

    /**
     * Sets additional cabarc options that are not supported directly.
     */
    public void setOptions(String options) {
        cmdOptions = options;
    }

    /**
     * Adds a set of files to archive.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /*
     * I'm not fond of this pattern: "sub-method expected to throw
     * task-cancelling exceptions".  It feels too much like programming
     * for side-effects to me...
     */
    protected void checkConfiguration() throws BuildException {
        if (baseDir == null) {
            throw new BuildException("basedir attribute must be set!", getLocation());
        }
        if (!baseDir.exists()) {
            throw new BuildException("basedir does not exist!", getLocation());
        }
        if (cabFile == null) {
            throw new BuildException("cabfile attribute must be set!" , getLocation());
        }
    }

    /**
     * Create a new exec delegate.  The delegate task is populated so that
     * it appears in the logs to be the same task as this one.
     */
    protected ExecTask createExec() throws BuildException {
        ExecTask exec = (ExecTask) project.createTask("exec");
        exec.setOwningTarget(this.getOwningTarget());
        exec.setTaskName(this.getTaskName());
        exec.setDescription(this.getDescription());

        return exec;
    }

    /**
     * Check to see if the target is up to date with respect to input files.
     * @return true if the cab file is newer than its dependents.
     */
    protected boolean isUpToDate(Vector files) {
        boolean upToDate = true;
        for (int i = 0; i < files.size() && upToDate; i++) {
            String file = files.elementAt(i).toString();
            if (new File(baseDir, file).lastModified() >
                cabFile.lastModified()) {
                upToDate = false;
            }
        }
        return upToDate;
    }

    /**
     * Creates a list file.  This temporary file contains a list of all files
     * to be included in the cab, one file per line.
     */
    protected File createListFile(Vector files)
        throws IOException {
        File listFile = fileUtils.createTempFile("ant", "", null);

        PrintWriter writer = new PrintWriter(new FileOutputStream(listFile));

        for (int i = 0; i < files.size(); i++) {
            writer.println(files.elementAt(i).toString());
        }
        writer.close();

        return listFile;
    }

    /**
     * Append all files found by a directory scanner to a vector.
     */
    protected void appendFiles(Vector files, DirectoryScanner ds) {
        String[] dsfiles = ds.getIncludedFiles();

        for (int i = 0; i < dsfiles.length; i++) {
            files.addElement(dsfiles[i]);
        }
    }

    /**
     * Get the complete list of files to be included in the cab.  Filenames
     * are gathered from filesets if any have been added, otherwise from the
     * traditional include parameters.
     */
    protected Vector getFileList() throws BuildException {
        Vector files = new Vector();

        if (filesets.size() == 0) {
            // get files from old methods - includes and nested include
            appendFiles(files, super.getDirectoryScanner(baseDir));
        } else {
            // get files from filesets
            for (int i = 0; i < filesets.size(); i++) {
                FileSet fs = (FileSet) filesets.elementAt(i);
                if (fs != null) {
                    appendFiles(files, fs.getDirectoryScanner(project));
                }
            }
        }

        return files;
    }

    public void execute() throws BuildException {

        checkConfiguration();

        Vector files = getFileList();

        // quick exit if the target is up to date
        if (isUpToDate(files)) {
            return;
        }

        log("Building " + archiveType + ": " + cabFile.getAbsolutePath());

        if (!Os.isFamily("windows")) {
            log("Using listcab/libcabinet", Project.MSG_VERBOSE);

            StringBuffer sb = new StringBuffer();

            Enumeration fileEnum = files.elements();

            while (fileEnum.hasMoreElements()) {
                sb.append(fileEnum.nextElement()).append("\n");
            }
            sb.append("\n").append(cabFile.getAbsolutePath()).append("\n");

            try {
                Process p = Execute.launch(getProject(),
                                           new String[] {"listcab"}, null,
                                           baseDir, true);
                OutputStream out = p.getOutputStream();
                out.write(sb.toString().getBytes());
                out.flush();
                out.close();

                // Create the stream pumpers to forward listcab's stdout and stderr to the log
                // note: listcab is an interactive program, and issues prompts for every new line.
                //       Therefore, make it show only with verbose logging turned on.
                LogOutputStream outLog = new LogOutputStream(this, Project.MSG_VERBOSE);
                LogOutputStream errLog = new LogOutputStream(this, Project.MSG_ERR);
                StreamPumper    outPump = new StreamPumper(p.getInputStream(), outLog);
                StreamPumper    errPump = new StreamPumper(p.getErrorStream(), errLog);

                // Pump streams asynchronously
                (new Thread(outPump)).start();
                (new Thread(errPump)).start();

                int result = -99; // A wild default for when the thread is interrupted

                try {
                    // Wait for the process to finish
                    result = p.waitFor();

                    // Wait for the end of output and error streams
                    outPump.waitFor();
                    outLog.close();
                    errPump.waitFor();
                    errLog.close();
                } catch (InterruptedException ie) {
                    log("Thread interrupted: " + ie);
                }

                // Informative summary message in case of errors
                if (result != 0) {
                    log("Error executing listcab; error code: " + result);
                }
            } catch (IOException ex) {
                String msg = "Problem creating " + cabFile + " " + ex.getMessage();
                throw new BuildException(msg, getLocation());
            }
        } else {
            try {
                File listFile = createListFile(files);
                ExecTask exec = createExec();
                File outFile = null;

                // die if cabarc fails
                exec.setFailonerror(true);
                exec.setDir(baseDir);

                if (!doVerbose) {
                    outFile = fileUtils.createTempFile("ant", "", null);
                    exec.setOutput(outFile);
                }

                exec.setExecutable("cabarc");
                exec.createArg().setValue("-r");
                exec.createArg().setValue("-p");

                if (!doCompress) {
                    exec.createArg().setValue("-m");
                    exec.createArg().setValue("none");
                }

                if (cmdOptions != null) {
                    exec.createArg().setLine(cmdOptions);
                }

                exec.createArg().setValue("n");
                exec.createArg().setFile(cabFile);
                exec.createArg().setValue("@" + listFile.getAbsolutePath());

                exec.execute();

                if (outFile != null) {
                    outFile.delete();
                }

                listFile.delete();
            } catch (IOException ioe) {
                String msg = "Problem creating " + cabFile + " " + ioe.getMessage();
                throw new BuildException(msg, getLocation());
            }
        }
    }
}
