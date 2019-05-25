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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OwnedBySelectorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final File TEST_FILE = new File("/etc/passwd");

    private final String SELF = System.getProperty("user.name");

    private final String ROOT_USER = "root";

    private OwnedBySelector s;

    @Before
    public void setUp() {
        // at least on Jenkins the file is owned by "BUILTIN\Administrators"
        assumeFalse(Os.isFamily("windows"));

        s = new OwnedBySelector();
    }

    @Test
    public void ownedByIsTrueForSelf() throws Exception {
        File file = folder.newFile("f.txt");
        UserPrincipal user = Files.getOwner(file.toPath());
        assertEquals(SELF, user.getName());

        s.setOwner(SELF);
        assertTrue(s.isSelected(null, null, file));
    }

    @Test
    public void ownedByFollowSymlinks() throws IOException {
        File target = new File(folder.getRoot(), "link");
        Path symbolicLink = Files.createSymbolicLink(target.toPath(), TEST_FILE.toPath());

        UserPrincipal root = Files.getOwner(symbolicLink);
        assertEquals(ROOT_USER, root.getName());

        UserPrincipal user = Files.getOwner(symbolicLink, LinkOption.NOFOLLOW_LINKS);
        assertEquals(SELF, user.getName());

        s.setOwner(SELF);
        assertFalse(s.isSelected(null, null, symbolicLink.toFile()));
        s.setFollowSymlinks(false);
        assertTrue(s.isSelected(null, null, symbolicLink.toFile()));
    }
}
