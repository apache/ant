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
package org.apache.tools.ant.taskdefs;

import java.io.File;

import org.apache.tools.ant.BuildFileTest;

/**
 */
public class AbstractCvsTaskTest extends BuildFileTest {

    public AbstractCvsTaskTest() {
        this( "AbstractCvsTaskTest" );
    }

    public AbstractCvsTaskTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/abstractcvstask.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testAbstractCvsTask() {
        executeTarget( "all" );
    }

    public void testPackageAttribute() {
        File f = getProject().resolveFile("tmpdir/ant/build.xml");
        assertTrue("starting empty", !f.exists());
        expectLogContaining("package-attribute", "U ant/build.xml");
        assertTrue("now it is there", f.exists());
    }

    public void testTagAttribute() {
        File f = getProject().resolveFile("tmpdir/ant/build.xml");
        assertTrue("starting empty", !f.exists());
        expectLogContaining("tag-attribute", "ANT_141 (revision: 1.175.2.13)");
        assertTrue("now it is there", f.exists());
    }
}
