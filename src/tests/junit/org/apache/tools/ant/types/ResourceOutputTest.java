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

package org.apache.tools.ant.types;

import java.io.File;
import java.io.IOException;
import java.net.UnknownServiceException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class ResourceOutputTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    private Project project;

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/resources/resourcelist.xml");
        project = buildRule.getProject();
    }

    /**
     * Expected failure
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testresourceoutput() {
        testoutputbe(new Resource("foo"));
    }

    @Test
    public void teststringoutput1() {
        StringResource r = new StringResource();
        testoutputbe(r);
        assertEquals("foo", r.getValue());
    }

    /**
     * Expected failure
     *
     * @throws IOException if something goes wrong
     */
    @Test(expected = ImmutableResourceException.class)
    public void teststringoutput2() throws IOException {
        StringResource r = new StringResource("bar");
        testoutput(r);
    }

    @Test
    public void teststringoutput3() {
        StringResource r = new StringResource("bar");
        assertEquals("bar", r.getValue());
    }

    @Test
    public void testpropertyoutput1() {
        PropertyResource r = new PropertyResource(project, "bar");
        testoutputbe(r);
        assertEquals("foo", project.getProperty("bar"));
    }

    /**
     * Expected failure
     *
     * @throws IOException if something goes wrong
     */
    @Test(expected = ImmutableResourceException.class)
    public void testpropertyoutput2() throws IOException {
        project.setNewProperty("bar", "bar");
        PropertyResource r = new PropertyResource(project, "bar");
        testoutput(r);
    }

    @Test
    public void testpropertyoutput3() {
        project.setNewProperty("bar", "bar");
        assertEquals("bar", project.getProperty("bar"));
    }

    @Test
    public void testurloutput() throws IOException {
        thrown.expect(UnknownServiceException.class);
        // TODO assert exception message
        File f = project.resolveFile("testurloutput");
        try {
            FileUtils.getFileUtils().createNewFile(f);
            testoutput(new URLResource(f));
        } finally {
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
    }

    @Test
    public void testzipentryoutput() {
        thrown.expect(UnsupportedOperationException.class);
        // TODO assert exception message
        Zip z = new Zip();
        z.setProject(project);
        Zip.WhenEmpty create = new Zip.WhenEmpty();
        create.setValue("create");
        z.setWhenempty(create);
        z.setBasedir(project.getBaseDir());
        z.setExcludes("**/*");
        File f = project.resolveFile("foo");
        z.setDestFile(f);
        z.execute();
        ZipResource r = new ZipResource();
        r.setZipfile(f);
        r.setName("foo");
        try {
            testoutputbe(r);
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
