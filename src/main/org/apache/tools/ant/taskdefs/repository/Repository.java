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

package org.apache.tools.ant.taskdefs.repository;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;

import java.io.IOException;

/**
 * This type represents a repository; a place that stores libraries for
 * retrieval. To use this type, you must use a non-abstract class, either one
 * that ships with Ant, or one you implement and declare yourself.
 * <p/>
 * The &lt;getlibraries&gt; task lets you supply a repository by reference
 * inline {@link Libraries#add(Repository)} or on the command line {@link
 * GetLibraries#setRepositoryRef(org.apache.tools.ant.types.Reference)}
 *
 * @since Ant1.7
 */
public abstract class Repository extends DataType {


    /**
     * validate yourself
     *
     * @throws BuildException if unhappy
     */
    public void validate() {
    }

    /**
     * recursively resolve any references to get the real repository
     *
     * @return
     */
    public final Repository resolve() {
        if (getRefid() == null) {
            return this;
        } else {
            Repository repository = (Repository) getCheckedRef(Repository.class,
                    "Repository");
            return repository;
        }
    }

    /**
     * override point: connection is called at the start of the retrieval
     * process
     *
     * @param owner owner of the libraries
     *
     * @throws BuildException
     */
    public void connect(Libraries owner) {

    }

    /**
     * override point: connection is called at the start of the retrieval
     * process
     *
     * @throws BuildException
     */

    public void disconnect() {

    }


    /**
     * Test for a repository being reachable. This method is called after {@link
     * #connect(GetLibraries)} is called, before any files are to be retrieved.
     * <p/>
     * If it returns false the repository considers itself offline. Similarly,
     * any ioexception is interpreted as being offline.
     *
     * @return true if the repository is online.
     *
     * @throws IOException
     */
    public abstract boolean checkRepositoryReachable() throws IOException;

    /**
     * fetch a library from the repository
     *
     * @param library library to fetch
     *
     * @param useTimestamp flag to indicate the timestamp of the lib should be used
     * @return
     */
    public abstract boolean fetch(Library library, boolean useTimestamp) throws IOException;


    /**
     * this is a string that uniquely describes the repository
     * and can be used for equality tests <i>across</i> instances.
     * @return
     */
    public abstract String getRepositoryURI();
}
