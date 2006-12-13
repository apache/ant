/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.rjunit.remote;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;
import junit.extensions.TestSetup;

/**
 *
 */
public class TestCases {

    /** null testcase w/ 3 tests */
    public static class NullTestCase extends TestCase {
        public NullTestCase(String s) {
            super(s);
        }
        public void testSuccess(){}
        public void testFailure(){ assertTrue(false); }
        public void testError(){ throw new RuntimeException("on purpose"); }
    }

    /** testcase w/ a static suite method */
    public static class NullTestSuite extends TestCase {
        public NullTestSuite(String s) {
            super(s);
        }
        public static Test suite(){
            return new TestSuite(NullTestCase.class);
        }
    }

    public static class SimpleTestCase extends TestCase {
        public SimpleTestCase(String s) {
            super(s);
        }
        public void testSuccess(){}
    }

    public static class FailSetupTestSuite extends TestCase {
        public FailSetupTestSuite(String s) {
            super(s);
        }
        public static Test suite(){
            return new FailTestSetup( new TestSuite(SimpleTestCase.class) );
        }
    }

    public static class FailTestSetup extends TestSetup {
        public FailTestSetup(Test test) {
            super(test);
        }
        protected void setUp(){
            throw new IllegalArgumentException("on purpose");
        }
    }
}
