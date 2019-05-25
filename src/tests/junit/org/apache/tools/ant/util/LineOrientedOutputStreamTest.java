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

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LineOrientedOutputStreamTest {

    private static String LINE = "This is a line";
    private DummyStream stream;


    @Before
    public void setUp() {
        stream = new DummyStream();
    }

    @After
    public void tearDown() throws IOException {
        if (stream != null) {
            stream.close();
        }
    }

    @Test
    public void testLineWithLinefeedArray() throws IOException {
        writeByteArray();
        writeAsArray('\n');
        stream.assertInvoked();
    }

    @Test
    public void testLineWithLinefeedSingleBytes() throws IOException {
        writeSingleBytes();
        stream.write('\n');
        stream.assertInvoked();
    }

    @Test
    public void testLineWithCarriageReturnArray() throws IOException {
        writeByteArray();
        writeAsArray('\r');
        stream.assertInvoked();
    }

    @Test
    public void testLineWithCarriageReturnSingleBytes() throws IOException {
        writeSingleBytes();
        stream.write('\r');
        stream.assertInvoked();
    }

    @Test
    public void testLineWithCarriageReturnLineFeedArray() throws IOException {
        writeByteArray();
        writeAsArray('\r');
        writeAsArray('\n');
        stream.assertInvoked();
    }

    @Test
    public void testLineWithCarriageReturnLineFeedSingleBytes() throws IOException {
        writeSingleBytes();
        stream.write('\r');
        stream.write('\n');
        stream.assertInvoked();
    }

    @Test
    public void testFlushArray() throws IOException {
        writeByteArray();
        stream.flush();
        stream.assertNotInvoked();
    }

    @Test
    public void testFlushSingleBytes() throws IOException {
        writeSingleBytes();
        stream.flush();
        stream.assertNotInvoked();
    }

    @Test
    public void testCloseArray() throws IOException {
        writeByteArray();
        stream.close();
        stream.assertInvoked();
        stream = null;
    }

    @Test
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
        for (byte b : LINE.getBytes()) {
            stream.write(b);
        }
    }

    private void writeAsArray(char c) throws IOException {
        stream.write(new byte[] {(byte) c}, 0, 1);
    }

    private class DummyStream extends LineOrientedOutputStream {
        private boolean invoked;

        protected void processLine(String line) {
            assertFalse("Only one line", invoked);
            assertEquals(LINE, line);
            invoked = true;
        }

        private void assertInvoked() {
            assertTrue("At least one line", invoked);
        }

        private void assertNotInvoked() {
            assertFalse("No output", invoked);
        }
    }
}
