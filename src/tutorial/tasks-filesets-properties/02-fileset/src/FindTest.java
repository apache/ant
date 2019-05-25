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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FindTest {

    @Rule
    public BuildFileRule rule = new BuildFileRule();

    @Rule
    public ExpectedException tried = ExpectedException.none();

    @Before
    public void setUp() {
        rule.configureProject("build.xml");
    }

    @Test
    public void testMissingFile() {
        tried.expect(BuildException.class);
        tried.expectMessage("file not set");
        Find find = new Find();
        find.execute();
    }

    @Test
    public void testMissingLocation() {
        tried.expect(BuildException.class);
        tried.expectMessage("location not set");
        Find find = new Find();
        find.setFile("ant.jar");
        find.execute();
    }

    @Test
    public void testMissingFileset() {
        tried.expect(BuildException.class);
        tried.expectMessage("fileset not set");
        Find find = new Find();
        find.setFile("ant.jar");
        find.setLocation("location.ant-jar");
        find.execute();
    }

    @Test
    public void testFileNotPresent() {
        rule.executeTarget("testFileNotPresent");
        String result = rule.getProject().getProperty("location.ant-jar");
        assertNull("Property set to wrong value.", result);
    }

    @Test
    public void testFilePresent() {
        rule.executeTarget("testFilePresent");
        String result = rule.getProject().getProperty("location.ant-jar");
        assertNotNull("Property not set.", result);
        assertTrue("Wrong file found.", result.endsWith("ant.jar"));
    }

}
