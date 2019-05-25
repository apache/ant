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
import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Enclosed.class)
public class PosixPermissionsSelectorTest {

    @RunWith(Parameterized.class)
    public static class IllegalArgumentTest {

        private PosixPermissionsSelector s;

        // requires JUnit 4.12
        @Parameterized.Parameters(name = "illegal argument: |{0}|")
        public static Collection<String> data() {
            return Arrays.asList("855", "4555", "-rwxr-xr-x", "xrwr-xr-x");
        }

        @Parameterized.Parameter
        public String argument;

        @Before
        public void setUp() {
            assumeTrue("Not POSIX", Os.isFamily("unix"));
            s = new PosixPermissionsSelector();
        }

        @Test(expected = BuildException.class)
        public void test() {
            s.setPermissions(argument);
        }
    }

    @RunWith(Parameterized.class)
    public static class LegalArgumentTest {

        private PosixPermissionsSelector s;

        @Rule
        public TemporaryFolder folder = new TemporaryFolder();

        // requires JUnit 4.12
        @Parameterized.Parameters(name = "legal argument (self): |{0}|")
        public static Collection<String> data() {
            return Arrays.asList("750", "rwxr-x---");
        }

        @Parameterized.Parameter
        public String argument;

        @Before
        public void setUp() {
            assumeTrue("Not POSIX", Os.isFamily("unix"));
            s = new PosixPermissionsSelector();
        }

        @Test
        public void test() throws Exception {
            // do not depend on default umask
            File subFolder = folder.newFolder();
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_READ);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            Files.setPosixFilePermissions(subFolder.toPath(), permissions);

            s.setPermissions(argument);
            assertTrue(s.isSelected(null, null, subFolder));
        }
    }

    @RunWith(Parameterized.class)
    public static class LegalSymbolicLinkArgumentTest {

        private final File TEST_FILE = new File("/etc/passwd");

        private PosixPermissionsSelector s;

        @Rule
        public TemporaryFolder folder = new TemporaryFolder();

        // requires JUnit 4.12
        @Parameterized.Parameters(name = "legal argument (link): |{0}|")
        public static Collection<String> data() {
            return Arrays.asList("644", "rw-r--r--");
        }

        @Parameterized.Parameter
        public String argument;

        @Before
        public void setUp() {
            assumeTrue("Not POSIX", Os.isFamily("unix"));
            s = new PosixPermissionsSelector();
        }

        @Test
        public void test() throws Exception {
            // symlinks have execute bit set by default
            File target = new File(folder.getRoot(), "link");
            Path symbolicLink = Files.createSymbolicLink(target.toPath(), TEST_FILE.toPath());

            s.setPermissions(argument);
            assertTrue(s.isSelected(null, null, symbolicLink.toFile()));
            s.setFollowSymlinks(false);
            assertFalse(s.isSelected(null, null, symbolicLink.toFile()));
        }
    }

}
