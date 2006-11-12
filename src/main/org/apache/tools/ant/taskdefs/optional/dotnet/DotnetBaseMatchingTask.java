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

package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.condition.Os;
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
 */
public class DotnetBaseMatchingTask extends MatchingTask {
    // CheckStyle:VisibilityModifier OFF - bc
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
     * Are we running on Windows?
     *
     * @since Ant 1.6.3
     */
    // CheckStyle:ConstantNameCheck OFF - bc
    protected static final boolean isWindows = Os.isFamily("windows");

    // CheckStyle:ConstantNameCheck ON
    // CheckStyle:VisibilityModifier ON

    /**
    * Overridden because we need to be able to set the srcDir.
    * @return the source directory.
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
     * @param src a fileset.
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
     * @param command the command to create the files for.
     * @param filesToBuild vector to add files to
     * @param outputTimestamp timestamp to compare against
     * @return number of files out of date
     */
    protected int buildFileList(NetCommand command, Hashtable filesToBuild, long outputTimestamp) {
        int filesOutOfDate = 0;
        boolean scanImplicitFileset
            = getSrcDir() != null || filesets.size() == 0;
        if (scanImplicitFileset) {
            //scan for an implicit fileset if there was a srcdir set
            //or there was no srcDir set but there was no contained classes
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
            filesOutOfDate += command.scanOneFileset(
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
        int count = filesToBuild.size();
        log("compiling " + count + " file" + ((count == 1) ? "" : "s"),
                Project.MSG_VERBOSE);
        Enumeration files = filesToBuild.elements();
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
     * @param command the command to update.
     * @param ignoreTimestamps not used.
     */
    protected void addFilesAndExecute(NetCommand command, boolean ignoreTimestamps) {
        long outputTimestamp = getOutputFileTimestamp();
        Hashtable filesToBuild = new Hashtable();
        int filesOutOfDate = buildFileList(command, filesToBuild, outputTimestamp);

        //now run the command of exe + settings + files
        if (filesOutOfDate > 0) {
            //add the files to the command
            addFilesToCommand(filesToBuild, command);
            command.runCommand();
        } else {
            log("output file is up to date", Project.MSG_VERBOSE);
        }
    }



}
