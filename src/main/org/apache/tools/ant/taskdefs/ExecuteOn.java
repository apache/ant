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
import org.apache.tools.ant.types.*;

import java.util.Vector;
import java.io.File;
import java.io.IOException;

/**
 * Executes a given command, supplying a set of files as arguments. 
 *
 * @author <a href="mailto:stefan.bodewig@megabit.net">Stefan Bodewig</a> 
 * @author <a href="mailto:mariusz@rakiura.org">Mariusz Nowostawski</a> 
 */
public class ExecuteOn extends ExecTask {

    protected Vector filesets = new Vector();
    private boolean parallel = false;

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Adds a reference to a set of files (nested filesetref element).
     */
    public void addFilesetref(Reference ref) {
        filesets.addElement(ref);
    }

    /**
     * Shall the command work on all specified files in parallel?
     */
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    protected void checkConfiguration() {
        super.checkConfiguration();
        if (filesets.size() == 0) {
            throw new BuildException("no filesets specified", location);
        }
    }

    protected void runExec(Execute exe) throws BuildException {
        try {

            Vector v = new Vector();
            for (int i=0; i<filesets.size(); i++) {

                Object o = filesets.elementAt(i);
                FileSet fs = null;
                if (o instanceof FileSet) {
                    fs = (FileSet) o;
                } else {
                    Reference r = (Reference) o;
                    o = r.getReferencedObject(project);
                    if (o instanceof FileSet) {
                        fs = (FileSet) o;
                    } else {
                        String msg = r.getRefId()+" doesn\'t denote a fileset";
                        throw new BuildException(msg, location);
                    }
                }
                
                DirectoryScanner ds = fs.getDirectoryScanner(project);
                String[] s = ds.getIncludedFiles();
                for (int j=0; j<s.length; j++) {
                    v.addElement(new File(fs.getDir(), s[j]).getAbsolutePath());
                }
            }

            String[] s = new String[v.size()];
            v.copyInto(s);

            int err = -1;
            String myos = System.getProperty("os.name");

            // antRun.bat currently limits us to directory + executable 
            //                                             + 7 args
            if (parallel && 
                (myos.toLowerCase().indexOf("windows") < 0 || s.length+cmdl.size() <= 8)
                ) {
                cmdl.addLine(s);
                exe.setCommandline(cmdl.getCommandline());
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
                String[] cmd = new String[cmdl.size()+1];
                System.arraycopy(cmdl.getCommandline(), 0, cmd, 0, cmdl.size());
                for (int i=0; i<s.length; i++) {
                    cmd[cmdl.size()] = s[i];
                    exe.setCommandline(cmd);
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

        } catch (IOException e) {
            throw new BuildException("Execute failed: " + e, e, location);
        } finally {
            // close the output file if required
            logFlush();
        }
    }

}
