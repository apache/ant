/*
 * Copyright  2004 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.repository;


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
     * bind to the main maven repository
     */
    public MavenRepository() {
        setUrl(MAVEN_URL);
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

}
