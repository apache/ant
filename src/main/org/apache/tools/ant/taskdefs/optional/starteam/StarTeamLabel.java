/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.starteam;

import com.starbase.starteam.Label;
import com.starbase.starteam.View;
import com.starbase.starteam.ViewConfiguration;
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
 * @author Christopher Charlier, ThoughtWorks, Inc. 2001
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @see <A HREF="http://www.starbase.com/">StarBase Web Site</A>
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
     * If true, this will be a build label.  If false, it will be a build
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
    */
    public void setLabel(String label) {
        this.labelName = label;
    }

    /**
     * Optional description of the label to be stored in the StarTeam project.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * set the type of label based on the supplied value - if true, this 
     * label will be a revision label, if false, a build label.
     * 
     * @param revision If true this will be a revision label; if false, 
     * a build label
     */
    public void setBuildLabel( boolean buildlabel ) {
        this.buildlabel = buildlabel;
    }
    
    /**
     * set the type of label based on the supplied value - if true, this 
     * label will be a revision label, if false, a build label.
     * 
     * @param revision If true this will be a revision label; if false, 
     * a build label
     */
    public void setRevisionLabel( boolean revisionlabel ) {
        this.revisionlabel = revisionlabel;
    }



    /**
     * The timestamp of the build that will be stored with the label; required.  
     * Must be formatted <code>yyyyMMddHHmmss</code>
     */
    public void setLastBuild(String lastbuild) throws BuildException {
        try {
            Date lastBuildTime = DATE_FORMAT.parse(lastbuild);
            this.lastBuild = new OLEDate(lastBuildTime);
        } catch (ParseException e) {
            throw new BuildException("Unable to parse the date '" + 
                                     lastbuild + "'", e);
        }
    }

    /**
     * This method does the work of creating the new view and checking it into
     * Starteam.
     *
     */
    public void execute() throws BuildException {

        if (this.revisionlabel && this.buildlabel) {
            throw new BuildException(
                "'revisionlabel' and 'buildlabel' both specified.  " +
                "A revision label cannot be a build label.");
        }

        View snapshot = openView();

        // Create the new label and update the repository

        if (this.revisionlabel) {
            new Label(snapshot, this.labelName, this.description).update();
            log("Created Revision Label " + this.labelName);
        } 
        else if (null != lastBuild){
            new Label(snapshot, this.labelName, this.description,this.lastBuild,
                      this.buildlabel).update();
            log("Created View Label (" 
                +(this.buildlabel ? "" : "non-") + "build) " + this.labelName
                +" as of " + this.lastBuild.toString());
        }
        else {
            new Label(snapshot, this.labelName, this.description,
                      this.buildlabel).update();
            log("Created View Label (" 
                +(this.buildlabel ? "" : "non-") + "build) " + this.labelName);
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

