/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.util.Hashtable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

public class WeblogicTOPLinkDeploymentTool extends WeblogicDeploymentTool {

    private static final String TL_DTD_LOC = "http://www.objectpeople.com/tlwl/dtd/toplink-cmp_2_5_1.dtd";

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
     * This is expected to be an URL (file or otherwise). If running this on NT using a file URL, the safest 
     * thing would be to not use a drive spec in the URL and make sure the file resides on the drive that 
     * ANT is running from.  This will keep the setting in the build XML platform independent.
     * @param inString the string to use as the DTD location.
     */
    public void setToplinkdtd(String inString) {
        this.toplinkDTD = inString;
    }

    protected DescriptorHandler getDescriptorHandler(File srcDir) {
        DescriptorHandler handler = super.getDescriptorHandler(srcDir);
        if (toplinkDTD != null) {
            handler.registerDTD("-//The Object People, Inc.//DTD TOPLink for WebLogic CMP 2.5.1//EN",
                                toplinkDTD);
        } else {
            handler.registerDTD("-//The Object People, Inc.//DTD TOPLink for WebLogic CMP 2.5.1//EN",
                                TL_DTD_LOC);
        }
        return handler;                                    
    }

    /**
     * Add any vendor specific files which should be included in the 
     * EJB Jar.
     */
    protected void addVendorFiles(Hashtable ejbFiles, String ddPrefix) {
        super.addVendorFiles(ejbFiles, ddPrefix);
        // Then the toplink deployment descriptor

        // Setup a naming standard here?.


        File toplinkDD = new File(getConfig().descriptorDir, ddPrefix + toplinkDescriptor);
        
        if (toplinkDD.exists()) {
            ejbFiles.put(META_DIR + toplinkDescriptor,
                         toplinkDD);
        } else {
            log("Unable to locate toplink deployment descriptor. It was expected to be in " + 
                toplinkDD.getPath(), Project.MSG_WARN);
        }                
    }
    
    /**
     * Called to validate that the tool parameters have been configured.
     *
     */
    public void validateConfigured() throws BuildException {
        super.validateConfigured();
        if (toplinkDescriptor == null) {
            throw new BuildException("The toplinkdescriptor attribute must " 
                + "be specified");
        }
    }
}
