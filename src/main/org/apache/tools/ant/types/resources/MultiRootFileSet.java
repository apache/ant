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
    private List<File> baseDirs = new ArrayList<File>();
    private Union union;

    public void setDir(File dir) {
        throw new BuildException(getDataTypeName()
                                 + " doesn't support the dir attribute");
    }

    /**
     * Determines the types of resources to return.
     * @param type the types of resources to return
     */
    public void setType(SetType type) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.type = type;
    }

    /**
     * Set whether to cache collections.
     * @param b boolean cache flag.
     */
    public synchronized void setCache(boolean b) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        cache = b;
    }

    /**
     * Adds basedirs as a comman separated list.
     * @param b boolean cache flag.
     */
    public void setBaseDirs(String dirs) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (dirs != null && dirs.length() > 0) {
            String[] ds = dirs.split(",");
            for (String d : ds) {
                baseDirs.add(getProject().resolveFile(d));
            }
        }
    }

    /**
     * Adds a basedir as nested element.
     */
    public void addConfiguredBaseDir(FileResource r) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        baseDirs.add(r.getFile());
    }

    public void setRefid(Reference r) {
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
    public Object clone() {
        if (isReference()) {
            return ((MultiRootFileSet) getRef(getProject())).clone();
        } else {
            MultiRootFileSet fs = (MultiRootFileSet) super.clone();
            fs.baseDirs = new ArrayList<File>(baseDirs);
            fs.union = null;
            return fs;
        }
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return an Iterator of Resources.
     */
    public Iterator<Resource> iterator() {
        if (isReference()) {
            return ((MultiRootFileSet) getRef(getProject())).iterator();
        }
        return merge().iterator();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     */
    public int size() {
        if (isReference()) {
            return ((MultiRootFileSet) getRef(getProject())).size();
        }
        return merge().size();
    }

    /**
     * Always returns true.
     * @return true indicating that all elements will be FileResources.
     */
    public boolean isFilesystemOnly() {
        return true;
    }

    /**
     * Returns included directories as a list of semicolon-separated paths.
     *
     * @return a <code>String</code> of included directories.
     */
    public String toString() {
        if (isReference()) {
            return ((MultiRootFileSet) getRef(getProject())).toString();
        }
        return merge().toString();
    }

    private synchronized Union merge() {
        if (cache && union != null) {
            return union;
        }
        Union u = new Union();
        setup(u);
        if (cache) {
            union = u;
        }
        return u;
    }

    private void setup(Union u) {
        for (File d : baseDirs) {
            u.add(new Worker(this, type, d));
        }
    }

    /**
     * What to return from the set: files, directories or both.
     */
    public static enum SetType {
        file, dir, both
    }

    private static class Worker extends AbstractFileSet
        implements ResourceCollection {

        private final SetType type;
        
        private Worker(MultiRootFileSet fs, SetType type, File dir) {
            super(fs);
            this.type = type;
            setDir(dir);
        }

        public boolean isFilesystemOnly() {
            return true;
        }

        public Iterator<Resource> iterator() {
            DirectoryScanner ds = getDirectoryScanner(getProject());
            String[] names = type == SetType.file
                ? ds.getIncludedFiles()
                : ds.getIncludedDirectories();
            if (type == SetType.both) {
                String[] files = ds.getIncludedFiles();
                String[] merged = new String[names.length + files.length];
                System.arraycopy(names, 0, merged, 0, names.length);
                System.arraycopy(files, 0, merged, names.length, files.length);
                names = merged;
            }
            return new FileResourceIterator(getProject(), getDir(getProject()),
                                            names);
        }

        public int size() {
            DirectoryScanner ds = getDirectoryScanner(getProject());
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
