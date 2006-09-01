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
 * Testcases for the &lt;isreference&gt; condition.
 *
 */
public class IsReferenceTest extends BuildFileTest {

    public IsReferenceTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/conditions/isreference.xml");
    }

    public void testBasic() {
       expectPropertySet("basic", "global-path");
       assertPropertySet("target-path");
       assertPropertyUnset("undefined");
    }

    public void testNotEnoughArgs() {
        expectSpecificBuildException("isreference-incomplete",
                                     "refid attribute has been omitted",
                                     "No reference specified for isreference "
                                     + "condition");
    }

    public void testType() {
       expectPropertySet("type", "global-path");
       assertPropertyUnset("global-path-as-fileset");
       assertPropertyUnset("global-path-as-foo");
       assertPropertySet("global-echo");
    }

}
