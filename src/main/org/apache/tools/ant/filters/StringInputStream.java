/*
 * Copyright  2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * Wraps a String as an InputStream.
 *
 * @author Magesh Umasankar
 */
public class StringInputStream
    extends InputStream {

    /** Source string, stored as a StringReader */
    private StringReader in;

    private String encoding;

    private byte[] slack;

    private int begin;

    /**
     * Composes a stream from a String
     *
     * @param source The string to read from. Must not be <code>null</code>.
     */
    public StringInputStream(String source) {
        in = new StringReader(source);
    }

    /**
     * Composes a stream from a String with the specified encoding
     *
     * @param source The string to read from. Must not be <code>null</code>.
     * @param encoding The encoding scheme.
     */
    public StringInputStream(String source, String encoding) {
        in = new StringReader(source);
        this.encoding = encoding;
    }

    /**
     * Reads from the Stringreader, returning the same value.
     *
     * @return the value of the next character in the StringReader
     *
     * @exception IOException if the original StringReader fails to be read
     */
    public synchronized int read() throws IOException {
        if (in == null) {
            throw new IOException("Stream Closed");
        }

        byte result;
        if (slack != null && begin < slack.length) {
            result = slack[begin];
            if (++begin == slack.length) {
                slack = null;
            }
        } else {
            byte[] buf = new byte[1];
            if (read(buf, 0, 1) <= 0) {
                return -1;
            }
            result = buf[0];
        }
        if (result < 0) {
            return 256 + result;
        } else {
            return result;
        }
    }

    /**
     * Reads from the Stringreader into a byte array
     *
     * @param b  the byte array to read into
     * @param off the offset in the byte array
     * @param len the length in the byte array to fill
     * @return the actual number read into the byte array, -1 at
     *         the end of the stream
     * @exception IOException if an error occurs
     */
    public synchronized int read(byte[] b, int off, int len)
        throws IOException {

        if (in == null) {
            throw new IOException("Stream Closed");
        }

        while (slack == null) {
            char[] buf = new char[len]; // might read too much
            int n = in.read(buf);
            if (n == -1) {
                return -1;
            }
            if (n > 0) {
                String s = new String(buf, 0, n);
                if (encoding == null) {
                    slack = s.getBytes();
                } else {
                    slack = s.getBytes(encoding);
                }
                begin = 0;
            }
        }

        if (len > slack.length - begin) {
            len = slack.length - begin;
        }

        System.arraycopy(slack, begin, b, off, len);

        if ((begin += len) >= slack.length) {
            slack = null;
        }
        return len;
    }

    /**
     * Marks the read limit of the StringReader.
     *
     * @param limit the maximum limit of bytes that can be read before the
     *              mark position becomes invalid
     */
    public synchronized void mark(final int limit) {
        try {
            in.mark(limit);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }


    /**
     * @return   the current number of bytes ready for reading
     * @exception IOException if an error occurs
     */
    public synchronized int available() throws IOException {
        if (in == null) {
            throw new IOException("Stream Closed");
        }
        if (slack != null) {
            return slack.length - begin;
        }
        if (in.ready()) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * @return false - mark is not supported
     */
    public boolean markSupported () {
        return false;   // would be imprecise
    }

    /**
     * Resets the StringReader.
     *
     * @exception IOException if the StringReader fails to be reset
     */
    public synchronized void reset() throws IOException {
        if (in == null) {
            throw new IOException("Stream Closed");
        }
        slack = null;
        in.reset();
    }

    /**
     * Closes the Stringreader.
     *
     * @exception IOException if the original StringReader fails to be closed
     */
    public synchronized void close() throws IOException {
        in.close();
        slack = null;
        in = null;
    }
}
