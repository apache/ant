/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * P4Fstat  - find out which files are under Perforce control and which are not.
 *
 * <br><b>Example Usage:</b><br>
 * <pre>
 * &lt;project name=&quot;p4fstat&quot; default=&quot;p4fstat&quot; basedir=&quot;C:\dev\gnu&quot;&gt;
 *     &lt;target name=&quot;p4fstat&quot; &gt;
 *         &lt;p4fstat showfilter=&quot;all&quot;&gt;
 *             &lt;fileset dir=&quot;depot&quot; includes=&quot;**\/*&quot;/&gt;
 *         &lt;/p4fstat&gt;
 *     &lt;/target&gt;
 * &lt;/project&gt;
 * </pre>
 *
 * @author <A HREF="mailto:miha@softhome.net">Miha</A>
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 * @author <A HREF="mailto:ashundi@tibco.com">Anli Shundi</A>
 *
 * @ant.task category="scm"
 */
public class P4Fstat extends P4Base {

    private int changelist;
    private String addCmd = "";
    private Vector filesets = new Vector();
    private int cmdLength = 300;
    private static final int SHOW_ALL = 0;
    private static final int SHOW_EXISTING = 1;
    private static final int SHOW_NON_EXISTING = 2;
    private int show = SHOW_NON_EXISTING;
    private FStatP4OutputHandler handler;
    private StringBuffer filelist;
    private int fileNum = 0;
    private int doneFileNum = 0;
    private boolean debug = false;

    private static final String EXISTING_HEADER
        = "Following files exist in perforce";
    private static final String NONEXISTING_HEADER
        = "Following files do not exist in perforce";


    public void setShowFilter(String filter) {
        if (filter.equalsIgnoreCase("all")) {
            show = SHOW_ALL;
        } else if (filter.equalsIgnoreCase("existing")) {
            show = SHOW_EXISTING;
        } else if (filter.equalsIgnoreCase("non-existing")) {
            show = SHOW_NON_EXISTING;
        } else {
            throw new BuildException("P4Fstat: ShowFilter should be one of: "
                + "all, existing, non-existing");
        }
    }


    public void setChangelist(int changelist) throws BuildException {
        if (changelist <= 0) {
            throw new BuildException("P4FStat: Changelist# should be a "
                + "positive number");
        }

        this.changelist = changelist;
    }

    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    public void execute() throws BuildException {

        handler = new FStatP4OutputHandler(this);
        if (P4View != null) {
            addCmd = P4View;
        }

        P4CmdOpts = (changelist > 0) ? ("-c " + changelist) : "";

        filelist = new StringBuffer();

        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            //File fromDir = fs.getDir(project);

            String[] srcFiles = ds.getIncludedFiles();
            fileNum = srcFiles.length;

            if (srcFiles != null) {
                for (int j = 0; j < srcFiles.length; j++) {
                    File f = new File(ds.getBasedir(), srcFiles[j]);
                    filelist.append(" ").append('"').append(f.getAbsolutePath()).append('"');
                    doneFileNum++;
                    if (filelist.length() > cmdLength) {

                        execP4Fstat(filelist);
                        filelist = new StringBuffer();
                    }
                }
                if (filelist.length() > 0) {
                    execP4Fstat(filelist);
                }
            } else {
                log("No files specified to add!", Project.MSG_WARN);
            }
        }

        if (show == SHOW_ALL || show == SHOW_EXISTING) {
            printRes(handler.getExisting(), EXISTING_HEADER);
        }

        if (show == SHOW_ALL || show == SHOW_NON_EXISTING) {
            printRes(handler.getNonExisting(), NONEXISTING_HEADER);
        }

    }


    public int getLengthOfTask() {
        return fileNum;
    }

    int getPasses() {
        return filesets.size();
    }


    private void printRes(ArrayList ar, String header) {
        log(header, Project.MSG_INFO);
        for (int i = 0; i < ar.size(); i++) {
            log((String) ar.get(i), Project.MSG_INFO);
        }
    }

    private void execP4Fstat(StringBuffer list) {
        String l = list.substring(0);
        if (debug) {
            log("Executing fstat " + P4CmdOpts + " " + addCmd + l + "\n",
                Project.MSG_INFO);
        }
        execP4Command("fstat " + P4CmdOpts + " " + addCmd + l, handler);
    }

}
