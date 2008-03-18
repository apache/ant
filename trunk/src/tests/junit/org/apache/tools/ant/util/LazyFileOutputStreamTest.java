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

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;

/**
 * @since Ant 1.6
 */
public class LazyFileOutputStreamTest extends TestCase {
    private LazyFileOutputStream los;
    private final static File f = new File("test.txt");

    public LazyFileOutputStreamTest(String s) {
        super(s);
    }

    public void setUp() {
        los = new LazyFileOutputStream(f);
    }

    public void tearDown() throws IOException {
        try {
            los.close();
        } finally {
            f.delete();
        }
    }

    public void testNoFileWithoutWrite() throws IOException {
        los.close();
        assertTrue(f + " has not been written.", !f.exists());
    }

    public void testOpen() throws IOException {
        los.open();
        los.close();
        assertTrue(f + " has been written.", f.exists());
    }

    public void testSingleByte() throws IOException {
        los.write(0);
        los.close();
        assertTrue(f + " has been written.", f.exists());
    }

    public void testByteArray() throws IOException {
        los.write(new byte[] {0});
        los.close();
        assertTrue(f + " has been written.", f.exists());
    }
}
