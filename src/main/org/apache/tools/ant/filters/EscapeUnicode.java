/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
 * Converts non latin characters to unicode escapes
 * Useful to load properties containing non latin
 * Example:
 *
 * <pre>&lt;escapeunicode&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader
        classname=&quot;org.apache.tools.ant.filters.EscapeUnicode&quot;/&gt;
 *  </pre>
 *
 * @author <a href="mailto:levylambert@tiscali-dsl.de">Antoine Levy-Lambert</a>
 * @since Ant 1.6
 */
public class EscapeUnicode
    extends BaseParamFilterReader
    implements ChainableReader {
    //this field will hold unnnn right after reading a non latin character
    //afterwards it will be truncated of one char every call to read
    private StringBuffer unicodeBuf;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public EscapeUnicode() {
        super();
        unicodeBuf = new StringBuffer();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public EscapeUnicode(final Reader in) {
        super(in);
        unicodeBuf = new StringBuffer();
    }

    /**
     * Returns the next character in the filtered stream, converting non latin
     * characters to unicode escapes.
     *
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws
     * an IOException during reading
     */
    public final int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        int ch = -1;
        if (unicodeBuf.length() == 0) {
            ch = in.read();
            if (ch != -1) {
                char achar = (char) ch;
                if (achar >= '\u0080') {
                    unicodeBuf = new StringBuffer("u0000");
                    String s = Integer.toHexString(ch);
                    //replace the last 0s by the chars contained in s
                    for (int i = 0; i < s.length(); i++) {
                        unicodeBuf.setCharAt(unicodeBuf.length()
                                             - s.length() + i,
                                             s.charAt(i));
                    }
                    ch = '\\';
                }
            }
        } else {
            ch = (int) unicodeBuf.charAt(0);
            unicodeBuf.deleteCharAt(0);
        }
        return ch;
    }

    /**
     * Creates a new EscapeUnicode using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public final Reader chain(final Reader rdr) {
        EscapeUnicode newFilter = new EscapeUnicode(rdr);
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Parses the parameters (currently unused)
     */
    private final void initialize() {
    }
}

