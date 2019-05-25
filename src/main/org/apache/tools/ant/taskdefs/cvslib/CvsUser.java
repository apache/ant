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

import org.apache.tools.ant.BuildException;

/**
 * Represents a CVS user with a userID and a full name.
 *
 */
public class CvsUser {
    /** The user's Id */
    private String userID;

    /** The user's full name */
    private String displayName;

    /**
     * Set the user's fullname
     *
     * @param displayName the user's full name
     */
    public void setDisplayname(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Set the user's id
     *
     * @param userID the user's new id value.
     */
    public void setUserid(final String userID) {
        this.userID = userID;
    }

    /**
     * Get the user's id.
     *
     * @return The userID value
     */
    public String getUserID() {
        return userID;
    }

    /**
     * Get the user's full name
     *
     * @return the user's full name
     */
    public String getDisplayname() {
        return displayName;
    }

    /**
     * Validate that this object is configured.
     *
     * @exception BuildException if the instance has not be correctly
     *            configured.
     */
    public void validate() throws BuildException {
        if (null == userID) {
            throw new BuildException("Username attribute must be set.");
        }
        if (null == displayName) {
            throw new BuildException(
                "Displayname attribute must be set for userID %s", userID);
        }
    }
}
