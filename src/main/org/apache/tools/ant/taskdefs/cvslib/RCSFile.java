/*
 * Copyright  2002-2004 The Apache Software Foundation
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
 * Represents a RCS File change.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @version $Revision$ $Date$
 */
class RCSFile {
    private String m_name;
    private String m_revision;
    private String m_previousRevision;


    RCSFile(final String name, final String rev) {
        this(name, rev, null);
    }


    RCSFile(final String name,
                  final String revision,
                  final String previousRevision) {
        m_name = name;
        m_revision = revision;
        if (!revision.equals(previousRevision)) {
            m_previousRevision = previousRevision;
        }
    }


    String getName() {
        return m_name;
    }


    String getRevision() {
        return m_revision;
    }


    String getPreviousRevision() {
        return m_previousRevision;
    }
}

