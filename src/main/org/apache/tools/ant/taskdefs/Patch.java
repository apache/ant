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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.Commandline;
import java.io.File;
import java.io.IOException;

/**
 * Task as a layer on top of patch. Patch applies a diff file to an original.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Patch extends Task {

    private File originalFile;
    private boolean havePatchfile = false;
    private Commandline cmd = new Commandline();

    /**
     * The file to patch.
     */
    public void setOriginalfile(File file) {
        originalFile = file;
    }

    /**
     * The file containing the diff output.
     */
    public void setPatchfile(File file) {
        if (!file.exists()) {
            throw new BuildException("patchfile "+file+" doesn\'t exist", 
                                     location);
        }
        cmd.createArgument().setValue("-i");
        cmd.createArgument().setFile(file);
        havePatchfile = true;
    }

    /**
     * Shall patch write backups.
     */
    public void setBackups(boolean backups) {
        if (backups) {
            cmd.createArgument().setValue("-b");
        }
    }

    /**
     * Ignore whitespace differences.
     */
    public void setIgnorewhitespace(boolean ignore) {
        if (ignore) {
            cmd.createArgument().setValue("-l");
        }
    }

    /**
     * Strip the smallest prefix containing <i>num</i> leading slashes
     * from filenames.
     *
     * <p>patch's <i>-p</i> option.
     */
    public void setStrip(int num) throws BuildException {
        if (num < 0) {
            throw new BuildException("strip has to be >= 0", location);
        }
        cmd.createArgument().setValue("-p"+num);
    }

    /**
     * Work silently unless an error occurs.
     */
    public void setQuiet(boolean q) {
        if (q) {
            cmd.createArgument().setValue("-s");
        }
    }

    /**
     * Assume patch was created with old and new files swapped.
     */
    public void setReverse(boolean r) {
        if (r) {
            cmd.createArgument().setValue("-R");
        }
    }

    public void execute() throws BuildException {
        if (!havePatchfile) {
            throw new BuildException("patchfile argument is required", 
                                     location);
        } 
        
        Commandline toExecute = (Commandline)cmd.clone();
        toExecute.setExecutable("patch");

        if (originalFile != null) {
            toExecute.createArgument().setFile(originalFile);
        }

        Execute exe = new Execute(new LogStreamHandler(this, Project.MSG_INFO,
                                                       Project.MSG_WARN), 
                                  null);
        exe.setCommandline(toExecute.getCommandline());
        try {
            exe.execute();
        } catch (IOException e) {
            throw new BuildException(e, location);
        }
    }

}// Patch
