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
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 * Builds EJB support classes using WebLogic's ejbc tool from a directory containing
 * a set of deployment descriptors.
 *
 *
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

    // CheckStyle:VisibilityModifier OFF - bc
    /** Whether to keep the generated files */
    public boolean keepgenerated;
    // CheckStyle:VisibilityModifier ON

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
        if (descriptorDirectory == null
            || !descriptorDirectory.isDirectory()) {
            throw new BuildException("descriptors directory "
                + descriptorDirectory + " is not valid");
        }
        if (generatedFilesDirectory == null
            || !generatedFilesDirectory.isDirectory()) {
            throw new BuildException("dest directory "
                + generatedFilesDirectory + " is not valid");
        }

        if (sourceDirectory == null
            || !sourceDirectory.isDirectory()) {
            throw new BuildException("src directory "
                + sourceDirectory + " is not valid");
        }

        String systemClassPath = System.getProperty("java.class.path");
        String execClassPath
            = FileUtils.translatePath(systemClassPath + ":" + classpath
                                         + ":" + generatedFilesDirectory);
        // get all the files in the descriptor directory
        DirectoryScanner ds = super.getDirectoryScanner(descriptorDirectory);

        String[] files = ds.getIncludedFiles();

        Java helperTask = new Java(this);
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
        helperTask.setClasspath(new Path(getProject(), execClassPath));
        if (helperTask.executeJava() != 0) {
            throw new BuildException("Execution of ejbc helper failed");
        }
    }

    /**
     * get the keep generated attribute.
     * @return the attribute.
     */
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
     * @param newKeepgenerated a boolean as a string.
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
     * @param s the classpath (as a string) to use.
     */
    public void setClasspath(String s) {
        this.classpath = FileUtils.translatePath(s);
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
