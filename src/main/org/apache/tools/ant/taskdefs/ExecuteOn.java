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
import org.apache.tools.ant.types.*;

import java.util.Vector;
import java.io.File;
import java.io.IOException;

/**
 * Executes a given command, supplying a set of files as arguments. 
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @author <a href="mailto:mariusz@rakiura.org">Mariusz Nowostawski</a> 
 */
public class ExecuteOn extends ExecTask {

    protected Vector filesets = new Vector();
    private boolean parallel = false;
    protected String type = "file";
    protected Commandline.Marker srcFilePos = null;

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Shall the command work on all specified files in parallel?
     */
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * Shall the command work only on files, directories or both?
     */
    public void setType(FileDirBoth type) {
        this.type = type.getValue();
    }

    /**
     * Marker that indicates where the name of the source file should
     * be put on the command line.
     */
    public Commandline.Marker createSrcfile() {
        if (srcFilePos != null) {
            throw new BuildException(taskType + " doesn\'t support multiple srcfile elements.",
                                     location);
        }
        srcFilePos = cmdl.createMarker();
        return srcFilePos;
    }

    protected void checkConfiguration() {
        super.checkConfiguration();
        if (filesets.size() == 0) {
            throw new BuildException("no filesets specified", location);
        }
    }

    protected void runExec(Execute exe) throws BuildException {
        try {

            for (int i=0; i<filesets.size(); i++) {
                Vector v = new Vector();
                FileSet fs = (FileSet) filesets.elementAt(i);
                File base = fs.getDir(project);
                DirectoryScanner ds = fs.getDirectoryScanner(project);

                if (!"dir".equals(type)) {
                    String[] s = getFiles(base, ds);
                    for (int j=0; j<s.length; j++) {
                        v.addElement(s[j]);
                    }
                }

                if (!"file".equals(type)) {
                    String[] s = getDirs(base, ds);;
                    for (int j=0; j<s.length; j++) {
                        v.addElement(s[j]);
                    }
                }

                String[] s = new String[v.size()];
                v.copyInto(s);

                int err = -1;
                
                if (parallel) {
                    String[] command = getCommandline(s, base);
                    log("Executing " + Commandline.toString(command), 
                        Project.MSG_VERBOSE);
                    exe.setCommandline(command);
                    err = exe.execute();
                    if (err != 0) {
                        if (failOnError) {
                            throw new BuildException("Exec returned: "+err, 
                                                     location);
                        } else {
                            log("Result: " + err, Project.MSG_ERR);
                        }
                    }

                } else {
                    for (int j=0; j<s.length; j++) {
                        String[] command = getCommandline(s[j], base);
                        log("Executing " + Commandline.toString(command), 
                            Project.MSG_VERBOSE);
                        exe.setCommandline(command);
                        err = exe.execute();
                        if (err != 0) {
                            if (failOnError) {
                                throw new BuildException("Exec returned: "+err, 
                                                         location);
                            } else {
                                log("Result: " + err, Project.MSG_ERR);
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new BuildException("Execute failed: " + e, e, location);
        } finally {
            // close the output file if required
            logFlush();
        }
    }

    /**
     * Construct the command line for parallel execution.
     *
     * @param srcFiles The filenames to add to the commandline
     * @param baseDir filenames are relative to this dir
     */
    protected String[] getCommandline(String[] srcFiles, File baseDir) {
        String[] orig = cmdl.getCommandline();
        String[] result = new String[orig.length+srcFiles.length];

        int index = orig.length;
        if (srcFilePos != null) {
            index = srcFilePos.getPosition();
        }
        System.arraycopy(orig, 0, result, 0, index);

        for (int i=0; i < srcFiles.length; i++) {
            result[index+i] = (new File(baseDir, srcFiles[i])).getAbsolutePath();
        }
        
        System.arraycopy(orig, index, result, index+srcFiles.length, 
                         orig.length-index);
        return result;
    }

    /**
     * Construct the command line for serial execution.
     *
     * @param srcFile The filename to add to the commandline
     * @param baseDir filename is relative to this dir
     */
    protected String[] getCommandline(String srcFile, File baseDir) {
        return getCommandline(new String[] {srcFile}, baseDir);
    }

    /**
     * Return the list of files from this DirectoryScanner that should
     * be included on the command line.
     */
    protected String[] getFiles(File basedir, DirectoryScanner ds) {
        return ds.getIncludedFiles();
    }

    /**
     * Return the list of Directories from this DirectoryScanner that
     * should be included on the command line.
     */
    protected String[] getDirs(File basedir, DirectoryScanner ds) {
        return ds.getIncludedDirectories();
    }

    /**
     * Enumerated attribute with the values "file", "dir" and "both"
     * for the type attribute.  
     */
    public static class FileDirBoth extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"file", "dir", "both"};
        }
    }
}
