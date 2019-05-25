/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.tools.zip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test zip encodings.
 */
public class ZipEncodingTest {
    private static final String UNENC_STRING = "\u2016";

    // stress test for internal grow method.
    private static final String BAD_STRING =
        "\u2016\u2015\u2016\u2015\u2016\u2015\u2016\u2015\u2016\u2015\u2016";

    private static final String BAD_STRING_ENC =
        "%U2016%U2015%U2016%U2015%U2016%U2015%U2016%U2015%U2016%U2015%U2016";

    @Test
    public void testSimpleCp437Encoding() throws IOException {

        doSimpleEncodingTest("Cp437", null);
    }

    @Test
    public void testSimpleCp850Encoding() throws IOException {

        doSimpleEncodingTest("Cp850", null);
    }

    @Test
    public void testNioCp1252Encoding() throws IOException {
        // CP1252 has some undefined code points, these are
        // the defined ones
        // retrieved by
        //    awk '/^0x/ && NF>2 {print $1;}' CP1252.TXT
        byte[] b = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                         0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
                         0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
                         0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
                         0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
                         0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
                         0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
                         0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
                         0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
                         0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
                         0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57,
                         0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F,
                         0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67,
                         0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F,
                         0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77,
                         0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F,
                         (byte) 0x80, (byte) 0x82, (byte) 0x83, (byte) 0x84,
                         (byte) 0x85, (byte) 0x86, (byte) 0x87, (byte) 0x88,
                         (byte) 0x89, (byte) 0x8A, (byte) 0x8B, (byte) 0x8C,
                         (byte) 0x8E, (byte) 0x91, (byte) 0x92, (byte) 0x93,
                         (byte) 0x94, (byte) 0x95, (byte) 0x96, (byte) 0x97,
                         (byte) 0x98, (byte) 0x99, (byte) 0x9A, (byte) 0x9B,
                         (byte) 0x9C, (byte) 0x9E, (byte) 0x9F, (byte) 0xA0,
                         (byte) 0xA1, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4,
                         (byte) 0xA5, (byte) 0xA6, (byte) 0xA7, (byte) 0xA8,
                         (byte) 0xA9, (byte) 0xAA, (byte) 0xAB, (byte) 0xAC,
                         (byte) 0xAD, (byte) 0xAE, (byte) 0xAF, (byte) 0xB0,
                         (byte) 0xB1, (byte) 0xB2, (byte) 0xB3, (byte) 0xB4,
                         (byte) 0xB5, (byte) 0xB6, (byte) 0xB7, (byte) 0xB8,
                         (byte) 0xB9, (byte) 0xBA, (byte) 0xBB, (byte) 0xBC,
                         (byte) 0xBD, (byte) 0xBE, (byte) 0xBF, (byte) 0xC0,
                         (byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4,
                         (byte) 0xC5, (byte) 0xC6, (byte) 0xC7, (byte) 0xC8,
                         (byte) 0xC9, (byte) 0xCA, (byte) 0xCB, (byte) 0xCC,
                         (byte) 0xCD, (byte) 0xCE, (byte) 0xCF, (byte) 0xD0,
                         (byte) 0xD1, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4,
                         (byte) 0xD5, (byte) 0xD6, (byte) 0xD7, (byte) 0xD8,
                         (byte) 0xD9, (byte) 0xDA, (byte) 0xDB, (byte) 0xDC,
                         (byte) 0xDD, (byte) 0xDE, (byte) 0xDF, (byte) 0xE0,
                         (byte) 0xE1, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4,
                         (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8,
                         (byte) 0xE9, (byte) 0xEA, (byte) 0xEB, (byte) 0xEC,
                         (byte) 0xED, (byte) 0xEE, (byte) 0xEF, (byte) 0xF0,
                         (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4,
                         (byte) 0xF5, (byte) 0xF6, (byte) 0xF7, (byte) 0xF8,
                         (byte) 0xF9, (byte) 0xFA, (byte) 0xFB, (byte) 0xFC,
                         (byte) 0xFD, (byte) 0xFE, (byte) 0xFF};

        doSimpleEncodingTest("Cp1252", b);
    }

    private static void assertByteEquals(byte[] expected, ByteBuffer actual) {

        assertEquals(expected.length, actual.limit());

        for (byte expectedByte : expected) {
            assertEquals(expectedByte, actual.get());
        }

    }

    private void doSimpleEncodingTest(String name, byte[] testBytes)
        throws IOException {

        ZipEncoding enc = ZipEncodingHelper.getZipEncoding(name);

        if (testBytes == null) {

            testBytes = new byte[256];
            for (int i = 0; i < 256; ++i) {
                testBytes[i] = (byte) i;
            }
        }

        String decoded = enc.decode(testBytes);

        assertTrue(enc.canEncode(decoded));

        ByteBuffer encoded = enc.encode(decoded);

        assertByteEquals(testBytes, encoded);

        assertFalse(enc.canEncode(UNENC_STRING));
        assertByteEquals("%U2016".getBytes(StandardCharsets.US_ASCII), enc.encode(UNENC_STRING));
        assertFalse(enc.canEncode(BAD_STRING));
        assertByteEquals(BAD_STRING_ENC.getBytes(StandardCharsets.US_ASCII),
                enc.encode(BAD_STRING));
    }

}
