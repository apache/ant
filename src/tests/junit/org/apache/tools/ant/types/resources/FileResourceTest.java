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
package org.apache.tools.ant.types.resources;

import java.io.File;

import org.apache.tools.ant.Project;

import junit.framework.TestCase;

/**
 * Test Java API of {@link FileResource}.
 */
public class FileResourceTest extends TestCase {

    private File root;

    public void setUp() {
        root = new File(System.getProperty("root"));
    }

    public void testAttributes() {
        FileResource f = new FileResource();
        f.setBaseDir(root);
        f.setName("foo");
        assertEquals(new File(root, "foo"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo", f.getName());
    }

    public void testNonImmediateBasedir() {
        FileResource f = new FileResource();
        f.setBaseDir(root);
        f.setName("foo/bar");
        assertEquals(new File(root, "foo/bar"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo/bar", f.getName().replace(File.separatorChar, '/'));
    }

    public void testFile() {
        FileResource f = new FileResource(new File(root, "foo"));
        assertEquals(new File(root, "foo"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo", f.getName());
    }

    public void testBasedirAndName() {
        FileResource f = new FileResource(root, "foo");
        assertEquals(new File(root, "foo"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo", f.getName());
    }

    public void testNonImmediateBasedirAndName() {
        FileResource f = new FileResource(root, "foo/bar");
        assertEquals(new File(root, "foo/bar"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo/bar", f.getName().replace(File.separatorChar, '/'));
    }

    public void testProjectAndFilename() {
        Project p = new Project();
        p.setBaseDir(root);
        FileResource f = new FileResource(p, "foo");
        assertEquals(new File(root, "foo"), f.getFile());
        assertEquals(root, f.getBaseDir());
        assertEquals("foo", f.getName());
    }

    public void testRelativeFactoryResource() {
        FileResource f = new FileResource(root, "foo");
        FileResource relative = f.getResource("bar").as(FileResource.class);
        assertEquals(new File(root, "foo/bar"), relative.getFile());
        assertEquals("foo/bar", relative.getName().replace(File.separatorChar, '/'));
        assertEquals(root, relative.getBaseDir());
    }

    public void testAbsoluteFactoryResource() {
        FileResource f = new FileResource(new File(root, "foo/a"));
        assertEquals(new File(root, "foo"), f.getBaseDir());
        File bar = new File(root, "bar");
        FileResource fromFactory = f.getResource(bar.getAbsolutePath()).as(FileResource.class);
        assertEquals(bar, fromFactory.getFile());
        assertEquals(root, fromFactory.getBaseDir());
    }

    public void testParentSiblingFactoryResource() {
        FileResource f = new FileResource(new File(root, "foo/a"));
        assertEquals(new File(root, "foo"), f.getBaseDir());
        FileResource parentSibling = f.getResource("../../bar").as(FileResource.class);
        assertEquals(root, parentSibling.getBaseDir());
        assertEquals("bar", parentSibling.getName());
    }
}
