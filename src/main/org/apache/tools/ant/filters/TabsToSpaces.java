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
 * Converts tabs to spaces.
 *
 * Example:
 *
 * <pre>&lt;tabtospaces tablength=&quot;8&quot;/&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader classname=&quot;org.apache.tools.ant.filters.TabsToSpaces&quot;&gt;
 *   &lt;param name=&quot;tablength&quot; value=&quot;8&quot;/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 * @author Magesh Umasankar
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
    public final int read() throws IOException {
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
    public final void setTablength(final int tabLength) {
        this.tabLength = tabLength;
    }

    /**
     * Returns the tab length.
     * 
     * @return the number of spaces used when converting a tab
     */
    private final int getTablength() {
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
    public final Reader chain(final Reader rdr) {
        TabsToSpaces newFilter = new TabsToSpaces(rdr);
        newFilter.setTablength(getTablength());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Parses the parameters to set the tab length.
     */
    private final void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    if (TAB_LENGTH_KEY.equals(params[i].getName())) {
                        tabLength =
                            new Integer(params[i].getValue()).intValue();
                        break;
                    }
                }
            }
        }
    }
}
