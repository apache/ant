/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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
import java.util.Vector;

import org.apache.tools.ant.types.Parameter;

/**
 * Filter which includes only those lines that contain all the user-specified
 * strings.
 *
 * Example:
 *
 * <pre>&lt;linecontains&gt;
 *   &lt;contains value=&quot;foo&quot;&gt;
 *   &lt;contains value=&quot;bar&quot;&gt;
 * &lt;/linecontains&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader classname=&quot;org.apache.tools.ant.filters.LineContains&quot;&gt;
 *    &lt;param type=&quot;contains&quot; value=&quot;foo&quot;/&gt;
 *    &lt;param type=&quot;contains&quot; value=&quot;bar&quot;/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 * This will include only those lines that contain <code>foo</code> and
 * <code>bar</code>.
 *
 * @author Magesh Umasankar
 */
public final class LineContains
    extends BaseParamFilterReader
    implements ChainableReader {
    /** Parameter name for the words to filter on. */
    private static final String CONTAINS_KEY = "contains";

    /** Vector that holds the strings that input lines must contain. */
    private Vector contains = new Vector();

    /**
     * Remaining line to be read from this filter, or <code>null</code> if
     * the next call to <code>read()</code> should read the original stream
     * to find the next matching line.
     */
    private String line = null;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public LineContains() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public LineContains(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream, only including
     * lines from the original stream which contain all of the specified words.
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

        if (line != null) {
            ch = line.charAt(0);
            if (line.length() == 1) {
                line = null;
            } else {
                line = line.substring(1);
            }
        } else {
            line = readLine();
            final int containsSize = contains.size();

            while (line != null) {
                for (int i = 0; i < containsSize; i++) {
                    String containsStr = (String) contains.elementAt(i);
                    if (line.indexOf(containsStr) == -1) {
                        line = null;
                        break;
                    }
                }

                if (line == null) {
                    // line didn't match
                    line = readLine();
                } else {
                    break;
                }
            }

            if (line != null) {
                return read();
            }
        }

        return ch;
    }

    /**
     * Adds a <code>contains</code> element.
     *
     * @param contains The <code>contains</code> element to add.
     *                 Must not be <code>null</code>.
     */
    public final void addConfiguredContains(final Contains contains) {
        this.contains.addElement(contains.getValue());
    }

    /**
     * Sets the vector of words which must be contained within a line read
     * from the original stream in order for it to match this filter.
     *
     * @param contains A vector of words which must be contained within a line
     * in order for it to match in this filter. Must not be <code>null</code>.
     */
    private void setContains(final Vector contains) {
        this.contains = contains;
    }

    /**
     * Returns the vector of words which must be contained within a line read
     * from the original stream in order for it to match this filter.
     *
     * @return the vector of words which must be contained within a line read
     * from the original stream in order for it to match this filter. The
     * returned object is "live" - in other words, changes made to the
     * returned object are mirrored in the filter.
     */
    private final Vector getContains() {
        return contains;
    }

    /**
     * Creates a new LineContains using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public final Reader chain(final Reader rdr) {
        LineContains newFilter = new LineContains(rdr);
        newFilter.setContains(getContains());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Parses the parameters to add user-defined contains strings.
     */
    private final void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (CONTAINS_KEY.equals(params[i].getType())) {
                    contains.addElement(params[i].getValue());
                }
            }
        }
    }

    /**
     * Holds a contains element
     */
    public static class Contains {

        /** User defined contains string */
        private String value;

        /**
         * Sets the contains string
         *
         * @param contains The contains string to set.
         *                 Must not be <code>null</code>.
         */
        public final void setValue(String contains) {
            value = contains;
        }

        /**
         * Returns the contains string.
         *
         * @return the contains string for this element
         */
        public final String getValue() {
            return value;
        }
    }
}
