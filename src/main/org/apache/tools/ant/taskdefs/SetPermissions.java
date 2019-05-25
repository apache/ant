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

package org.apache.tools.ant.taskdefs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
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
 * in less platform dependent way.</p>
 *
 * <p>It requires a file system that supports PosixFilePermissions for
 * its full potential. It can optionally fall back to
 * DosFilePermissions (only changing the readonly state) on file
 * systems that don't support POSIX permissions. See {@link
 * SetPermissions.NonPosixMode}</p>
 *
 * @since Ant 1.10.0
 */
public class SetPermissions extends Task {
    private final Set<PosixFilePermission> permissions =
        EnumSet.noneOf(PosixFilePermission.class);
    private Resources resources = null;
    private boolean failonerror = true;
    private NonPosixMode nonPosixMode = NonPosixMode.fail;

    /**
     * Options for dealing with file systems that don't support POSIX
     * permissions.
     */
    public enum NonPosixMode {
        /** Fail the build. */
        fail,
        /** Log an error and go on. */
        pass,
        /**
         * Try DosFilePermissions - setting the read-only flag - and
         * fail the build if that fails as well.
         */
        tryDosOrFail,
        /**
         * Try DosFilePermissions - setting the read-only flag - and
         * log an error and go on if that fails as well.
         */
        tryDosOrPass
    }

    /**
     * Adds permissions as a comma separated list.
     * @param perms comma separated list of names of {@link PosixFilePermission}s.
     */
    public void setPermissions(String perms) {
        if (perms != null) {
            Arrays.stream(perms.split(",")) //NOSONAR
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
     * <p>Only applies to IO and SecurityExceptions, see {@link
     * #setNonPosixMode} for ways to deal with file-systems that don't
     * support PosixPermissions.</p>
     * @param failonerror true or false.
     */
    public void setFailOnError(final boolean failonerror) {
        this.failonerror = failonerror;
    }

    /**
     * Set what to do if changing the permissions of a file is not
     * possible because the file-system doesn't support POSIX file
     * permissions.
     * <p>The default is {@link NonPosixMode#fail}.</p>
     * @param m what to do if changing the permissions of a file is not possible
     */
    public void setNonPosixMode(NonPosixMode m) {
        this.nonPosixMode = m;
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

    @Override
    public void execute() {
        if (resources == null) {
            throw new BuildException("At least one resource-collection is required");
        }
        Resource currentResource = null;
        try {
            for (Resource r : resources) {
                currentResource = r;
                try {
                    PermissionUtils.setPermissions(r, permissions, this::posixPermissionsNotSupported);
                } catch (IOException ioe) {
                    maybeThrowException(ioe, "Failed to set permissions on '%s' due to %s", r, ioe.getMessage());
                }
            }
        } catch (ClassCastException cce) {
            maybeThrowException(null,
                "some specified permissions are not of type PosixFilePermission: %s",
                StringUtils.join(permissions, ", "));
        } catch (SecurityException se) {
            maybeThrowException(null,
                "the SecurityManager denies role accessUserInformation or write access for SecurityManager.checkWrite for resource '%s'",
                currentResource);
        } catch (BuildException be) {
            // maybe thrown by callback method this::posixPermissionsNotSupported.
            maybeThrowException(be, be.getMessage());
        }
    }

    private void maybeThrowException(Exception exc, String msgFormat, Object... msgArgs) {
        String msg = String.format(msgFormat, msgArgs);
        if (failonerror) {
            if (exc instanceof BuildException) {
                throw (BuildException) exc;
            }
            throw new BuildException(msg, exc);
        }
        log("Warning: " + msg, Project.MSG_ERR);
    }

    private void posixPermissionsNotSupported(Path p) {
        String msg = String.format(
            "the associated path '%s' does not support the PosixFileAttributeView",
            p);
        switch (nonPosixMode) {
        case fail:
            throw new BuildException(msg);
        case pass:
            log("Warning: " + msg, Project.MSG_ERR);
            break;
        case tryDosOrFail:
            tryDos(p, true);
            break;
        case tryDosOrPass:
            tryDos(p, false);
            break;
        }
    }

    private void tryDos(Path p, boolean failIfDosIsNotSupported) {
        log("Falling back to DosFileAttributeView");
        boolean readOnly = !isWritable();
        DosFileAttributeView view = Files.getFileAttributeView(p, DosFileAttributeView.class);
        if (view != null) {
            try {
                view.setReadOnly(readOnly);
            } catch (IOException ioe) {
                maybeThrowException(ioe, "Failed to set permissions on '%s' due to %s",
                                    p, ioe.getMessage());
            } catch (SecurityException uoe) {
                maybeThrowException(null,
                    "the SecurityManager denies role accessUserInformation or write access for SecurityManager.checkWrite for resource '%s'",
                    p);
            }
        } else {
            String msg = String.format(
                "the associated path '%s' does not support the DosFileAttributeView",
                p);
            if (failIfDosIsNotSupported) {
                throw new BuildException(msg);
            }
            log("Warning: " + msg, Project.MSG_ERR);
        }
    }

    private boolean isWritable() {
        return permissions.contains(PosixFilePermission.OWNER_WRITE)
            || permissions.contains(PosixFilePermission.GROUP_WRITE)
            || permissions.contains(PosixFilePermission.OTHERS_WRITE);
    }
}
