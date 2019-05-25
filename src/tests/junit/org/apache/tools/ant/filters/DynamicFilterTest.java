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

package org.apache.tools.ant.filters;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class DynamicFilterTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/filters/dynamicfilter.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void testCustomFilter() throws IOException {
        buildRule.executeTarget("dynamicfilter");
        String content = FileUtilities.getFileContents(
                new File(buildRule.getProject().getProperty("output") + "/dynamicfilter"));
        assertThat(content, containsString("hellO wOrld"));
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
                public int read() throws IOException {
                    int c = in.read();
                    if (c == replace) {
                        return with;
                    } else {
                        return c;
                    }
                }
            };
        }
    }
}
