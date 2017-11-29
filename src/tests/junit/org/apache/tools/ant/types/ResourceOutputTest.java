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

package org.apache.tools.ant.types;

import java.io.File;
import java.io.IOException;
import java.net.UnknownServiceException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.resources.ImmutableResourceException;
import org.apache.tools.ant.types.resources.PropertyResource;
import org.apache.tools.ant.types.resources.StringResource;
import org.apache.tools.ant.types.resources.URLResource;
import org.apache.tools.ant.types.resources.ZipResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ResourceUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ResourceOutputTest {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final File basedir = new File(System.getProperty("root"),
        "src/etc/testcases/types/resources");

    private Project project;

    @Before
    public void setUp() {
        project = new Project();
        project.init();
        project.setUserProperty("basedir" , basedir.getAbsolutePath());
    }

    @Test
    public void testresourceoutput() {
        try {
            testoutputbe(new Resource("foo"));
            fail("should have caught UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            //TODO assert exception message
        }
    }

    @Test
    public void teststringoutput1() {
        StringResource r = new StringResource();
        testoutputbe(r);
        assertEquals("foo", r.getValue());
    }

    @Test
    public void teststringoutput2() throws IOException {
        StringResource r = new StringResource("bar");
        try {
            testoutput(r);
            fail("should have caught ImmutableResourceException");
        } catch (ImmutableResourceException e) {
            //TODO assert exception message
        }
        assertEquals("bar", r.getValue());
    }

    @Test
    public void testpropertyoutput1() {
        PropertyResource r = new PropertyResource(project, "bar");
        testoutputbe(r);
        assertEquals("foo", project.getProperty("bar"));
    }

    @Test
    public void testpropertyoutput2() throws IOException {
        project.setNewProperty("bar", "bar");
        PropertyResource r = new PropertyResource(project, "bar");
        try {
            testoutput(r);
            fail("should have caught ImmutableResourceException");
        } catch (ImmutableResourceException e) {
            //TODO assert exception message
        }
        assertEquals("bar", project.getProperty("bar"));
    }

    @Test
    public void testurloutput() throws IOException {
        File f = project.resolveFile("testurloutput");
        try {
            FILE_UTILS.createNewFile(f);
            testoutput(new URLResource(f));
            fail("should have caught UnknownServiceException");
        } catch (UnknownServiceException e) {
            //TODO assert exception message
        } finally {
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
    }

    @Test
    public void testzipentryoutput() {
        Zip z = new Zip();
        z.setProject(project);
        Zip.WhenEmpty create = new Zip.WhenEmpty();
        create.setValue("create");
        z.setWhenempty(create);
        z.setBasedir(basedir);
        z.setExcludes("**/*");
        File f = project.resolveFile("foo");
        z.setDestFile(f);
        z.execute();
        ZipResource r = new ZipResource();
        r.setZipfile(f);
        r.setName("foo");
        try {
            testoutputbe(r);
            fail("should have caught UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            //TODO assert exception message
        } finally {
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
    }

    private void testoutputbe(Resource dest) {
        try {
            testoutput(dest);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private void testoutput(Resource dest) throws IOException {
        ResourceUtils.copyResource(new StringResource("foo"), dest, null);
    }

}
