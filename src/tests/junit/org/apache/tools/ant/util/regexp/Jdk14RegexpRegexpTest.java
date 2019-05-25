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

package org.apache.tools.ant.util.regexp;

import org.junit.Test;

/**
 * Tests for the JDK 1.4 implementation of the Regexp interface.
 *
 */
public class Jdk14RegexpRegexpTest extends RegexpTest {

    public Regexp getRegexpImplementation() {
        return new Jdk14RegexpRegexp();
    }

    /**
     * Should trigger once fixed. {@since JDK 1.4RC1}
     */
    @Test
    public void testParagraphCharacter() {
        super.testParagraphCharacter();
    }

    /**
     * Should trigger once fixed. {@since JDK 1.4RC1}
     */
    @Test
    public void testLineSeparatorCharacter() {
        super.testLineSeparatorCharacter();
    }

    /**
     * Should trigger once fixed. {@since JDK 1.4RC1}
     */
    @Test
    public void testStandaloneCR() {
        super.testStandaloneCR();
    }

    /**
     * Should trigger once fixed. {@since JDK 1.4RC1}
     */
    @Test
    public void testWindowsLineSeparator() {
        super.testWindowsLineSeparator();
    }

}
