/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 * <p>An extension of &lt;jar&gt; to create a WAR archive.
 * Contains special treatment for files that should end up in the
 * <code>WEB-INF/lib</code>, <code>WEB-INF/classes</code> or
 * <code>WEB-INF</code> directories of the Web Application Archive.</p>
 *
 * <p>(The War task is a shortcut for specifying the particular layout of a WAR file.
 * The same thing can be accomplished by using the <i>prefix</i> and <i>fullpath</i>
 * attributes of zipfilesets in a Zip or Jar task.)</p>
 *
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

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    /** path to web.xml file */
    private static final String XML_DESCRIPTOR_PATH = "WEB-INF/web.xml";

    /**
     * our web.xml deployment descriptor
     */
    private File deploymentDescriptor;

    /**
     * flag set if the descriptor is added
     */
    private boolean needxmlfile = true;
    private File addedWebXmlFile;

    /** Constructor for the War Task. */
    public War() {
        super();
        archiveType = "war";
        emptyBehavior = "create";
    }

    /**
     * <i>Deprecated</i> name of the file to create
     * -use <code>destfile</code> instead.
     * @param warFile the destination file
     * @deprecated since 1.5.x.
     *             Use setDestFile(File) instead
     * @ant.attribute ignore="true"
     */
    @Deprecated
    public void setWarfile(File warFile) {
        setDestFile(warFile);
    }

    /**
     * set the deployment descriptor to use (WEB-INF/web.xml);
     * required unless <code>update=true</code>
     * @param descr the deployment descriptor file
     */
    public void setWebxml(File descr) {
        deploymentDescriptor = descr;
        if (!deploymentDescriptor.exists()) {
            throw new BuildException("Deployment descriptor:  does not exist.",
                deploymentDescriptor);
        }

        // Create a ZipFileSet for this file, and pass it up.
        ZipFileSet fs = new ZipFileSet();
        fs.setFile(deploymentDescriptor);
        fs.setFullpath(XML_DESCRIPTOR_PATH);
        super.addFileset(fs);
    }

    /**
     * Set the policy on the web.xml file, that is, whether or not it is needed
     * @param needxmlfile whether a web.xml file is needed. Default: true
     */
    public void setNeedxmlfile(boolean needxmlfile) {
        this.needxmlfile = needxmlfile;
    }

    /**
     * add files under WEB-INF/lib/
     * @param fs the zip file set to add
     */
    public void addLib(ZipFileSet fs) {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix("WEB-INF/lib/");
        super.addFileset(fs);
    }

    /**
     * add files under WEB-INF/classes
     * @param fs the zip file set to add
     */
    public void addClasses(ZipFileSet fs) {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix("WEB-INF/classes/");
        super.addFileset(fs);
    }

    /**
     * files to add under WEB-INF;
     * @param fs the zip file set to add
     */
    public void addWebinf(ZipFileSet fs) {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix("WEB-INF/");
        super.addFileset(fs);
    }

    /**
     * override of  parent; validates configuration
     * before initializing the output stream.
     * @param zOut the zip output stream
     * @throws IOException on output error
     * @throws BuildException if invalid configuration
     */
    @Override
    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {
        super.initZipOutputStream(zOut);
    }

    /**
     * Overridden from Zip class to deal with web.xml
     *
     * Here are cases that can arise
     * -not a web.xml file : add
     * -first web.xml : add, remember we added it
     * -same web.xml again: skip
     * -alternate web.xml : warn and skip
     *
     * @param file the file to add to the archive
     * @param zOut the stream to write to
     * @param vPath the name this entry shall have in the archive
     * @param mode the Unix permissions to set.
     * @throws IOException on output error
     */
    @Override
    protected void zipFile(File file, ZipOutputStream zOut, String vPath,
                           int mode)
        throws IOException {
        // If the file being added is WEB-INF/web.xml, we warn if it's
        // not the one specified in the "webxml" attribute - or if
        // it's being added twice, meaning the same file is specified
        // by the "webxml" attribute and in a <fileset> element.
        //by default, we add the file.
        boolean addFile = true;
        if (XML_DESCRIPTOR_PATH.equalsIgnoreCase(vPath)) {
            //a web.xml file was found. See if it is a duplicate or not
            if (addedWebXmlFile != null) {
                //a second web.xml file, so skip it
                addFile = false;
                //check to see if we warn or not
                if (!FILE_UTILS.fileNameEquals(addedWebXmlFile, file)) {
                    logWhenWriting("Warning: selected " + archiveType
                        + " files include a second " + XML_DESCRIPTOR_PATH
                        + " which will be ignored.\nThe duplicate entry is at "
                        + file + "\nThe file that will be used is "
                        + addedWebXmlFile, Project.MSG_WARN);
                }
            } else {
                //no added file, yet
                addedWebXmlFile = file;
                //there is no web.xml file, so add it
                addFile = true;
                //and remember that we did
                deploymentDescriptor = file;
            }
        }
        if (addFile) {
            super.zipFile(file, zOut, vPath, mode);
        }
    }

    /**
     * Make sure we don't think we already have a web.xml next time this task
     * gets executed.
     */
    @Override
    protected void cleanUp() {
        if (addedWebXmlFile == null
            && deploymentDescriptor == null
            && needxmlfile
            && !isInUpdateMode()
            && hasUpdatedFile()) {
            throw new BuildException(
                "No WEB-INF/web.xml file was added.\nIf this is your intent, set needxmlfile='false' ");
        }
        addedWebXmlFile = null;
        super.cleanUp();
    }
}
