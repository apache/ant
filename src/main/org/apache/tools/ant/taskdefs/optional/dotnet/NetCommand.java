/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
/*
 *  build notes
 *  The reference CD to listen to while editing this file is
 *  Underworld Everything, Everything
 *  variable naming policy from Fowler's refactoring book.
 */
// place below the optional ant tasks package

package org.apache.tools.ant.taskdefs.optional.dotnet;

// imports

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;

/**
 *  This is a helper class to spawn net commands out. In its initial form it
 *  contains no .net specifics, just contains all the command line/exe
 *  construction stuff. However, it may be handy in future to have a means of
 *  setting the path to point to the dotnet bin directory; in which case the
 *  shared code should go in here.
 *
 *@author     Steve Loughran steve_l@iseran.com
 *@version    0.5
 */

public class NetCommand {

    /**
     *  owner project
     */
    protected Task owner;

    /**
     *  executabe
     */
    protected Execute executable;

    /**
     *  what is the command line
     */
    protected Commandline commandLine;

    /**
     *  title of the command
     */
    protected String title;

    /**
     *  actual program to invoke
     */
    protected String program;

    /**
     *  trace flag
     */
    protected boolean traceCommandLine = false;

    /**
     *  flag to control action on execution trouble
     */
    protected boolean failOnError;


    /**
     *  constructor
     *
     *@param  title        (for logging/errors)
     *@param  owner        owner task
     *@param  program      app we are to run
     */

    public NetCommand(Task owner, String title, String program) {
        this.owner = owner;
        this.title = title;
        this.program = program;
        commandLine = new Commandline();
        commandLine.setExecutable(program);
        prepareExecutor();
    }


    /**
     *  turn tracing on or off
     *
     *@param  b  trace flag
     */
    public void setTraceCommandLine(boolean b) {
        traceCommandLine = b;
    }


    /**
     *  set fail on error flag
     *
     *@param  b  fail flag -set to true to cause an exception to be raised if
     *      the return value != 0
     */
    public void setFailOnError(boolean b) {
        failOnError = b;
    }


    /**
     *  query fail on error flag
     *
     *@return    The failFailOnError value
     */
    public boolean getFailFailOnError() {
        return failOnError;
    }


    /**
     *  verbose text log
     *
     *@param  msg  string to add to log iff verbose is defined for the build
     */
    protected void logVerbose(String msg) {
        owner.getProject().log(msg, Project.MSG_VERBOSE);
    }


    /**
     *  error text log
     *
     *@param  msg  message to display as an error
     */
    protected void logError(String msg) {
        owner.getProject().log(msg, Project.MSG_ERR);
    }


    /**
     *  add an argument to a command line; do nothing if the arg is null or
     *  empty string
     *
     *@param  argument  The feature to be added to the Argument attribute
     */
    public void addArgument(String argument) {
        if (argument != null && argument.length() != 0) {
            commandLine.createArgument().setValue(argument);
        }
    }

    public void addArgument(String argument1, String argument2) {
        if (argument2 != null && argument2.length() != 0) {
            commandLine.createArgument().setValue(argument1 + argument2);
        }
    }    

    /**
     *  set up the command sequence..
     */
    protected void prepareExecutor() {
        // default directory to the project's base directory
        if (owner == null) {
            throw new RuntimeException("no owner"); 
        }
        if (owner.getProject() == null) {
            throw new RuntimeException("Owner has no project"); 
        }
        File dir = owner.getProject().getBaseDir();
        ExecuteStreamHandler handler = new LogStreamHandler(owner,
                Project.MSG_INFO, Project.MSG_WARN);
        executable = new Execute(handler, null);
        executable.setAntRun(owner.getProject());
        executable.setWorkingDirectory(dir);
    }


    /**
     *  Run the command using the given Execute instance.
     *
     *@exception  BuildException  iff something goes wrong and the
     *      failOnError flag is true
     */
    public void runCommand()
             throws BuildException {
        int err = -1;
        // assume the worst
        try {
            if (traceCommandLine) {
                owner.log(commandLine.describeCommand());
            } else {
                //in verbose mode we always log stuff
                logVerbose(commandLine.describeCommand());
            }
            executable.setCommandline(commandLine.getCommandline());
            err = executable.execute();
            if (err != 0) {
                if (failOnError) {
                    throw new BuildException(title + " returned: " + err, owner.getLocation());
                } else {
                    owner.log(title + "  Result: " + err, Project.MSG_ERR);
                }
            }
        } catch (IOException e) {
            throw new BuildException(title + " failed: " + e, e, owner.getLocation());
        }
    }
}

