/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.vss;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Performs CP (Change Project) commands to Microsoft Visual SourceSafe.
 * <p>This task is typically used before a VssAdd in order to set the target project</p>
 * Based on the VSS Checkin code by Martin Poeschl
 *
 * @author Nigel Magnay
 *
 * @ant.task name="vsscp" category="scm"
 */
public class MSVSSCP extends MSVSS {

    private String m_AutoResponse = null;

    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute ss and then calls Exec's run method
     * to execute the command line.
     */
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();
        int result = 0;

        // first off, make sure that we've got a command and a vssdir ...
        if (getVsspath() == null) {
            String msg = "vsspath attribute must be set!";
            throw new BuildException(msg, location);
        }

        // now look for illegal combinations of things ...

        // build the command line from what we got the format is
        // ss CP VSS items [-H] [-I-] [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable(getSSCommand());
        commandLine.createArgument().setValue(COMMAND_CP);

        // VSS items
        commandLine.createArgument().setValue(getVsspath());      
        // -I- or -I-Y or -I-N
        getAutoresponse(commandLine);        
        // -Y
        getLoginCommand(commandLine);
        
        result = run(commandLine);
        if (result != 0) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }
    }

    /**
     * What to respond with (sets the -I option). By default, -I- is
     * used; values of Y or N will be appended to this.
     */     
    public void setAutoresponse(String response) {
        if (response.equals("") || response.equals("null")) {
            m_AutoResponse = null;
        } else {
            m_AutoResponse = response;
        }
    }

    /**
     * Checks the value set for the autoResponse.
     * if it equals "Y" then we return -I-Y
     * if it equals "N" then we return -I-N
     * otherwise we return -I
     */
    public void getAutoresponse(Commandline cmd) {

        if (m_AutoResponse == null) {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_DEF);
        } else if (m_AutoResponse.equalsIgnoreCase("Y")) {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_YES);

        } else if (m_AutoResponse.equalsIgnoreCase("N")) {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_NO);
        } else {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_DEF);
        } // end of else

    }
}
