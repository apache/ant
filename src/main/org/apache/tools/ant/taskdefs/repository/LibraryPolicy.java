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
 * An interface that things can support to change the library behaviour.
 * Example uses could be: extra validation (signatures, etc), filename remapping
 *
 *
 * Here is the use
 * <ol>
 * <li>Policies are executed in order of declaration.
 * <li>The {@link #beforeConnect(org.apache.tools.ant.taskdefs.repository.Libraries,
 * java.util.ListIterator)} call,
 * is called before any connection has been initiated; policies can manipulate
 * the library list, set/reset their toFetch list, rename destination files, etc.
 * <li>If any policy returns false from the method, the connection does not proceed.
 * This is not an error, provided the files are actually present.
 * <li>After running through the fetch of all files marked for download,
 * every policy implementation will again be called in order of declaration.
 * <li>The {@link #afterFetched(org.apache.tools.ant.taskdefs.repository.Libraries,
 * java.util.ListIterator)} method
 * does not return anything.
 * <li>Either method can throw a BuildException to indicate some kind of error.
 * </ol>
 *
 */
public interface LibraryPolicy extends EnabledLibraryElement {


    /**
     * Method called before we connect. Caller can manipulate the list,
     *
     *
     * @param owner
     *
     * @param libraries
     * @return true if the connection is to go ahead
     *
     * @throws org.apache.tools.ant.BuildException
     *          if needed
     */
    boolean beforeConnect(Libraries owner, ListIterator libraries);

    /**
     * method called after a successful connection process.
     * @param owner
     * @param libraries
     * @throws org.apache.tools.ant.BuildException
     */
    void afterFetched(Libraries owner, ListIterator libraries);


}
