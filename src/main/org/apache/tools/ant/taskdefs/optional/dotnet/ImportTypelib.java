/*
 * Copyright  2000-2004 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;

/**
 * Import a COM type library into the .NET framework.
 * <p>
 *
 * This task is a wrapper to .NET's tlbimport; it imports a tlb file to a NET assembly
 * by generating a binary assembly (.dll) that contains all the binding
 * metadata. It uses date timestamps to minimise rebuilds.
 * <p>
 * Example
 * <pre>
 *     &lt;importtypelib
 *       srcfile="xerces.tlb"
 *       destfile="xerces.dll"
 *       namespace="Apache.Xerces"/&gt;
 * </pre>
 * @since Ant 1.6
 * @author steve loughran
 * @ant.task category="dotnet"
 */
public class ImportTypelib extends Task {


    /**
     * input file; precedes options
     */
    private File srcFile;

    /**
     * /out:file
     */
    private File destFile;

    /**
     *  /namespace:[string]
     */
    private String namespace;

    /**
     * /sysarray
     */
    private boolean useSysArray = false;

    /**
     * /unsafe
     */
    private boolean unsafe = false;

    /**
     * extra commands?
     */
    private String extraOptions = null;

    /**
     * name the output file. required
     * @param destFile
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    /**
     * what namespace is the typelib to be in. required
     * @param namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * what is the source .tlb file? required.
     * @param srcFile
     */
    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }

    /**
     * do you want unsafe code.
     * @param unsafe
     */
    public void setUnsafe(boolean unsafe) {
        this.unsafe = unsafe;
    }

    /**
     * set this to map a COM SafeArray to the System.Array class
     * @param useSysArray
     */
    public void setUseSysArray(boolean useSysArray) {
        this.useSysArray = useSysArray;
    }

    /**
     * set any extra options that are not yet supported by this task.
     * @param extraOptions
     */
    public void setExtraOptions(String extraOptions) {
        this.extraOptions = extraOptions;
    }

    /**
     * validation code
     * @throws  BuildException  if validation failed
     */
    protected void validate()
            throws BuildException {
        if (destFile == null) {
            throw new BuildException("destination file must be specified");
        }
        if (destFile.isDirectory()) {
            throw new BuildException(
                    "destination file is a directory");
        }
        if (srcFile == null || !srcFile.exists()) {
            throw new BuildException(
                    "source file does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new BuildException(
                    "source file is a directory");
        }
        if (namespace == null) {
            throw new BuildException("No namespace");
        }
    }

    /**
     * Test for disassembly being needed; use existence and granularity
     * correct date stamps
     * @return true iff a rebuild is required.
     */
    private boolean isExecuteNeeded() {
        if (!destFile.exists()) {
            log("Destination file does not exist: a build is required",
                    Project.MSG_VERBOSE);
            return true;
        }
        long sourceTime = srcFile.lastModified();
        long destTime = destFile.lastModified();
        if (sourceTime > (destTime + FileUtils.newFileUtils().getFileTimestampGranularity())) {
            log("Source file is newer than the dest file: a rebuild is required",
                    Project.MSG_VERBOSE);
            return true;
        } else {
            log("The output file is up to date", Project.MSG_VERBOSE);
            return false;
        }

    }


    /**
     * Create a typelib command
     * @exception BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException {
        validate();
        log("Importing typelib " + srcFile
            + " to assembly " + destFile
            + " in namespace " + namespace, Project.MSG_VERBOSE);
        //rebuild unless the dest file is newer than the source file
        if(!isExecuteNeeded()) {
            return;
        }

        NetCommand command = new NetCommand(this, "ImportTypelib", "tlbimp");
        command.setFailOnError(true);
        command.addArgument(srcFile.toString());
        //fill in args
        command.addArgument("/nologo");
        command.addArgument("/out:" + destFile);
        command.addArgument("/namespace:", namespace);
        if (useSysArray) {
            command.addArgument("/sysarray");
        }
        if (unsafe) {
            command.addArgument("/unsafe");
        }
        command.addArgument(extraOptions);
        command.runCommand();
    }
}
