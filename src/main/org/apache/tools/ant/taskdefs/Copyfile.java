/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;

/**
 * Copies a file.
 *
 * @author duncan@x180.com
 *
 * @deprecated The copyfile task is deprecated.  Use copy instead.
 */

public class Copyfile extends Task {

    private File srcFile;
    private File destFile;
    private boolean filtering = false;
    private boolean forceOverwrite = false;
 
    public void setSrc(File src) {
        srcFile = src;
    }

    public void setForceoverwrite(boolean force) {
        forceOverwrite = force;
    }

    public void setDest(File dest) {
        destFile = dest;
    }

    public void setFiltering(String filter) {
        filtering = Project.toBoolean(filter);
    }

    public void execute() throws BuildException {
        log("DEPRECATED - The copyfile task is deprecated.  Use copy instead.");

        if (srcFile == null) {
            throw new BuildException("The src attribute must be present.", location);
        }
        
        if (!srcFile.exists()) {
            throw new BuildException("src " + srcFile.toString()
                                     + " does not exist.", location);
        }

        if (destFile == null) {
            throw new BuildException("The dest attribute must be present.", location);
        }

        if (srcFile.equals(destFile)) {
            log("Warning: src == dest");
        }

        if (forceOverwrite || srcFile.lastModified() > destFile.lastModified()) {
            try {
                project.copyFile(srcFile, destFile, filtering, forceOverwrite);
            } catch (IOException ioe) {
                String msg = "Error copying file: " + srcFile.getAbsolutePath()
                    + " due to " + ioe.getMessage();
                throw new BuildException(msg);
            }
        }
    }
}
