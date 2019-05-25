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

import org.apache.tools.ant.types.Parameter;

/**
 * Converts tabs to spaces.
 *
 * Example:
 *
 * <pre>&lt;tabstospaces tablength=&quot;8&quot;/&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader classname=&quot;org.apache.tools.ant.filters.TabsToSpaces&quot;&gt;
 *   &lt;param name=&quot;tablength&quot; value=&quot;8&quot;/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 */
public final class TabsToSpaces
    extends BaseParamFilterReader
    implements ChainableReader {
    /** The default tab length. */
    private static final int DEFAULT_TAB_LENGTH = 8;

    /** Parameter name for the length of a tab. */
    private static final String TAB_LENGTH_KEY = "tablength";

    /** Tab length in this filter. */
    private int tabLength = DEFAULT_TAB_LENGTH;

    /** The number of spaces still to be read to represent the last-read tab. */
    private int spacesRemaining = 0;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public TabsToSpaces() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public TabsToSpaces(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream, converting tabs
     * to the specified number of spaces.
     *
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws an IOException
     * during reading
     */
    public int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        int ch = -1;

        if (spacesRemaining > 0) {
            spacesRemaining--;
            ch = ' ';
        } else {
            ch = in.read();
            if (ch == '\t') {
                spacesRemaining = tabLength - 1;
                ch = ' ';
            }
        }
        return ch;
    }

    /**
     * Sets the tab length.
     *
     * @param tabLength the number of spaces to be used when converting a tab.
     */
    public void setTablength(final int tabLength) {
        this.tabLength = tabLength;
    }

    /**
     * Returns the tab length.
     *
     * @return the number of spaces used when converting a tab
     */
    private int getTablength() {
        return tabLength;
    }

    /**
     * Creates a new TabsToSpaces using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public Reader chain(final Reader rdr) {
        TabsToSpaces newFilter = new TabsToSpaces(rdr);
        newFilter.setTablength(getTablength());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Parses the parameters to set the tab length.
     */
    private void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (Parameter param : params) {
                if (param != null) {
                    if (TAB_LENGTH_KEY.equals(param.getName())) {
                        tabLength = Integer.parseInt(param.getValue());
                        break;
                    }
                }
            }
        }
    }
}
