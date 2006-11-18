/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.filters.ChainableReader;
import org.apache.tools.ant.types.RedirectorElement;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.resources.FileResource;

import java.util.Iterator;
import java.io.File;
import java.io.Reader;
import java.io.IOException;

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
     * The string we look for in the text to indicate direct verification
     */
    private static final String VERIFIED_TEXT = "jar verified.";

    /**
     * certification flag
     */
    private boolean certificates = false;
    private BufferingOutputFilter outputCache = new BufferingOutputFilter();
    /** Error output if there is a failure to verify the jar. */
    public static final String ERROR_NO_VERIFY = "Failed to verify ";

    /**
     * Ask for certificate information to be printed
     * @param certificates if true print certificates.
     */
    public void setCertificates(boolean certificates) {
        this.certificates = certificates;
    }

    /**
     * verify our jar files
     * @throws BuildException on error.
     */
    public void execute() throws BuildException {
        //validation logic
        final boolean hasJar = jar != null;

        if (!hasJar && !hasResources()) {
            throw new BuildException(ERROR_NO_SOURCE);
        }

        beginExecution();

        //patch the redirector to save output to a file
        RedirectorElement redirector = getRedirector();
        redirector.setAlwaysLog(true);
        FilterChain outputFilterChain = redirector.createOutputFilterChain();
        outputFilterChain.add(outputCache);

        try {
            Path sources = createUnifiedSourcePath();
            Iterator iter = sources.iterator();
            while (iter.hasNext()) {
                FileResource fr = (FileResource) iter.next();
                verifyOneJar(fr.getFile());
            }

        } finally {
            endExecution();
        }

    }

    /**
     * verify a JAR.
     * @param jar the jar to verify.
     * @throws BuildException if the file could not be verified
     */
    private void verifyOneJar(File jar) {
        if (!jar.exists()) {
            throw new BuildException(ERROR_NO_FILE + jar);
        }
        final ExecTask cmd = createJarSigner();

        setCommonOptions(cmd);
        bindToKeystore(cmd);

        //verify special operations
        addValue(cmd, "-verify");

        if (certificates) {
            addValue(cmd, "-certs");
        }

        //JAR  is required
        addValue(cmd, jar.getPath());

        log("Verifying JAR: " + jar.getAbsolutePath());
        outputCache.clear();
        BuildException ex = null;
        try {
            cmd.execute();
        } catch (BuildException e) {
            ex = e;
        }
        String results = outputCache.toString();
        //deal with jdk1.4.2 bug:
        if (ex != null) {
            if (results.indexOf("zip file closed") >= 0) {
                log("You are running " + JARSIGNER_COMMAND + " against a JVM with"
                    + " a known bug that manifests as an IllegalStateException.",
                    Project.MSG_WARN);
            } else {
                throw ex;
            }
        }
        if (results.indexOf(VERIFIED_TEXT) < 0) {
            throw new BuildException(ERROR_NO_VERIFY + jar);
        }
    }

    /**
     * we are not thread safe here. Do not use on multiple threads at the same time.
     */
    private static class BufferingOutputFilter implements ChainableReader {

        private BufferingOutputFilterReader buffer;

        public Reader chain(Reader rdr) {
            buffer = new BufferingOutputFilterReader(rdr);
            return buffer;
        }

        public String toString() {
            return buffer.toString();
        }

        public void clear() {
            if (buffer != null) {
                buffer.clear();
            }
        }
    }

    /**
     * catch the output of the buffer
     */
    private static class BufferingOutputFilterReader extends Reader {

        private Reader next;

        private StringBuffer buffer = new StringBuffer();

        public BufferingOutputFilterReader(Reader next) {
            this.next = next;
        }

        public int read(char[] cbuf, int off, int len) throws IOException {
            //hand down
            int result = next.read(cbuf, off, len);
            //cache
            buffer.append(cbuf, off, len);
            //return
            return result;
        }

        public void close() throws IOException {
            next.close();
        }

        public String toString() {
            return buffer.toString();
        }

        public void clear() {
            buffer = new StringBuffer();
        }
    }
}
