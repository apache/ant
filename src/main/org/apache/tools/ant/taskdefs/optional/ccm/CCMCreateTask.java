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

package org.apache.tools.ant.taskdefs.optional.ccm;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.StringUtils;

/**
 * Creates new Continuus ccm task and sets it as the default.
 *
 * @ant.task name="ccmcreatetask" category="scm"
 */
public class CCMCreateTask extends Continuus implements ExecuteStreamHandler {
    /**
     * /comment -- comments associated to the task
     */
    public static final String FLAG_COMMENT = "/synopsis";

    /**
     *  /platform flag -- target platform
     */
    public static final String FLAG_PLATFORM = "/plat";

    /**
     * /resolver flag
     */
    public static final String FLAG_RESOLVER = "/resolver";

    /**
     * /release flag
     */
    public static final String FLAG_RELEASE = "/release";

    /**
     * /release flag
     */
    public static final String FLAG_SUBSYSTEM = "/subsystem";

    /**
     *  -task flag -- associate checkout task with task
     */
    public static final String FLAG_TASK = "/task";

    private String comment = null;
    private String platform = null;
    private String resolver = null;
    private String release = null;
    private String subSystem = null;
    private String task = null;

    /**
     * Constructor for CCMCreateTask.
     */
    public CCMCreateTask() {
        super();
        setCcmAction(COMMAND_CREATE_TASK);
    }


    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute ccm and then calls Exec's run method
     * to execute the command line.
     * </p>
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();

        // build the command line from what we got the format
        // as specified in the CCM.EXE help
        commandLine.setExecutable(getCcmCommand());
        commandLine.createArgument().setValue(getCcmAction());

        checkOptions(commandLine);

        if (Execute.isFailure(run(commandLine, this))) {
            throw new BuildException("Failed executing: " + commandLine,
                getLocation());
        }

        //create task ok, set this task as the default one
        Commandline commandLine2 = new Commandline();
        commandLine2.setExecutable(getCcmCommand());
        commandLine2.createArgument().setValue(COMMAND_DEFAULT_TASK);
        commandLine2.createArgument().setValue(getTask());

        log(commandLine.describeCommand(), Project.MSG_DEBUG);

        if (run(commandLine2) != 0) {
            throw new BuildException("Failed executing: " + commandLine2,
                getLocation());
        }
    }

    /**
     * Check the command line options.
     */
    private void checkOptions(Commandline cmd) {
        if (getComment() != null) {
            cmd.createArgument().setValue(FLAG_COMMENT);
            cmd.createArgument().setValue("\"" + getComment() + "\"");
        }

        if (getPlatform() != null) {
            cmd.createArgument().setValue(FLAG_PLATFORM);
            cmd.createArgument().setValue(getPlatform());
        }

        if (getResolver() != null) {
            cmd.createArgument().setValue(FLAG_RESOLVER);
            cmd.createArgument().setValue(getResolver());
        }

        if (getSubSystem() != null) {
            cmd.createArgument().setValue(FLAG_SUBSYSTEM);
            cmd.createArgument().setValue("\"" + getSubSystem() + "\"");
        }

        if (getRelease() != null) {
            cmd.createArgument().setValue(FLAG_RELEASE);
            cmd.createArgument().setValue(getRelease());
        }
    }

    /**
     * Get the value of comment.
     * @return value of comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Specifies a comment.
     *
     * @param v  Value to assign to comment.
     */
    public void setComment(String v) {
        this.comment = v;
    }

    /**
     * Get the value of platform.
     * @return value of platform.
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * Specifies the target platform.
     *
     * @param v  Value to assign to platform.
     */
    public void setPlatform(String v) {
        this.platform = v;
    }

    /**
     * Get the value of resolver.
     * @return value of resolver.
     */
    public String getResolver() {
        return resolver;
    }

    /**
     * Specifies the resolver.
     *
     * @param v  Value to assign to resolver.
     */
    public void setResolver(String v) {
        this.resolver = v;
    }

    /**
     * Get the value of release.
     * @return value of release.
     */
    public String getRelease() {
        return release;
    }

    /**
     * Specify the CCM release.
     *
     * @param v  Value to assign to release.
     */
    public void setRelease(String v) {
        this.release = v;
    }

    /**
     * Get the value of subSystem.
     * @return value of subSystem.
     */
    public String getSubSystem() {
        return subSystem;
    }

    /**
     * Specifies the subsystem.
     *
     * @param v  Value to assign to subSystem.
     */
    public void setSubSystem(String v) {
        this.subSystem = v;
    }

    /**
     * Get the value of task.
     * @return value of task.
     */
    public String getTask() {
        return task;
    }

    /**
     * Specifies the task number used to checkin
     * the file (may use 'default').
     *
     * @param v  Value to assign to task.
     */
    public void setTask(String v) {
        this.task = v;
    }

    // implementation of org.apache.tools.ant.taskdefs.ExecuteStreamHandler interface

    /**
     *
     * @throws IOException on error
     */
    @Override
    public void start() throws IOException {
    }

    /**
     *
     */
    @Override
    public void stop() {
    }

    /**
     *
     * @param param1 the output stream
     * @exception IOException on error
     */
    @Override
    public void setProcessInputStream(OutputStream param1) throws IOException {
    }

    /**
     *
     * @param is the input stream
     * @exception IOException on error
     */
    @Override
    public void setProcessErrorStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String s = reader.readLine();
            if (s != null) {
                log("err " + s, Project.MSG_DEBUG);
            }
        }
    }

    /**
     * read the output stream to retrieve the new task number.
     * @param is InputStream
     * @throws IOException on error
     */
    @Override
    public void setProcessOutputStream(InputStream is) throws IOException {
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(is))) {
            String buffer = reader.readLine();
            if (buffer != null) {
                log("buffer:" + buffer, Project.MSG_DEBUG);
                String taskstring = buffer.substring(buffer.indexOf(' ')).trim();
                taskstring = taskstring.substring(0, taskstring.lastIndexOf(' ')).trim();
                setTask(taskstring);
                log("task is " + getTask(), Project.MSG_DEBUG);
            }
        } catch (NullPointerException npe) {
            log("error procession stream, null pointer exception", Project.MSG_ERR);
            log(StringUtils.getStackTrace(npe), Project.MSG_ERR);
            throw new BuildException(npe);
        } catch (Exception e) {
            log("error procession stream " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException(e.getMessage());
        }

    }

}
