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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;

/**
 * Touch a file and/or fileset(s) -- corresponds to the Unix touch command.
 *
 * <p>If the file to touch doesn't exist, an empty one is
 * created. </p>
 *
 * <p>Note: Setting the modification time of files is not supported in
 * JDK 1.1.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @author <a href="mailto:mj@servidium.com">Michael J. Sikorsky</a>
 * @author <a href="mailto:shaw@servidium.com">Robert Shaw</a>
 */
public class Touch extends Task {

    private File file;              // required
    private long millis = -1;
    private String dateTime;
    private Vector filesets = new Vector();
    private FileUtils fileUtils;

    public Touch() {
        fileUtils = FileUtils.newFileUtils();
    }

    /**
     * Sets a single source file to touch.  If the file does not exist
     * an  empty file will be created.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Milliseconds since 01/01/1970 00:00 am.
     */
    public void setMillis(long millis) {
        this.millis = millis;
    }

    /**
     * Date in the format MM/DD/YYYY HH:MM AM_PM.
     */
    public void setDatetime(String dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Execute the touch operation.
     */
    public void execute() throws BuildException {
        if (file == null && filesets.size() == 0) {
            throw 
                new BuildException("Specify at least one source - a file or a fileset.");
        }

        if (file != null && file.exists() && file.isDirectory()) {
            throw new BuildException("Use a fileset to touch directories.");
        }

        if (dateTime != null) {
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                           DateFormat.SHORT,
                                                           Locale.US);
            try {
                setMillis(df.parse(dateTime).getTime());
                if (millis < 0) {
                    throw new BuildException("Date of " + dateTime
                                             + " results in negative milliseconds value relative to epoch (January 1, 1970, 00:00:00 GMT).");
                }
            } catch (ParseException pe) {
                throw new BuildException(pe.getMessage(), pe, location);
            }
        }

        touch();
    }

    /**
     * Does the actual work. Entry point for Untar and Expand as well.
     */
    protected void touch() throws BuildException {
        if (file != null) {
            if (!file.exists()) {
                log("Creating "+file, Project.MSG_INFO);
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(new byte[0]);
                    fos.close();
                } catch (IOException ioe) {
                    throw new BuildException("Could not create "+file, ioe, 
                                             location);
                }
            }
            touch(file);
        }

        if (millis >= 0 && project.getJavaVersion() == Project.JAVA_1_1) {
            log("modification time of files cannot be set in JDK 1.1",
                Project.MSG_WARN);
            return;
        }

        // deal with the filesets
        for (int i=0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            File fromDir = fs.getDir(project);

            String[] srcFiles = ds.getIncludedFiles();
            String[] srcDirs = ds.getIncludedDirectories();

            for(int j=0; j < srcFiles.length ; j++) {
                touch(new File(fromDir, srcFiles[j]));
            }
         
            for(int j=0; j < srcDirs.length ; j++) {
                touch(new File(fromDir, srcDirs[j]));
            }
        }
    }

    protected void touch(File file) throws BuildException {
        if (!file.canWrite()) {
            throw new BuildException("Can not change modification date of read-only file " + file);
        }

        if (project.getJavaVersion() == Project.JAVA_1_1) {
            return;
        }

        if (millis < 0) {
            fileUtils.setFileLastModified(file, System.currentTimeMillis());
        } else {
            fileUtils.setFileLastModified(file, millis);
        }
    }

}
