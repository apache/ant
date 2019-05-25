/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Generates a Borland Application Server 4.5 client JAR using as
 * input the EJB JAR file.
 *
 * Two mode are available: java mode (default) and fork mode. With the fork mode,
 * it is impossible to add classpath to the command line.
 *
 * @ant.task name="blgenclient" category="ejb"
 */
public class BorlandGenerateClient extends Task {
    static final String JAVA_MODE = "java";
    static final String FORK_MODE = "fork";

    // CheckStyle:VisibilityModifier OFF - bc
    /** debug the generateclient task */
    boolean debug = false;

    /** hold the ejbjar file name */
    File ejbjarfile = null;

    /** hold the client jar file name */
    File clientjarfile = null;

    /** hold the classpath */
    Path classpath;

    /** hold the mode (java|fork) */
    String mode = FORK_MODE;

    /** hold the version */
    int version = BorlandDeploymentTool.BAS;
    // CheckStyle:VisibilityModifier ON

    /**
     * Set the version attribute.
     * @param version the value to use.
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Command launching mode: java or fork.
     * @param s the mode to use.
     */
    public void setMode(String s) {
        mode = s;
    }

    /**
     * If true, turn on the debug mode for each of the Borland tools launched.
     * @param debug a <code>boolean</code> value.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * EJB JAR file.
     * @param ejbfile the file to use.
     */
    public void setEjbjar(File ejbfile) {
        ejbjarfile = ejbfile;
    }

    /**
     * Client JAR file name.
     * @param clientjar the file to use.
     */
    public void setClientjar(File clientjar) {
        clientjarfile = clientjar;
    }

    /**
     * Path to use for classpath.
     * @param classpath the path to use.
     */
    public void setClasspath(Path classpath) {
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * Adds path to the classpath.
     * @return a path to be configured as a nested element.
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Reference to existing path, to use as a classpath.
     * @param r the reference to use.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Do the work.
     *
     * The work is actually done by creating a separate JVM to run a java task.
     *
     * @exception BuildException if something goes wrong with the build
     */
    @Override
    public void execute() throws BuildException {
        if (ejbjarfile == null || ejbjarfile.isDirectory()) {
            throw new BuildException("invalid ejb jar file.");
        }

        if (clientjarfile == null || clientjarfile.isDirectory()) {
            log("invalid or missing client jar file.", Project.MSG_VERBOSE);
            String ejbjarname = ejbjarfile.getAbsolutePath();
            //clientname = ejbjarfile+client.jar
            String clientname = ejbjarname.substring(0, ejbjarname.lastIndexOf("."));
            clientname += "client.jar";
            clientjarfile = new File(clientname);
        }

        if (mode == null) {
            log("mode is null default mode  is java");
            setMode(JAVA_MODE);
        }

        if (version != BorlandDeploymentTool.BES && version != BorlandDeploymentTool.BAS) {
            throw new BuildException("version %d is not supported", version);
        }

        log("client jar file is " + clientjarfile);

        if (FORK_MODE.equalsIgnoreCase(mode)) {
            executeFork();
        } else {
            executeJava();
        } // end of else
    }

    /**
     * launch the generate client using java api.
     * @throws BuildException if there is an error.
     */
    protected void executeJava() throws BuildException {
        try {
            if (version == BorlandDeploymentTool.BES)  {
                throw new BuildException(
                    "java mode is supported only for previous version <= %d",
                    BorlandDeploymentTool.BAS);
            }

            log("mode : java");

            Java execTask = new Java(this);
            execTask.setDir(new File("."));
            execTask.setClassname("com.inprise.server.commandline.EJBUtilities");
            //classpath
            //add at the end of the classpath
            //the system classpath in order to find the tools.jar file
            execTask.setClasspath(classpath.concatSystemClasspath());

            execTask.setFork(true);
            execTask.createArg().setValue("generateclient");
            if (debug) {
                execTask.createArg().setValue("-trace");
            }

            execTask.createArg().setValue("-short");
            execTask.createArg().setValue("-jarfile");
            // ejb jar file
            execTask.createArg().setValue(ejbjarfile.getAbsolutePath());
            //client jar file
            execTask.createArg().setValue("-single");
            execTask.createArg().setValue("-clientjarfile");
            execTask.createArg().setValue(clientjarfile.getAbsolutePath());

            log("Calling EJBUtilities", Project.MSG_VERBOSE);
            execTask.execute();

        } catch (Exception e) {
            // Have to catch this because of the semantics of calling main()
            throw new BuildException("Exception while calling generateclient", e);
        }
    }

    /**
     * launch the generate client using system api.
     * @throws BuildException if there is an error.
     */
    protected void executeFork() throws BuildException {
        if (version == BorlandDeploymentTool.BAS) {
            executeForkV4();
        }
        if (version == BorlandDeploymentTool.BES) {
            executeForkV5();
        }
    }

    /**
     * launch the generate client using system api.
     * @throws BuildException if there is an error.
     */
    protected void executeForkV4() throws BuildException {
        try {
            log("mode : fork " + BorlandDeploymentTool.BAS, Project.MSG_DEBUG);

            ExecTask execTask = new ExecTask(this);
            execTask.setDir(new File("."));
            execTask.setExecutable("iastool");
            execTask.createArg().setValue("generateclient");
            if (debug) {
                execTask.createArg().setValue("-trace");
            }

            execTask.createArg().setValue("-short");
            execTask.createArg().setValue("-jarfile");
            // ejb jar file
            execTask.createArg().setValue(ejbjarfile.getAbsolutePath());
            //client jar file
            execTask.createArg().setValue("-single");
            execTask.createArg().setValue("-clientjarfile");
            execTask.createArg().setValue(clientjarfile.getAbsolutePath());

            log("Calling iastool", Project.MSG_VERBOSE);
            execTask.execute();
        } catch (Exception e) {
            // Have to catch this because of the semantics of calling main()
            throw new BuildException("Exception while calling generateclient", e);
        }
    }

    /**
     * launch the generate client using system api.
     * @throws BuildException if there is an error.
     */
    protected void executeForkV5() throws BuildException {
        try {
            log("mode : fork " + BorlandDeploymentTool.BES, Project.MSG_DEBUG);
            ExecTask execTask = new ExecTask(this);
            execTask.setDir(new File("."));
            execTask.setExecutable("iastool");
            if (debug) {
                execTask.createArg().setValue("-debug");
            }
            execTask.createArg().setValue("-genclient");
            execTask.createArg().setValue("-jars");
            // ejb jar file
            execTask.createArg().setValue(ejbjarfile.getAbsolutePath());
            //client jar file
            execTask.createArg().setValue("-target");
            execTask.createArg().setValue(clientjarfile.getAbsolutePath());
            //classpath
            execTask.createArg().setValue("-cp");
            execTask.createArg().setValue(classpath.toString());
            log("Calling iastool", Project.MSG_VERBOSE);
            execTask.execute();
        } catch (Exception e) {
            // Have to catch this because of the semantics of calling main()
            throw new BuildException("Exception while calling generateclient", e);
        }
    }

}
