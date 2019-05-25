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
package org.apache.tools.ant.taskdefs.cvslib;

import java.util.Date;
import java.util.Vector;

/**
 * CVS Entry.
 *
 */
public class CVSEntry {
    private Date date;
    private String author;
    private final String comment;
    private final Vector<RCSFile> files = new Vector<>();

    /**
     * Creates a new instance of a CVSEntry
     * @param date the date
     * @param author the author
     * @param comment a comment to be added to the revision
     */
    public CVSEntry(final Date date, final String author, final String comment) {
        this.date = date;
        this.author = author;
        this.comment = comment;
    }

    /**
     * Adds a file to the CVSEntry
     * @param file the file to add
     * @param revision the revision
     */
    public void addFile(final String file, final String revision) {
        files.add(new RCSFile(file, revision));
    }

    /**
     * Adds a file to the CVSEntry
     * @param file the file to add
     * @param revision the revision
     * @param previousRevision the previous revision
     */
    public void addFile(final String file, final String revision, final String previousRevision) {
        files.add(new RCSFile(file, revision, previousRevision));
    }

    /**
     * Gets the date of the CVSEntry
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Sets the author of the CVSEntry
     * @param author the author
     */
    public void setAuthor(final String author) {
        this.author = author;
    }

    /**
     * Gets the author of the CVSEntry
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Gets the comment for the CVSEntry
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * Gets the files in this CVSEntry
     * @return the files
     */
    public Vector<RCSFile> getFiles() {
        return files;
    }

    /**
     * Gets a String containing author, date, files and comment
     * @return a string representation of this CVSEntry
     */
    @Override
    public String toString() {
        return getAuthor() + "\n" + getDate() + "\n" + getFiles() + "\n"
            + getComment();
    }
}
