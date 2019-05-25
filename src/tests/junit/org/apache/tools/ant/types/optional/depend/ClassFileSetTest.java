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

package org.apache.tools.ant.types.optional.depend;

import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Testcase for the Classfileset optional type.
 *
 */
public class ClassFileSetTest {
    public static final String RESULT_FILESET = "result";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        // share the setup for testing the depend task
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/depend/depend.xml");
    }

    /**
     * Test basic classfileset
     */
    @Test
    public void testBasicSet() {
        buildRule.executeTarget("testbasicset");
        Hashtable<String, String> files = getFiles();
        assertEquals("Classfileset did not pick up expected number of class files",
                4, files.size());
        assertThat("Result did not contain A.class", files, hasKey("A.class"));
        assertThat("Result did not contain B.class", files, hasKey("B.class"));
        assertThat("Result did not contain C.class", files, hasKey("C.class"));
        assertThat("Result did not contain D.class", files, hasKey("D.class"));
    }

    /**
     * Test small classfileset
     */
    @Test
    public void testSmallSet() {
        buildRule.executeTarget("testsmallset");
        Hashtable<String, String> files = getFiles();
        assertEquals("Classfileset did not pick up expected number of class files",
                2, files.size());
        assertThat("Result did not contain B.class", files, hasKey("B.class"));
        assertThat("Result did not contain C.class", files, hasKey("C.class"));
    }

    /**
     * Test combo classfileset
     */
    @Test
    public void testComboSet() {
        buildRule.executeTarget("testcomboset");
        Hashtable<String, String> files = getFiles();
        assertEquals("Classfileset did not pick up expected number of class files",
                1, files.size());
        assertThat("Result did not contain C.class", files, hasKey("C.class"));
    }

    /**
     * Test that you can pass a classfileset by reference to a fileset.
     */
    @Test
    public void testByReference() {
        buildRule.executeTarget("testbyreference");
    }

    /**
     * Test that classes included in a method "System.out.println(MyClass.class)" are included.
     */
    @Test
    public void testMethodParam() {
        buildRule.executeTarget("testmethodparam");
        Hashtable<String, String> files = getFiles();
        assertEquals("Classfileset did not pick up expected number of class files",
                5, files.size());
        assertThat("Result did not contain A.class", files, hasKey("A.class"));
        assertThat("Result did not contain B.class", files, hasKey("B.class"));
        assertThat("Result did not contain C.class", files, hasKey("C.class"));
        assertThat("Result did not contain D.class", files, hasKey("D.class"));
        assertThat("Result did not contain E.class", files, hasKey("E.class"));
    }

    /**
     * Test that classes included in a method "System.out.println(Outer.Inner.class)" are included.
     */
    @Test
    public void testMethodParamInner() {
        buildRule.executeTarget("testmethodparaminner");
        Hashtable<String, String> files = getFiles();
        assertEquals("Classfileset did not pick up expected number of class files",
                4, files.size());
        assertThat("Result did not contain test" + File.separator + "Outer$Inner.class",
            files, hasKey("test" + File.separator + "Outer$Inner.class"));
        assertThat("Result did not contain test" + File.separator + "Outer.class",
            files, hasKey("test" + File.separator + "Outer.class"));
        assertThat("Result did not contain test" + File.separator + "ContainsOnlyInner.class",
            files, hasKey("test" + File.separator + "ContainsOnlyInner.class"));
        assertThat("Result did not contain test" + File.separator + "ContainsOnlyInner.class",
            files, hasKey("test" + File.separator + "MethodParam.class"));
    }

    @Test
    public void testResourceCollection() {
        buildRule.executeTarget("testresourcecollection");
    }

    private Hashtable<String, String> getFiles() {
        FileSet resultFileSet = buildRule.getProject().getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(buildRule.getProject());
        String[] scannedFiles = scanner.getIncludedFiles();
        return Arrays.stream(scannedFiles)
                .collect(Collectors.toMap(file -> file, file -> file, (a, b) -> b, Hashtable::new));
    }
}
