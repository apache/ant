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


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;


/**
 * Task allows to reconfigure a project, recurcively or not
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public class CCMReconfigure extends Continuus {

    private String project = null;
    private boolean recurse = false;
    private boolean verbose = false;

    public CCMReconfigure() {
        super();
        setCcmAction(COMMAND_RECONFIGURE);
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
        Project aProj = getProject();
        int result = 0;

        // build the command line from what we got the format
        // as specified in the CCM.EXE help
        commandLine.setExecutable(getCcmCommand());
        commandLine.createArgument().setValue(getCcmAction());

        checkOptions(commandLine);

        result = run(commandLine);
        if (result != 0) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }
    }


    /**
     * Check the command line options.
     */
    private void checkOptions(Commandline cmd) {

        if (isRecurse() == true) {
            cmd.createArgument().setValue(FLAG_RECURSE);
        } // end of if ()

        if (isVerbose() == true) {
            cmd.createArgument().setValue(FLAG_VERBOSE);
        } // end of if ()

        if (getCcmProject() != null) {
            cmd.createArgument().setValue(FLAG_PROJECT);
            cmd.createArgument().setValue(getCcmProject());
        }

    }

    /**
     * Get the value of project.
     * @return value of project.
     */
    public String getCcmProject() {
        return project;
    }

    /**
     * Sets the ccm project on which the operation is applied.
     * @param v  Value to assign to project.
     */
    public void setCcmProject(String v) {
        this.project = v;
    }


    /**
     * Get the value of recurse.
     * @return value of recurse.
     */
    public boolean isRecurse() {
        return recurse;
    }

    /**
     * If true, recurse on subproject (default false).
     *
     * @param v  Value to assign to recurse.
     */
    public void setRecurse(boolean v) {
        this.recurse = v;
    }


    /**
     * Get the value of verbose.
     * @return value of verbose.
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * If true, do a verbose reconfigure operation (default false).
     * @param v  Value to assign to verbose.
     */
    public void setVerbose(boolean v) {
        this.verbose = v;
    }


    /**
     * /recurse --
     */
    public static final String FLAG_RECURSE = "/recurse";

    /**
     * /recurse --
     */
    public static final String FLAG_VERBOSE = "/verbose";


    /**
     *  /project flag -- target project
     */
    public static final String FLAG_PROJECT = "/project";

}

