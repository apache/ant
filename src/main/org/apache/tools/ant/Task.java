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

package org.apache.tools.ant;

import java.io.*;
import java.util.*;

/**
 * Base class for all tasks.
 *
 * @author duncan@x180.com
 */
public abstract class Task {

    protected Project project = null;
    protected Target target = null;

    /**
     * Sets the project object of this task. This method is used by
     * project when a task is added to it so that the task has
     * access to the functions of the project. It should not be used
     * for any other purpose.
     *
     * @param project Project in whose scope this task belongs.
     */
    void setProject(Project project) {
        this.project = project;
    }

    /**
     * Sets a task attribute.
     *
     * @param name the attribute name
     * @param value the attribute value
     */
    public void setAttribute(String name, Object value) {
        if (name.equals("target")) {
            this.target = (Target) value;
            this.project = this.target.getProject();
        }
    }

    /**
     * Called by the project to let the task initialize properly. Normally it does nothing.
     *
     * @throws BuildException if someting goes wrong with the build
     */
    public void init() throws BuildException {}

    /**
     * Called by the project to let the task do it's work. Normally it does nothing.
     *
     * @throws BuildException if someting goes wrong with the build
     */
    public void execute() throws BuildException {};

    /**
     * Convienence method to copy a file from a source to a destination
     *
     * @throws IOException
     */
    protected void copyFile(String sourceFile, String destFile)
        throws IOException
    {
        copyFile(new File(sourceFile), new File(destFile));
    }

    /**
     * Convienence method to copy a file from a source to a destination.
     *
     * @throws IOException
     */
    protected void copyFile(File sourceFile, File destFile) throws IOException {

        if (destFile.lastModified() < sourceFile.lastModified()) {
            project.log("Copy: " + sourceFile.getAbsolutePath() + " > "
                    + destFile.getAbsolutePath(), project.MSG_VERBOSE);

            // ensure that parent dir of dest file exists!
            // not using getParentFile method to stay 1.1 compat
            File parent = new File(destFile.getParent());
            if (!parent.exists()) {
                parent.mkdirs();
            }

            // open up streams and copy using a decent buffer
            FileInputStream in = new FileInputStream(sourceFile);
            FileOutputStream out = new FileOutputStream(destFile);

            byte[] buffer = new byte[8 * 1024];
            int count = 0;
            do {
                out.write(buffer, 0, count);
                count = in.read(buffer, 0, buffer.length);
            } while (count != -1);

            in.close();
            out.close();
        }
    }
}

