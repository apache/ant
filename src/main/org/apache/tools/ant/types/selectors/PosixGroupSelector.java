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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;

/**
 * A selector that selects files based on their POSIX group.
 *
 * <p>Group is defined in terms of {@link java.nio.file.Files#readAttributes}
 * group attribute as provided by {@link java.nio.file.attribute.PosixFileAttributes},
 * this means the selector will accept any file that exists and has the given
 * group attribute.</p>
 *
 * @since Ant 1.10.4
 */
public class PosixGroupSelector implements FileSelector {

    private String group;

    private boolean followSymlinks = true;

    /**
     * Sets the group name to look for.
     * @param group the group name
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Sets the "follow symbolic links" option.
     * @param followSymlinks whether or not symbolic links should be followed.
     */
    public void setFollowSymlinks(boolean followSymlinks) {
        this.followSymlinks = followSymlinks;
    }

    @Override
    public boolean isSelected(File basedir, String filename, File file) {
        if (group == null) {
            throw new BuildException("the group attribute is required");
        }
        try {
            GroupPrincipal actualGroup = followSymlinks ? Files.readAttributes(file.toPath(),
                    PosixFileAttributes.class).group() : Files.readAttributes(file.toPath(),
                    PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group();
            return actualGroup != null && actualGroup.getName().equals(group);
        } catch (IOException e) {
            // => not the expected group
        }
        return false;
    }
}
