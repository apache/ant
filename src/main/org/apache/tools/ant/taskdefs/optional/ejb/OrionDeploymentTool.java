/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;

/**
 * The deployment tool to add the orion specific deployment descriptor to the 
 * ejb jar file. Orion only requires one additional file orion-ejb-jar.xml 
 * and does not require any additional compilation.
 *
 * @author <a href="mailto:atul.setlur@med.ge.com">Atul Setlur</a>
 * @version 1.0
 * @see EjbJar#createOrion
 */

public class OrionDeploymentTool extends GenericDeploymentTool {
    
    protected static final String ORION_DD = "orion-ejb-jar.xml";
    

    /** Instance variable that stores the suffix for the jboss jarfile. */
    private String jarSuffix = ".jar";

    /**
     * Add any vendor specific files which should be included in the
     * EJB Jar.
     */
    protected void addVendorFiles(Hashtable ejbFiles, String baseName) {
        String ddPrefix = (usingBaseJarName() ? "" : baseName );
        File orionDD = new File(getConfig().descriptorDir, ddPrefix + ORION_DD);
        
        if (orionDD.exists()) {
            ejbFiles.put(META_DIR + ORION_DD, orionDD);
        } else {
            log("Unable to locate Orion deployment descriptor. It was expected to be in " + orionDD.getPath(), Project.MSG_WARN);
            return;
        }
        
    }

    /**
     * Get the vendor specific name of the Jar that will be output. The modification date
     * of this jar will be checked against the dependent bean classes.
     */
    File getVendorOutputJarFile(String baseName) {
        return new File(getDestDir(), baseName + jarSuffix);
    }
}
