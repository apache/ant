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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.AbstractFileSetTest;
import org.apache.tools.ant.types.Reference;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This doesn't actually test much, mainly reference handling.
 */
public class MultiRootFileSetTest extends AbstractFileSetTest {


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
    public void testEmptyElementIfIsReferenceAdditionalAttributes() {
        MultiRootFileSet f = new MultiRootFileSet();
        f.setProject(getProject());
        f.setBaseDirs("a");
        try {
            f.setRefid(new Reference(getProject(), "dummyref"));
            fail("Can add reference to multirootfileset "
                 + " with elements from setBasedirs");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }
        f = new MultiRootFileSet();
        f.addConfiguredBaseDir(new FileResource(new File(".")));
        try {
            f.setRefid(new Reference(getProject(), "dummyref"));
            fail("Can add reference to multirootfileset"
                 + " with elements from addConfiguredBaseDir");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }

        f = new MultiRootFileSet();
        f.setRefid(new Reference(getProject(), "dummyref"));
        try {
            f.setBaseDirs("a");
            fail("Can set basedirs in multirootfileset"
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }
        try {
            f.setCache(true);
            fail("Can set cache in multirootfileset"
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }
        try {
            f.setType(MultiRootFileSet.SetType.file);
            fail("Can set type in multirootfileset"
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }
        try {
            f.addConfiguredBaseDir(new FileResource(new File(".")));
            fail("Can add nested basedir in multirootfileset "
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using "
                         + "refid", be.getMessage());
        }
    }

    @Test
    public void testDirCannotBeSet() {
        try {
            new MultiRootFileSet().setDir(new File("."));
            fail("Can set dir in a multirootfileset");
        } catch (BuildException e) {
            assertTrue(e.getMessage()
                       .endsWith(" doesn't support the dir attribute"));
        }
    }
}
