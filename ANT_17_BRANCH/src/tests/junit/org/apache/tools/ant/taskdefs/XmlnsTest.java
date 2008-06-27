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
import org.apache.tools.ant.Task;

/**
 */
public class XmlnsTest extends BuildFileTest {
    public XmlnsTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/xmlns.xml");
    }

    public void testXmlns() {
        expectLog("xmlns", "MyTask called");
    }

    public void testXmlnsFile() {
        expectLog("xmlns.file", "MyTask called");
    }

    public void testCore() {
        expectLog("core", "MyTask called");
    }

    public void testExcluded() {
        expectBuildExceptionContaining(
            "excluded", "excluded uri",
            "Attempt to use a reserved URI ant:notallowed");
    }

    public void testOther() {
        expectLog("other", "a message");
    }

    public void testNsAttributes() {
        expectLog("ns.attributes", "hello world");
    }

    public static class MyTask extends Task {
        public void execute() {
            log("MyTask called");
        }
    }

}

