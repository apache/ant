/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
/* 
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.tools.ant.taskdefs.optional.perforce;


import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/** Adds specified files to Perforce.
 *
 * <b>Example Usage:</b>
 * <table border="1">
 * <th>Function</th><th>Command</th>
 * <tr><td>Add files using P4USER, P4PORT and P4CLIENT settings specified</td><td>&lt;P4add <br>P4view="//projects/foo/main/source/..." <br>P4User="fbloggs" <br>P4Port="km01:1666" <br>P4Client="fbloggsclient"&gt;<br>&lt;fileset basedir="dir" includes="**&#47;*.java"&gt;<br>&lt;/p4add&gt;</td></tr>
 * <tr><td>Add files using P4USER, P4PORT and P4CLIENT settings defined in environment</td><td>&lt;P4add P4view="//projects/foo/main/source/..." /&gt;<br>&lt;fileset basedir="dir" includes="**&#47;*.java"&gt;<br>&lt;/p4add&gt;</td></tr>
 * <tr><td>Specify the length of command line arguments to pass to each invocation of p4</td><td>&lt;p4add Commandlength="450"&gt;</td></tr>
 * </table>
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 * @author <A HREF="mailto:ashundi@tibco.com">Anli Shundi</A>
 */
public class P4Add extends P4Base {

    private int changelist;
    private String addCmd = "";
    private Vector filesets = new Vector();
    private int cmdLength = 450;

    /**
     *   positive integer specifying the maximum length
     *   of the commandline when calling Perforce to add the files. 
     *   Defaults to 450, higher values mean faster execution,
     *   but also possible failures.
     */
     
    public void setCommandlength(int len) throws BuildException {
        if (len <= 0) {
            throw new BuildException("P4Add: Commandlength should be a positive number");
        }
        this.cmdLength = len;
    }

    /**
     * If specified the open files are associated with the
     * specified pending changelist number; otherwise the open files are
     * associated with the default changelist.
     */
    public void setChangelist(int changelist) throws BuildException {
        if (changelist <= 0) {
            throw new BuildException("P4Add: Changelist# should be a positive number");
        }

        this.changelist = changelist;
    }

    /**
     * files to add
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    public void execute() throws BuildException {

        if (P4View != null) {
            addCmd = P4View;
        }

        P4CmdOpts = (changelist > 0) ? ("-c " + changelist) : "";

        StringBuffer filelist = new StringBuffer();

        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            //File fromDir = fs.getDir(project);

            String[] srcFiles = ds.getIncludedFiles();
            if (srcFiles != null) {
                for (int j = 0; j < srcFiles.length; j++) {
                    File f = new File(ds.getBasedir(), srcFiles[j]);
                    filelist.append(" ").append('"').append(f.getAbsolutePath()).append('"');
                    if (filelist.length() > cmdLength) {
                        execP4Add(filelist);
                        filelist.setLength(0);
                    }
                }
                if (filelist.length() > 0) {
                    execP4Add(filelist);
                }
            } else {
                log("No files specified to add!", Project.MSG_WARN);
            }
        }

    }

    private void execP4Add(StringBuffer list) {
        log("Execing add " + P4CmdOpts + " " + addCmd + list, Project.MSG_INFO);

        execP4Command("-s add " + P4CmdOpts + " " + addCmd + list, new SimpleP4OutputHandler(this));
    }
}
