/*
 * Copyright 2004-2005 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.repository;

import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;


/**
 * A Maven repository knows about maven repository layout rules It also defaults
 * to http://www.ibiblio.org/maven/
 *
 * @link http://maven.apache.org/reference/user-guide.html#Remote_Repository_Layout
 * @link
 * @since Ant1.7
 */
public class MavenRepository extends HttpRepository {
    public static final String MAVEN_URL = "http://www.ibiblio.org/maven/";


    /**
     * check the MD5 flag
     */
    public boolean checkMD5;

    /**
     * this is what we think the MD5 type is
     */
    protected static final String MAVEN_MD5_FILE_TYPE = "US-ASCII";
    public static final String TYPE_NAME = "mavenrepository";

    /**
     * bind to the main maven repository
     */
    public MavenRepository() {
    }

    
    /**
     * set this to check the MD5 signatures. SECURITY IS NOT YET FUNCTIONAL
     * @param checkMD5
     */
    public void setCheckMD5(boolean checkMD5) {
        this.checkMD5 = checkMD5;
    }


    /**
     * Validation time is where the final fixup of repositories exist; this
     * is the last chance to examine properties to see if there is an override.
     *
     * @throws BuildException if unhappy
     */
    public void validate() {
        if(getUrl()==null) {
            //we have no URL yet; so use the maven one
            if(getProject()!=null) {
                String urlProperty=getProject()
                        .getProperty(Libraries.REPOSITORY_URL_PROPERTY);
                if(urlProperty!=null) {
                    setUrl(urlProperty);
                } else {
                    setUrl(MAVEN_URL);
                }
            }
        }
        super.validate();
    }

    /**
     * Get the path to a remote library. This is the full URL
     *
     * @param library
     *
     * @return URL to library
     */
    protected String getRemoteLibraryURL(Library library) {
        String base = getUrl();
        if (!base.endsWith("/")) {
            base = base + '/';
        }

        return base + library.getMavenPath('/');
    }

    /**
     * Returns a string representation of the repository
     *
     * @return the base URL
     */
    public String toString() {
        return "Maven Repository at " + getUrl();
    }

    /**
     * this is a string that uniquely describes the repository and can be used
     * for equality tests <i>across</i> instances.
     *
     * @return maven identifier
     */
    public String getRepositoryURI() {
        return "maven://" + getUrl();
    }

    /**
     * fetch a library from the repository
     *
     * @param library
     *
     * @param useTimestamp
     * @return true if we retrieved
     *
     * @throws org.apache.tools.ant.BuildException
     *
     */
    public boolean fetch(Library library, boolean useTimestamp) throws IOException {
        boolean  fetched = super.fetch(library, useTimestamp);
        if (fetched && checkMD5) {
            //we got here if there was a fetch. so we now get the MD5 info from the file,
            boolean successful = false;
            String md5path = getRemoteLibraryURL(library) + ".md5";
            File md5file = File.createTempFile(library.getArchive(), ".md5");
            Reader in = null;
            try {
                URL md5url = new URL(md5path);
                logVerbose("getting md5 file from " + md5path + " to " + md5file.getAbsolutePath());
                get(md5url, md5file, false, getUsername(), getPassword());
                in = new InputStreamReader(new FileInputStream(md5file), MAVEN_MD5_FILE_TYPE);
                char md5data[] = new char[32];
                in.read(md5data);
                logDebug("md5 data " + md5data);
                //TODO: verify this against a <checksum> generated signature.

                successful = true;
            } catch (IOException e) {
                logVerbose("IO failure on MD5 fetch " + e.getMessage());
                throw e;
            } finally {
                FileUtils.close(in);
                if (md5file.exists()) {
                    md5file.delete();
                }
                if (!successful) {
                    //if security checks failed for any reason,
                    //delete the library file
                    //brute force paranoia
                    library.getLibraryFile().delete();
                }
            }
        }
        return fetched;

    }

}
