/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * Wraps a String as an InputStream.
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
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
