/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.sos;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Labels Visual SourceSafe files via a SourceOffSite server.
 *
 * @author    Jesse Stockall
 *
 * @ant.task name="soslabel" category="scm"
 */
public class SOSLabel extends SOS {

    /**
     * The version number to label.
     *
     * @param  version  The new version value
     */
    public void setVersion(String version) {
        super.setInternalVersion(version);
    }

    /**
     * The label to apply the the files in SourceSafe.
     *
     * @param  label  The new label value
     *
     * @ant.attribute group="required"
     */
    public void setLabel(String label) {
        super.setInternalLabel(label);
    }

    /**
     * The comment to apply to all files being labelled.
     *
     * @param  comment  The new comment value
     */
    public void setComment(String comment) {
        super.setInternalComment(comment);
    }

    /**
     *  Build the command line <br>
     *  AddLabel required parameters: -server -name -password -database -project -label<br>
     *  AddLabel optional parameters: -verbose -comment<br>
     *
     * @return    Commandline the generated command to be executed
     */
    protected Commandline buildCmdLine() {
        commandLine = new Commandline();

        // add -command AddLabel to the commandline
        commandLine.createArgument().setValue(SOSCmd.FLAG_COMMAND);
        commandLine.createArgument().setValue(SOSCmd.COMMAND_LABEL);

        getRequiredAttributes();

        // a label is required
        if (getLabel() == null) {
            throw new BuildException("label attribute must be set!", getLocation());
        }
        commandLine.createArgument().setValue(SOSCmd.FLAG_LABEL);
        commandLine.createArgument().setValue(getLabel());

        // -verbose
        commandLine.createArgument().setValue(getVerbose());
        // Look for a comment
        if (getComment() != null) {
            commandLine.createArgument().setValue(SOSCmd.FLAG_COMMENT);
            commandLine.createArgument().setValue(getComment());
        }
        return commandLine;
    }
}
