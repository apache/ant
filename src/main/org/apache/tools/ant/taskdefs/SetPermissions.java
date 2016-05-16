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
import org.apache.tools.ant.util.StringUtils;

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
        Resource currentResource = null;
        try {
	        for (Resource r : resources) {
	        	currentResource = r;
	            try {
	                PermissionUtils.setPermissions(r, permissions, null);
	            } catch (IOException ioe) {
	                maybeThrowException(ioe, "Failed to set permissions on '%s' due to %s", r, ioe.getMessage());
	            }
	        }
        } catch (UnsupportedOperationException uoe) {
        	maybeThrowException(null, "the associated file system of resource '%s' does not support the PosixFileAttributeView", currentResource);
        } catch (ClassCastException uoe) {
        	maybeThrowException(null, "some specified permissions are not of type PosixFilePermission: %s", StringUtils.join(permissions, ", "));
        } catch (SecurityException uoe) {
        	maybeThrowException(null, "the SecurityManager denies role accessUserInformation or write access for SecurityManager.checkWrite for resource '%s'", currentResource);
        }
    }

	private void maybeThrowException(Exception ioe, String msgFormat, Object... msgArgs) {
		String msg = String.format(msgFormat, msgArgs);
		if (failonerror) {
		    throw new BuildException(msg, ioe);
		} else {
		    log("Warning: " + msg, Project.MSG_ERR);
		}
	}
}
