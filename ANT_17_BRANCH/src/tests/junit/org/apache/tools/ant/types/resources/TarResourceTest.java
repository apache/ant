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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

public class TarResourceTest extends BuildFileTest {

    private static final FileUtils FU = FileUtils.getFileUtils();

    public TarResourceTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        configureProject("src/etc/testcases/types/resources/tarentry.xml");
    }

    protected void tearDown() throws Exception {
        executeTarget("tearDown");
    }

    public void testUncompressSource() throws java.io.IOException {
        executeTarget("uncompressSource");
        assertTrue(FU.contentEquals(project.resolveFile("../../asf-logo.gif"),
                                    project.resolveFile("testout/asf-logo.gif")));
    }
}
