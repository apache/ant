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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.UserPrincipal;

import org.apache.tools.ant.BuildException;

/**
 * A selector that selects files based on their owner.
 *
 * <p>Owner is defined in terms of {@link
 * java.nio.file.Files#getOwner}, this means the selector will accept
 * any file that exists and is owned by the given user. If the {@code
 * getOwner} method throws an {@code UnsupportedOperationException}
 * the file in question is not included.</p>
 *
 * @since Ant 1.10.0
 */
public class OwnedBySelector implements FileSelector {

    private String owner;

    private boolean followSymlinks = true;

    /**
     * Sets the user name to look for.
     * @param owner the user name
     */
    public void setOwner(String owner) {
        this.owner = owner;
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
        if (owner == null) {
            throw new BuildException("the owner attribute is required");
        }
        if (file != null) {
            try {
                UserPrincipal user = followSymlinks ? Files.getOwner(file.toPath())
                        : Files.getOwner(file.toPath(), LinkOption.NOFOLLOW_LINKS);
                return user != null && owner.equals(user.getName());
            } catch (UnsupportedOperationException | IOException ex) {
                // => not the expected owner
            }
        }
        return false;
    }

}
