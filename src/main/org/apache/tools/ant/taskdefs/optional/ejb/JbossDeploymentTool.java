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
 * The deployment tool to add the jboss specific deployment descriptor to the ejb jar file.
 * Jboss only requires one additional file jboss.xml and does not require any additional
 * compilation.
 *
 * @version 1.0
 * @see EjbJar#createJboss
 */
public class JbossDeploymentTool extends GenericDeploymentTool {
    protected static final String JBOSS_DD = "jboss.xml";
    protected static final String JBOSS_CMP10D = "jaws.xml";
    protected static final String JBOSS_CMP20D = "jbosscmp-jdbc.xml";

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
     * @param ejbFiles the hashtable of files to populate.
     * @param ddPrefix the prefix to use.
     */
    @Override
    protected void addVendorFiles(Hashtable<String, File> ejbFiles, String ddPrefix) {
        File jbossDD = new File(getConfig().descriptorDir, ddPrefix + JBOSS_DD);
        if (jbossDD.exists()) {
            ejbFiles.put(META_DIR + JBOSS_DD, jbossDD);
        } else {
            log("Unable to locate jboss deployment descriptor. It was expected to be in "
                + jbossDD.getPath(), Project.MSG_WARN);
            return;
        }
        String descriptorFileName = JBOSS_CMP10D;
        if (EjbJar.CMPVersion.CMP2_0.equals(getParent().getCmpversion())) {
            descriptorFileName = JBOSS_CMP20D;
        }
        File jbossCMPD
            = new File(getConfig().descriptorDir, ddPrefix + descriptorFileName);

        if (jbossCMPD.exists()) {
            ejbFiles.put(META_DIR + descriptorFileName, jbossCMPD);
        } else {
            log("Unable to locate jboss cmp descriptor. It was expected to be in "
                + jbossCMPD.getPath(), Project.MSG_VERBOSE);
        }
    }

    /**
     * Get the vendor specific name of the Jar that will be output. The modification date
     * of this jar will be checked against the dependent bean classes.
     */
    @Override
    File getVendorOutputJarFile(String baseName) {
        if (getDestDir() == null && getParent().getDestdir() == null) {
            throw new BuildException("DestDir not specified");
        }
        if (getDestDir() == null) {
            return new File(getParent().getDestdir(), baseName + jarSuffix);
        }
        return new File(getDestDir(), baseName + jarSuffix);
    }

    /**
     * Called to validate that the tool parameters have been configured.
     *
     * @throws BuildException If the Deployment Tool's configuration isn't
     *                        valid
     * @since ant 1.6
     */
    @Override
    public void validateConfigured() throws BuildException {
    }

    private EjbJar getParent() {
        return (EjbJar) this.getTask();
    }
}
