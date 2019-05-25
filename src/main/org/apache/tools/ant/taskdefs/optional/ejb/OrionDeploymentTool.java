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
import org.apache.tools.ant.Project;

/**
 * The deployment tool to add the orion specific deployment descriptor to the
 * ejb jar file. Orion only requires one additional file orion-ejb-jar.xml
 * and does not require any additional compilation.
 *
 * @since Ant 1.10.2
 * @see EjbJar#createOrion
 */

public class OrionDeploymentTool extends GenericDeploymentTool {

    protected static final String ORION_DD = "orion-ejb-jar.xml";

    /** Instance variable that stores the suffix for the JBoss jarfile. */
    private String jarSuffix = ".jar";

    /**
     * Add any vendor specific files which should be included in the
     * EJB Jar.
     *
     * @param ejbFiles Hashtable&lt;String, File&gt;
     * @param baseName String
     */
    protected void addVendorFiles(Hashtable<String, File> ejbFiles, String baseName) {
        String ddPrefix = usingBaseJarName() ? "" : baseName;
        File orionDD = new File(getConfig().descriptorDir, ddPrefix + ORION_DD);

        if (orionDD.exists()) {
            ejbFiles.put(META_DIR + ORION_DD, orionDD);
        } else {
            log("Unable to locate Orion deployment descriptor. It was expected to be in " + orionDD.getPath(), Project.MSG_WARN);
        }

    }

    /**
     * Get the vendor specific name of the Jar that will be output. The modification date
     * of this jar will be checked against the dependent bean classes.
     *
     * @param baseName String
     */
    File getVendorOutputJarFile(String baseName) {
        return new File(getDestDir(), baseName + jarSuffix);
    }
}
