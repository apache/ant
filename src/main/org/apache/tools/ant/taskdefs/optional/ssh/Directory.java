/* 
 * Copyright  2003-2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.ssh;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.File;

public class Directory {

    private File directory;
    private ArrayList childDirectories;
    private ArrayList files;
    private Directory parent;

    public Directory(File directory) {
        this(directory,  null);
    }

    public Directory(File directory , Directory parent) {
        this.parent = parent;
        this.childDirectories = new ArrayList();
        this.files = new ArrayList();
        this.directory = directory;
    }

    public void addDirectory(Directory directory) {
        if (!childDirectories.contains(directory)) {
            childDirectories.add(directory);
        }
    }

    public void addFile(File file) {
        files.add(file);
    }

    public Iterator directoryIterator() {
        return childDirectories.iterator();
    }

    public Iterator filesIterator() {
        return files.iterator();
    }

    public Directory getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public File getDirectory() {
        return directory;
    }

    public Directory getChild(File dir) {
        for (int i = 0; i < childDirectories.size(); i++) {
            Directory current = (Directory) childDirectories.get(i);
            if (current.getDirectory().equals(dir)) {
                return current;
            }
        }

        return null;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Directory)) {
            return false;
        }

        Directory d = (Directory) obj;

        return this.directory.equals(d.directory);
    }

    public int hashCode() {
        return directory.hashCode();
    }

    public String[] getPath() {
        return getPath(directory.getAbsolutePath());
    }

    public static String[] getPath(String thePath) {
        StringTokenizer tokenizer = new StringTokenizer(thePath,
                File.separator);
        String[] path = new String[ tokenizer.countTokens() ];

        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            path[i] = tokenizer.nextToken();
            i++;
        }

        return path;
    }

    public int fileSize() {
        return files.size();
    }
}
