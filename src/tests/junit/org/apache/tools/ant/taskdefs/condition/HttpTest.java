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
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildFileTest;

/**
 * Testcases for the &lt;http&gt; condition. All these tests require
 * us to be online as they attempt to get the status of various pages
 * on the Ant Apache web site.
 */
public class HttpTest extends BuildFileTest {

    public HttpTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/conditions/http.xml");
    }

    public void testNoMethod() {
       expectPropertySet("basic-no-method", "basic-no-method");
       assertPropertyUnset("basic-no-method-bad-url");
    }

    public void testHeadRequest() {
       expectPropertySet("test-head-request", "test-head-request");
       assertPropertyUnset("test-head-request-bad-url");
    }

    public void testGetRequest() {
       expectPropertySet("test-get-request", "test-get-request");
       assertPropertyUnset("test-get-request-bad-url");
    }

    public void testBadRequestMethod() {
        expectSpecificBuildException("bad-request-method",
                                     "invalid HTTP request method specified",
                                     null);
    }

}
