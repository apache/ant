/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
 * Performs Label commands to Microsoft Visual SourceSafe.
 *
 * @author Phillip Wells
 * @author Jesse Stockall
 *
 * @ant.task name="vsslabel" category="scm"
 */
public class MSVSSLABEL extends MSVSS {

    /**
     * Builds a command line to execute ss.
     * @return     The constructed commandline.
     */
    Commandline buildCmdLine() {
        Commandline commandLine = new Commandline();

        // first off, make sure that we've got a command and a vssdir and a label ...
        if (getVsspath() == null) {
            throw new BuildException("vsspath attribute must be set!", getLocation());
        }

        String label = getLabel();
        if (label.equals("")) {
            String msg = "label attribute must be set!";
            throw new BuildException(msg, getLocation());
        }

        // build the command line from what we got the format is
        // ss Label VSS items [-C] [-H] [-I-] [-Llabel] [-N] [-O] [-V] [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable(getSSCommand());
        commandLine.createArgument().setValue(COMMAND_LABEL);

        // VSS items
        commandLine.createArgument().setValue(getVsspath());
        // -C
        commandLine.createArgument().setValue(getComment());
        // -I- or -I-Y or -I-N
        commandLine.createArgument().setValue(getAutoresponse());
        // -L Specify the new label on the command line (instead of being prompted)
        commandLine.createArgument().setValue(label);
        // -V Label an existing file or project version
        commandLine.createArgument().setValue(getVersion());
        // -Y
        commandLine.createArgument().setValue(getLogin());

        return commandLine;
    }

    /**
     * Label to apply in SourceSafe.
     *
     * @param  label The label to apply.
     *
     * @ant.attribute group="required"
     */
    public void setLabel(String label) {
        super.setInternalLabel(label);
    }

    /**
     * Version to label.
     *
     * @param  version The version to label.
     */
    public void setVersion(String version) {
        super.setInternalVersion(version);
    }

    /**
     * Comment to apply to files labeled in SourceSafe.
     *
     * @param comment The comment to apply in SourceSafe
     */
    public void setComment(String comment) {
        super.setInternalComment(comment);
    }

    /**
     * Autoresponce behaviour. Valid options are Y and N.
     *
     * @param response The auto response value.
     */
    public void setAutoresponse(String response) {
        super.setInternalAutoResponse(response);
    }
}
