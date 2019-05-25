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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.types.Parameter;

/**
 * Concats a file before and/or after the file.
 *
 * <p>Example:</p><pre>
 * &lt;copy todir="build"&gt;
 *     &lt;fileset dir="src" includes="*.java"/&gt;
 *     &lt;filterchain&gt;
 *         &lt;concatfilter prepend="apache-license-java.txt"/&gt;
 *     &lt;/filterchain&gt;
 * &lt;/copy&gt;
 * </pre>
 *
 * <p>Copies all java sources from <i>src</i> to <i>build</i> and adds the
 * content of <i>apache-license-java.txt</i> add the beginning of each
 * file.</p>
 *
 * @since 1.6
 * @version 2003-09-23
 */
public final class ConcatFilter extends BaseParamFilterReader
    implements ChainableReader {

    /** File to add before the content. */
    private File prepend;

    /** File to add after the content. */
    private File append;

    /** Reader for prepend-file. */
    private Reader prependReader = null;

    /** Reader for append-file. */
    private Reader appendReader = null;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public ConcatFilter() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public ConcatFilter(final Reader in) {
        super(in);
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
    @Override
    public int read() throws IOException {
        // do the "singleton" initialization
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        int ch = -1;

        // The readers return -1 if they end. So simply read the "prepend"
        // after that the "content" and at the end the "append" file.
        if (prependReader != null) {
            ch = prependReader.read();
            if (ch == -1) {
                // I am the only one so I have to close the reader
                prependReader.close();
                prependReader = null;
            }
        }
        if (ch == -1) {
            ch = super.read();
        }
        if (ch == -1) {
            // don't call super.close() because that reader is used
            // on other places ...
            if (appendReader != null) {
                ch = appendReader.read();
                if (ch == -1) {
                    // I am the only one so I have to close the reader
                    appendReader.close();
                    appendReader = null;
                }
            }
        }

        return ch;
    }

    /**
     * Sets <i>prepend</i> attribute.
     * @param prepend new value
     */
    public void setPrepend(final File prepend) {
        this.prepend = prepend;
    }

    /**
     * Returns <i>prepend</i> attribute.
     * @return prepend attribute
     */
    public File getPrepend() {
        return prepend;
    }

    /**
     * Sets <i>append</i> attribute.
     * @param append new value
     */
    public void setAppend(final File append) {
        this.append = append;
    }

    /**
     * Returns <i>append</i> attribute.
     * @return append attribute
     */
    public File getAppend() {
        return append;
    }

    /**
     * Creates a new ConcatReader using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public Reader chain(final Reader rdr) {
        final ConcatFilter newFilter = new ConcatFilter(rdr);
        newFilter.setPrepend(getPrepend());
        newFilter.setAppend(getAppend());
        // Usually the initialized is set to true. But here it must not.
        // Because the prepend and append readers have to be instantiated
        // on runtime
        //newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Scans the parameters list for the "lines" parameter and uses
     * it to set the number of lines to be returned in the filtered stream.
     * also scan for skip parameter.
     */
    private void initialize() throws IOException {
        // get parameters
        final Parameter[] params = getParameters();
        if (params != null) {
            for (Parameter param : params) {
                final String paramName = param.getName();
                if ("prepend".equals(paramName)) {
                    setPrepend(new File(param.getValue()));
                } else if ("append".equals(paramName)) {
                    setAppend(new File(param.getValue()));
                }
            }
        }
        if (prepend != null) {
            if (!prepend.isAbsolute()) {
                prepend = new File(getProject().getBaseDir(), prepend.getPath());
            }
            prependReader = new BufferedReader(new FileReader(prepend));
        }
        if (append != null) {
            if (!append.isAbsolute()) {
                append = new File(getProject().getBaseDir(), append.getPath());
            }
            appendReader = new BufferedReader(new FileReader(append));
        }
   }
}
