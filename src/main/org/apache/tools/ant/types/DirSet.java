/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.types;

import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.DirectoryScanner;
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
    public DirSet clone() {
        if (isReference()) {
            return ((DirSet) getRef(getProject())).clone();
        }
        return (DirSet) super.clone();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return an Iterator of Resources.
     * @since Ant 1.7
     */
    @Override
    public Iterator<Resource> iterator() {
        if (isReference()) {
            return ((DirSet) getRef(getProject())).iterator();
        }
        return new FileResourceIterator(getProject(), getDir(getProject()),
            getDirectoryScanner(getProject()).getIncludedDirectories());
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     * @since Ant 1.7
     */
    @Override
    public int size() {
        if (isReference()) {
            return ((DirSet) getRef(getProject())).size();
        }
        return getDirectoryScanner(getProject()).getIncludedDirsCount();
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
        DirectoryScanner ds = getDirectoryScanner(getProject());
        return Stream.of(ds.getIncludedDirectories()).collect(Collectors.joining(";"));
    }

}
