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

import org.apache.tools.ant.taskdefs.optional.repository.GetLibraries;

import java.util.ListIterator;

/**
 * Mark all files for fetching, but timestamp driven
 * This will only update changed files. Unlike {@link ForceUpdatePolicy},
 * there is no post-download verification that everything got fetched
 */
public class TimestampPolicy extends BaseLibraryPolicy {

    /**
     * Method called before we connect. Caller can manipulate the list,
     *
     * @param owner
     * @param libraries
     *
     * @return true if the connection is to go ahead
     *
     * @throws org.apache.tools.ant.BuildException
     *          if needed
     */
    public boolean beforeConnect(GetLibraries owner, ListIterator libraries) {
        owner.markAllLibrariesForFetch(true);
        owner._setUseTimestamp(true);
        return true;
    }

}
