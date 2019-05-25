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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.FileNameMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


/**
 */
public class MultiMapTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/multimap.xml");
    }

    @Test
    public void testMultiCopy() {
        buildRule.executeTarget("multicopy");
    }

    @Test
    public void testMultiMove() {
        buildRule.executeTarget("multimove");
    }

    @Test
    public void testSingleCopy() {
        buildRule.executeTarget("singlecopy");
    }

    @Test
    public void testSingleMove() {
        buildRule.executeTarget("singlemove");
    }

    @Test
    public void testCopyWithEmpty() {
        buildRule.executeTarget("copywithempty");
    }

    @Test
    public void testMoveWithEmpty() {
        buildRule.executeTarget("movewithempty");
    }

    public static class TestMapper implements FileNameMapper {
        public TestMapper() {
        }

        public void setFrom(String from) {
        }

        public void setTo(String to) {
        }

        public String[] mapFileName(final String source_file_name) {
            return new String[] {source_file_name, source_file_name + ".copy2"};
        }
    }
}
