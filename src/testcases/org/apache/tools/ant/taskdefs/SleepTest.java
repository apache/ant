/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileTest;
/**
 * @author steve_l@iseran.com steve loughran
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

