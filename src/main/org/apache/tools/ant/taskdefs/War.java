/*
 * Copyright  2000-2004 The Apache Software Foundation
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
 * An extension of &lt;jar&gt; to create a WAR archive.
 * Contains special treatment for files that should end up in the
 * <code>WEB-INF/lib</code>, <code>WEB-INF/classes</code> or
 * <code>WEB-INF</code> directories of the Web Application Archive.</p>
 * <p>(The War task is a shortcut for specifying the particular layout of a WAR file.
 * The same thing can be accomplished by using the <i>prefix</i> and <i>fullpath</i>
 * attributes of zipfilesets in a Zip or Jar task.)</p>
 * <p>The extended zipfileset element from the zip task
 * (with attributes <i>prefix</i>, <i>fullpath</i>, and <i>src</i>)
 * is available in the War task.</p>
 *
 * @since Ant 1.2
 *
 * @ant.task category="packaging"
 * @see Jar
 */
public class War extends Jar {

    /**
     * our web.xml deployment descriptor
     */
    private File deploymentDescriptor;

    /**
     * flag set if the descriptor is added
     */
    private boolean descriptorAdded;

    private static final FileUtils fu = FileUtils.newFileUtils();

    public War() {
        super();
        archiveType = "war";
        emptyBehavior = "create";
    }

    /**
     * <i>Deprecated<i> name of the file to create
     * -use <tt>destfile</tt> instead.
     * @deprecated Use setDestFile(File) instead
     * @ant.attribute ignore="true"
     */
    public void setWarfile(File warFile) {
        setDestFile(warFile);
    }

    /**
     * set the deployment descriptor to use (WEB-INF/web.xml);
     * required unless <tt>update=true</tt>
     */
    public void setWebxml(File descr) {
        deploymentDescriptor = descr;
        if (!deploymentDescriptor.exists()) {
            throw new BuildException("Deployment descriptor: "
                                     + deploymentDescriptor
                                     + " does not exist.");
        }

        // Create a ZipFileSet for this file, and pass it up.
        ZipFileSet fs = new ZipFileSet();
        fs.setFile(deploymentDescriptor);
        fs.setFullpath("WEB-INF/web.xml");
        super.addFileset(fs);
    }

    /**
     * add files under WEB-INF/lib/
     */

    public void addLib(ZipFileSet fs) {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix("WEB-INF/lib/");
        super.addFileset(fs);
    }

    /**
     * add files under WEB-INF/classes
     */
    public void addClasses(ZipFileSet fs) {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix("WEB-INF/classes/");
        super.addFileset(fs);
    }

    /**
     * files to add under WEB-INF;
     */
    public void addWebinf(ZipFileSet fs) {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix("WEB-INF/");
        super.addFileset(fs);
    }

    /**
     * override of  parent; validates configuration
     * before initializing the output stream.
     */
    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {
        // If no webxml file is specified, it's an error.
        if (deploymentDescriptor == null && !isInUpdateMode()) {
            throw new BuildException("webxml attribute is required", getLocation());
        }

        super.initZipOutputStream(zOut);
    }

    /**
     * Overridden from Zip class to deal with web.xml
     */
    protected void zipFile(File file, ZipOutputStream zOut, String vPath,
                           int mode)
        throws IOException {
        // If the file being added is WEB-INF/web.xml, we warn if it's
        // not the one specified in the "webxml" attribute - or if
        // it's being added twice, meaning the same file is specified
        // by the "webxml" attribute and in a <fileset> element.
        if (vPath.equalsIgnoreCase("WEB-INF/web.xml"))  {
            if (deploymentDescriptor == null
                || !fu.fileNameEquals(deploymentDescriptor, file)
                || descriptorAdded) {
                log("Warning: selected " + archiveType
                    + " files include a WEB-INF/web.xml which will be ignored "
                    + "(please use webxml attribute to "
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
     * Make sure we don't think we already have a web.xml next time this task
     * gets executed.
     */
    protected void cleanUp() {
        descriptorAdded = false;
        super.cleanUp();
    }
}
