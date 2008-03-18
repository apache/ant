/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Requests a new changelist from the Perforce server.
 * P4Change creates a new changelist in perforce. P4Change sets the property
 * ${p4.change} with the new changelist number. This should then be passed into
 * p4edit and p4submit.
 *
 *
 * @see P4Edit
 * @see P4Submit
 *
 * @ant.task category="scm"
 */
public class P4Change extends P4Base {
    // CheckStyle:VisibilityModifier OFF - bc

    protected String emptyChangeList = null;
    protected String description = "AutoSubmit By Ant";
    // CheckStyle:VisibilityModifier ON

    /**
     * creates a new Perforce change list
     * sets the p4.change property to the number of the new change list
     * @throws BuildException if the word error appears in the output coming from Perforce
     */
    public void execute() throws BuildException {

        if (emptyChangeList == null) {
            emptyChangeList = getEmptyChangeList();
        }
        final Project myProj = getProject();

        P4Handler handler = new P4HandlerAdapter() {
            public void process(String line) {
                if (util.match("/Change/", line)) {

                    //Remove any non-numerical chars - should leave the change number
                    line = util.substitute("s/[^0-9]//g", line);

                    int changenumber = Integer.parseInt(line);
                    log("Change Number is " + changenumber, Project.MSG_INFO);
                    myProj.setProperty("p4.change", "" + changenumber);

                } else if (util.match("/error/", line)) {
                    throw new BuildException("Perforce Error, check client settings and/or server");
                }

            }
        };

        handler.setOutput(emptyChangeList);

        execP4Command("change -i", handler);
    }

    /**
     * returns the text of an empty change list
     * @return  the text of an empty change list
     * @throws BuildException  if the text error is displayed
     * in the Perforce output outside of a comment line
     */
    public String getEmptyChangeList() throws BuildException {
        final StringBuffer stringbuf = new StringBuffer();

        execP4Command("change -o", new P4HandlerAdapter() {
            public void process(String line) {
                if (!util.match("/^#/", line)) {
                    if (util.match("/error/", line)) {
                        log("Client Error", Project.MSG_VERBOSE);
                        throw new BuildException("Perforce Error, "
                        + "check client settings and/or server");
                    } else if (util.match("/<enter description here>/", line)) {
                        // we need to escape the description in case there are /
                        description = backslash(description);
                        line = util.substitute("s/<enter description here>/"
                            + description + "/", line);
                    } else if (util.match("/\\/\\//", line)) {
                        //Match "//" for begining of depot filespec
                        return;
                    }
                    stringbuf.append(line);
                    stringbuf.append("\n");
                }
            }
        });
        return stringbuf.toString();
    }

    /**
     * Ensure that a string is backslashing slashes so that  it does not
     * confuse them with Perl substitution delimiter in Oro. Backslashes are
     * always backslashes in a string unless they escape the delimiter.
     * @param value the string to backslash for slashes
     * @return the backslashed string
     * @see <a href="http://jakarta.apache.org/oro/api/org/apache/oro/text/perl/Perl5Util.html
     * #substitute(java.lang.String,%20java.lang.String)">Oro</a>
     */
    public static final String backslash(String value) {
        final StringBuffer buf = new StringBuffer(value.length());
        final int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c == '/') {
                buf.append('\\');
            }
            buf.append(c);
        }
        return buf.toString();
    }

    /**
     * Description for ChangeList;optional.
     * If none is specified, it will default to "AutoSubmit By Ant"
     * @param desc description for the change list
     */
    public void setDescription(String desc) {
        this.description = desc;
    }

} //EoF
