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
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.types.Commandline;

import org.junit.Test;

import static org.junit.Assert.fail;
import static org.apache.tools.ant.AntAssert.assertContains;

public class RpmTest {

    @Test
    public void testShouldThrowExceptionWhenRpmFails() throws Exception {
        Rpm rpm = new MyRpm();
        rpm.setProject(new org.apache.tools.ant.Project());
        rpm.setFailOnError(true);
        // execute
        try {
            rpm.execute();
            fail("should have thrown a build exception");
        } catch (BuildException ex) {
            assertContains("' failed with exit code 2", ex.getMessage());
        }
    }

    @Test
    public void testShouldNotThrowExceptionWhenRpmFails() throws Exception {
        Rpm rpm = new MyRpm();
        rpm.execute();
    }

    // override some of the code so we can test the handling of the
    // return code only.
    public static class MyRpm extends Rpm {
        protected Execute getExecute(Commandline toExecute,
                                     ExecuteStreamHandler streamhandler) {
            return new Execute() {
                    public int execute() {
                        // 2 is != 0 and even, so it is considered
                        // failure on any platform currently supported
                        // by Execute#isFailure.
                        return 2;
                    }
                };
        }

        public void log(String msg, int msgLevel) {
        }
    }

}
