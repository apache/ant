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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;

import java.io.IOException;

/**
 * not a real repository; one to paste a reference into the chain for
 * resolution.
 *
 * @since Ant1.7
 */
public final class RepositoryRef extends Repository {
    /** this constant name is only funny to COM developers  
     */
    public static final String E_NOTIMPL = "Not Implemented";


    /**
     * create a repository reference
     *
     * @param reference
     */
    public RepositoryRef(Project project, Reference reference) {
        setRefid(reference);
        setProject(project);
    }

    /**
     * empty constructor
     */
    public RepositoryRef() {
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
     * @throws java.io.IOException
     */
    public boolean checkRepositoryReachable() throws IOException {
        return false;
    }

    /**
     * fetch a library from the repository
     *
     * @param library
     *
     * @param useTimestamp
     * @return
     */
    public boolean fetch(Library library, boolean useTimestamp) throws IOException {
        throw new BuildException(E_NOTIMPL);
    }

    /**
     * this is a string that uniquely describes the repository and can be used
     * for equality tests <i>across</i> instances.
     *
     * @return
     */
    public String getRepositoryURI() {
        return "ref://"+getRefid();
    }
}
