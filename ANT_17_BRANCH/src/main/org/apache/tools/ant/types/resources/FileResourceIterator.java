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
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator of FileResources from filenames.
 * @since Ant 1.7
 */
public class FileResourceIterator implements Iterator {
    private File basedir;
    private String[] files;
    private int pos = 0;

    /**
     * Construct a new FileResourceIterator.
     */
    public FileResourceIterator() {
    }

    /**
     * Construct a new FileResourceIterator relative to the specified
     * base directory.
     * @param f the base directory of this instance.
     */
    public FileResourceIterator(File f) {
        basedir = f;
    }

    /**
     * Construct a new FileResourceIterator over the specified filenames,
     * relative to the specified base directory.
     * @param f the base directory of this instance.
     * @param s the String[] of filenames.
     */
    public FileResourceIterator(File f, String[] s) {
        this(f);
        addFiles(s);
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
    public boolean hasNext() {
        return pos < files.length;
    }

    /**
     * Get the next element from this FileResourceIterator.
     * @return the next Object.
     */
    public Object next() {
        return nextResource();
    }

    /**
     * Not implemented.
     */
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
        return new FileResource(basedir, files[pos++]);
    }

}
