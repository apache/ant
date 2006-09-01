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

import java.io.IOException;
import junit.framework.TestCase;

/**
 */

public class LineOrientedOutputStreamTest extends TestCase {

    private static String LINE = "This is a line";
    private DummyStream stream;

    public LineOrientedOutputStreamTest(String name) {
        super(name);
    }
    
    public void setUp() {
        stream = new DummyStream();
    }

    public void tearDown() throws IOException {
        if (stream != null) {
            stream.close();
        }
    }

    public void testLineWithLinefeedArray() throws IOException {
        writeByteArray();
        writeAsArray('\n');
        stream.assertInvoked();
    }

    public void testLineWithLinefeedSingleBytes() throws IOException {
        writeSingleBytes();
        stream.write('\n');
        stream.assertInvoked();
    }

    public void testLineWithCariagereturnArray() throws IOException {
        writeByteArray();
        writeAsArray('\r');
        stream.assertInvoked();
    }

    public void testLineWithCariagereturnSingleBytes() throws IOException {
        writeSingleBytes();
        stream.write('\r');
        stream.assertInvoked();
    }

    public void testLineWithCariagereturnLinefeedArray() throws IOException {
        writeByteArray();
        writeAsArray('\r');
        writeAsArray('\n');
        stream.assertInvoked();
    }

    public void testLineWithCariagereturnLinefeedSingleBytes() throws IOException {
        writeSingleBytes();
        stream.write('\r');
        stream.write('\n');
        stream.assertInvoked();
    }

    public void testFlushArray() throws IOException {
        writeByteArray();
        stream.flush();
        stream.assertInvoked();
    }

    public void testFlushSingleBytes() throws IOException {
        writeSingleBytes();
        stream.flush();
        stream.assertInvoked();
    }

    public void testCloseArray() throws IOException {
        writeByteArray();
        stream.close();
        stream.assertInvoked();
        stream = null;
    }

    public void testCloseSingleBytes() throws IOException {
        writeSingleBytes();
        stream.close();
        stream.assertInvoked();
        stream = null;
    }

    private void writeByteArray() throws IOException {
        stream.write(LINE.getBytes(), 0, LINE.length());
    }

    private void writeSingleBytes() throws IOException {
        byte[] b = LINE.getBytes();
        for (int i = 0; i < b.length; i++) {
            stream.write(b[i]);
        }
    }

    private void writeAsArray(char c) throws IOException {
        stream.write(new byte[] {(byte) c}, 0, 1);
    }

    private class DummyStream extends LineOrientedOutputStream {
        private boolean invoked;
        protected void processLine(String line) {
            LineOrientedOutputStreamTest.this.assertFalse("Only one line",
                                                          invoked);
            LineOrientedOutputStreamTest.this.assertEquals(LINE, line);
            invoked = true;
        }

        private void assertInvoked() {
            LineOrientedOutputStreamTest.this.assertTrue("At least one line",
                                                          invoked);
        }
    }
}// LineOrientedOutputStreamTest
