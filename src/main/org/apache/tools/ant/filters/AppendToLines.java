/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
import java.io.Reader;
import org.apache.tools.ant.types.Parameter;

/**
 * Attaches a suffix to every line.
 *
 * Example:
 * <pre>&lt;appendtolines append=&quot;Foo&quot;/&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader classname=&quot;org.apache.tools.ant.filters.AppendToLines&quot;&gt;
 *  &lt;param name=&quot;append&quot; value=&quot;Foo&quot;/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 * @since Ant 1.8.0
 */
public final class AppendToLines
    extends BaseParamFilterReader
    implements ChainableReader {
    /** Parameter name for the prefix. */
    private static final String APPEND_KEY = "append";

    /** The appendix to be used. */
    private String append = null;

    /** Data that must be read from, if not null. */
    private String queuedData = null;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public AppendToLines() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public AppendToLines(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream. One line is read
     * from the original input, and the appendix added. The resulting
     * line is then used until it ends, at which point the next original line
     * is read, etc.
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

        if (queuedData != null && queuedData.length() == 0) {
            queuedData = null;
        }

        if (queuedData != null) {
            ch = queuedData.charAt(0);
            queuedData = queuedData.substring(1);
            if (queuedData.length() == 0) {
                queuedData = null;
            }
        } else {
            queuedData = readLine();
            if (queuedData == null) {
                ch = -1;
            } else {
                if (append != null) {
                    String lf = "";
                    if (queuedData.endsWith("\r\n")) {
                        lf = "\r\n";
                    } else if (queuedData.endsWith("\n")) {
                        lf = "\n";
                    }
                    queuedData =
                        queuedData.substring(0,
                                             queuedData.length() - lf.length())
                        + append + lf;
                }
                return read();
            }
        }
        return ch;
    }

    /**
     * Sets the appendix to add at the end of each input line.
     *
     * @param append The appendix to add at the end of each input line.
     *               May be <code>null</code>, in which case no appendix
     *               is added.
     */
    public void setAppend(final String append) {
        this.append = append;
    }

    /**
     * Returns the appendix which will be added at the end of each input line.
     *
     * @return the appendix which will be added at the end of each input line
     */
    private String getAppend() {
        return append;
    }

    /**
     * Creates a new AppendToLines filter using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public Reader chain(final Reader rdr) {
        AppendToLines newFilter = new AppendToLines(rdr);
        newFilter.setAppend(getAppend());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Initializes the appendix if it is available from the parameters.
     */
    private void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (APPEND_KEY.equals(params[i].getName())) {
                    append = params[i].getValue();
                    break;
                }
            }
        }
    }
}
