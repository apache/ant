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

/**
 * Task to perform LABEL commands to Microsoft Visual Source Safe.
 * <p>
 * The following attributes are interpreted:
 * <table border="1">
 *   <tr>
 *     <th>Attribute</th>
 *     <th>Values</th>
 *     <th>Required</th>
 *   </tr>
 *   <tr>
 *      <td>login</td>
 *      <td>username,password</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>vsspath</td>
 *      <td>SourceSafe path</td>
 *      <td>Yes</td>
 *   </tr>
 *   <tr>
 *      <td>ssdir</td>
 *      <td>directory where <code>ss.exe</code> resides. By default the task
 *      expects it to be in the PATH.</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>label</td>
 *      <td>A label to apply to the hierarchy</td>
 *      <td>Yes</td>
 *   </tr>
 *   <tr>
 *      <td>version</td>
 *      <td>An existing file or project version to label</td>
 *      <td>No</td>
 *   </tr>
 * </table>
 *
 * @author Phillip Wells
 */
public class MSVSSLABEL extends MSVSS
{

    private String m_Label = null;
    private String m_Version = null;

    public static final String FLAG_LABEL = "-L";

    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute ss and then calls Exec's run method
     * to execute the command line.
     */
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();
        int result = 0;

        // first off, make sure that we've got a command and a vssdir and a label ...
        if (getVsspath() == null) {
            String msg = "vsspath attribute must be set!";
            throw new BuildException(msg, location);
        }
        if (getLabel() == null) {
            String msg = "label attribute must be set!";
            throw new BuildException(msg, location);
        }

        // now look for illegal combinations of things ...

        // build the command line from what we got the format is
        // ss Label VSS items [-C]      [-H] [-I-] [-Llabel] [-N] [-O]      [-V]      [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable(getSSCommand());
        commandLine.createArgument().setValue(COMMAND_LABEL);

        // VSS items
        commandLine.createArgument().setValue(getVsspath());

        // -C
        // Use the same comment for all labels
        // Not required

        // -I-
        commandLine.createArgument().setValue("-I-");  // ignore all errors

        // -L
        // Specify the new label on the command line (instead of being prompted)
        getLabelCommand(commandLine);

        // -V
        // Label an existing file or project version
        getVersionCommand(commandLine);

        // -Y
        getLoginCommand(commandLine);

        result = run(commandLine);
        if ( result != 0 ) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }

    }

    /**
     * Set the label to apply in SourceSafe
     * <p>
     * Note we assume that if the supplied string has the value "null" that something
     * went wrong and that the string value got populated from a null object. This
     * happens if a ant variable is used e.g. label="${label_server}" when label_server
     * has not been defined to ant!
     */
    public void setLabel(String label) {
        if ( label.equals("") || label.equals("null") ) {
            m_Label = null;
        } else {
            m_Label = label;
        }
    }

    /**
     * Builds the version command.
     * @param cmd the commandline the command is to be added to
     */
    public void getVersionCommand(Commandline cmd) {
        if ( m_Version != null) {
            cmd.createArgument().setValue(FLAG_VERSION + m_Version);
        }
    }

    /**
     * Builds the label command.
     * @param cmd the commandline the command is to be added to
     */
    public void getLabelCommand(Commandline cmd) {
        if ( m_Label != null) {
            cmd.createArgument().setValue(FLAG_LABEL + m_Label);
        }
    }

    /**
     * Set the stored version string
     * <p>
     * Note we assume that if the supplied string has the value "null" that something
     * went wrong and that the string value got populated from a null object. This
     * happens if a ant variable is used e.g. version="${ver_server}" when ver_server
     * has not been defined to ant!
     */
    public void setVersion(String version) {
        if (version.equals("") || version.equals("null") ) {
            m_Version = null;
        } else {
            m_Version = version;
        }
    }

    /**
     * Gets the label to be applied.
     * @return the label to be applied.
     */
    public String getLabel() {
        return m_Label;
    }

}
