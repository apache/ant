/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
