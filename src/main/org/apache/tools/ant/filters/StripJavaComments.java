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

/**
 * This is a Java comment and string stripper reader that filters
 * those lexical tokens out for purposes of simple Java parsing.
 * (if you have more complex Java parsing needs, use a real lexer).
 * Since this class heavily relies on the single char read function,
 * you are reccomended to make it work on top of a buffered reader.
 */
public final class StripJavaComments
    extends BaseFilterReader
    implements ChainableReader {
        
    /** 
     * The read-ahead character, used for effectively pushing a single
     * character back. -1 indicates that no character is in the buffer.
     */
    private int readAheadCh = -1;

    /** 
     * Whether or not the parser is currently in the middle of a string
     * literal.
     */
    private boolean inString = false;

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
    public final int read() throws IOException {
        int ch = -1;
        if (readAheadCh != -1) {
            ch = readAheadCh;
            readAheadCh = -1;
        } else {
            ch = in.read();
            if (ch == '"') {
                inString = !inString;
            } else {
                if (!inString) {
                    if (ch == '/') {
                        ch = in.read();
                        if (ch == '/') {
                            while (ch != '\n' && ch != -1) {
                                ch = in.read();
                            }
                        } else if (ch == '*') {
                            while (ch != -1) {
                                ch = in.read();
                                if (ch == '*') {
                                    ch = in.read();
                                    while (ch == '*' && ch != -1) {
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

    public final Reader chain(final Reader rdr) {
        StripJavaComments newFilter = new StripJavaComments(rdr);
        return newFilter;
    }
}
