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

package org.apache.tools.ant;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import junit.framework.TestCase;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 * Test case for ant class loader
 *
 */
public class AntClassLoaderDelegationTest extends TestCase {

    /** Instance of a utility class to use for file operations. */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private Project p;

    public AntClassLoaderDelegationTest(String name) {
        super(name);
    }

    public void setUp() {
        p = new Project();
        p.init();
    }

    /** Sample resource present in build/testcases/ */
    private static final String TEST_RESOURCE
        = "apache/tools/ant/IncludeTest.class";
    
    public void testFindResources() throws Exception {
        // This path should contain the class files for these testcases:
        String buildTestcases = System.getProperty("build.tests");
        assertNotNull("defined ${build.tests}", buildTestcases);
        assertTrue("have a dir " + buildTestcases,
                   new File(buildTestcases).isDirectory());
        Path path = new Path(p, buildTestcases + "/org");
        // A special parent loader which is not the system class loader:
        ClassLoader parent = new ParentLoader();
        // An AntClassLoader which is supposed to delegate to
        // the parent and then to the disk path:
        ClassLoader acl = new AntClassLoader(parent, p, path, true);
        // The intended result URLs:
        URL urlFromPath = new URL(
            FILE_UTILS.toURI(buildTestcases) + "org/" + TEST_RESOURCE);
        URL urlFromParent = new URL("http://ant.apache.org/" + TEST_RESOURCE);
        assertEquals("correct resources (regular delegation order)",
            Arrays.asList(new URL[] {urlFromParent, urlFromPath}),
            enum2List(acl.getResources(TEST_RESOURCE)));
        acl = new AntClassLoader(parent, p, path, false);
        assertEquals("correct resources (reverse delegation order)",
            Arrays.asList(new URL[] {urlFromPath, urlFromParent}),
            enum2List(acl.getResources(TEST_RESOURCE)));
    }

    public void testFindIsolateResources() throws Exception {
        String buildTestcases = System.getProperty("build.tests");
        assertNotNull("defined ${build.tests}", buildTestcases);
        assertTrue("have a dir " + buildTestcases,
                   new File(buildTestcases).isDirectory());
        Path path = new Path(p, buildTestcases + "/org");
        // A special parent loader which is not the system class loader:
        ClassLoader parent = new ParentLoader();

        URL urlFromPath = new URL(
            FILE_UTILS.toURI(buildTestcases) + "org/" + TEST_RESOURCE);
        AntClassLoader acl = new AntClassLoader(parent, p, path, false);
        acl.setIsolated(true);
        assertEquals("correct resources (reverse delegation order)",
            Arrays.asList(new URL[] {urlFromPath}),
            enum2List(acl.getResources(TEST_RESOURCE)));
    }
    
    private static List enum2List(Enumeration e) {
        // JDK 1.4: return Collections.list(e);
        List l = new ArrayList();
        while (e.hasMoreElements()) {
            l.add(e.nextElement());
        }
        return l;
    }
    
    /** Special loader that just knows how to find TEST_RESOURCE. */
    private static final class ParentLoader extends ClassLoader {
        
        public ParentLoader() {}
        
        protected Enumeration findResources(String name) throws IOException {
            if (name.equals(TEST_RESOURCE)) {
                return Collections.enumeration(
                    Collections.singleton(
                        new URL("http://ant.apache.org/" + name)));
            } else {
                return Collections.enumeration(Collections.EMPTY_SET);
            }
        }
        
    }
    
}
