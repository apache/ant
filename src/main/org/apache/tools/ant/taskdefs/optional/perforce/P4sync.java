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

package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Exec;
import org.apache.tools.ant.Task;

/**
 * A Perforce checkout utility.
 * Implements the ability to write:
 * <pre>
 *  &gt;p4sync dir="src"
 *      force="no"
 *      date="2000/04/01"
 *      time="14:15:29"
 *      label="labelname"
 *      revision="#have"
 *      viewonly="true"
 *   />
 * </pre>
 * Where:
 *  1. "dir" is mandatory, the rest are optional;
 *  2. If you specify time, you have to specify date.
 *  2a. If you specify date but not time, midnight is implied.
 *  3. You cannot specify more than one of (date, label, revision).
 *  4. Any "sync" error is considered a fatal error, and a BuildException is returned.
 */
public class P4sync extends Exec {
    private String p4user;
    private String p4port;
    private String directory;
    private boolean force;
    private String date;
    private String label;
    private String revision;
    private String changenum;
    private String error = "";

    // The method executing the task
    public void execute() throws BuildException {
        StringBuffer cmdline = new StringBuffer();
        String RevisionString = "";
        int nRevisions = 0;

        cmdline.append("p4");
        cmdline.append(" " + "-s"); // 'p4 -s' gives a tag for parsing each line

        if (p4user != null) {
            cmdline.append( " " + "-u");
            cmdline.append( " " + p4user);
        }

        if (p4port != null) {
            cmdline.append( " " + "-p");
            cmdline.append( " " + p4port);
        }
        cmdline.append( " " + "sync");

        if (force) {
            cmdline.append( " " + "-f");
        }

        if (directory == null) {
            throw new BuildException("dir= not specified for 'p4sync'");
        }

        if (label != null) {
            String prefix = "";
            if (!label.startsWith("@")) {
               prefix = "@";
            }
            RevisionString = prefix + label;
            nRevisions = nRevisions + 1;
        }

        if (changenum != null) {
            String prefix = "";
            if (!changenum.startsWith("@")) {
                prefix = "@";
            }
            RevisionString = prefix + changenum;
            nRevisions = nRevisions + 1;
        }

        if (date != null) {
            String prefix = "";
            if (!date.startsWith("@")) {
                prefix = "@";
            }
            RevisionString = prefix + date;
            nRevisions = nRevisions + 1;
        }

        if (revision != null) {
            if (revision.startsWith("#")) {
                RevisionString = revision;
            } else {
                RevisionString = "#" + revision;
            }
            nRevisions = nRevisions + 1;
        }

        if (nRevisions > 1) {
            throw new BuildException("date/revision/label/changenumber are mutually exclusive - specify only one.");
        }

        cmdline.append(" " + directory + RevisionString);

        String command = cmdline.toString();
        System.out.println("executing: "+command);

        run(command);

        if (error.length() != 0) {
            throw new BuildException(error);
        }
    }

    protected void outputLog(String line, int messageLevel) {
        if (line.startsWith("error: ") && !line.endsWith("file(s) up-to-date.")) {
            error += line.substring(7);
        }

        super.outputLog(line, messageLevel);
    };

    // The setter for the attributes
    public void setForce(boolean force) { this.force = force; }
    public void setLabel(String label) { this.label = label; }
    public void setDate(String date) { this.date = date; }
    public void setLocalpath(String directory) { this.directory = directory; }
    public void setVersion(String revision) { this.revision = revision; }

    public void setRevision(String revision) { this.revision = revision; }
    public void setChange(String changenum) { this.changenum = changenum; }

    public void setP4user(String p4user) { this.p4user = p4user; }
    public void setUser(String p4user) { this.p4user = p4user; }
    public void setPort(String p4port) { this.p4port = p4port; }
    public void setP4port(String p4port) { this.p4port = p4port; }
}
