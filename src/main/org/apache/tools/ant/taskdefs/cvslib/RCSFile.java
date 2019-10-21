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

/**
 * Represents a RCS File change.
 *
 */
class RCSFile {
    private String name;
    private String revision;
    private String previousRevision;

    RCSFile(final String name, final String revision) {
        this(name, revision, null);
    }

    RCSFile(final String name,
                  final String revision,
                  final String previousRevision) {
        this.name = name;
        this.revision = revision;
        if (!revision.equals(previousRevision)) {
            this.previousRevision = previousRevision;
        }
    }

    /**
     * Gets the name of the RCSFile
     * @return name of the file
     */
    String getName() {
        return name;
    }

    /**
     * Gets the revision number of the RCSFile
     * @return the revision number (as String)
     */
    String getRevision() {
        return revision;
    }

    /**
     * Gets the previous revision of the RCSFile
     * @return the previous revision number (as String)
     */
    String getPreviousRevision() {
        return previousRevision;
    }
}
