/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000,2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;

/**
 * Copies a directory.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 *
 * @since Ant 1.1
 *
 * @deprecated The copydir task is deprecated since Ant 1.2.  Use copy instead.
 */

public class Copydir extends MatchingTask {

    private File srcDir;
    private File destDir;
    private boolean filtering = false;
    private boolean flatten = false;
    private boolean forceOverwrite = false;
    private Hashtable filecopyList = new Hashtable();

    public void setSrc(File src) {
        srcDir = src;
    }

    public void setDest(File dest) {
        destDir = dest;
    }

    public void setFiltering(boolean filter) {
        filtering = filter;
    }

    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
    }

    public void setForceoverwrite(boolean force) {
        forceOverwrite = force;
    }

    public void execute() throws BuildException {
        log("DEPRECATED - The copydir task is deprecated.  Use copy instead.");

        if (srcDir == null) {
            throw new BuildException("src attribute must be set!", 
                                     location);
        }

        if (!srcDir.exists()) {
            throw new BuildException("srcdir " + srcDir.toString()
                                     + " does not exist!", location);
        }

        if (destDir == null) {
            throw new BuildException("The dest attribute must be set.", 
                                     location);
        }

        if (srcDir.equals(destDir)) {
            log("Warning: src == dest", Project.MSG_WARN);
        }

        DirectoryScanner ds = super.getDirectoryScanner(srcDir);

        try {
            String[] files = ds.getIncludedFiles();
            scanDir(srcDir, destDir, files);
            if (filecopyList.size() > 0) {
                log("Copying " + filecopyList.size() + " file"
                    + (filecopyList.size() == 1 ? "" : "s")
                    + " to " + destDir.getAbsolutePath());
                Enumeration enum = filecopyList.keys();
                while (enum.hasMoreElements()) {
                    String fromFile = (String) enum.nextElement();
                    String toFile = (String) filecopyList.get(fromFile);
                    try {
                        project.copyFile(fromFile, toFile, filtering, 
                                         forceOverwrite);
                    } catch (IOException ioe) {
                        String msg = "Failed to copy " + fromFile + " to " 
                            + toFile + " due to " + ioe.getMessage();
                        throw new BuildException(msg, ioe, location);
                    }
                }
            }
        } finally {
            filecopyList.clear();
        }
    }

    private void scanDir(File from, File to, String[] files) {
        for (int i = 0; i < files.length; i++) {
            String filename = files[i];
            File srcFile = new File(from, filename);
            File destFile;
            if (flatten) {
                destFile = new File(to, new File(filename).getName());
            } else {
                destFile = new File(to, filename);
            }
            if (forceOverwrite ||
                (srcFile.lastModified() > destFile.lastModified())) {
                filecopyList.put(srcFile.getAbsolutePath(),
                                 destFile.getAbsolutePath());
            }
        }
    }
}
