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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.ZipOutputStream;

import java.io.File;
import java.io.IOException;


/**
 * An extension of &lt;jar&gt; to create a WAR archive.
 * Contains special treatment for files that should end up in the
 * <code>WEB-INF/lib</code>, <code>WEB-INF/classes</code> or
 * <code>WEB-INF</code> directories of the Web Application Archive.</p>
 * <p>(The War task is a shortcut for specifying the particular layout of a WAR file. 
 * The same thing can be accomplished by using the <i>prefix</i> and <i>fullpath</i>
 * attributes of zipfilesets in a Zip or Jar task.)</p>
 * <p>The extended zipfileset element from the zip task (with attributes <i>prefix</i>, <i>fullpath</i>, and <i>src</i>) is available in the War task.</p>
 *
 * @author Stefan Bodewig
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
     * Overriden from Zip class to deal with web.xml
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
