/*
 * Copyright  2003-2004 Apache Software Foundation
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

package org.apache.tools.ant.filters;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

/**
 * @author Peter Reilly
 */
public class DynamicFilterTest extends BuildFileTest {

    public DynamicFilterTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/filters/dynamicfilter.xml");
        executeTarget("init");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }
    public void testCustomFilter() throws IOException {
        expectFileContains("dynamicfilter", "result/dynamicfilter",
                           "hellO wOrld");
    }

    // ------------------------------------------------------
    //   Helper methods
    // -----------------------------------------------------


    private void assertStringContains(String string, String contains) {
        assertTrue("[" + string + "] does not contain [" + contains +"]",
                   string.indexOf(contains) > -1);
    }

    private void assertStringNotContains(String string, String contains) {
        assertTrue("[" + string + "] does contain [" + contains +"]",
                   string.indexOf(contains) == -1);
    }

    private String getFileString(String filename)
        throws IOException
    {
        Reader r = null;
        try {
            r = new FileReader(getProject().resolveFile(filename));
            return  FileUtils.newFileUtils().readFully(r);
        }
        finally {
            try {r.close();} catch (Throwable ignore) {}
        }

    }

    private String getFileString(String target, String filename)
        throws IOException
    {
        executeTarget(target);
        return getFileString(filename);
    }

    private void expectFileContains(String name, String contains)
        throws IOException
    {
        String content = getFileString(name);
        assertTrue(
            "expecting file " + name + " to contain " + contains +
            " but got " + content, content.indexOf(contains) > -1);
    }

    private void expectFileContains(
        String target, String name, String contains)
        throws IOException
    {
        executeTarget(target);
        expectFileContains(name, contains);
    }

    public static class CustomFilter implements ChainableReader {
        char replace = 'x';
        char with    = 'y';

        public void setReplace(char replace) {
            this.replace = replace;
        }

        public void setWith(char with) {
            this.with = with;
        }

        public Reader chain(final Reader rdr) {
            return new BaseFilterReader(rdr) {
                public int read()
                    throws IOException
                {
                    int c = in.read();
                    if (c == replace)
                        return with;
                    else
                        return c;
                }
            };
        }
    }
}
