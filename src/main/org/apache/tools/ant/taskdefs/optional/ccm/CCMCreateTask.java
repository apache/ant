/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.ccm;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.types.Commandline;


/**
 * Creates new Continuus ccm task and sets it as the default.
 *
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 *
 * @ant.task name="ccmcreatetask" category="scm"
 */
public class CCMCreateTask extends Continuus implements ExecuteStreamHandler {

    private String comment = null;
    private String platform = null;
    private String resolver = null;
    private String release = null;
    private String subSystem = null;
    private String task = null;

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
     */
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();
        int result = 0;

        // build the command line from what we got the format
        // as specified in the CCM.EXE help
        commandLine.setExecutable(getCcmCommand());
        commandLine.createArgument().setValue(getCcmAction());

        checkOptions(commandLine);

        result = run(commandLine, this);
        if (result != 0) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }

        //create task ok, set this task as the default one
        Commandline commandLine2 = new Commandline();
        commandLine2.setExecutable(getCcmCommand());
        commandLine2.createArgument().setValue(COMMAND_DEFAULT_TASK);
        commandLine2.createArgument().setValue(getTask());

        log(commandLine.describeCommand(), Project.MSG_DEBUG);

        result = run(commandLine2);
        if (result != 0) {
            String msg = "Failed executing: " + commandLine2.toString();
            throw new BuildException(msg, location);
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
        } // end of if ()

        if (getResolver() != null) {
            cmd.createArgument().setValue(FLAG_RESOLVER);
            cmd.createArgument().setValue(getResolver());
        } // end of if ()

        if (getSubSystem() != null) {
            cmd.createArgument().setValue(FLAG_SUBSYSTEM);
            cmd.createArgument().setValue("\"" + getSubSystem() + "\"");
        } // end of if ()

        if (getRelease() != null) {
            cmd.createArgument().setValue(FLAG_RELEASE);
            cmd.createArgument().setValue(getRelease());
        } // end of if ()
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
     *  -task flag -- associate checckout task with task
     */
    public static final String FLAG_TASK = "/task";


    // implementation of org.apache.tools.ant.taskdefs.ExecuteStreamHandler interface

    /**
     *
     * @exception java.io.IOException
     */
    public void start() throws IOException {
    }

    /**
     *
     */
    public void stop() {
    }

    /**
     *
     * @param param1
     * @exception java.io.IOException
     */
    public void setProcessInputStream(OutputStream param1) throws IOException {
    }

    /**
     *
     * @param is
     * @exception java.io.IOException
     */
    public void setProcessErrorStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String s = reader.readLine();
        if (s != null) {
            log("err " + s, Project.MSG_DEBUG);
        } // end of if ()
    }

    /**
     * read the output stream to retrieve the new task number.
     * @param is InputStream
     * @exception java.io.IOException
     */
    public void setProcessOutputStream(InputStream is) throws IOException {

        String buffer = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            buffer = reader.readLine();
            if (buffer != null) {
                log("buffer:" + buffer, Project.MSG_DEBUG);
                String taskstring = buffer.substring(buffer.indexOf(' ')).trim();
                taskstring = taskstring.substring(0, taskstring.lastIndexOf(' ')).trim();
                setTask(taskstring);
                log("task is " + getTask(), Project.MSG_DEBUG);
            } // end of if ()
        } catch (NullPointerException npe) {
            log("error procession stream , null pointer exception", Project.MSG_ERR);
            npe.printStackTrace();
            throw new BuildException(npe.getClass().getName());
        } catch (Exception e) {
            log("error procession stream " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException(e.getMessage());
        } // end of try-catch

    }

}

