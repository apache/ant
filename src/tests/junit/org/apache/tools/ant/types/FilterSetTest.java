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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * FilterSet testing
 *
 */
public class FilterSetTest {

    private static final int BUF_SIZE = 32768;

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/filterset.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    @Test
    public void test1() throws IOException {
        buildRule.executeTarget("test1");
        assertTrue("Filterset 1 failed", compareFiles("gold/filterset1.txt", "dest1.txt"));
    }

    @Test
    public void test2() throws IOException {
        buildRule.executeTarget("test2");
        assertTrue("Filterset 2 failed", compareFiles("gold/filterset2.txt", "dest2.txt"));
    }

    @Test
    public void test3() throws IOException {
        buildRule.executeTarget("test3");
        assertTrue("Filterset 3 failed", compareFiles("gold/filterset3.txt", "dest3.txt"));
    }

    /**
     * This will test the recursive FilterSet.  Which means that if
     * the filter value @test@ contains another filter value, it will
     * actually resolve.
     */
    @Test
    public void testRecursive() {
        String result = "it works line";
        String line = "@test@ line";
        FilterSet fs = new FilterSet();
        fs.addFilter("test", "@test1@");
        fs.addFilter("test1", "@test2@");
        fs.addFilter("test2", "it works");
        fs.setBeginToken("@");
        fs.setEndToken("@");
        assertEquals(result, fs.replaceTokens(line));
    }

    /**
     * Test to see what happens when the resolving occurs in an
     * infinite loop.
     */
    @Test
    public void testInfinite() {
        String result = "@test@ line testvalue";
        String line = "@test@ line @test3@";
        FilterSet fs = new FilterSet();
        fs.addFilter("test", "@test1@");
        fs.addFilter("test1", "@test2@");
        fs.addFilter("test2", "@test@");
        fs.addFilter("test3", "testvalue");
        fs.setBeginToken("@");
        fs.setEndToken("@");
        assertEquals(result, fs.replaceTokens(line));
    }

    /**
     * Test to see what happens when the resolving occurs in
     * what would be an infinite loop, but with recursion disabled.
     */
    @Test
    public void testRecursionDisabled() {
        String result = "@test1@ line testvalue";
        String line = "@test@ line @test2@";
        FilterSet fs = new FilterSet();
        fs.addFilter("test", "@test1@");
        fs.addFilter("test1", "@test@");
        fs.addFilter("test2", "testvalue");
        fs.setBeginToken("@");
        fs.setEndToken("@");
        fs.setRecurse(false);
        assertEquals(result, fs.replaceTokens(line));
    }

    @Test
    public void testNonInfiniteRecursiveMultipleOnSingleLine() {
        FilterSet filters = new FilterSet();

        filters.setBeginToken("<");
        filters.setEndToken(">");

        filters.addFilter("ul", "<itemizedlist>");
        filters.addFilter("/ul", "</itemizedList>");
        filters.addFilter("li", "<listitem>");
        filters.addFilter("/li", "</listitem>");

        String result = "<itemizedlist><listitem>Item 1</listitem> <listitem>Item 2</listitem></itemizedList>";
        String line = "<ul><li>Item 1</li> <li>Item 2</li></ul>";

        assertEquals(result, filters.replaceTokens(line));
    }

    @Test
    public void testNestedFilterSets() {
        buildRule.executeTarget("test-nested-filtersets");

        FilterSet fs = buildRule.getProject().getReference("1");
        Hashtable<String, String> filters = fs.getFilterHash();
        assertEquals(1, filters.size());
        assertEquals("value1", filters.get("token1"));

        fs = buildRule.getProject().getReference("2");
        filters = fs.getFilterHash();
        assertEquals(2, filters.size());
        assertEquals("1111", filters.get("aaaa"));
        assertEquals("2222", filters.get("bbbb"));

        fs = buildRule.getProject().getReference("3");
        filters = fs.getFilterHash();
        assertEquals(1, filters.size());
        assertEquals("value4", filters.get("token4"));

        fs = buildRule.getProject().getReference("5");
        filters = fs.getFilterHash();
        assertEquals(1, filters.size());
        assertEquals("value1", filters.get("token1"));
    }

    @Test
    public void testFiltersFileElement() {
        buildRule.executeTarget("testFiltersFileElement");
    }

    @Test
    public void testFiltersFileAttribute() {
        buildRule.executeTarget("testFiltersFileAttribute");
    }

    @Test
    public void testMultipleFiltersFiles() {
        buildRule.executeTarget("testMultipleFiltersFiles");
    }

    @Test(expected = BuildException.class)
    public void testMissingFiltersFile() {
        buildRule.executeTarget("testMissingFiltersFile");
        // TODO assert exception text
    }

    @Test
    public void testAllowMissingFiltersFile() {
        buildRule.executeTarget("testAllowMissingFiltersFile");
    }

    private boolean compareFiles(String name1, String name2) throws IOException {
        File file1 = buildRule.getProject().resolveFile(name1);
        File file2 = buildRule.getProject().resolveFile(name2);

        if (!file1.exists() || !file2.exists() || file1.length() != file2.length()) {
            return false;
        }

        // byte - byte compare
        byte[] buffer1 = new byte[BUF_SIZE];
        byte[] buffer2 = new byte[BUF_SIZE];

        try (FileInputStream fis1 = new FileInputStream(file1);
             FileInputStream fis2 = new FileInputStream(file2)) {
            int read = 0;
            while ((read = fis1.read(buffer1)) != -1) {
                fis2.read(buffer2);
                for (int i = 0; i < read; ++i) {
                    if (buffer1[i] != buffer2[i]) {
                        return false;
                    }
                }
            }
        }
        return true;

    }
}
