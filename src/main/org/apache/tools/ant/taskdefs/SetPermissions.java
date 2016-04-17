/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs;

import java.io.IOException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.util.PermissionUtils;

/**
 * Sets {@link PosixFilePermission}s for resources.
 *
 * <p>This task provides a subset of {@link Chmod}'s and {@link
 * org.apache.tools.ant.taskdefs.optional.windows.Attrib}'s abilities
 * in a platform independent way.</p>
 *
 * @since Ant 1.10.0
 */
public class SetPermissions extends Task {
    private final Set<PosixFilePermission> permissions =
        EnumSet.noneOf(PosixFilePermission.class);
    private Resources resources = null;
    private boolean failonerror = true;

    /**
     * Adds permissions as a comma separated list.
     * @param perms comma separated list of names of {@link PosixFilePermission}s.
     */
    public void setPermissions(String perms) {
        if (perms != null) {
            Arrays.stream(perms.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> Enum.valueOf(PosixFilePermission.class, s))
                .forEach(permissions::add);
        }
    }

    /**
     * A 3 digit octal string, specify the user, group and
     * other modes in the standard Unix fashion;
     * @param octalString a <code>String</code> value
     */
    public void setMode(String octalString) {
        int mode = Integer.parseInt(octalString, 8);
        permissions.addAll(PermissionUtils.permissionsFromMode(mode));
    }

    /**
     * Set whether to fail when errors are encountered. If false, note errors
     * to the output but keep going. Default is true.
     * @param failonerror true or false.
     */
    public void setFailOnError(final boolean failonerror) {
        this.failonerror = failonerror;
    }

    /**
     * Adds a collection of resources to set permissions on.
     * @param rc a resource collection
     */
    public void add(ResourceCollection rc) {
        if (resources == null) {
            resources = new Resources();
        }
        resources.add(rc);
    }

    public void execute() {
        if (resources == null) {
            throw new BuildException("At least one resource-collection is required");
        }
        for (Resource r : resources) {
            try {
                PermissionUtils.setPermissions(r, permissions);
            } catch (IOException ioe) {
                String msg = "Failed to set permissions on " + r + " due to "
                    + ioe.getMessage();
                if (failonerror) {
                    throw new BuildException(msg, ioe);
                } else {
                    log("Warning: " + msg, Project.MSG_ERR);
                }
            }
        }
    }
}
