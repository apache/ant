/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.StringUtils;

/**
 *  Syncs an existing Perforce label against the Perforce client
 *  or against a set of files/revisions
 *
 *
 * Example Usage:
 * <pre>
 *   &lt;p4labelsync name="MyLabel-${TSTAMP}-${DSTAMP}" view="//depot/...#head;//depot2/file1#25" /&gt;
 * </pre>
 *
 * @author <A HREF="mailto:levylambert@tiscali-dsl.de">Antoine Levy-Lambert</A>
 *
 * @ant.task category="scm"
 */
public class P4Labelsync extends P4Base {

    protected String name;
    private boolean add; /* -a */
    private boolean delete; /* -n */
    private boolean simulationmode;  /* -n */
    /**
     * -a flag of p4 labelsync - preserve files which exist in the label, but not in the current view
     * @return  add attribute
     * if set to true the task will not remove any files from the label
     * only add files which were not there previously or update these where the revision has changed
     * the add attribute is the -a flag of p4 labelsync
     */
    public boolean isAdd() {
        return add;
    }
    /**
     * -a flag of p4 labelsync - preserve files which exist in the label, but not in the current view
     * @param add  if set to true the task will not remove any files from the label
     * only add files which were not there previously or update these where the revision has changed
     * the add attribute is the -a flag of p4 labelsync
     */
    public void setAdd(boolean add) {
        this.add = add;
    }
    /**
     * -d flag of p4 labelsync; indicates an intention of deleting from the label the files specified in the view
     * @return  delete attribute
     */
    public boolean isDelete() {
        return delete;
    }

    /**
     * -d flag of p4 labelsync; indicates an intention of deleting from the label the files specified in the view
     * @param delete
     */
    public void setDelete(boolean delete) {
        this.delete = delete;
    }


    /**
     * The name of the label; optional, default "AntLabel"
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * -n flag of p4 labelsync - display changes without actually doing them
     * @return -n flag of p4 labelsync
     */
    public boolean isSimulationmode() {
        return simulationmode;
    }
    /**
     * -n flag of p4 labelsync - display changes without actually doing them
     * @param simulationmode
     */
    public void setSimulationmode(boolean simulationmode) {
        this.simulationmode = simulationmode;
    }


    /**
     *  do the work
     */
    public void execute() throws BuildException {
        log("P4Labelsync exec:", Project.MSG_INFO);

        if (P4View != null && P4View.length() >= 1) {
            P4View = StringUtils.replace(P4View, ":", "\n\t");
            P4View = StringUtils.replace(P4View, ";", "\n\t");
        }
        if (P4View == null) {
            P4View="";
        }

        if (name == null || name.length() < 1) {
            throw new BuildException("name attribute is compulsory for labelsync");
        }

        if ( this.isSimulationmode() ) {
            P4CmdOpts = P4CmdOpts + " -n";
        }
        if ( this.isDelete() ) {
            P4CmdOpts = P4CmdOpts + " -d";
        }
        if ( this.isAdd() ) {
            P4CmdOpts = P4CmdOpts + " -a";
        }

        execP4Command("-s labelsync -l "+name +" "+ P4CmdOpts + " " + P4View, new SimpleP4OutputHandler(this));


    }
}

