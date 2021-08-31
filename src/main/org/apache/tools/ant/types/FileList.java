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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.FileResourceIterator;

/**
 * FileList represents an explicitly named list of files.  FileLists
 * are useful when you want to capture a list of files regardless of
 * whether they currently exist.  By contrast, FileSet operates as a
 * filter, only returning the name of a matched file if it currently
 * exists in the file system.
 */
public class FileList extends DataType implements ResourceCollection {

    private List<String> filenames = new ArrayList<>();
    private File dir;

    /**
     * The default constructor.
     *
     */
    public FileList() {
        super();
    }

    /**
     * A copy constructor.
     *
     * @param filelist a <code>FileList</code> value
     */
    protected FileList(FileList filelist) {
        this.dir       = filelist.dir;
        this.filenames = filelist.filenames;
        setProject(filelist.getProject());
    }

    /**
     * Makes this instance in effect a reference to another FileList
     * instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     * @param r the reference to another filelist.
     * @exception BuildException if an error occurs.
     */
    @Override
    public void setRefid(Reference r) throws BuildException {
        if (dir != null || !filenames.isEmpty()) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Set the dir attribute.
     *
     * @param dir the directory this filelist is relative to.
     * @exception BuildException if an error occurs
     */
    public void setDir(File dir) throws BuildException {
        checkAttributesAllowed();
        this.dir = dir;
    }

    /**
     * @param p the current project
     * @return the directory attribute
     */
    public File getDir(Project p) {
        if (isReference()) {
            return getRef(p).getDir(p);
        }
        return dir;
    }

    /**
     * Set the filenames attribute.
     *
     * @param filenames a string containing filenames, separated by comma or
     *        by whitespace.
     */
    public void setFiles(String filenames) {
        checkAttributesAllowed();
        if (filenames != null && !filenames.isEmpty()) {
            StringTokenizer tok = new StringTokenizer(
                filenames, ", \t\n\r\f", false);
            while (tok.hasMoreTokens()) {
               this.filenames.add(tok.nextToken());
            }
        }
    }

    /**
     * Returns the list of files represented by this FileList.
     * @param p the current project
     * @return the list of files represented by this FileList.
     */
    public String[] getFiles(Project p) {
        if (isReference()) {
            return getRef(p).getFiles(p);
        }

        if (dir == null) {
            throw new BuildException("No directory specified for filelist.");
        }

        if (filenames.isEmpty()) {
            throw new BuildException("No files specified for filelist.");
        }

        return filenames.toArray(new String[0]);
    }

    /**
     * Inner class corresponding to the &lt;file&gt; nested element.
     */
    public static class FileName {
        private String name;

        /**
         * The name attribute of the file element.
         *
         * @param name the name of a file to add to the file list.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the name of the file for this element.
         */
        public String getName() {
            return name;
        }
    }

    /**
     * Add a nested &lt;file&gt; nested element.
     *
     * @param name a configured file element with a name.
     * @since Ant 1.6.2
     */
    public void addConfiguredFile(FileName name) {
        if (name.getName() == null) {
            throw new BuildException(
                "No name specified in nested file element");
        }
        filenames.add(name.getName());
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
        return new FileResourceIterator(getProject(), dir,
            filenames.toArray(new String[0]));
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
        return filenames.size();
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

    private FileList getRef() {
        return getCheckedRef(FileList.class);
    }

    private FileList getRef(Project p) {
        return getCheckedRef(FileList.class, getDataTypeName(), p);
    }

}
