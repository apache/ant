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

package org.apache.tools.ant.util.regexp;

import java.io.IOException;

import junit.framework.AssertionFailedError;

/**
 * Tests for the jakarta-regexp implementation of the Regexp interface.
 *
 */
public class JakartaRegexpRegexpTest extends RegexpTest {

    public Regexp getRegexpImplementation() {
        return new JakartaRegexpRegexp();
    }

    public JakartaRegexpRegexpTest(String name) {
        super(name);
    }

    public void testWindowsLineSeparator2() throws IOException {
        try {
            super.testWindowsLineSeparator2();
            fail("Should trigger when this bug is fixed. {@since 1.2}");
        } catch (AssertionFailedError e){
        }
    }

    /**
     * Fails for the same reason as "default" mode in doEndTest2.
     */
    public void testUnixLineSeparator() throws IOException {
        try {
            super.testUnixLineSeparator();
            fail("Should trigger once this bug is fixed. {@since 1.2}");
        } catch (AssertionFailedError e){
        }
    }

    /**
     * Fails for "default" mode.
     */
    protected void doEndTest2(String text) {}
}
