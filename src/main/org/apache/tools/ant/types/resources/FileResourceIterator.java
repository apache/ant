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
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;

/**
 * Iterator of FileResources from filenames.
 * @since Ant 1.7
 */
public class FileResourceIterator implements Iterator<Resource> {
    private Project project;
    private File basedir;
    private String[] files;
    private int pos = 0;

    /**
     * Construct a new FileResourceIterator.
     * @deprecated in favor of {@link FileResourceIterator#FileResourceIterator(Project)}
     */
    @Deprecated
    public FileResourceIterator() {
    }

    /**
     * Create a new FileResourceIterator.
     * @param project associated Project instance
     * @since Ant 1.8
     */
    public FileResourceIterator(Project project) {
        this.project = project;
    }

    /**
     * Construct a new FileResourceIterator relative to the specified
     * base directory.
     * @param basedir the base directory of this instance.
     * @deprecated in favor of {@link FileResourceIterator#FileResourceIterator(Project, File)}
     */
    @Deprecated
    public FileResourceIterator(File basedir) {
        this(null, basedir);
    }

    /**
     * Construct a new FileResourceIterator relative to the specified
     * base directory.
     * @param project associated Project instance
     * @param basedir the base directory of this instance.
     * @since Ant 1.8
     */
    public FileResourceIterator(Project project, File basedir) {
        this(project);
        this.basedir = basedir;
    }

    /**
     * Construct a new FileResourceIterator over the specified filenames,
     * relative to the specified base directory.
     * @param basedir the base directory of this instance.
     * @param filenames the String[] of filenames.
     * @deprecated in favor of {@link FileResourceIterator#FileResourceIterator(Project, File, String[])}
     */
    @Deprecated
    public FileResourceIterator(File basedir, String[] filenames) {
        this(null, basedir, filenames);
    }

    /**
     * Construct a new FileResourceIterator over the specified filenames,
     * relative to the specified base directory.
     * @param project associated Project instance
     * @param basedir the base directory of this instance.
     * @param filenames the String[] of filenames.
     * @since Ant 1.8
     */
    public FileResourceIterator(Project project, File basedir, String[] filenames) {
        this(project, basedir);
        addFiles(filenames);
    }

    /**
     * Add an array of filenames to this FileResourceIterator.
     * @param s the filenames to add.
     */
    public void addFiles(String[] s) {
        int start = (files == null) ? 0 : files.length;
        String[] newfiles = new String[start + s.length];
        if (start > 0) {
            System.arraycopy(files, 0, newfiles, 0, start);
        }
        files = newfiles;
        System.arraycopy(s, 0, files, start, s.length);
    }

    /**
     * Find out whether this FileResourceIterator has more elements.
     * @return whether there are more Resources to iterate over.
     */
    @Override
    public boolean hasNext() {
        return pos < files.length;
    }

    /**
     * Get the next element from this FileResourceIterator.
     * @return the next Object.
     */
    @Override
    public Resource next() {
        return nextResource();
    }

    /**
     * Not implemented.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Convenience method to return the next resource.
     * @return the next File.
     */
    public FileResource nextResource() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        FileResource result = new FileResource(basedir, files[pos++]);
        result.setProject(project);
        return result;
    }

}
