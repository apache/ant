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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.ZipOutputStream;

import java.io.File;
import java.io.IOException;

/**
 * Creates a EAR archive. Based on WAR task
 *
 * @author Stefan Bodewig
 * @author <a href="mailto:leslie.hughes@rubus.com">Les Hughes</a>
 *
 * @since Ant 1.4
 *
 * @ant.task category="packaging"
 */
public class Ear extends Jar {

    private File deploymentDescriptor;
    private boolean descriptorAdded;
    private static final FileUtils fu = FileUtils.newFileUtils();

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
     * Overriden from Zip class to deal with application.xml
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
                || !fu.fileNameEquals(deploymentDescriptor, file)
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
