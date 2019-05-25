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

package org.apache.tools.ant.taskdefs.optional.ssh;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * A helper object for Scp representing a directory in a file system.
 */
public class Directory {

    private File directory;
    private Set<Directory> childDirectories;
    private List<File> files;
    private Directory parent;

    /**
     * Constructor for a Directory.
     * @param directory a directory.
     */
    public Directory(File directory) {
        this(directory,  null);
    }

    /**
     * Constructor for a Directory.
     * @param directory a directory
     * @param parent    a parent Directory
     */
    public Directory(File directory, Directory parent) {
        this.parent = parent;
        this.childDirectories = new LinkedHashSet<>();
        this.files = new ArrayList<>();
        this.directory = directory;
    }

    /**
     * Add a directory to the child directories.
     * @param directory a Directory
     */
    public void addDirectory(Directory directory) {
        childDirectories.add(directory);
    }

    /**
     * Add a file to the list of files.
     * @param file a file to add
     */
    public void addFile(File file) {
        files.add(file);
    }

    /**
     * Get an iterator over the child Directories.
     * @return an iterator
     */
    public Iterator<Directory> directoryIterator() {
        return childDirectories.iterator();
    }

    /**
     * Get an iterator over the files.
     * @return an iterator
     */
    public Iterator<File> filesIterator() {
        return files.iterator();
    }

    /**
     * Get the parent Directory.
     * @return the parent Directory.
     */
    public Directory getParent() {
        return parent;
    }

    /**
     * Is this a root Directory?
     * @return true if there is no parent Directory
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Get the directory file.
     * @return the directory file
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Get a child directory of this directory.
     * @param dir the directory to look for
     * @return the child directory, or null if not found
     */
    public Directory getChild(File dir) {
        for (Directory current : childDirectories) {
            if (current.getDirectory().equals(dir)) {
                return current;
            }
        }
        return null;
    }

    /**
     * The equality method.
     * This checks if the directory field is the same.
     * @param obj the object to compare to
     * @return true if this object has an equal directory field as the other object
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Directory) {
            Directory d = (Directory) obj;
            return this.directory.equals(d.directory);
        }

        return false;
    }

    /**
     * The hashcode method.
     * @return the hash code of the directory field
     */
    @Override
    public int hashCode() {
        return directory.hashCode();
    }

    /**
     * Get the path components of this directory.
     * @return the path components as an array of strings.
     */
    public String[] getPath() {
        return getPath(directory.getAbsolutePath());
    }

    /**
     * Convert a file path to an array of path components.
     * This uses File.separator to split the file path string.
     * @param thePath the file path string to convert
     * @return an array of path components
     */
    public static String[] getPath(String thePath) {
        StringTokenizer tokenizer = new StringTokenizer(thePath,
                File.separator);
        String[] path = new String[tokenizer.countTokens()];

        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            path[i] = tokenizer.nextToken();
            i++;
        }

        return path;
    }

    /**
     * Get the number of files in the files attribute.
     * @return the number of files
     */
    public int fileSize() {
        return files.size();
    }
}
