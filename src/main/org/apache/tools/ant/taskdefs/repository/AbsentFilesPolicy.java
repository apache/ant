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

import java.util.ListIterator;

/**
 * This policy only marks absent(enabled) files.
 */
public class AbsentFilesPolicy extends BaseLibraryPolicy {

    /**
     * Tell owner to mark all missing libraries as fetchable
     *
     * @param owner
     * @param libraries
     *
     * @return true if the connection is to go ahead
     *
     * @throws org.apache.tools.ant.BuildException
     *          if needed
     */
    public boolean beforeConnect(Libraries owner, ListIterator libraries) {
        owner.markMissingLibrariesForFetch();
        return true;
    }
}
