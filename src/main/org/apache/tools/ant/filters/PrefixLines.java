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
 * Attaches a prefix to every line.
 *
 * Example:
 * <pre>&lt;prefixlines prefix=&quot;Foo&quot;/&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader classname=&quot;org.apache.tools.ant.filters.PrefixLines&quot;&gt;
 *  &lt;param name=&quot;prefix&quot; value=&quot;Foo&quot;/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 * @author Magesh Umasankar
 */
public final class PrefixLines
    extends BaseParamFilterReader
    implements ChainableReader {
    /** Parameter name for the prefix. */
    private static final String PREFIX_KEY = "prefix";

    /** The prefix to be used. */
    private String prefix = null;

    /** Data that must be read from, if not null. */
    private String queuedData = null;

    /**
     * Constructor for "dummy" instances.
     * 
     * @see BaseFilterReader#BaseFilterReader()
     */
    public PrefixLines() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public PrefixLines(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream. One line is read
     * from the original input, and the prefix added. The resulting
     * line is then used until it ends, at which point the next original line
     * is read, etc.
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
                if (prefix != null) {
                    queuedData = prefix + queuedData;
                }
                return read();
            }
        }
        return ch;
    }

    /**
     * Sets the prefix to add at the start of each input line.
     * 
     * @param prefix The prefix to add at the start of each input line.
     *               May be <code>null</code>, in which case no prefix
     *               is added.
     */
    public final void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the prefix which will be added at the start of each input line.
     * 
     * @return the prefix which will be added at the start of each input line
     */
    private final String getPrefix() {
        return prefix;
    }

    /**
     * Creates a new PrefixLines filter using the passed in
     * Reader for instantiation.
     * 
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     * 
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public final Reader chain(final Reader rdr) {
        PrefixLines newFilter = new PrefixLines(rdr);
        newFilter.setPrefix(getPrefix());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Initializes the prefix if it is available from the parameters.
     */
    private final void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (PREFIX_KEY.equals(params[i].getName())) {
                    prefix = params[i].getValue();
                    break;
                }
            }
        }
    }
}
