/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.clearcase;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;

import java.io.File;

/**
 * A base class for creating tasks for executing commands on ClearCase.
 * <p>
 * The class extends the 'exec' task as it operates by executing the cleartool program
 * supplied with ClearCase. By default the task expects the cleartool executable to be
 * in the path, * you can override this be specifying the cleartooldir attribute.
 * </p>
 * <p>
 * This class provides set and get methods for the 'viewpath' attribute. It
 * also contains constants for the flags that can be passed to cleartool.
 * </p>
 *
 * @author Curtis White
 */
public abstract class ClearCase extends Task {
    private String m_ClearToolDir = "";
    private String m_viewPath = null;

    /**
     * Set the directory where the cleartool executable is located
     *
     * @param dir the directory containing the cleartool executable
     */
    public final void setClearToolDir(String dir) {
        m_ClearToolDir = project.translatePath(dir);
    }

    /**
     * Builds and returns the command string to execute cleartool
     *
     * @return String containing path to the executable
     */
    protected final String getClearToolCommand() {
        String toReturn = m_ClearToolDir;
        if ( !toReturn.equals("") && !toReturn.endsWith("/") ) {
            toReturn += "/";
        }

        toReturn += CLEARTOOL_EXE;

        return toReturn;
    }

    /**
     * Set the path to the item in a clearcase view to operate on
     *
     * @param viewPath Path to the view directory or file
     */
    public final void setViewPath(String viewPath) {
        m_viewPath = viewPath;
    }

    /**
     * Get the path to the item in a clearcase view
     *
     * @return m_viewPath
     */
    public String getViewPath() {
        return m_viewPath;
    }


    protected int run(Commandline cmd) {
        try {
            Project aProj = getProject();
            Execute exe = new Execute(new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN));
            exe.setAntRun(aProj);
            exe.setWorkingDirectory(aProj.getBaseDir());
            exe.setCommandline(cmd.getCommandline());
            return exe.execute();
        }
        catch (java.io.IOException e) {
            throw new BuildException(e, location);
        }
    }

    /**
     * Constant for the thing to execute
     */
    private static final String CLEARTOOL_EXE = "cleartool";

    /**
     * The 'Update' command
     */
    public static final String COMMAND_UPDATE = "update";
    /**
     * The 'Checkout' command
     */
    public static final String COMMAND_CHECKOUT = "checkout";
    /**
     * The 'Checkin' command
     */
    public static final String COMMAND_CHECKIN = "checkin";
    /**
     * The 'UndoCheckout' command
     */
    public static final String COMMAND_UNCHECKOUT = "uncheckout";

}

