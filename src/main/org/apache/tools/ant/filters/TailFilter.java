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
 * 4. The names "Ant" and "Apache Software
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
import java.io.Reader;

import org.apache.tools.ant.types.Parameter;

/**
 * Reads the last <code>n</code> lines of a stream. (Default is last10 lines.)
 *
 * Example:
 *
 * <pre>&lt;tailfilter lines=&quot;3&quot;/&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader classname=&quot;org.apache.tools.ant.filters.TailFilter&quot;&gt;
 *   &lt;param name=&quot;lines&quot; value=&quot;3&quot;/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 * @author Magesh Umasankar
 */
public final class TailFilter
    extends BaseParamFilterReader
    implements ChainableReader {
    /** Parameter name for the number of lines to be returned. */
    private static final String LINES_KEY = "lines";

    /** Number of lines currently read in. */
    private long linesRead = 0;

    /** Number of lines to be returned in the filtered stream. */
    private long lines = 10;

    /** Buffer to hold in characters read ahead. */
    private char[] buffer = new char[4096];

    /** The character position that has been returned from the buffer. */
    private int returnedCharPos = -1;

    /** Whether or not read-ahead been completed. */
    private boolean completedReadAhead = false;

    /** Current index position on the buffer. */
    private int bufferPos = 0;

    /**
     * Constructor for "dummy" instances.
     * 
     * @see BaseFilterReader#BaseFilterReader()
     */
    public TailFilter() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public TailFilter(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream. If the read-ahead
     * has been completed, the next character in the buffer is returned.
     * Otherwise, the stream is read to the end and buffered (with the buffer
     * growing as necessary), then the appropriate position in the buffer is
     * set to read from.
     * 
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     * 
     * @exception IOException if the underlying stream throws an IOException
     * during reading     
     */
    public final int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        if (!completedReadAhead) {
            int ch = -1;
            while ((ch = in.read()) != -1) {
                if (buffer.length == bufferPos) {
                    if (returnedCharPos != -1) {
                        final char[] tmpBuffer = new char[buffer.length];
                        System.arraycopy(buffer, returnedCharPos + 1, tmpBuffer,
                                         0, buffer.length - (returnedCharPos + 1));
                        buffer = tmpBuffer;
                        bufferPos = bufferPos - (returnedCharPos + 1);
                        returnedCharPos = -1;
                    } else {
                        final char[] tmpBuffer = new char[buffer.length * 2];
                        System.arraycopy(buffer, 0, tmpBuffer, 0, bufferPos);
                        buffer = tmpBuffer;
                    }
                }

                if (ch == '\n' || ch == -1) {
                    ++linesRead;

                    if (linesRead == lines) {
                        int i = 0;
                        for (i = returnedCharPos + 1;
                            buffer[i] != 0 && buffer[i] != '\n'; i++) {
                        }
                        returnedCharPos = i;
                        --linesRead;
                    }
                }
                if (ch == -1) {
                    break;
                }

                buffer[bufferPos] = (char) ch;
                bufferPos++;
            }
            completedReadAhead = true;
        }

        ++returnedCharPos;
        if (returnedCharPos >= bufferPos) {
            return -1;
        } else {
            return buffer[returnedCharPos];
        }
    }

    /**
     * Sets the number of lines to be returned in the filtered stream.
     * 
     * @param lines the number of lines to be returned in the filtered stream
     */
    public final void setLines(final long lines) {
        this.lines = lines;
    }

    /**
     * Returns the number of lines to be returned in the filtered stream.
     * 
     * @return the number of lines to be returned in the filtered stream
     */
    private final long getLines() {
        return lines;
    }

    /**
     * Creates a new TailFilter using the passed in
     * Reader for instantiation.
     * 
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     * 
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public final Reader chain(final Reader rdr) {
        TailFilter newFilter = new TailFilter(rdr);
        newFilter.setLines(getLines());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Scans the parameters list for the "lines" parameter and uses
     * it to set the number of lines to be returned in the filtered stream.
     */
    private final void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (LINES_KEY.equals(params[i].getName())) {
                    setLines(new Long(params[i].getValue()).longValue());
                    break;
                }
            }
        }
    }
}
