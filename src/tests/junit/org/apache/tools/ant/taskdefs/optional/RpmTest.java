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
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.types.Commandline;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RpmTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testShouldThrowExceptionWhenRpmFails() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("' failed with exit code 2");
        Rpm rpm = new MyRpm();
        rpm.setProject(new Project());
        rpm.setFailOnError(true);
        rpm.execute();
    }

    @Test
    public void testShouldNotThrowExceptionWhenRpmFails() {
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
