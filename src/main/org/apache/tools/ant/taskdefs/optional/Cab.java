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

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;

import java.io.*;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Random;
import java.text.DecimalFormat;

/**
 * Create a CAB archive.
 *
 * @author Roger Vaughn <a href="mailto:rvaughn@seaconinc.com">rvaughn@seaconinc.com</a>
 */

public class Cab extends MatchingTask {

    private File cabFile;
    private File baseDir;
    private Vector filesets = new Vector();
    private boolean doCompress = true;
    private boolean doVerbose = false;
    private String cmdOptions;
    
    protected String archiveType = "cab";

    private static String myos;
    private static boolean isWindows;

    static {
        myos = System.getProperty("os.name");
        isWindows = myos.toLowerCase().indexOf("windows") >= 0;
    }
    
    /**
     * This is the name/location of where to 
     * create the .cab file.
     */
    public void setCabfile(File cabFile) {
        this.cabFile = cabFile;
    }
    
    /**
     * This is the base directory to look in for 
     * things to cab.
     */
    public void setBasedir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Sets whether we want to compress the files or only store them.
     */
    public void setCompress(boolean compress) {
        doCompress = compress;
    }

    /**
     * Sets whether we want to see or suppress cabarc output.
     */
    public void setVerbose(boolean verbose) {
        doVerbose = verbose;
    }

    /**
     * Sets additional cabarc options that aren't supported directly.
     */
    public void setOptions(String options) {
        cmdOptions = options;
    }

    /**
     * Adds a set of files (nested fileset attribute).
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
            throw new BuildException("basedir attribute must be set!");
        }
        if (!baseDir.exists()) {
            throw new BuildException("basedir does not exist!");
        }
        if (cabFile == null) {
            throw new BuildException("cabfile attribute must be set!");
        }
    }

    /**
     * Create a new exec delegate.  The delegate task is populated so that
     * it appears in the logs to be the same task as this one.
     */
    protected ExecTask createExec() throws BuildException
    {
        ExecTask exec = (ExecTask)project.createTask("exec");
        exec.setOwningTarget(this.getOwningTarget());
        exec.setTaskName(this.getTaskName());
        exec.setDescription(this.getDescription());

        return exec;
    }

    /**
     * Check to see if the target is up to date with respect to input files.
     * @return true if the cab file is newer than its dependents.
     */
    protected boolean isUpToDate(Vector files)
    {
        boolean upToDate = true;
        for (int i=0; i<files.size() && upToDate; i++)
        {
            String file = files.elementAt(i).toString();
            if (new File(baseDir,file).lastModified() > 
                cabFile.lastModified())
                upToDate = false;
        }
        return upToDate;
    }

    /**
     * Create the cabarc command line to use.
     */
    protected Commandline createCommand(File listFile)
    {
        Commandline command = new Commandline();
        command.setExecutable("cabarc");
        command.createArgument().setValue("-r");
        command.createArgument().setValue("-p");

        if (!doCompress)
        {
            command.createArgument().setValue("-m");
            command.createArgument().setValue("none");
        }

        if (cmdOptions != null)
        {
            command.createArgument().setLine(cmdOptions);
        }
        
        command.createArgument().setValue("n");
        command.createArgument().setFile(cabFile);
        command.createArgument().setValue("@" + listFile.getAbsolutePath());

        return command;
    }

    private static int counter = new Random().nextInt() % 100000;
    protected File createTempFile(String prefix, String suffix)
    {
        if (suffix == null)
        {
            suffix = ".tmp";
        }

        String name = prefix +
            new DecimalFormat("#####").format(new Integer(counter++)) +
            suffix;

        String tmpdir = System.getProperty("java.io.tmpdir");

        // java.io.tmpdir is not present in 1.1
        if (tmpdir == null)
            return new File(name);
        else
            return new File(tmpdir, name);
    }

    /**
     * Creates a list file.  This temporary file contains a list of all files
     * to be included in the cab, one file per line.
     */
    protected File createListFile(Vector files)
        throws IOException
    {
        File listFile = createTempFile("ant", null);
        
        PrintWriter writer = new PrintWriter(new FileOutputStream(listFile));

        for (int i = 0; i < files.size(); i++)
        {
            writer.println(files.elementAt(i).toString());
        }
        writer.close();

        return listFile;
    }

    /**
     * Append all files found by a directory scanner to a vector.
     */
    protected void appendFiles(Vector files, DirectoryScanner ds)
    {
        String[] dsfiles = ds.getIncludedFiles();

        for (int i = 0; i < dsfiles.length; i++)
        {
            files.addElement(dsfiles[i]);
        }
    }

    /**
     * Get the complete list of files to be included in the cab.  Filenames
     * are gathered from filesets if any have been added, otherwise from the
     * traditional include parameters.
     */
    protected Vector getFileList() throws BuildException
    {
        Vector files = new Vector();

        if (filesets.size() == 0)
        {
            // get files from old methods - includes and nested include
            appendFiles(files, super.getDirectoryScanner(baseDir));
        }
        else
        {
            // get files from filesets
            for (int i = 0; i < filesets.size(); i++)
            {
                FileSet fs = (FileSet) filesets.elementAt(i);
                if (fs != null)
                {
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
        if (isUpToDate(files)) return;

        log("Building "+ archiveType +": "+ cabFile.getAbsolutePath());

        // we must be on Windows to continue
        if (!isWindows) {
            log("Using listcab/libcabinet", Project.MSG_VERBOSE);
            
            StringBuffer sb = new StringBuffer();
            
            Enumeration fileEnum = files.elements();
            
            while (fileEnum.hasMoreElements()) {
                sb.append(fileEnum.nextElement()).append("\n");
            }
            sb.append("\n").append(cabFile.getAbsolutePath()).append("\n");
            
            try {
                Process p = Runtime.getRuntime().exec("listcab");
                OutputStream out = p.getOutputStream();
                out.write(sb.toString().getBytes());
                out.flush();
                out.close();
            } catch (IOException ex) {
                String msg = "Problem creating " + cabFile + " " + ex.getMessage();
                throw new BuildException(msg);
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
                    outFile = createTempFile("ant", null);
                    exec.setOutput(outFile);
                }
                    
                exec.setCommand(createCommand(listFile));
                exec.execute();
    
                if (outFile != null) {
                    outFile.delete();
                }
                
                listFile.delete();
            } catch (IOException ioe) {
                String msg = "Problem creating " + cabFile + " " + ioe.getMessage();
                throw new BuildException(msg);
            }
        }
    }
}
