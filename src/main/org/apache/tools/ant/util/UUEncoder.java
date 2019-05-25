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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * UUEncoding of an input stream placed into an OutputStream.
 * This class is meant to be a drop in replacement for
 * sun.misc.UUEncoder, which was previously used by Ant.
 * The uuencode algorithm code has been copied from the
 * geronimo project.
 **/

public class UUEncoder {
    protected static final int DEFAULT_MODE = 644;
    private static final int MAX_CHARS_PER_LINE = 45;
    private static final int INPUT_BUFFER_SIZE = MAX_CHARS_PER_LINE * 100;
    private OutputStream out;
    private String name;

    /**
     * Constructor specifying a name for the encoded buffer, begin
     * line will be:
     * <pre>
     *   begin 644 [NAME]
     * </pre>
     * @param name the name of the encoded buffer.
     */
    public UUEncoder(String name) {
        this.name = name;
    }

    /**
     * UUEncode bytes from the input stream, and write them as text characters
     * to the output stream. This method will run until it exhausts the
     * input stream.
     * @param is the input stream.
     * @param out the output stream.
     * @throws IOException if there is an error.
     */
    public void encode(InputStream is, OutputStream out)
        throws IOException {
        this.out = out;
        encodeBegin();
        byte[] buffer = new byte[INPUT_BUFFER_SIZE];
        int count;
        while ((count = is.read(buffer, 0, buffer.length)) != -1) {
            int pos = 0;
            while (count > 0) {
                int num = count > MAX_CHARS_PER_LINE
                    ? MAX_CHARS_PER_LINE
                    : count;
                encodeLine(buffer, pos, num, out);
                pos += num;
                count -= num;
            }
        }
        out.flush();
        encodeEnd();
    }

    /**
     * Encode a string to the output.
     */
    private void encodeString(String n) {
        PrintStream writer = new PrintStream(out);
        writer.print(n);
        writer.flush();
    }

    private void encodeBegin() {
        encodeString("begin " + DEFAULT_MODE + " " + name + "\n");
    }

    private void encodeEnd() {
        encodeString(" \nend\n");
    }

    /**
     * Encode a single line of data (less than or equal to 45 characters).
     *
     * @param data   The array of byte data.
     * @param offset The starting offset within the data.
     * @param length Length of the data to encode.
     * @param out    The output stream the encoded data is written to.
     * @exception IOException if something goes wrong
     */
    private void encodeLine(
        byte[] data, int offset, int length, OutputStream out)
        throws IOException {
        // write out the number of characters encoded in this line.
        // CheckStyle:MagicNumber OFF
        out.write((byte) ((length & 0x3F) + ' '));
        // CheckStyle:MagicNumber ON
        byte a;
        byte b;
        byte c;

        for (int i = 0; i < length;) {
            // set the padding defaults
            b = 1;
            c = 1;
            // get the next 3 bytes (if we have them)
            a = data[offset + i++];
            if (i < length) {
                b = data[offset + i++];
                if (i < length) {
                    c = data[offset + i++];
                }
            }

            // CheckStyle:MagicNumber OFF
            byte d1 = (byte) (((a >>> 2) & 0x3F) + ' ');
            byte d2 = (byte) ((((a << 4) & 0x30) | ((b >>> 4) & 0x0F)) + ' ');
            byte d3 = (byte) ((((b << 2) & 0x3C) | ((c >>> 6) & 0x3)) + ' ');
            byte d4 = (byte) ((c & 0x3F) + ' ');
            // CheckStyle:MagicNumber ON

            out.write(d1);
            out.write(d2);
            out.write(d3);
            out.write(d4);
        }

        // terminate with a linefeed alone
        out.write('\n');
    }
}
