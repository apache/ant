/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildFileTest;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.io.*;
import java.util.Hashtable;

/**
 * FilterSet testing
 *
 * @author Conor MacNeill
 * @author <a href="mailto:martin@mvdb.net">Martin van den Bemt</a>
 */
public class FilterSetTest extends BuildFileTest {

    static private final int BUF_SIZE = 32768;

    public FilterSetTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/types/filterset.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test1() {
        executeTarget("test1");
        assertTrue("Filterset 1 failed", compareFiles("src/etc/testcases/types/gold/filterset1.txt",
                                                      "src/etc/testcases/types/dest1.txt"));
    }

    public void test2() {
        executeTarget("test2");
        assertTrue("Filterset 2 failed", compareFiles("src/etc/testcases/types/gold/filterset2.txt",
                                                      "src/etc/testcases/types/dest2.txt"));
    }

    public void test3() {
        executeTarget("test3");
        assertTrue("Filterset 3 failed", compareFiles("src/etc/testcases/types/gold/filterset3.txt",
                                                      "src/etc/testcases/types/dest3.txt"));
    }

    /**
     * This will test the recursive FilterSet.  Which means that if
     * the filter value @test@ contains another filter value, it will
     * actually resolve.
     */
    public void testRecursive() {
        String result = "it works line";
        String line="@test@ line";
        FilterSet fs = new FilterSet();
        fs.addFilter("test", "@test1@");
        fs.addFilter("test1","@test2@");
        fs.addFilter("test2", "it works");
        fs.setBeginToken("@");
        fs.setEndToken("@");
        assertEquals(result, fs.replaceTokens(line));
    }

    /**
     * Test to see what happens when the resolving occurs in an
     * infinite loop.
     */
    public void testInfinite() {
        String result = "@test@ line testvalue";
        String line = "@test@ line @test3@";
        FilterSet fs = new FilterSet();
        fs.addFilter("test", "@test1@");
        fs.addFilter("test1","@test2@");
        fs.addFilter("test2", "@test@");
        fs.addFilter("test3", "testvalue");
        fs.setBeginToken("@");
        fs.setEndToken("@");
        assertEquals(result, fs.replaceTokens(line));
    }

    public void testNestedFilterSets() {
        executeTarget("test-nested-filtersets");

        FilterSet fs = (FilterSet) getProject().getReference("1");
        Hashtable filters = fs.getFilterHash();
        assertEquals(1, filters.size());
        assertEquals("value1", filters.get("token1"));

        fs = (FilterSet) getProject().getReference("2");
        filters = fs.getFilterHash();
        assertEquals(2, filters.size());
        assertEquals("1111", filters.get("aaaa"));
        assertEquals("2222", filters.get("bbbb"));

        fs = (FilterSet) getProject().getReference("3");
        filters = fs.getFilterHash();
        assertEquals(1, filters.size());
        assertEquals("value4", filters.get("token4"));

        fs = (FilterSet) getProject().getReference("5");
        filters = fs.getFilterHash();
        assertEquals(1, filters.size());
        assertEquals("value1", filters.get("token1"));
    }

    private boolean compareFiles(String name1, String name2) {
        File file1 = new File(name1);
        File file2 = new File(name2);

        try {
            if (!file1.exists() || !file2.exists()) {
                System.out.println("One or both files do not exist:" + name1 + ", " + name2);
                return false;
            }

            if (file1.length() != file2.length()) {
                System.out.println("File size mismatch:" + name1 + "(" + file1.length() + "), " +
                                   name2  + "(" + file2.length() + ")");
                return false;
            }

            // byte - byte compare
            byte[] buffer1 = new byte[BUF_SIZE];
            byte[] buffer2 = new byte[BUF_SIZE];

            FileInputStream fis1 = new FileInputStream(file1);
            FileInputStream fis2 = new FileInputStream(file2);
            int index = 0;
            int read = 0;
            while ((read = fis1.read(buffer1)) != -1) {
                fis2.read(buffer2);
                for (int i = 0; i < read; ++i, ++index) {
                    if (buffer1[i] != buffer2[i]) {
                        System.out.println("Bytes mismatch:" + name1 + ", " + name2 +
                                           " at byte " + index);
                        return false;
                    }
                }
            }
            return true;
        }
        catch (IOException e) {
            System.out.println("IOException comparing files: " + name1 + ", " + name2);
            return false;
        }
    }
}
