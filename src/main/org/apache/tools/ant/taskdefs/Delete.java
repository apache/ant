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
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if
 * any, must include the following acknowlegement:
 * "This product includes software developed by the
 * Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself,
 * if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 * Foundation" must not be used to endorse or promote products derived
 * from this software without prior written permission. For written
 * permission, please contact apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache"
 * nor may "Apache" appear in their names without prior written
 * permission of the Apache Group.
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
import java.io.*;

/**
 * Deletes a single file or a set of files defined by a pattern.
 * 
 * @author stefano@apache.org
 * @author Tom Dimock <a href="mailto:tad1@cornell.edu">tad1@cornell.edu</a>
 */
public class Delete extends MatchingTask {
    private File delDir = null;
    private int verbosity = Project.MSG_VERBOSE;
    private File f = null;

    /**
     * Set the name of a single file to be removed.
     * 
     * @param file the file to be deleted
     */
    public void setFile(String file) {
        f = project.resolveFile(file);
    } 

    /**
     * Set the directory from which files are to be deleted
     * 
     * @param dir the directory path.
     */
    public void setDir(String dir) {
        delDir = project.resolveFile(dir);
    } 

    /**
     * Used to force listing of all names of deleted files.
     * 
     * @param verbose "true" or "on"
     */
    public void setVerbose(String verbose) {
        if ("true".equalsIgnoreCase(verbose.trim()) || "on".equalsIgnoreCase(verbose.trim())) {
            this.verbosity = Project.MSG_INFO;
        } else {
            this.verbosity = Project.MSG_VERBOSE;
        } 
    } 

    /**
     * Make it so.  Delete the file(s).
     * 
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (f == null && delDir == null) {
            throw new BuildException("<file> or <dir> attribute must be set!");
        } 

        // old <delete> functionality must still work
        if (f != null) {
            if (f.exists()) {
                if (f.isDirectory()) {
                    log("Directory: " + f.getAbsolutePath() + " cannot be removed with delete.  Use Deltree instead.");
                } else {
                    log("Deleting: " + f.getAbsolutePath());

                    if (!f.delete()) {
                        throw new BuildException("Unable to delete file " + f.getAbsolutePath());
                    } 
                } 
            } 
        } 

        // now we'll do the fancy pattern-driven deletes
        if (delDir == null) {
            return;
        } 

        if (!delDir.exists()) {
            throw new BuildException("dir does not exist!");
        } 

        DirectoryScanner ds = super.getDirectoryScanner(delDir);
        String[] files = ds.getIncludedFiles();

        if (files.length > 0) {
            log("Deleting " + files.length + " files from " + delDir.getAbsolutePath());

            for (int i = 0; i < files.length; i++) {
                File f = new File(delDir, files[i]);

                if (f.exists()) {
                    log("Deleting: " + f.getAbsolutePath(), verbosity);

                    if (!f.delete()) {
                        throw new BuildException("Unable to delete " + f.getAbsolutePath());
                    } 
                } 
            } 
        } 
    } 

}

