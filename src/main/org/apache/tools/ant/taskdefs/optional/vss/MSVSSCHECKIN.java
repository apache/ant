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

package org.apache.tools.ant.taskdefs.optional.vss;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

import java.io.File;

/**
 * Task to perform CheckIn commands to Microsoft Visual Source Safe.
 *
 * @author Martin Poeschl
 */
public class MSVSSCHECKIN extends MSVSS {

    private String m_LocalPath = null;
    private boolean m_Recursive = false;
    private boolean m_Writable = false;
    private String m_AutoResponse = null;
    private String m_Comment = "-";

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
        // ss Checkin VSS items [-H] [-C] [-I-] [-N] [-O] [-R] [-W] [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable(getSSCommand());
        commandLine.createArgument().setValue(COMMAND_CHECKIN);

        // VSS items
        commandLine.createArgument().setValue(getVsspath());
        // -GL
        getLocalpathCommand(commandLine);
        // -I- or -I-Y or -I-N
        getAutoresponse(commandLine);
        // -R
        getRecursiveCommand(commandLine);
        // -W
        getWritableCommand(commandLine);
        // -Y
        getLoginCommand(commandLine);
        // -C
        commandLine.createArgument().setValue("-C"+getComment());

        result = run(commandLine);
        if ( result != 0 ) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }
    }

    /**
     * Set the local path.
     */
    public void setLocalpath(Path localPath) {
        m_LocalPath = localPath.toString();
    }

    /**
     * Builds and returns the -GL flag command if required
     * <p>
     * The localpath is created if it didn't exist
     */
    public void getLocalpathCommand(Commandline cmd) {
        if (m_LocalPath == null) {
            return;
        } else {
            // make sure m_LocalDir exists, create it if it doesn't
            File dir = project.resolveFile(m_LocalPath);
            if (!dir.exists()) {
                boolean done = dir.mkdirs();
                if (done == false) {
                    String msg = "Directory " + m_LocalPath + " creation was not " +
                        "succesful for an unknown reason";
                    throw new BuildException(msg, location);
                }
                project.log("Created dir: " + dir.getAbsolutePath());
            }

            cmd.createArgument().setValue(FLAG_OVERRIDE_WORKING_DIR + m_LocalPath);
        }
    }

    /**
     * Set behaviour recursive or non-recursive
     */
    public void setRecursive(boolean recursive) {
        m_Recursive = recursive;
    }

    /**
     * @return the 'recursive' command if the attribute was 'true', otherwise an empty string
     */
    public void getRecursiveCommand(Commandline cmd) {
        if ( !m_Recursive ) {
            return;
        } else {
            cmd.createArgument().setValue(FLAG_RECURSION);
        }
    }

    /**
     * Set behaviour, used in get command to make files that are 'got' writable
     */
    public final void setWritable(boolean argWritable) {
        m_Writable = argWritable;
    }

    /**
     * @return the 'make writable' command if the attribute was 'true', otherwise an empty string
     */
    public void getWritableCommand(Commandline cmd) {
        if ( !m_Writable ) {
            return;
        } else {
            cmd.createArgument().setValue(FLAG_WRITABLE);
        }
    }

    public void setAutoresponse(String response){
        if ( response.equals("") || response.equals("null") ) {
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

        if ( m_AutoResponse == null) {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_DEF);
        } else if ( m_AutoResponse.equalsIgnoreCase("Y")) {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_YES);

        } else if ( m_AutoResponse.equalsIgnoreCase("N")) {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_NO);
        }else {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_DEF);
        } // end of else

    }

    /**
     * Set the comment to apply in SourceSafe
     * <p>
     * If this is null or empty, it will be replaced with "-" which
     * is what SourceSafe uses for an empty comment.
     */
    public void setComment(String comment) {
        if ( comment.equals("") || comment.equals("null") ) {
            m_Comment = "-";
        } else {
            m_Comment = comment;
        }
    }

    /**
     * Gets the comment to be applied.
     * @return the comment to be applied.
     */
    public String getComment() {
        return m_Comment;
    }
}
