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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
import java.io.File;

/**
 * Task as a layer on top of patch. Patch applies a diff file to an original.
 *
 * @author Stefan Bodewig <a href="mailto:stefan.bodewig@megabit.net">stefan.bodewig@megabit.net</a>
 */
public class Patch extends Exec {

    private File originalFile;
    private File patchFile;
    private boolean backup = false;
    private boolean ignoreWhitespace = false;
    private int strip = -1;
    private boolean quiet = false;
    private boolean reverse = false;

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
        patchFile = file;
    }

    /**
     * Shall patch write backups.
     */
    public void setBackups(boolean backups) {
        backup = backups;
    }

    /**
     * Ignore whitespace differences.
     */
    public void setIgnorewhitespace(boolean ignore) {
        ignoreWhitespace = ignore;
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
        strip = num;
    }

    /**
     * Work silently unless an error occurs.
     */
    public void setQuiet(boolean q) {
        quiet = q;
    }

    /**
     * Assume patch was created with old and new files swapped.
     */
    public void setReverse(boolean r) {
        reverse = r;
    }

    public final void setCommand(String command) throws BuildException {
        throw new BuildException("Cannot set attribute command in patch task",
                                 location);
    }

    public void execute() throws BuildException {
        if (patchFile == null) {
            throw new BuildException("patchfile argument is required", 
                                     location);
        } 
        if (!patchFile.exists()) {
            throw new BuildException("patchfile "+patchFile+" doesn\'t exist", 
                                     location);
        }
        
        StringBuffer command = new StringBuffer("patch -i "+patchFile+" ");

        if (backup) {
            command.append("-b ");
        }
        
        if (ignoreWhitespace) {
            command.append("-l ");
        }
        
        if (strip >= 0) {
            command.append("-p"+strip+" ");
        }
        
        if (quiet) {
            command.append("-s ");
        }
        
        if (reverse) {
            command.append("-R ");
        }
        
        if (originalFile != null) {
            command.append(originalFile);
        } 

        run(command.toString());
    }

}// Patch
