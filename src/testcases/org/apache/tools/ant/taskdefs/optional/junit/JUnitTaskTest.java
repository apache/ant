/*
 * Copyright  2002,2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.junit;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
import java.lang.reflect.InvocationTargetException;

public class JUnitTaskTest extends BuildFileTest {

    /**
     * Constructor for the JUnitTaskTest object
     *
     * @param name we dont know
     */
    public JUnitTaskTest(String name) {
        super(name);
    }


    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/optional/junit.xml");
    }


    /**
     * The teardown method for JUnit
     */
    public void tearDown() {
        //executeTarget("cleanup");
    }

    public void testCrash() {
       expectPropertySet("crash", "crashed");
    }

    public void testNoCrash() {
       expectPropertyUnset("nocrash", "crashed");
    }

    public void testTimeout() {
       expectPropertySet("timeout", "timeout");
    }

    public void testNoTimeout() {
       expectPropertyUnset("notimeout", "timeout");
    }

}

