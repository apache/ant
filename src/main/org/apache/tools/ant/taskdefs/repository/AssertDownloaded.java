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

import java.util.ListIterator;

/**
 * This policy is really there for testing the tasks, but you can use
 * it for debugging your own logic.
 */
public class AssertDownloaded extends BaseLibraryPolicy {

    /**
     * our count of files to fetch; null means undefined
     */
    Integer count;
    public static final String ERROR_NO_COUNT = "No count declared";
    public static final String ERROR_DOWNLOAD_FAILURE = "Download count mismatch: expected ";

    /**
     * set the number of files that must be fetched.
     * It is an error if the count does not match.
     * @param count
     */
    public void setCount(Integer count) {
        this.count = count;
    }

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
    public boolean beforeConnect(Libraries owner, ListIterator libraries) {
        if(count==null) {
            throw new BuildException(ERROR_NO_COUNT);
        }
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
        int fetched=owner.calculateDownloadedCount();
        if(fetched!=count.intValue()) {
            throw new BuildException(ERROR_DOWNLOAD_FAILURE
                    +count
                    +" but fetched "+fetched);
        }
    }

}
