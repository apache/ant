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
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.util.Hashtable;
import org.apache.tools.ant.Project;

/**
 * The deployment tool to add the jboss specific deployment descriptor to the ejb jar file.
 * Jboss only requires one additional file jboss.xml and does not require any additional
 * compilation.
 *
 * @author <a href="mailto:p.austin@talk21.com">Paul Austin</a>
 * @version 1.0
 * @see EjbJar#createJboss
 */
public class JbossDeploymentTool extends GenericDeploymentTool {
    protected static final String JBOSS_DD = "jboss.xml";
    protected static final String JBOSS_CMPD = "jaws.xml";

    /** Instance variable that stores the suffix for the jboss jarfile. */
    private String jarSuffix = ".jar";

    /**
     * Setter used to store the suffix for the generated JBoss jar file.
     * @param inString the string to use as the suffix.
     */
    public void setSuffix(String inString) {
        jarSuffix = inString;
    }
    
    /**
     * Add any vendor specific files which should be included in the
     * EJB Jar.
     */
    protected void addVendorFiles(Hashtable ejbFiles, String ddPrefix) {
        File jbossDD = new File(getConfig().descriptorDir, ddPrefix + JBOSS_DD);
        if (jbossDD.exists()) {
            ejbFiles.put(META_DIR + JBOSS_DD, jbossDD);
        } else {
            log("Unable to locate jboss deployment descriptor. It was expected to be in " + jbossDD.getPath(), Project.MSG_WARN);
            return;
        }
        
        File jbossCMPD = new File(getConfig().descriptorDir, ddPrefix + JBOSS_CMPD);
        if (jbossCMPD.exists()) {
            ejbFiles.put(META_DIR + JBOSS_CMPD, jbossCMPD);
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
