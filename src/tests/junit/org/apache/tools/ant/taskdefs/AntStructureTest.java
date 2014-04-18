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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.PrintWriter;
import java.util.Hashtable;

import static org.junit.Assert.fail;

/**
 */
public class AntStructureTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/antstructure.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("tearDown");
    }

    @Test
    public void test1() {
        try {
            buildRule.executeTarget("test1");
            fail("required argument not specified");
        } catch (BuildException ex) {
            //TODO assert exception message
        }
    }

    @Test
    public void testCustomPrinter() {
        buildRule.executeTarget("testCustomPrinter");
        // can't access the booleans in MyPrinter here (even if they
        // were static) since the MyPrinter instance that was used in
        // the test has likely been loaded via a different classloader
        // than this class.  Therefore we make the printer assert its
        // state and only check for the tail invocation.
        AntAssert.assertContains(MyPrinter.TAIL_CALLED, buildRule.getLog());
    }

    public static class MyPrinter implements AntStructure.StructurePrinter {
        private static final String TAIL_CALLED = "tail has been called";
        private boolean headCalled = false;
        private boolean targetCalled = false;
        private boolean tailCalled = false;
        private int elementCalled = 0;
        private Project p;

        public void printHead(PrintWriter out, Project p, Hashtable tasks,
                              Hashtable types) {
            Assert.assertTrue(!headCalled);
            Assert.assertTrue(!targetCalled);
            Assert.assertTrue(!tailCalled);
            Assert.assertEquals(0, elementCalled);
            headCalled = true;
        }
        public void printTargetDecl(PrintWriter out) {
            Assert.assertTrue(headCalled);
            Assert.assertTrue(!targetCalled);
            Assert.assertTrue(!tailCalled);
            Assert.assertEquals(0, elementCalled);
            targetCalled = true;
        }
        public void printElementDecl(PrintWriter out, Project p, String name,
                                     Class element) {
            Assert.assertTrue(headCalled);
            Assert.assertTrue(targetCalled);
            Assert.assertTrue(!tailCalled);
            elementCalled++;
            this.p = p;
        }
        public void printTail(PrintWriter out) {
            Assert.assertTrue(headCalled);
            Assert.assertTrue(targetCalled);
            Assert.assertTrue(!tailCalled);
            Assert.assertTrue(elementCalled > 0);
            tailCalled = true;
            p.log(TAIL_CALLED);
        }
    }
}
