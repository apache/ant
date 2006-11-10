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
import java.util.Vector;

/** Submits a numbered changelist to Perforce.
 *
 * <B>Note:</B> P4Submit cannot (yet) submit the default changelist.
 * This shouldn't be a problem with the ANT task as the usual flow is
 * P4Change to create a new numbered change followed by P4Edit then P4Submit.
 *
 * Example Usage:-<br>
 * &lt;p4submit change="${p4.change}" /&gt;
 *
 * @ant.task category="scm"
 */
public class P4Submit extends P4Base {

    // CheckStyle:VisibilityModifier OFF - bc
    //ToDo: If dealing with default cl need to parse out <enter description here>
    /**
     * change list number
     */
    public String change;
    // CheckStyle:VisibilityModifier ON
    /**
     * change property
     */
    private String changeProperty;
    /**
     * needsresolveproperty
     */
    private String needsResolveProperty;
    /**
     * set the change list number to submit
     * @param change The changelist number to submit; required.
     */
    public void setChange(String change) {
        this.change = change;
    }
    /**
     * property defining the change number if the change number gets renumbered
     * @param changeProperty name of a new property to which the change number
     * will be assigned if it changes
     * @since ant 1.6.1
     */
    public void setChangeProperty(String changeProperty) {
        this.changeProperty = changeProperty;
    }
    /**
     * property defining the need to resolve the change list
     * @param needsResolveProperty a property which will be set if the change needs resolve
     * @since ant 1.6.1
     */
    public void setNeedsResolveProperty(String needsResolveProperty) {
        this.needsResolveProperty = needsResolveProperty;
    }

    /**
     * do the work
     * @throws BuildException if no change list specified
     */
    public void execute() throws BuildException {
        if (change != null) {
            execP4Command("submit -c " + change, (P4HandlerAdapter) new P4SubmitAdapter(this));
        } else {
            //here we'd parse the output from change -o into submit -i
            //in order to support default change.
            throw new BuildException("No change specified (no support for default change yet....");
        }
    }

    /**
     * internal class used to process the output of p4 submit
     */
    public class P4SubmitAdapter extends SimpleP4OutputHandler {
        /**
         * Constructor.
         * @param parent a P4Base instance.
         */
        public P4SubmitAdapter(P4Base parent) {
            super(parent);
        }
        /**
         * process a line of stdout/stderr coming from Perforce
         * @param line line of stdout or stderr coming from Perforce
         */
        public void process(String line) {
            super.process(line);
            getProject().setProperty("p4.needsresolve", "0");
            // this type of output might happen
            // Change 18 renamed change 20 and submitted.
            if (util.match("/renamed/", line)) {
                try {
                    Vector myarray = new Vector();
                    util.split(myarray, line);
                    boolean found = false;
                    for (int counter = 0; counter < myarray.size(); counter++) {
                        if (found) {
                            String chnum = (String) myarray.elementAt(counter + 1);
                            int changenumber = Integer.parseInt(chnum);
                            log("Perforce change renamed " + changenumber, Project.MSG_INFO);
                            getProject().setProperty("p4.change", "" + changenumber);
                            if (changeProperty != null) {
                                getProject().setNewProperty(changeProperty, chnum);
                            }
                            found = false;
                        }
                        if (((myarray.elementAt(counter))).equals("renamed")) {
                            found = true;
                        }
                    }
                // NumberFormatException or ArrayOutOfBondsException could happen here
                } catch (Exception e) {
                    String msg = "Failed to parse " + line  + "\n"
                            + " due to " + e.getMessage();
                    throw new BuildException(msg, e, getLocation());
                }
            }
            if (util.match("/p4 submit -c/", line)) {
                getProject().setProperty("p4.needsresolve", "1");
                if (needsResolveProperty != null) {
                    getProject().setNewProperty(needsResolveProperty, "true");
                }
            }

        }
    }

}
