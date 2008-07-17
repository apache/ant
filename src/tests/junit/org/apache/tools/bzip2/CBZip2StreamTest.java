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
package org.apache.tools.bzip2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

public class CBZip2StreamTest extends TestCase {

    public void testNullPointer() throws IOException {
        try {
            CBZip2InputStream cb = new CBZip2InputStream(new ByteArrayInputStream(new byte[0]));
            fail("expected an exception");
        } catch (IOException e) {
            // expected
        }
    }
    
    public void testDivisionByZero() throws IOException {
        CBZip2OutputStream cb = new CBZip2OutputStream(new ByteArrayOutputStream());
        cb.close();
        // expected no exception
    }
}
