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

import org.apache.tools.ant.util.UnicodeUtil;

/**
 * This method converts non-latin characters to unicode escapes.
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
        if (unicodeBuf.length() > 0) {
            ch = (int) unicodeBuf.charAt(0);
            unicodeBuf.deleteCharAt(0);
        } else {
            ch = in.read();
            if (ch != -1) {
                char achar = (char) ch;
                if (achar >= '\u0080') {
                    unicodeBuf = UnicodeUtil.EscapeUnicode(achar);
                    ch = '\\';
                }
            }
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
    private void initialize() {
    }
}

