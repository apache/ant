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
 */
public abstract class BaseLibraryPolicy implements LibraryPolicy {

    /**
     * enabled flag
     */
    private boolean enabled=true;

    /**
     * turn policy on/off
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * are we enabled
     * @return true if {@link #enabled} is set
     */
    public boolean getEnabled() {
        return enabled;
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

    }
}
