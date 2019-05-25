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
package org.apache.tools.ant.types.resources;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.MagicTestNames;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Test Java API of {@link FileResource}.
 */
public class FileResourceTest {

    private File root;

    @Before
    public void setUp() throws IOException {
        root = (System.getProperty(MagicTestNames.TEST_ROOT_DIRECTORY) == null)
                ? new File(".").getCanonicalFile()
                : new File(System.getProperty(MagicTestNames.TEST_ROOT_DIRECTORY));
    }

    @Test
    public void testAttributes() {
        FileResource f = new FileResource();
        f.setBaseDir(root);
        f.setName("foo");
        assertEquals(new File(root, "foo"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo", f.getName());
    }

    @Test
    public void testNonImmediateBasedir() {
        FileResource f = new FileResource();
        f.setBaseDir(root);
        f.setName("foo/bar");
        assertEquals(new File(root, "foo/bar"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo/bar", f.getName().replace(File.separatorChar, '/'));
    }

    @Test
    public void testFile() {
        FileResource f = new FileResource(new File(root, "foo"));
        assertEquals(new File(root, "foo"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo", f.getName());
    }

    @Test
    public void testBasedirAndName() {
        FileResource f = new FileResource(root, "foo");
        assertEquals(new File(root, "foo"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo", f.getName());
    }

    @Test
    public void testNonImmediateBasedirAndName() {
        FileResource f = new FileResource(root, "foo/bar");
        assertEquals(new File(root, "foo/bar"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo/bar", f.getName().replace(File.separatorChar, '/'));
    }

    @Test
    public void testProjectAndFilename() {
        Project p = new Project();
        p.setBaseDir(root);
        FileResource f = new FileResource(p, "foo");
        assertEquals(new File(root, "foo"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo", f.getName());
    }

    @Test
    public void testRelativeFactoryResource() {
        FileResource f = new FileResource(root, "foo");
        FileResource relative = f.getResource("bar").as(FileResource.class);
        assertEquals(new File(root, "foo/bar"), relative.getFile());
        assertEquals("foo/bar", relative.getName().replace(File.separatorChar, '/'));
        assertEquals(root, relative.getBaseDir());
    }

    @Test
    public void testAbsoluteFactoryResource() {
        FileResource f = new FileResource(new File(root, "foo/a"));
        assertEquals(new File(root, "foo"), f.getBaseDir());
        File bar = new File(root, "bar");
        FileResource fromFactory = f.getResource(bar.getAbsolutePath()).as(FileResource.class);
        assertEquals(bar, fromFactory.getFile());
        assertEquals(root, fromFactory.getBaseDir());
    }

    @Test
    public void testParentSiblingFactoryResource() {
        FileResource f = new FileResource(new File(root, "foo/a"));
        assertEquals(new File(root, "foo"), f.getBaseDir());
        FileResource parentSibling = f.getResource("../../bar").as(FileResource.class);
        assertEquals(root, parentSibling.getBaseDir());
        assertEquals("bar", parentSibling.getName());
    }

    @Test
    public void testEqualsUsesFiles() {
        FileResource f1 = new FileResource(new File(root, "foo/a"));
        FileResource f2 = new FileResource(new File(root + "/foo"), "a");
        assertEquals(f1, f2);
    }

    @Test
    public void testEqualsUsesRelativeNames() {
        FileResource f1 = new FileResource(root, "foo/a");
        FileResource f2 = new FileResource(new File(root + "/foo"), "a");
        assertNotEquals(f1, f2);
    }
}
