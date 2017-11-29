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
package org.apache.tools.ant.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * TestCase for Base64Converter.
 *
 */
public class Base64ConverterTest {

    @Test
    public void testOneValue() {
        byte[] mybytes = {0, 0, (byte)0xFF};
        Base64Converter base64Converter = new Base64Converter();
        assertEquals("AAD/",base64Converter.encode(mybytes));
    }

    @Test
    public void testHelloWorld() {
        byte[] mybytes = "Hello World".getBytes();
        Base64Converter base64Converter = new Base64Converter();
        assertEquals("SGVsbG8gV29ybGQ=", base64Converter.encode(mybytes));
    }
}
