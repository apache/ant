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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
 * Filter Reader to fetch only those lines that contain user specified
 * strings.
 *
 * Example:
 * =======
 *
 * &lt;linecontains&gt;
 *   &lt;contains value=&quot;foo&quot;&gt;
 *   &lt;contains value=&quot;bar&quot;&gt;
 * &lt;/linecontains&gt;
 *
 * Or:
 *
 * &lt;filterreader classname="org.apache.tools.ant.filters.LineContains"&gt;
 *    &lt;param type="contains" value="foo"/&gt;
 *    &lt;param type="contains" value="bar"/&gt;
 * &lt;/filterreader&gt;
 *
 * This will fetch all those lines that contain foo and bar
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class LineContains
    extends BaseParamFilterReader
    implements ChainableReader {
    /** contains key */
    private static final String CONTAINS_KEY = "contains";

    /** Vector that holds the strings that input lines must contain. */
    private Vector contains = new Vector();

    /** Currently read in line. */
    private String line = null;

    /**
     * This constructor is a dummy constructor and is
     * not meant to be used by any class other than Ant's
     * introspection mechanism. This will close the filter
     * that is created making it useless for further operations.
     */
    public LineContains() {
        super();
    }

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public LineContains(final Reader in) {
        super(in);
    }

    /**
     * Choose only those lines that contains
     * user defined values.
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
            if (line == null) {
                ch = -1;
            } else {
                int containsSize = contains.size();
                for (int i = 0; i < containsSize; i++) {
                    String containsStr = (String) contains.elementAt(i);
                    if (line.indexOf(containsStr) == -1) {
                        line = null;
                        break;
                    }
                }

                return read();
            }
        }

        return ch;
    }

    /**
     * Add a contains element.
     */
    public final void addConfiguredContains(final Contains contains) {
        this.contains.addElement(contains.getValue());
    }

    /**
     * Set contains vector.
     */
    private void setContains(final Vector contains) {
        this.contains = contains;
    }

    /**
     * Get contains vector.
     */
    private final Vector getContains() {
        return contains;
    }

    /**
     * Create a new LineContains using the passed in
     * Reader for instantiation.
     */
    public final Reader chain(final Reader rdr) {
        LineContains newFilter = new LineContains(rdr);
        newFilter.setContains(getContains());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Parse params to add user defined contains strings.
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
         * Set the contains string
         */
        public final void setValue(String contains) {
            value = contains;
        }

        /**
         * Get the contains string
         */
        public final String getValue() {
            return value;
        }
    }
}
