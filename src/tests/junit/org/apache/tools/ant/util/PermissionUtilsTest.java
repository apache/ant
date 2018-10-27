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

package org.apache.tools.ant.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.TarResource;
import org.apache.tools.ant.types.resources.ZipResource;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.junit.Assume;
import org.junit.Test;

public class PermissionUtilsTest {

    @Test
    public void modeFromPermissionsReturnsExpectedResult() {
        int mode = PermissionUtils.modeFromPermissions(EnumSet.of(PosixFilePermission.OWNER_READ,
                                                                  PosixFilePermission.OWNER_WRITE,
                                                                  PosixFilePermission.OWNER_EXECUTE),
                                                       PermissionUtils.FileType.REGULAR_FILE);
        assertEquals("100700", Integer.toString(mode, 8));
    }

    @Test
    public void permissionsFromModeReturnsExpectedResult() {
        Set<PosixFilePermission> s = PermissionUtils.permissionsFromMode(0100753);
        assertEquals(EnumSet.of(PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_WRITE,
                                PosixFilePermission.OWNER_EXECUTE,
                                PosixFilePermission.GROUP_READ,
                                PosixFilePermission.GROUP_EXECUTE,
                                PosixFilePermission.OTHERS_WRITE,
                                PosixFilePermission.OTHERS_EXECUTE),
                     s);
    }

    @Test
    public void detectsFileTypeOfRegularFileFromPath() throws IOException {
        File f = File.createTempFile("ant", ".tst");
        f.deleteOnExit();
        assertEquals(PermissionUtils.FileType.REGULAR_FILE,
                     PermissionUtils.FileType.of(f.toPath()));
    }

    @Test
    public void detectsFileTypeOfRegularFileFromResource() throws IOException {
        File f = File.createTempFile("ant", ".tst");
        f.deleteOnExit();
        assertEquals(PermissionUtils.FileType.REGULAR_FILE,
                     PermissionUtils.FileType.of(new FileResource(f)));
    }

    @Test
    public void detectsFileTypeOfDirectoryFromPath() throws IOException {
        File f = File.createTempFile("ant", ".dir");
        f.delete();
        f.mkdirs();
        f.deleteOnExit();
        assertEquals(PermissionUtils.FileType.DIR,
                     PermissionUtils.FileType.of(f.toPath()));
    }

    @Test
    public void detectsFileTypeOfDirectoryFromResource() throws IOException {
        File f = File.createTempFile("ant", ".tst");
        f.delete();
        f.mkdirs();
        f.deleteOnExit();
        assertEquals(PermissionUtils.FileType.DIR,
                     PermissionUtils.FileType.of(new FileResource(f)));
    }

    @Test
    public void getSetPermissionsWorksForFiles() throws IOException {
        File f = File.createTempFile("ant", ".tst");
        f.deleteOnExit();
        Assume.assumeNotNull(Files.getFileAttributeView(f.toPath(),
                                                        PosixFileAttributeView.class));
        Set<PosixFilePermission> s =
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.GROUP_READ);
        PermissionUtils.setPermissions(new FileResource(f), s, null);
        assertEquals(s, PermissionUtils.getPermissions(new FileResource(f), null));
    }

    @Test
    public void getSetPermissionsWorksForZipResources() throws IOException {
        File f = File.createTempFile("ant", ".zip");
        f.deleteOnExit();
        try (ZipOutputStream os = new ZipOutputStream(f)) {
            ZipEntry e = new ZipEntry("foo");
            os.putNextEntry(e);
            os.closeEntry();
        }

        ZipResource r = new ZipResource();
        r.setName("foo");
        r.setArchive(f);
        Set<PosixFilePermission> s =
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.GROUP_READ);
        PermissionUtils.setPermissions(r, s, null);
        assertEquals(s, PermissionUtils.getPermissions(r, null));
    }

    @Test
    public void getSetPermissionsWorksForTarResources() throws IOException {
        File f = File.createTempFile("ant", ".zip");
        f.deleteOnExit();
        try (TarOutputStream os = new TarOutputStream(new FileOutputStream(f))) {
            TarEntry e = new TarEntry("foo");
            os.putNextEntry(e);
            os.closeEntry();
        }

        TarResource r = new TarResource();
        r.setName("foo");
        r.setArchive(f);
        Set<PosixFilePermission> s =
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.GROUP_READ);
        PermissionUtils.setPermissions(r, s, null);
        assertEquals(s, PermissionUtils.getPermissions(r, null));
    }
}
