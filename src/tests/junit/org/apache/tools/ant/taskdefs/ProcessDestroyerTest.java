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

/*
 * Created on Feb 19, 2003
 */
package org.apache.tools.ant.taskdefs;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
//TODO this test seems really unsafe due to the infinite loop
public class ProcessDestroyerTest {

    @Test
    public void testProcessDestroyer() throws IOException {
        ProcessDestroyer processDestroyer = new ProcessDestroyer();
        Process process =
            Runtime.getRuntime().exec(
                "java -cp "
                    + System.getProperty("java.class.path")
                    + " "
                    + getClass().getName());

        assertFalse("Not registered as shutdown hook",
                    processDestroyer.isAddedAsShutdownHook());

        processDestroyer.add(process);

        assertTrue("Registered as shutdown hook",
                   processDestroyer.isAddedAsShutdownHook());
        try {
            process.destroy();
        } finally {
            processDestroyer.remove(process);
        }

        assertFalse("Not registered as shutdown hook",
                    processDestroyer.isAddedAsShutdownHook());

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new ProcessDestroyerTest().testProcessDestroyer();
        Thread.sleep(60000);
    }
}
