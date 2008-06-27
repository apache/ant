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
 * @created 01 May 2001
 */
public class SleepTest extends BuildFileTest {


    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/";
    private final static boolean TRACE=false;
	private final static int ERROR_RANGE=1000;
	
    public SleepTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(TASKDEFS_DIR + "sleep.xml");
    }

    public void test1() {
       Timer timer=new Timer();
        executeTarget("test1");
        timer.stop();
        if(TRACE) System.out.println(" test1 elapsed time="+timer.time());
        assertTrue(timer.time()>=0);
    }

    public void test2() {
        Timer timer=new Timer();
        executeTarget("test2");
        timer.stop();
        if(TRACE) System.out.println(" test2 elapsed time="+timer.time());
        assertTrue(timer.time()>=0);
    }

    public void test3() {
        Timer timer=new Timer();
        executeTarget("test3");
        timer.stop();
        if(TRACE) System.out.println(" test3 elapsed time="+timer.time());
        assertTrue(timer.time()>=(2000-ERROR_RANGE));
    }

    public void test4() {
        Timer timer=new Timer();
        executeTarget("test3");
        timer.stop();
        if(TRACE) System.out.println(" test4 elapsed time="+timer.time());
        assertTrue(timer.time()>=(2000-ERROR_RANGE) && timer.time()<60000);
    }

    public void test5() {
        expectBuildException("test5",
            "Negative sleep periods are not supported");
    }

    public void test6() {
        Timer timer=new Timer();
        executeTarget("test6");
        timer.stop();
        if(TRACE) System.out.println(" test6 elapsed time="+timer.time());
        assertTrue(timer.time()<2000);
    }


    /**
    * inner timer class
    */
    private static class Timer {
        long start=0;
        long stop=0;

        public Timer() {
            start();
        }

        public void start() {
            start=System.currentTimeMillis();
        }

        public void stop() {
            stop=System.currentTimeMillis();
        }

        public long time() {
            return stop-start;
        }
    }

}

