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

package org.apache.tools.ant.types.selectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.PermissionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * A selector that selects files based on their POSIX permissions.
 *
 * <p>Permissions are defined in terms of {@link
 * java.nio.file.Files#getPosixFilePermissions}, this means the selector will accept
 * any file that exists and has given POSIX permissions.</p>
 *
 * @since Ant 1.10.4
 */
public class PosixPermissionsSelector implements FileSelector {

    private String permissions;

    private boolean followSymlinks = true;

    /**
     * Sets the permissions to look for.
     * @param permissions the permissions string (rwxrwxrwx or octal)
     */
    public void setPermissions(String permissions) {
        if (permissions.length() == 3 && permissions.matches("^[0-7]+$")) {
            this.permissions = PosixFilePermissions.toString(
                    PermissionUtils.permissionsFromMode(Integer.parseInt(permissions, 8)));
            return;
        }

        try {
            this.permissions = PosixFilePermissions.toString(PosixFilePermissions.fromString(permissions));
        } catch (IllegalArgumentException ex) {
            throw new BuildException("the permissions attribute " + permissions
                    + " is invalid", ex);
        }
    }

    /**
     * Sets the "follow symbolic links" flag.
     * @param followSymlinks whether or not symbolic links should be followed.
     */
    public void setFollowSymlinks(boolean followSymlinks) {
        this.followSymlinks = followSymlinks;
    }

    @Override
    public boolean isSelected(File basedir, String filename, File file) {
        if (permissions == null) {
            throw new BuildException("the permissions attribute is required");
        }
        try {
            return PosixFilePermissions.toString(followSymlinks
                    ? Files.getPosixFilePermissions(file.toPath())
                    : Files.getPosixFilePermissions(file.toPath(), LinkOption.NOFOLLOW_LINKS))
                    .equals(permissions);
        } catch (IOException e) {
            // => not the expected permissions
        }
        return false;
    }
}
