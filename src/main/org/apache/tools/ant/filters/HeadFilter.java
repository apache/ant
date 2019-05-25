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
import org.apache.tools.ant.util.LineTokenizer;

/**
 * Reads the first <code>n</code> lines of a stream.
 * (Default is first 10 lines.)
 * <p>
 * Example:
 * <pre>&lt;headfilter lines=&quot;3&quot;/&gt;</pre>
 * Or:
 * <pre>&lt;filterreader classname=&quot;org.apache.tools.ant.filters.HeadFilter&quot;&gt;
 *    &lt;param name=&quot;lines&quot; value=&quot;3&quot;/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 */
public final class HeadFilter extends BaseParamFilterReader
    implements ChainableReader {
    /** Parameter name for the number of lines to be returned. */
    private static final String LINES_KEY = "lines";

    /** Parameter name for the number of lines to be skipped. */
    private static final String SKIP_KEY = "skip";

    /** Number of lines currently read in. */
    private long linesRead = 0;

    /** Default number of lines to show */
    private static final int DEFAULT_NUM_LINES = 10;

    /** Number of lines to be returned in the filtered stream. */
    private long lines = DEFAULT_NUM_LINES;

    /** Number of lines to be skipped. */
    private long skip = 0;

    /** A line tokenizer */
    private LineTokenizer lineTokenizer = null;

    /** the current line from the input stream */
    private String    line      = null;
    /** the position in the current line */
    private int       linePos   = 0;

    /** Whether this filter is finished */
    private boolean eof;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public HeadFilter() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public HeadFilter(final Reader in) {
        super(in);
        lineTokenizer = new LineTokenizer();
        lineTokenizer.setIncludeDelims(true);
    }

    /**
     * Returns the next character in the filtered stream. If the desired
     * number of lines have already been read, the resulting stream is
     * effectively at an end. Otherwise, the next character from the
     * underlying stream is read and returned.
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

        while (line == null || line.isEmpty()) {
            line = lineTokenizer.getToken(in);
            if (line == null) {
                return -1;
            }
            line = headFilter(line);
            if (eof) {
                return -1;
            }
            linePos = 0;
        }

        int ch = line.charAt(linePos);
        linePos++;
        if (linePos == line.length()) {
            line = null;
        }
        return ch;
    }

    /**
     * Sets the number of lines to be returned in the filtered stream.
     *
     * @param lines the number of lines to be returned in the filtered stream
     */
    public void setLines(final long lines) {
        this.lines = lines;
    }

    /**
     * Returns the number of lines to be returned in the filtered stream.
     *
     * @return the number of lines to be returned in the filtered stream
     */
    private long getLines() {
        return lines;
    }

    /**
     * Sets the number of lines to be skipped in the filtered stream.
     *
     * @param skip the number of lines to be skipped in the filtered stream
     */
    public void setSkip(final long skip) {
        this.skip = skip;
    }

    /**
     * Returns the number of lines to be skipped in the filtered stream.
     *
     * @return the number of lines to be skipped in the filtered stream
     */
    private long getSkip() {
        return skip;
    }

    /**
     * Creates a new HeadFilter using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public Reader chain(final Reader rdr) {
        HeadFilter newFilter = new HeadFilter(rdr);
        newFilter.setLines(getLines());
        newFilter.setSkip(getSkip());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Scans the parameters list for the "lines" parameter and uses
     * it to set the number of lines to be returned in the filtered stream.
     * also scan for skip parameter.
     */
    private void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (Parameter param : params) {
                final String paramName = param.getName();
                if (LINES_KEY.equals(paramName)) {
                    lines = Long.parseLong(param.getValue());
                } else if (SKIP_KEY.equals(paramName)) {
                    skip = Long.parseLong(param.getValue());
                }
            }
        }
    }

    /**
     * implements a head filter on the input stream
     */
    private String headFilter(String line) {
        linesRead++;
        if (skip > 0) {
            if ((linesRead - 1) < skip) {
                return null;
            }
        }

        if (lines > 0) {
            if (linesRead > (lines + skip)) {
                eof = true;
                return null;
            }
        }
        return line;
    }
}
