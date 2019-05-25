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

package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.util.Hashtable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Deployment tool for WebLogic TOPLink.
 */
public class WeblogicTOPLinkDeploymentTool extends WeblogicDeploymentTool {

    private static final String TL_DTD_LOC
        = "http://www.objectpeople.com/tlwl/dtd/toplink-cmp_2_5_1.dtd";

    private String toplinkDescriptor;
    private String toplinkDTD;

    /**
     * Setter used to store the name of the toplink descriptor.
     * @param inString the string to use as the descriptor name.
     */
    public void setToplinkdescriptor(String inString) {
        this.toplinkDescriptor = inString;
    }

    /**
     * Setter used to store the location of the toplink DTD file.
     * This is expected to be an URL (file or otherwise). If running
     * this on NT using a file URL, the safest thing would be to not use a
     * drive spec in the URL and make sure the file resides on the drive that
     * ANT is running from.  This will keep the setting in the build XML
     * platform independent.
     *
     * @param inString the string to use as the DTD location.
     */
    public void setToplinkdtd(String inString) {
        this.toplinkDTD = inString;
    }

    /**
     * Get the descriptor handler.
     * @param srcDir the source file.
     * @return the descriptor handler.
     */
    @Override
    protected DescriptorHandler getDescriptorHandler(File srcDir) {
        DescriptorHandler handler = super.getDescriptorHandler(srcDir);
        if (toplinkDTD != null) {
            handler.registerDTD(
                "-//The Object People, Inc.//DTD TOPLink for WebLogic CMP 2.5.1//EN",
                toplinkDTD);
        } else {
            handler.registerDTD(
                "-//The Object People, Inc.//DTD TOPLink for WebLogic CMP 2.5.1//EN",
                TL_DTD_LOC);
        }
        return handler;
    }

    /**
     * Add any vendor specific files which should be included in the
     * EJB Jar.
     * @param ejbFiles the hashtable to add files to.
     * @param ddPrefix the prefix to use.
     */
    @Override
    protected void addVendorFiles(Hashtable<String, File> ejbFiles, String ddPrefix) {
        super.addVendorFiles(ejbFiles, ddPrefix);
        // Then the toplink deployment descriptor

        // Setup a naming standard here?.

        File toplinkDD = new File(getConfig().descriptorDir, ddPrefix + toplinkDescriptor);

        if (toplinkDD.exists()) {
            ejbFiles.put(META_DIR + toplinkDescriptor,
                         toplinkDD);
        } else {
            log("Unable to locate toplink deployment descriptor. It was expected to be in "
                + toplinkDD.getPath(), Project.MSG_WARN);
        }
    }

    /**
     * Called to validate that the tool parameters have been configured.
     * @throws BuildException if there is an error.
     */
    @Override
    public void validateConfigured() throws BuildException {
        super.validateConfigured();
        if (toplinkDescriptor == null) {
            throw new BuildException(
                "The toplinkdescriptor attribute must be specified");
        }
    }
}
