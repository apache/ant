/*
 * Copyright  2001-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.ZipOutputStream;

/**
 * Creates a EAR archive. Based on WAR task
 *
 * @since Ant 1.4
 *
 * @ant.task category="packaging"
 */
public class Ear extends Jar {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private File deploymentDescriptor;
    private boolean descriptorAdded;


    /**
     * Create an Ear task.
     */
    public Ear() {
        super();
        archiveType = "ear";
        emptyBehavior = "create";
    }

    /**
     * @deprecated Use setDestFile(destfile) instead
     */
    public void setEarfile(File earFile) {
        setDestFile(earFile);
    }

    /**
     * File to incorporate as application.xml.
     */
    public void setAppxml(File descr) {
        deploymentDescriptor = descr;
        if (!deploymentDescriptor.exists()) {
            throw new BuildException("Deployment descriptor: "
                                     + deploymentDescriptor
                                     + " does not exist.");
        }

        // Create a ZipFileSet for this file, and pass it up.
        ZipFileSet fs = new ZipFileSet();
        fs.setFile(deploymentDescriptor);
        fs.setFullpath("META-INF/application.xml");
        super.addFileset(fs);
    }


    /**
     * Adds zipfileset.
     *
     * @param fs zipfileset to add
     */
    public void addArchives(ZipFileSet fs) {
        // We just set the prefix for this fileset, and pass it up.
        // Do we need to do this? LH
        fs.setPrefix("/");
        super.addFileset(fs);
    }


    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {
        // If no webxml file is specified, it's an error.
        if (deploymentDescriptor == null && !isInUpdateMode()) {
            throw new BuildException("appxml attribute is required", getLocation());
        }

        super.initZipOutputStream(zOut);
    }

    /**
     * Overridden from Zip class to deal with application.xml
     */
    protected void zipFile(File file, ZipOutputStream zOut, String vPath,
                           int mode)
        throws IOException {
        // If the file being added is META-INF/application.xml, we
        // warn if it's not the one specified in the "appxml"
        // attribute - or if it's being added twice, meaning the same
        // file is specified by the "appxml" attribute and in a
        // <fileset> element.
        if (vPath.equalsIgnoreCase("META-INF/application.xml"))  {
            if (deploymentDescriptor == null
                || !FILE_UTILS.fileNameEquals(deploymentDescriptor, file)
                || descriptorAdded) {
                log("Warning: selected " + archiveType
                    + " files include a META-INF/application.xml which will"
                    + " be ignored (please use appxml attribute to "
                    + archiveType + " task)", Project.MSG_WARN);
            } else {
                super.zipFile(file, zOut, vPath, mode);
                descriptorAdded = true;
            }
        } else {
            super.zipFile(file, zOut, vPath, mode);
        }
    }

    /**
     * Make sure we don't think we already have a application.xml next
     * time this task gets executed.
     */
    protected void cleanUp() {
        descriptorAdded = false;
        super.cleanUp();
    }
}
