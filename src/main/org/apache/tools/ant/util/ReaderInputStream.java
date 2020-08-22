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
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

/**
 * Adapts a <code>Reader</code> as an <code>InputStream</code>.
 * <p>This is a stripped down version of {@code org.apache.commons.io.input.ReaderInputStream} of Apache Commons IO 2.7.</p>
 */
public class ReaderInputStream extends InputStream {
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private final Reader reader;
    private final CharsetEncoder encoder;

    /**
     * CharBuffer used as input for the decoder. It should be reasonably
     * large as we read data from the underlying Reader into this buffer.
     */
    private final CharBuffer encoderIn;

    /**
     * ByteBuffer used as output for the decoder. This buffer can be small
     * as it is only used to transfer data from the decoder to the
     * buffer provided by the caller.
     */
    private final ByteBuffer encoderOut;

    private CoderResult lastCoderResult;
    private boolean endOfInput;

    /**
     * Construct a new {@link ReaderInputStream}.
     *
     * @param reader the target {@link Reader}
     * @param encoder the charset encoder
     * @since 1.10.9
     */
    public ReaderInputStream(final Reader reader, final CharsetEncoder encoder) {
        this(reader, encoder, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Construct a new {@link ReaderInputStream}.
     *
     * @param reader the target {@link Reader}
     * @param encoder the charset encoder
     * @param bufferSize the size of the input buffer in number of characters
     * @since 1.10.9
     */
    public ReaderInputStream(final Reader reader, final CharsetEncoder encoder, final int bufferSize) {
        this.reader = reader;
        this.encoder = encoder;
        this.encoderIn = CharBuffer.allocate(bufferSize);
        this.encoderIn.flip();
        this.encoderOut = ByteBuffer.allocate(128);
        this.encoderOut.flip();
    }

    /**
     * Construct a <code>ReaderInputStream</code>
     * for the specified <code>Reader</code>.
     *
     * @param reader   <code>Reader</code>.  Must not be <code>null</code>.
     */
    public ReaderInputStream(Reader reader) {
        this(reader, Charset.defaultCharset());
    }

    /**
     * Construct a <code>ReaderInputStream</code>
     * for the specified <code>Reader</code>,
     * with the specified encoding.
     *
     * @param reader     non-null <code>Reader</code>.
     * @param encoding   non-null <code>String</code> encoding.
     */
    public ReaderInputStream(Reader reader, String encoding) {
        this(reader, Charset.forName(encoding));
    }

    /**
     * Construct a <code>ReaderInputStream</code>
     * for the specified <code>Reader</code>,
     * with the specified encoding.
     *
     * @param reader     non-null <code>Reader</code>.
     * @param charset    non-null <code>Charset</code> charset.
     * @since Ant 1.10.6
     */
    public ReaderInputStream(Reader reader, Charset charset) {
        this(reader,
             charset.newEncoder()
                 .onMalformedInput(CodingErrorAction.REPLACE)
                 .onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    /**
     * Fills the internal char buffer from the reader.
     *
     * @throws IOException
     *             If an I/O error occurs
     */
    private void fillBuffer() throws IOException {
        if (!endOfInput && (lastCoderResult == null || lastCoderResult.isUnderflow())) {
            encoderIn.compact();
            final int position = encoderIn.position();
            // We don't use Reader#read(CharBuffer) here because it is more efficient
            // to write directly to the underlying char array (the default implementation
            // copies data to a temporary char array).
            final int c = reader.read(encoderIn.array(), position, encoderIn.remaining());
            if (c == EOF) {
                endOfInput = true;
            } else {
                encoderIn.position(position+c);
            }
            encoderIn.flip();
        }
        encoderOut.compact();
        lastCoderResult = encoder.encode(encoderIn, encoderOut, endOfInput);
        encoderOut.flip();
    }

    /**
     * Read the specified number of bytes into an array.
     *
     * @param array the byte array to read into
     * @param off the offset to start reading bytes into
     * @param len the number of bytes to read
     * @return the number of bytes read or <code>-1</code>
     *         if the end of the stream has been reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(final byte[] array, int off, int len) throws IOException {
        Objects.requireNonNull(array, "array");
        if (len < 0 || off < 0 || (off + len) > array.length) {
            throw new IndexOutOfBoundsException("Array Size=" + array.length +
                    ", offset=" + off + ", length=" + len);
        }
        int read = 0;
        if (len == 0) {
            return 0; // Always return 0 if len == 0
        }
        while (len > 0) {
            if (encoderOut.hasRemaining()) {
                final int c = Math.min(encoderOut.remaining(), len);
                encoderOut.get(array, off, c);
                off += c;
                len -= c;
                read += c;
            } else {
                fillBuffer();
                if (endOfInput && !encoderOut.hasRemaining()) {
                    break;
                }
            }
        }
        return read == 0 && endOfInput ? EOF : read;
    }

    /**
     * Read the specified number of bytes into an array.
     *
     * @param b the byte array to read into
     * @return the number of bytes read or <code>-1</code>
     *         if the end of the stream has been reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Read a single byte.
     *
     * @return either the byte read or <code>-1</code> if the end of the stream
     *         has been reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        for (;;) {
            if (encoderOut.hasRemaining()) {
                return encoderOut.get() & 0xFF;
            }
            fillBuffer();
            if (endOfInput && !encoderOut.hasRemaining()) {
                return EOF;
            }
        }
    }

    /**
     * Close the stream. This method will cause the underlying {@link Reader}
     * to be closed.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }
}
