/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2003 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */


package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * refactoring of some stuff so that different things (like ILASM)
 * can use shared code.
 * @author steve loughran
 */
public class DotnetBaseMatchingTask extends MatchingTask {
    /**
     *  output file. If not supplied this is derived from the source file
     */
    protected File outputFile;
    /**
     * filesets of file to compile
     */
    protected Vector filesets = new Vector();

    /**
     *  source directory upon which the search pattern is applied
     */
    protected File srcDir;

    /**
    * Overridden because we need to be able to set the srcDir.
    */
    public File getSrcDir() {
        return this.srcDir;
    }

    /**
     *  Set the source directory of the files to be compiled.
     *
     *@param  srcDirName  The new SrcDir value
     */
    public void setSrcDir(File srcDirName) {
        this.srcDir = srcDirName;
    }

    /**
     *  Set the name of exe/library to create.
     *
     *@param  file  The new outputFile value
     */
    public void setDestFile(File file) {
        outputFile = file;
    }

    /**
     * add a new source directory to the compile
     * @param src
     */
    public void addSrc(FileSet src) {
        filesets.add(src);
    }

    /**
     * get the destination file
     * @return the dest file or null for not assigned
     */
    public File getDestFile() {
        return outputFile;
    }

    /**
     * create the list of files
     * @param filesToBuild vector to add files to
     * @param outputTimestamp timestamp to compare against
     * @return number of files out of date
     */
    protected int buildFileList(NetCommand command, Hashtable filesToBuild, long outputTimestamp) {
        int filesOutOfDate=0;
        boolean scanImplicitFileset=getSrcDir()!=null || filesets.size()==0;
        if(scanImplicitFileset) {
            //scan for an implicit fileset if there was a srcdir set
            //or there was no srcDir set but the @
            if (getSrcDir() == null) {
                //if there is no src dir here, set it
                setSrcDir(getProject().resolveFile("."));
            }
            log("working from source directory " + getSrcDir(),
                    Project.MSG_VERBOSE);
            //get dependencies list.
            DirectoryScanner scanner = getDirectoryScanner(getSrcDir());
            filesOutOfDate = command.scanOneFileset(scanner,
                    filesToBuild, outputTimestamp);
        }
        //get any included source directories
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            filesOutOfDate+= command.scanOneFileset(
                    fs.getDirectoryScanner(getProject()),
                    filesToBuild,
                    outputTimestamp);
        }

        return filesOutOfDate;
    }

    /**
     * add the list of files to a command
     * @param filesToBuild vector of files
     * @param command the command to append to
     */
    protected void addFilesToCommand(Hashtable filesToBuild, NetCommand command) {
        int count=filesToBuild.size();
        log("compiling " + count + " file" + ((count== 1) ? "" : "s"));
        Enumeration files=filesToBuild.elements();
        while (files.hasMoreElements()) {
            File file = (File) files.nextElement();
            command.addArgument(file.toString());
        }
    }

    /**
     * determine the timestamp of the output file
     * @return a timestamp or 0 for no output file known/exists
     */
    protected long getOutputFileTimestamp() {
        long outputTimestamp;
        if (getDestFile() != null && getDestFile().exists()) {
            outputTimestamp = getDestFile().lastModified();
        } else {
            outputTimestamp = 0;
        }
        return outputTimestamp;
    }

    /**
     * finish off the command by adding all dependent files, execute
     * @param command
     */
    protected void addFilesAndExecute(NetCommand command, boolean ignoreTimestamps) {
        long outputTimestamp = getOutputFileTimestamp();
        Hashtable filesToBuild =new Hashtable();
        int filesOutOfDate = buildFileList(command,filesToBuild, outputTimestamp);

        //add the files to the command
        addFilesToCommand(filesToBuild, command);


        //now run the command of exe + settings + files
        if (filesOutOfDate > 0) {
            command.runCommand();
        } else {
            log("output file is up to date",Project.MSG_VERBOSE);
        }
    }



}
