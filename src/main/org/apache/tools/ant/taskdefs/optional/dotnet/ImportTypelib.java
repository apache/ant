/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.File;

/**
 * Wrapper to .NET's tlbimport; imports a tlb file to a NET assembly
 * by generating a binary assembly (.dll) that contains all the binding
 * metadata. Uses date timestamps to minimise rebuilds.
 * @since Ant 1.6
 * @uthor steve loughran
 * @ant.task    name="ImportTypelib" category="dotnet"
 */
public class ImportTypelib extends Task {


    /**
     * input file; preceeds options
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
    private boolean useSysArray=false;

    /**
     * /unsafe
     */
    private boolean unsafe=false;

    /**
     * extra commands?
     */
    private String extraOptions=null;

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
        if (srcFile != null || !srcFile.exists()) {
            throw new BuildException(
                    "source file does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new BuildException(
                    "source file is a directory");
        }
        if(namespace==null) {
            throw new BuildException("No namespace");
        }
    }

    /**
     * Create a typelib command
     * @exception BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException {
        validate();
        log("Importing typelib "+srcFile
            +" to assembly "+destFile
            +" in namespace "+namespace, Project.MSG_VERBOSE);
        //rebuild unless the dest file is newer than the source file
        if (srcFile.exists() && destFile.exists() &&
                srcFile.lastModified() <= destFile.lastModified()) {
            log("The typelib is up to date",Project.MSG_VERBOSE);
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


    }
}
