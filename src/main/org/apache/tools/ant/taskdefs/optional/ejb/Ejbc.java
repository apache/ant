/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000,2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.ejb;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

import java.io.File;

/**
 * Builds EJB support classes using WebLogic's ejbc tool from a directory containing
 * a set of deployment descriptors.
 *
 *
 * @author Conor MacNeill, Cortex ebusiness Pty Limited
 */
public class Ejbc extends MatchingTask {
    /**
     * The root directory of the tree containing the serialised deployment desciptors. The actual
     * deployment descriptor files are selected using include and exclude constructs
     * on the ejbc task provided by the MatchingTask superclass.
     */
    private File descriptorDirectory;

    /**
     * The directory where generated files are placed.
     */
    private File generatedFilesDirectory;

    /**
     * The name of the manifest file generated for the EJB jar.
     */
    private File generatedManifestFile;

    /**
     * The classpath to be used in the weblogic ejbc calls. It must contain the weblogic
     * classes <b>and</b> the implementation classes of the home and remote interfaces.
     */
    private String classpath;

    /**
     * The source directory for the home and remote interfaces. This is used to determine if
     * the generated deployment classes are out of date.
     */
    private File sourceDirectory;

    public boolean keepgenerated;

    /**
     * Do the work.
     *
     * The work is actually done by creating a separate JVM to run a helper task.
     * This approach allows the classpath of the helper task to be set. Since the
     * weblogic tools require the class files of the project's home and remote
     * interfaces to be available in the classpath, this also avoids having to
     * start ant with the class path of the project it is building.
     *
     * @exception BuildException if someting goes wrong with the build
     */
    public void execute() throws BuildException {
        if (descriptorDirectory == null ||
            !descriptorDirectory.isDirectory()) {
            throw new BuildException("descriptors directory " + descriptorDirectory.getPath() +
                                     " is not valid");
        }
        if (generatedFilesDirectory == null ||
            !generatedFilesDirectory.isDirectory()) {
            throw new BuildException("dest directory " + generatedFilesDirectory.getPath() +
                                     " is not valid");
        }

        if (sourceDirectory == null ||
            !sourceDirectory.isDirectory()) {
            throw new BuildException("src directory " + sourceDirectory.getPath() +
                                     " is not valid");
        }

        String systemClassPath = System.getProperty("java.class.path");
        String execClassPath = project.translatePath(systemClassPath + ":" + classpath +
                                                         ":" + generatedFilesDirectory);
        // get all the files in the descriptor directory
        DirectoryScanner ds = super.getDirectoryScanner(descriptorDirectory);

        String[] files = ds.getIncludedFiles();

        Java helperTask = (Java) project.createTask("java");
        helperTask.setTaskName(getTaskName());
        helperTask.setFork(true);
        helperTask.setClassname("org.apache.tools.ant.taskdefs.optional.ejb.EjbcHelper");
        String args = "";
        args += " " + descriptorDirectory;
        args += " " + generatedFilesDirectory;
        args += " " + sourceDirectory;
        args += " " + generatedManifestFile;
        args += " " + keepgenerated;

        for (int i = 0; i < files.length; ++i) {
            args += " " + files[i];
        }

        Commandline.Argument arguments = helperTask.createArg();
        arguments.setLine(args);
        helperTask.setClasspath(new Path(project, execClassPath));
        if (helperTask.executeJava() != 0) {
            throw new BuildException("Execution of ejbc helper failed");
        }
    }

    public boolean getKeepgenerated() {
        return keepgenerated;
    }

    /**
     * Set the directory from where the serialized deployment descriptors are
     * to be read.
     *
     * @param dirName the name of the directory containing the serialised deployment descriptors.
     */
    public void setDescriptors(String dirName) {
        descriptorDirectory = new File(dirName);
    }

    /**
     * Set the directory into which the support classes, RMI stubs, etc are to be written.
     *
     * @param dirName the name of the directory into which code is generated
     */
    public void setDest(String dirName) {
        generatedFilesDirectory = new File(dirName);
    }

    /**
     * If true, ejbc will keep the
     * intermediate Java files used to build the class files.
     * This can be useful when debugging.
     */
    public void setKeepgenerated(String newKeepgenerated) {
        keepgenerated = Boolean.valueOf(newKeepgenerated.trim()).booleanValue();

    }

    /**
     * Set the name of the generated manifest file.
     *
     * For each EJB that is processed an entry is created in this file. This can then be used
     * to create a jar file for dploying the beans.
     *
     * @param manifestFilename the name of the manifest file to be generated.
     */
    public void setManifest(String manifestFilename) {
        generatedManifestFile = new File(manifestFilename);
    }

    /**
     * Set the classpath to be used for this compilation.
     */
    public void setClasspath(String s) {
        this.classpath = project.translatePath(s);
    }

    /**
     * Set the directory containing the source code for the home interface, remote interface
     * and public key class definitions.
     *
     * @param dirName the directory containg the source tree for the EJB's interface classes.
     */
    public void setSrc(String dirName) {
        sourceDirectory = new File(dirName);
    }
}
