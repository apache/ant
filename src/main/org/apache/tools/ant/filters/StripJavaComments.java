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
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;

/**
 * This is a Java comment and string stripper reader that filters
 * those lexical tokens out for purposes of simple Java parsing.
 * (if you have more complex Java parsing needs, use a real lexer).
 * Since this class heavily relies on the single char read function,
 * you are recommended to make it work on top of a buffered reader.
 *
 */
public final class StripJavaComments
    extends BaseFilterReader
    implements ChainableReader {

    /**
     * The read-ahead character, used for effectively pushing a single
     * character back. A value of -1 indicates that no character is in the
     * buffer.
     */
    private int readAheadCh = -1;

    /**
     * Whether or not the parser is currently in the middle of a string
     * literal.
     */
    private boolean inString = false;

    /**
     * Whether or not the last char has been a backslash.
     */
    private boolean quoted = false;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public StripJavaComments() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public StripJavaComments(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream, not including
     * Java comments.
     *
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws an IOException
     * during reading
     */
    public int read() throws IOException {
        int ch = -1;
        if (readAheadCh != -1) {
            ch = readAheadCh;
            readAheadCh = -1;
        } else {
            ch = in.read();
            if (ch == '"' && !quoted) {
                inString = !inString;
                quoted = false;
            } else if (ch == '\\') {
                quoted = !quoted;
            } else {
                quoted = false;
                if (!inString) {
                    if (ch == '/') {
                        ch = in.read();
                        if (ch == '/') {
                            while (ch != '\n' && ch != -1 && ch != '\r') {
                                ch = in.read();
                            }
                        } else if (ch == '*') {
                            while (ch != -1) {
                                ch = in.read();
                                if (ch == '*') {
                                    ch = in.read();
                                    while (ch == '*') {
                                        ch = in.read();
                                    }

                                    if (ch == '/') {
                                        ch = read();
                                        break;
                                    }
                                }
                            }
                        } else {
                            readAheadCh = ch;
                            ch = '/';
                        }
                    }
                }
            }
        }

        return ch;
    }

    /**
     * Creates a new StripJavaComments using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */

    public Reader chain(final Reader rdr) {
        return new StripJavaComments(rdr);
    }
}
