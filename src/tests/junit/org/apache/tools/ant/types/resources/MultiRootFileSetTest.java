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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.AbstractFileSetTest;
import org.apache.tools.ant.types.Reference;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.endsWith;

/**
 * This doesn't actually test much, mainly reference handling.
 */
public class MultiRootFileSetTest extends AbstractFileSetTest {

    private MultiRootFileSet multiRootFileSet;

    @Before
    public void setUp() {
        super.setUp();
        multiRootFileSet = new MultiRootFileSet();
    }

    protected AbstractFileSet getInstance() {
        return new MultiRootFileSet() {
            // overriding so set/getDir works as expected by the base test class
            private File dir;
            public void setDir(File dir) {
                if (isReference()) {
                    throw tooManyAttributes();
                }
                this.dir = dir;
            }

            public synchronized File getDir(Project p) {
                if (isReference()) {
                    return getRef(p).getDir(p);
                }
                dieOnCircularReference();
                return dir;
            }
        };
    }

    @Test
    public void testCannotSetBaseDirsThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        multiRootFileSet.setProject(getProject());
        multiRootFileSet.setBaseDirs("a");
        multiRootFileSet.setRefid(new Reference(getProject(), "dummyref"));
    }

    @Test
    public void testCannotSetConfiguredBaseDirThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        multiRootFileSet.addConfiguredBaseDir(new FileResource(new File(".")));
        multiRootFileSet.setRefid(new Reference(getProject(), "dummyref"));
    }

    @Test
    public void testCannotSetRefidThenBaseDirs() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        multiRootFileSet.setRefid(new Reference(getProject(), "dummyref"));
        multiRootFileSet.setBaseDirs("a");
    }

    @Test
    public void testCannotSetRefidThenCache() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        multiRootFileSet.setRefid(new Reference(getProject(), "dummyref"));
        multiRootFileSet.setCache(true);
    }

    @Test
    public void testCannotSetRefidThenType() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        multiRootFileSet.setRefid(new Reference(getProject(), "dummyref"));
        multiRootFileSet.setType(MultiRootFileSet.SetType.file);
    }

    @Test
    public void testCannotSetRefidThenConfiguredBaseDir() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        multiRootFileSet.setRefid(new Reference(getProject(), "dummyref"));
        multiRootFileSet.addConfiguredBaseDir(new FileResource(new File(".")));
    }

    @Test
    public void testCannotSetDir() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(endsWith(" doesn't support the dir attribute"));
        multiRootFileSet.setDir(new File("."));
    }
}
