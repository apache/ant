/*
 * Copyright  2000-2005 The Apache Software Foundation
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.util.Vector;
import java.io.File;

/**
 * JAR verification task.
 * For every JAR passed in, we fork jarsigner to verify
 * that it is correctly signed. This is more rigorous than just checking for
 * the existence of a signature; the entire certification chain is tested
 * @since Ant 1.7
 */

public class VerifyJar extends AbstractJarSignerTask {
    /**
     * no file message {@value}
     */
    public static final String ERROR_NO_FILE = "Not found :";

    /**
     * certification flag
     */
    private boolean certificates=false;

    /**
     * Ask for certificate information to be printed
     * @param certificates
     */
    public void setCertificates(boolean certificates) {
        this.certificates = certificates;
    }

    /**
     * verify our jar files
     * @throws BuildException
     */
    public void execute() throws BuildException {
        //validation logic
        final boolean hasFileset = filesets.size() > 0;
        final boolean hasJar = jar != null;

        if (!hasJar && !hasFileset) {
            throw new BuildException(ERROR_NO_SOURCE);
        }

        beginExecution();
        try {
            Vector sources = createUnifiedSources();
            for (int i = 0; i < sources.size(); i++) {
                FileSet fs = (FileSet) sources.elementAt(i);
                //get all included files in a fileset
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                String[] jarFiles = ds.getIncludedFiles();
                File baseDir = fs.getDir(getProject());

                //loop through all jars in the fileset
                for (int j = 0; j < jarFiles.length; j++) {
                    String jarFile = jarFiles[j];
                    File jarSource = new File(baseDir, jarFile);
                    verifyOneJar(jarSource);
                }
            }

        } finally {
            endExecution();
        }

    }

    /**
     * verify a JAR.
     * @param jar
     * @throws BuildException if the file could not be verified
     */
    private void verifyOneJar(File jar) {
        if(!jar.exists()) {
            throw new BuildException(ERROR_NO_FILE+jar);
        }
        final ExecTask cmd = createJarSigner();

        setCommonOptions(cmd);
        bindToKeystore(cmd);

        //verify special operations
        addValue(cmd, "-verify");

        if(certificates) {
            addValue(cmd, "-certs");
        }

        //JAR  is required
        addValue(cmd, jar.getPath());

        log("Verifying JAR: " +
                jar.getAbsolutePath());

        cmd.execute();
    }
}
