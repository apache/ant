/*
 * Copyright  2002,2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.cvslib;

import java.util.Date;
import java.util.Vector;

/**
 * CVS Entry.
 *
 * @version $Revision$ $Date$
 */
class CVSEntry {
    private Date m_date;
    private String m_author;
    private final String m_comment;
    private final Vector m_files = new Vector();

    public CVSEntry(Date date, String author, String comment) {
        m_date = date;
        m_author = author;
        m_comment = comment;
    }

    public void addFile(String file, String revision) {
        m_files.addElement(new RCSFile(file, revision));
    }

    public void addFile(String file, String revision, String previousRevision) {
        m_files.addElement(new RCSFile(file, revision, previousRevision));
    }

    Date getDate() {
        return m_date;
    }

    void setAuthor(final String author) {
        m_author = author;
    }

    String getAuthor() {
        return m_author;
    }

    String getComment() {
        return m_comment;
    }

    Vector getFiles() {
        return m_files;
    }

    public String toString() {
        return getAuthor() + "\n" + getDate() + "\n" + getFiles() + "\n"
            + getComment();
    }
}
