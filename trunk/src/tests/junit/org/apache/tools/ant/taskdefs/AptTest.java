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

import org.apache.tools.ant.BuildFileTest;

/**
 */
public class AptTest extends BuildFileTest {
    public AptTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/apt.xml");
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() throws Exception {
        executeTarget("clean");
    }

    public void testApt() {
        executeTarget("testApt");
    }

    public void testAptFork() {
        executeTarget("testAptFork");
    }
 
    public void testAptForkFalse() {
        executeTarget("testAptForkFalse");
        assertLogContaining(Apt.WARNING_IGNORING_FORK);
    }

    public void testListAnnotationTypes() {
        executeTarget("testListAnnotationTypes");
        assertLogContaining("Set of annotations found:");
        assertLogContaining("Distributed");
    }

    public void testAptNewFactory() {
        executeTarget("testAptNewFactory");
        assertProcessed();
    }

    public void testAptNewFactoryFork() {
        executeTarget("testAptNewFactoryFork");
        assertProcessed();
    }
    
    private void assertProcessed() {
        assertLogContaining("DistributedAnnotationProcessor-is-go");
        assertLogContaining("[-Abuild.dir=");
        assertLogContaining("visiting DistributedAnnotationFactory");
    }
}

