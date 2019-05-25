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

package org.apache.tools.ant.types.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * Union of file/dirsets that share the same patterns and selectors
 * but have different roots.
 * @since Ant 1.9.4
 */
public class MultiRootFileSet extends AbstractFileSet
    implements ResourceCollection {

    private SetType type = SetType.file;
    private boolean cache = true;
    private List<File> baseDirs = new ArrayList<>();
    private Union union;

    @Override
    public void setDir(final File dir) {
        throw new BuildException(getDataTypeName()
                                 + " doesn't support the dir attribute");
    }

    /**
     * Determines the types of resources to return.
     * @param type the types of resources to return
     */
    public void setType(final SetType type) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.type = type;
    }

    /**
     * Set whether to cache collections.
     * @param b boolean cache flag.
     */
    public synchronized void setCache(final boolean b) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        cache = b;
    }

    /**
     * Adds basedirs as a comma separated list.
     * @param dirs directories as CSV
     */
    public void setBaseDirs(final String dirs) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (dirs != null && !dirs.isEmpty()) {
            for (final String d : dirs.split(",")) {
                baseDirs.add(getProject().resolveFile(d));
            }
        }
    }

    /**
     * Adds a basedir as nested element.
     * @param r basedir
     */
    public void addConfiguredBaseDir(final FileResource r) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        baseDirs.add(r.getFile());
    }

    @Override
    public void setRefid(final Reference r) {
        if (!baseDirs.isEmpty()) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Return a MultiRootFileSet that has the same basedirs and same patternsets
     * as this one.
     * @return the cloned MultiRootFileSet.
     */
    @Override
    public Object clone() {
        if (isReference()) {
            return getRef().clone();
        }
        final MultiRootFileSet fs = (MultiRootFileSet) super.clone();
        fs.baseDirs = new ArrayList<>(baseDirs);
        fs.union = null;
        return fs;
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return an Iterator of Resources.
     */
    @Override
    public Iterator<Resource> iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        return merge().iterator();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     */
    @Override
    public int size() {
        if (isReference()) {
            return getRef().size();
        }
        return merge().size();
    }

    /**
     * Always returns true.
     * @return true indicating that all elements will be FileResources.
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
        if (isReference()) {
            return getRef().toString();
        }
        return merge().toString();
    }

    private MultiRootFileSet getRef() {
        return getCheckedRef(MultiRootFileSet.class);
    }

    private synchronized Union merge() {
        if (cache && union != null) {
            return union;
        }
        final Union u = new Union();
        setup(u);
        if (cache) {
            union = u;
        }
        return u;
    }

    private void setup(final Union u) {
        for (final File d : baseDirs) {
            u.add(new Worker(this, type, d));
        }
    }

    /**
     * What to return from the set: files, directories or both.
     */
    public enum SetType {
        file, dir, both
    }

    private static class Worker extends AbstractFileSet
        implements ResourceCollection {

        private final SetType type;

        private Worker(final MultiRootFileSet fs, final SetType type, final File dir) {
            super(fs);
            this.type = type;
            setDir(dir);
        }

        @Override
        public boolean isFilesystemOnly() {
            return true;
        }

        @Override
        public Iterator<Resource> iterator() {
            final DirectoryScanner ds = getDirectoryScanner();
            String[] names = type == SetType.file
                ? ds.getIncludedFiles()
                : ds.getIncludedDirectories();
            if (type == SetType.both) {
                final String[] files = ds.getIncludedFiles();
                final String[] merged = new String[names.length + files.length];
                System.arraycopy(names, 0, merged, 0, names.length);
                System.arraycopy(files, 0, merged, names.length, files.length);
                names = merged;
            }
            return new FileResourceIterator(getProject(), getDir(getProject()),
                                            names);
        }

        @Override
        public int size() {
            final DirectoryScanner ds = getDirectoryScanner();
            int count = type == SetType.file
                ? ds.getIncludedFilesCount()
                : ds.getIncludedDirsCount();
            if (type == SetType.both) {
                count += ds.getIncludedFilesCount();
            }
            return count;
        }
    }

}
