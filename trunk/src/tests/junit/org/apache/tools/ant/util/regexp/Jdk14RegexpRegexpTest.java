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
 * Tests for the JDK 1.4 implementation of the Regexp interface.
 *
 */
public class Jdk14RegexpRegexpTest extends RegexpTest {

    public Regexp getRegexpImplementation() {
        return new Jdk14RegexpRegexp();
    }

    public Jdk14RegexpRegexpTest(String name) {
        super(name);
    }

    public void testParagraphCharacter() throws IOException {
        try {
            super.testParagraphCharacter();
            fail("Should trigger once fixed. {@since JDK 1.4RC1}");
        } catch (AssertionFailedError e){
        }
    }

    public void testLineSeparatorCharacter() throws IOException {
        try {
            super.testLineSeparatorCharacter();
            fail("Should trigger once fixed. {@since JDK 1.4RC1}");
        } catch (AssertionFailedError e){
        }
    }

    public void testStandaloneCR() throws IOException {
        try {
            super.testStandaloneCR();
            fail("Should trigger once fixed. {@since JDK 1.4RC1}");
        } catch (AssertionFailedError e){
        }
    }

    public void testWindowsLineSeparator() throws IOException {
        try {
            super.testWindowsLineSeparator();
            fail("Should trigger once fixed. {@since JDK 1.4RC1}");
        } catch (AssertionFailedError e){
        }
    }

}
