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
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import org.apache.tools.ant.types.Parameter;

/**
 * Concats a file before and/or after the file.
 *
 * <p>Example:<pre>
 * <copy todir="build">
 *     <fileset dir="src" includes="*.java"/>
 *     <filterchain>
 *         <concatfilter prepend="apache-license-java.txt"/>
 *     </filterchain>
 * </copy>
 * </pre>
 * Copies all java sources from <i>src</i> to <i>build</i> and adds the
 * content of <i>apache-license-java.txt</i> add the beginning of each
 * file.</p>
 *
 * @since 1.6
 * @version 2003-09-23
 * @author Jan Mat\u00e8rne
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
        ConcatFilter newFilter = new ConcatFilter(rdr);
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
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if ("prepend".equals(params[i].getName())) {
                    setPrepend(new File(params[i].getValue()));
                    continue;
                }
                if ("append".equals(params[i].getName())) {
                    setAppend(new File(params[i].getValue()));
                    continue;
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
