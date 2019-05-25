/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types;

import java.util.Iterator;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.FileResourceIterator;

/**
 * Subclass as hint for supporting tasks that the included directories
 * instead of files should be used.
 *
 * @since Ant 1.5
 */
public class DirSet extends AbstractFileSet implements ResourceCollection {

    /**
     * Constructor for DirSet.
     */
    public DirSet() {
        super();
    }

    /**
     * Constructor for DirSet, with DirSet to shallowly clone.
     * @param dirset the dirset to clone.
     */
    protected DirSet(DirSet dirset) {
        super(dirset);
    }

    /**
     * Return a DirSet that has the same basedir and same patternsets
     * as this one.
     * @return the cloned dirset.
     */
    @Override
    public Object clone() {
        if (isReference()) {
            return getRef().clone();
        }
        return super.clone();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return an Iterator of Resources.
     * @since Ant 1.7
     */
    @Override
    public Iterator<Resource> iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        return new FileResourceIterator(getProject(), getDir(getProject()),
            getDirectoryScanner().getIncludedDirectories());
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     * @since Ant 1.7
     */
    @Override
    public int size() {
        if (isReference()) {
            return getRef().size();
        }
        return getDirectoryScanner().getIncludedDirsCount();
    }

    /**
     * Always returns true.
     * @return true indicating that all elements will be FileResources.
     * @since Ant 1.7
     */
    @Override
    public boolean isFilesystemOnly() {
        return true;
    }

    /**
     * Returns included directories as a list of semicolon-separated paths.
     *
     * @return a <code>String</code> of included directories.
     */
    @Override
    public String toString() {
        return String.join(";", getDirectoryScanner().getIncludedDirectories());
    }

    @Override
    protected AbstractFileSet getRef(Project p) {
        return getCheckedRef(DirSet.class, getDataTypeName(), p);
    }

    private DirSet getRef() {
        return getCheckedRef(DirSet.class);
    }

}
