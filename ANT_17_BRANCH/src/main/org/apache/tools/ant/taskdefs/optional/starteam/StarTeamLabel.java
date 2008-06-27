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
package org.apache.tools.ant.taskdefs.optional.starteam;

import com.starbase.starteam.Label;
import com.starbase.starteam.View;
import com.starbase.util.OLEDate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.tools.ant.BuildException;

/**
 * Creates a view label in StarTeam at the specified view.
 *
 * Ant Usage:
 * <pre>
 * &lt;taskdef name="stlabel"
 *          classname="org.apache.tools.ant.taskdefs.optional.starteam.StarTeamLabel"/&lt;
 *     &lt;stlabel
 * label="1.0" lastbuild="20011514100000" description="Successful Build"
 * username="BuildMaster" password="ant"
 * starteamurl="server:port/project/view"/&gt;
 * </pre>
 *
  * @see <a href="http://www.borland.com/us/products/starteam/index.html"
  * >borland StarTeam Web Site</a>
 *
 * @ant.task name="stlabel" category="scm"
 */
public class StarTeamLabel extends StarTeamTask {

    /**
     * The name of the label to be set in Starteam.
     */
    private String labelName;

    /**
     * The label description to be set in Starteam.
     */
    private String description;

    /**
     * If true, this will be a build label.  If false, it will be a non-build
     * label.  The default is false.  Has no effect if revision label is
     * true.
     */
    private boolean buildlabel = false;

    /**
     * If true, this will be a revision label.  If false, it will be a build
     * label.  The default is false.
     */
    private boolean revisionlabel = false;

    /**
     * The time of the last successful. The new label will be a snapshot of the
     * repository at this time. String should be formatted as "yyyyMMddHHmmss"
     */
    private OLEDate lastBuild = null;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyyMMddHHmmss");


    /**
     * The name to be given to the label; required.
     * @param label the name to be used
     */
    public void setLabel(String label) {
        this.labelName = label;
    }

    /**
     * Description of the label to be stored in the StarTeam project.
     * @param description the description to be used
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * set the type of label based on the supplied value - if true, this
     * label will be a revision label, if false, a build label.
     *
     * @param buildlabel If true this will be a revision label; if false,
     * a build label
     */
    public void setBuildLabel(boolean buildlabel) {
        this.buildlabel = buildlabel;
    }

    /**
     * set the type of label based on the supplied value - if true, this
     * label will be a revision label, if false, a build label.
     *
     * @param revisionlabel If true this will be a revision label; if false,
     * a build label
     */
    public void setRevisionLabel(boolean revisionlabel) {
        this.revisionlabel = revisionlabel;
    }



    /**
     * The timestamp of the build that will be stored with the label; required.
     * Must be formatted <code>yyyyMMddHHmmss</code>
     * @param lastbuild the timestamp of the last build
     * @throws BuildException on error
     */
    public void setLastBuild(String lastbuild) throws BuildException {
        try {
            Date lastBuildTime = DATE_FORMAT.parse(lastbuild);
            this.lastBuild = new OLEDate(lastBuildTime);
        } catch (ParseException e) {
            throw new BuildException("Unable to parse the date '"
                + lastbuild + "'", e);
        }
    }

    /**
     * This method does the work of creating the new view and checking it into
     * Starteam.
     * @throws BuildException on error
     */
    public void execute() throws BuildException {

        if (this.revisionlabel && this.buildlabel) {
            throw new BuildException("'revisionlabel' and 'buildlabel' "
                + "both specified.  A revision label cannot be a build label.");
        }

        try {
            View snapshot = openView();

            // Create the new label and update the repository

            if (this.revisionlabel) {
                new Label(snapshot, this.labelName, this.description).update();
                log("Created Revision Label " + this.labelName);
            } else if (null != lastBuild) {
                new Label(snapshot, this.labelName, this.description, this.lastBuild,
                          this.buildlabel).update();
                log("Created View Label ("
                    + (this.buildlabel ? "" : "non-") + "build) " + this.labelName
                    + " as of " + this.lastBuild.toString());
            } else {
                new Label(snapshot, this.labelName, this.description,
                          this.buildlabel).update();
                log("Created View Label ("
                    + (this.buildlabel ? "" : "non-") + "build) " + this.labelName);
            }
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            disconnectFromServer();
        }

    }

    /**
     * Override of base-class abstract function creates an
     * appropriately configured view.  For labels this a view
     * configured as of this.lastBuild.
     *
     * @param raw the unconfigured <code>View</code>
     * @return the snapshot <code>View</code> appropriately configured.
     */
    protected View createSnapshotView(View raw) {
        /*
        if (this.revisionlabel) {
            return raw;
        }
        return new View(raw, ViewConfiguration.createFromTime(this.lastBuild));
        */
        return raw;
    }

}

