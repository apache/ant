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
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the examples of the &lt;script&gt; task docs.
 *
 * @since Ant 1.5.2
 */
public class RhinoScriptTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/script.xml");
    }

    @Test
    public void testExample1() {
        buildRule.executeTarget("example1");
        int index = buildRule.getLog().indexOf("1");
        assertTrue(index > -1);
        index = buildRule.getLog().indexOf("4", index);
        assertTrue(index > -1);
        index = buildRule.getLog().indexOf("9", index);
        assertTrue(index > -1);
        index = buildRule.getLog().indexOf("16", index);
        assertTrue(index > -1);
        index = buildRule.getLog().indexOf("25", index);
        assertTrue(index > -1);
        index = buildRule.getLog().indexOf("36", index);
        assertTrue(index > -1);
        index = buildRule.getLog().indexOf("49", index);
        assertTrue(index > -1);
        index = buildRule.getLog().indexOf("64", index);
        assertTrue(index > -1);
        index = buildRule.getLog().indexOf("81", index);
        assertTrue(index > -1);
        index = buildRule.getLog().indexOf("100", index);
        assertTrue(index > -1);
    }

    @Test
    public void testUseSrcAndEncoding() {
        final String readerEncoding = "UTF-8";
        buildRule.getProject().setProperty("useSrcAndEncoding.reader.encoding", readerEncoding);
        buildRule.executeTarget("useSrcAndEncoding");
    }

    @Test
    public void testUseSrcAndEncodingFailure() {
        final String readerEncoding = "ISO-8859-1";
        buildRule.getProject().setProperty("useSrcAndEncoding.reader.encoding", readerEncoding);
        try {
            buildRule.executeTarget("useSrcAndEncoding");
            fail("should have failed with reader's encoding [" + readerEncoding +
                "] different from the writer's encoding [" + buildRule.getProject().getProperty("useSrcAndEncoding.encoding") + "]");
        }
        catch(BuildException e) {
            assertTrue(e.getMessage().matches("expected <eacute \\[\u00e9]> but was <eacute \\[\u00c3\u00a9]>"));
        }
    }
}
