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

import static org.junit.Assert.assertTrue;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SleepTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    private static final int ERROR_RANGE = 1000;

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/sleep.xml");
    }

    @Test
    public void test1() {
       Timer timer = new Timer();
        buildRule.executeTarget("test1");
        timer.stop();
        assertTrue(timer.time() >= 0);
    }

    @Test
    public void test2() {
        Timer timer = new Timer();
        buildRule.executeTarget("test2");
        timer.stop();
        assertTrue(timer.time() >= 0);
    }

    @Test
    public void test3() {
        Timer timer = new Timer();
        buildRule.executeTarget("test3");
        timer.stop();
        assertTrue(timer.time() >= (2000 - ERROR_RANGE));
    }

    @Test
    public void test4() {
        Timer timer = new Timer();
        buildRule.executeTarget("test3");
        timer.stop();
        assertTrue(timer.time() >= (2000 - ERROR_RANGE) && timer.time() < 60000);
    }

    /**
     * Expected failure: negative sleep periods are not supported
     */
    @Test(expected = BuildException.class)
    public void test5() {
        buildRule.executeTarget("test5");
        // TODO assert value
    }

    @Test
    public void test6() {
        Timer timer = new Timer();
        buildRule.executeTarget("test6");
        timer.stop();
        assertTrue(timer.time() < 2000);
    }


    /**
    * inner timer class
    */
    private static class Timer {
        long start = 0;
        long stop = 0;

        public Timer() {
            start();
        }

        public void start() {
            start = System.currentTimeMillis();
        }

        public void stop() {
            stop = System.currentTimeMillis();
        }

        public long time() {
            return stop - start;
        }
    }

}
