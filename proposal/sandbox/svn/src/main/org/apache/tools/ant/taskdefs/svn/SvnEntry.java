/*
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.svn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class SvnEntry {
    private final Date date;
    private final String revision;
    private String author;
    private final String message;
    private final ArrayList paths = new ArrayList();

    /**
     * Creates a new instance of a SvnEntry
     * @param date the date
     * @param author the author
     * @param message a message to be added to the revision
     */
    public SvnEntry(final Date date, final String revision, 
                    final String author, final String message) {
        this(date, revision, author, message, Collections.EMPTY_LIST);
    }

    /**
     * Creates a new instance of a SvnEntry
     * @param date the date
     * @param author the author
     * @param message a message to be added to the revision
     */
    public SvnEntry(final Date date, final String revision, 
                    final String author, final String message,
                    final Collection paths) {
        this.date = date;
        this.revision = revision;
        this.author = author;
        this.message = message;
        this.paths.addAll(paths);
    }

    /**
     * Adds a path to the SvnEntry
     * @param path the path to add
     * @param revision the revision
     */
    public void addPath(final String name) {
        paths.add(name);
    }

    /**
     * Gets the date of the SvnEntry
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Gets the revision of the SvnEntry
     * @return the date
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Sets the author of the SvnEntry
     * @param author the author
     */
    public void setAuthor(final String author) {
        this.author = author;
    }

    /**
     * Gets the author of the SvnEntry
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Gets the message for the SvnEntry
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the paths in this SvnEntry
     * @return the files
     */
    public String[] getPaths() {
        return (String[]) paths.toArray(new String[paths.size()]);
    }

}
