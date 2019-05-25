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
package org.apache.tools.ant.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * TestCase for Base64Converter.
 *
 */
public class Base64ConverterTest {

    private Base64Converter base64Converter;

    @Before
    public void setUp() {
        base64Converter = new Base64Converter();
    }

    @Test
    public void testOneValue() {
        assertEquals("AAD/",
                base64Converter.encode(new byte[]{0, 0, (byte) 0xFF}));
    }

    @Test
    public void testHelloWorld() {
        assertEquals("SGVsbG8gV29ybGQ=",
                base64Converter.encode("Hello World".getBytes()));
    }
}
