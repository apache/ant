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

/**
 * Holds the information of a line of rdiff
 */
class CvsTagEntry {
    String m_filename;
    String m_prevRevision;
    String m_revision;

    public CvsTagEntry(String filename) {
        this(filename, null, null);
    }

    public CvsTagEntry(String filename, String revision) {
        this(filename, revision, null);
    }

    public CvsTagEntry(String filename, String revision,
                       String prevRevision) {
        m_filename = filename;
        m_revision = revision;
        m_prevRevision = prevRevision;
    }

    public String getFile() {
        return m_filename;
    }

    public String getRevision() {
        return m_revision;
    }

    public String getPreviousRevision() {
        return m_prevRevision;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(m_filename);
        if ((m_revision == null)) {
            buffer.append(" was removed");
            if (m_prevRevision != null) {
                buffer.append("; previous revision was ").append(m_prevRevision);
            }
        } else if (m_revision != null && m_prevRevision == null) {
            buffer.append(" is new; current revision is ")
                .append(m_revision);
        } else if (m_revision != null && m_prevRevision != null) {
            buffer.append(" has changed from ")
                .append(m_prevRevision).append(" to ").append(m_revision);
        }
        return buffer.toString();
    }
}
