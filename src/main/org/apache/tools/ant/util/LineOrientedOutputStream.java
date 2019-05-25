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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Invokes {@link #processLine processLine} whenever a full line has
 * been written to this stream.
 *
 * <p>Tries to be smart about line separators.</p>
 */
public abstract class LineOrientedOutputStream extends OutputStream {

    /** Initial buffer size. */
    private static final int INITIAL_SIZE = 132;

    /** Carriage return */
    private static final int CR = 0x0d;

    /** Linefeed */
    private static final int LF = 0x0a;

    private ByteArrayOutputStream buffer
        = new ByteArrayOutputStream(INITIAL_SIZE);
    private boolean skip = false;

    /**
     * Write the data to the buffer and flush the buffer, if a line
     * separator is detected.
     *
     * @param cc data to log (byte).
     * @throws IOException if there is an error.
     */
    @Override
    public final void write(int cc) throws IOException {
        final byte c = (byte) cc;
        if (c == LF || c == CR) {
            if (!skip) {
              processBuffer();
            }
        } else {
            buffer.write(cc);
        }
        skip = (c == CR);
    }

    /**
     * Flush this log stream
     * @throws IOException if there is an error.
     */
    @Override
    public void flush() throws IOException {
    }

    /**
     * Converts the buffer to a byte[] and sends it to
     * <code>processLine</code>
     * @throws IOException if there is an error.
     */
    protected void processBuffer() throws IOException {
        try {
            processLine(buffer.toByteArray());
        } finally {
            buffer.reset();
        }
    }

    /**
     * Processes a line.
     *
     * @param line the line to log.
     * @throws IOException if there is an error.
     */
    protected abstract void processLine(String line) throws IOException;

    /**
     * Processes a line.
     *
     * <p>This implementations invokes the string-arg version
     * converting the byte array using the default encoding.
     * Subclasses are encouraged to override this method (and provide
     * a dummy implementation of the String-arg version) so they don't
     * interfere with the encoding of the underlying stream.</p>
     *
     * @param line the line to log.
     * @throws IOException if there is an error.
     * @since Ant 1.8.3
     */
    protected void processLine(byte[] line) throws IOException {
        processLine(new String(line));
    }

    /**
     * Writes all remaining
     * @throws IOException if there is an error.
     */
    @Override
    public void close() throws IOException {
        if (buffer.size() > 0) {
            processBuffer();
        }
        super.close();
    }

    /**
     * Write a block of characters to the output stream
     *
     * @param b the array containing the data
     * @param off the offset into the array where data starts
     * @param len the length of block
     *
     * @throws IOException if the data cannot be written into the stream.
     */
    @Override
    public final void write(byte[] b, int off, int len) throws IOException {
        // find the line breaks and pass other chars through in blocks
        int offset = off;
        int blockStartOffset = offset;
        int remaining = len;
        while (remaining > 0) {
            while (remaining > 0 && b[offset] != LF && b[offset] != CR) {
                offset++;
                remaining--;
            }
            // either end of buffer or a line separator char
            int blockLength = offset - blockStartOffset;
            if (blockLength > 0) {
                buffer.write(b, blockStartOffset, blockLength);
            }
            while (remaining > 0 && (b[offset] == LF || b[offset] == CR)) {
                write(b[offset]);
                offset++;
                remaining--;
            }
            blockStartOffset = offset;
        }
    }

}
