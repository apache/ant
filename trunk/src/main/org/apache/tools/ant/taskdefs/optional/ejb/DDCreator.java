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
 * Builds a serialized deployment descriptor given a text file description of the
 * descriptor in the format supported by WebLogic.
 *
 * This ant task is a front end for the weblogic DDCreator tool.
 *
 */
public class DDCreator extends MatchingTask {
    /**
     * The root directory of the tree containing the textual deployment descriptors. The actual
     * deployment descriptor files are selected using include and exclude constructs
     * on the EJBC task, as supported by the MatchingTask superclass.
     */
    private File descriptorDirectory;

    /**
     * The directory where generated serialised deployment descriptors are placed.
     */
    private File generatedFilesDirectory;

    /**
     * The classpath to be used in the weblogic ejbc calls. It must contain the weblogic
     * classes necessary fro DDCreator <b>and</b> the implementation classes of the
     * home and remote interfaces.
     */
    private String classpath;

    /**
     * Do the work.
     *
     * The work is actually done by creating a helper task. This approach allows
     * the classpath of the helper task to be set. Since the weblogic tools require
     * the class files of the project's home and remote interfaces to be available in
     * the classpath, this also avoids having to start ant with the class path of the
     * project it is building.
     *
     * @exception BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException {
        if (descriptorDirectory == null
            || !descriptorDirectory.isDirectory()) {
            throw new BuildException("descriptors directory "
                + descriptorDirectory.getPath() + " is not valid");
        }
        if (generatedFilesDirectory == null
            || !generatedFilesDirectory.isDirectory()) {
            throw new BuildException("dest directory "
                + generatedFilesDirectory.getPath() + " is not valid");
        }

        String args = descriptorDirectory + " " + generatedFilesDirectory;

        // get all the files in the descriptor directory
        DirectoryScanner ds = super.getDirectoryScanner(descriptorDirectory);

        String[] files = ds.getIncludedFiles();

        for (int i = 0; i < files.length; ++i) {
            args += " " + files[i];
        }

        String systemClassPath = System.getProperty("java.class.path");
        String execClassPath = FileUtils.translatePath(systemClassPath + ":" + classpath);
        Java ddCreatorTask = new Java(this);
        ddCreatorTask.setFork(true);
        ddCreatorTask.setClassname("org.apache.tools.ant.taskdefs.optional.ejb.DDCreatorHelper");
        Commandline.Argument arguments = ddCreatorTask.createArg();
        arguments.setLine(args);
        ddCreatorTask.setClasspath(new Path(getProject(), execClassPath));
        if (ddCreatorTask.executeJava() != 0) {
            throw new BuildException("Execution of ddcreator helper failed");
        }
    }

    /**
     * Set the directory from where the text descriptions of the deployment descriptors are
     * to be read.
     *
     * @param dirName the name of the directory containing the text deployment descriptor files.
     */
    public void setDescriptors(String dirName) {
        descriptorDirectory = new File(dirName);
    }

    /**
     * Set the directory into which the serialized deployment descriptors are to
     * be written.
     *
     * @param dirName the name of the directory into which the serialised deployment
     *                descriptors are written.
     */
    public void setDest(String dirName) {
        generatedFilesDirectory = new File(dirName);
    }

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param s the classpath to use for the ddcreator tool.
     */
    public void setClasspath(String s) {
        this.classpath = FileUtils.translatePath(s);
    }
}
