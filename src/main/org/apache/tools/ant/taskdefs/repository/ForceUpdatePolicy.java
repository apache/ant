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

import java.util.Iterator;
import java.util.ListIterator;

/**
 * This update policy marks all libraries for download.
 * After downloading, it will raise an error if any one of the files was not
 * retrieved.
 */
public class ForceUpdatePolicy extends BaseLibraryPolicy {
    public static final String ERROR_FORCED_DOWNLOAD_FAILED = "Failed to download file:";


    public String getName() {
        return "force";
    }

    /**

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
        owner.markAllLibrariesForFetch(true);
        owner._setUseTimestamp(false);
        return true;
    }

    /**
     * method called after a successful connection process.
     *
     * @param owner
     * @param libraries
     *
     * @throws org.apache.tools.ant.BuildException
     *
     */
    public void afterFetched(Libraries owner, ListIterator libraries) {
        //here verify that everything came in
        Iterator downloaded = owner.enabledLibrariesIterator();
        while (downloaded.hasNext()) {
            Library library = (Library) downloaded.next();
            if (library.isToFetch() && !library.wasFetched()) {
                throw new BuildException(ERROR_FORCED_DOWNLOAD_FAILED
                        + library.getDestFilename());
            }
        }
    }
}
