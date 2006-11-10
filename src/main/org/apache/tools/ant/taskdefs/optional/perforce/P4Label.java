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

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.StringUtils;

/**
 *  Creates a new Perforce label and set contents to reflect current
 *  client file revisions.
 *
 *  Label name defaults to AntLabel if none set.
 *
 * Example Usage:
 * <pre>
 *   &lt;P4Label name="MyLabel-${TSTAMP}-${DSTAMP}" desc="Auto Build Label" /&gt;
 * </pre>
 *
 * @ant.task category="scm"
 */
public class P4Label extends P4Base {

    // CheckStyle:VisibilityModifier OFF - bc
    protected String name;
    protected String desc;
    protected String lock;
    // CheckStyle:VisibilityModifier ON

    /**
     * The name of the label; optional, default "AntLabel"
     * @param name the name of the label
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *Label Description; optional
     * @param desc description of the label
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * when set to "locked", Perforce will lock the label once created; optional.
     * @param lock only admissible value "locked"
     */
    public void setLock(String lock) {
        this.lock = lock;
    }

    /**
     * do the work
     * @throws BuildException if failonerror has been set to true and Perforce fails
     */
    public void execute() throws BuildException {
        log("P4Label exec:", Project.MSG_INFO);

        if (P4View == null || P4View.length() < 1) {
            log("View not set, assuming //depot/...", Project.MSG_WARN);
            P4View = "//depot/...";
        } else {
            P4View = StringUtils.replace(P4View, ":", "\n\t");
            P4View = StringUtils.replace(P4View, ";", "\n\t");
        }

        if (desc == null || desc.length() < 1) {
            log("Label Description not set, assuming 'AntLabel'",
                Project.MSG_WARN);
            desc = "AntLabel";
        }

        if (lock != null && !lock.equalsIgnoreCase("locked")) {
            log("lock attribute invalid - ignoring", Project.MSG_WARN);
        }

        if (name == null || name.length() < 1) {
            SimpleDateFormat formatter
                = new SimpleDateFormat("yyyy.MM.dd-hh:mm");
            Date now = new Date();
            name = "AntLabel-" + formatter.format(now);
            log("name not set, assuming '" + name + "'", Project.MSG_WARN);
        }


        //We have to create a unlocked label first
        String newLabel =
                "Label: " + name
                + "\nDescription: " + desc
                + "\nOptions: unlocked"
                + "\nView: \n\t" + P4View;

        P4Handler handler = new P4HandlerAdapter() {
            public void process(String line) {
                log(line, Project.MSG_VERBOSE);
            }
        };

        handler.setOutput(newLabel);

        execP4Command("label -i", handler);

        execP4Command("labelsync -l " + name, new P4HandlerAdapter() {
            public void process(String line) {
                log(line, Project.MSG_VERBOSE);
            }
        });


        log("Created Label " + name + " (" + desc + ") with view:\n" + P4View,
            Project.MSG_INFO);

        //Now lock if required
        if (lock != null && lock.equalsIgnoreCase("locked")) {

            log("Modifying lock status to 'locked'", Project.MSG_INFO);

            final StringBuffer labelSpec = new StringBuffer();

            //Read back the label spec from perforce,
            //Replace Options
            //Submit back to Perforce

            handler = new P4HandlerAdapter() {
                public void process(String line) {
                    log(line, Project.MSG_VERBOSE);

                    if (util.match("/^Options:/", line)) {
                        line = "Options: " + lock;
                    }

                    labelSpec.append(line + "\n");
                }
            };


            execP4Command("label -o " + name, handler);
            log(labelSpec.toString(), Project.MSG_DEBUG);

            log("Now locking label...", Project.MSG_VERBOSE);
            handler = new P4HandlerAdapter() {
                public void process(String line) {
                    log(line, Project.MSG_VERBOSE);
                }
            };

            handler.setOutput(labelSpec.toString());
            execP4Command("label -i", handler);
        }
    }
}
