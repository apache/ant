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
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.util.regexp.Regexp;

/**
 * Filter which includes only those lines that contain the user-specified
 * regular expression matching strings.
 *
 * Example:
 * <pre>&lt;linecontainsregexp&gt;
 *   &lt;regexp pattern=&quot;foo*&quot;&gt;
 * &lt;/linecontainsregexp&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader classname=&quot;org.apache.tools.ant.filters.LineContainsRegExp&quot;&gt;
 *    &lt;param type=&quot;regexp&quot; value=&quot;foo*&quot;/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 * This will fetch all those lines that contain the pattern <code>foo</code>
 *
 * @author Magesh Umasankar
 */
public final class LineContainsRegExp
    extends BaseParamFilterReader
    implements ChainableReader {
    /** Parameter name for the regular expression to filter on. */
    private static final String REGEXP_KEY = "regexp";

    /** Vector that holds the expressions that input lines must contain. */
    private Vector regexps = new Vector();

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
    public LineContainsRegExp() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public LineContainsRegExp(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream, only including
     * lines from the original stream which match all of the specified
     * regular expressions.
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
            final int regexpsSize = regexps.size();

            while (line != null) {
                for (int i = 0; i < regexpsSize; i++) {
                    RegularExpression regexp = (RegularExpression)
                                                        regexps.elementAt(i);
                    Regexp re = regexp.getRegexp(getProject());
                    boolean matches = re.matches(line);
                    if (!matches) {
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
     * Adds a <code>regexp</code> element.
     * 
     * @param regExp The <code>regexp</code> element to add. 
     *               Must not be <code>null</code>.
     */
    public final void addConfiguredRegexp(final RegularExpression regExp) {
        this.regexps.addElement(regExp);
    }

    /**
     * Sets the vector of regular expressions which must be contained within 
     * a line read from the original stream in order for it to match this 
     * filter.
     * 
     * @param regexps A vector of regular expressions which must be contained 
     * within a line in order for it to match in this filter. Must not be 
     * <code>null</code>.
     */
    private void setRegexps(final Vector regexps) {
        this.regexps = regexps;
    }

    /**
     * Returns the vector of regular expressions which must be contained within 
     * a line read from the original stream in order for it to match this 
     * filter.
     * 
     * @return the vector of regular expressions which must be contained within 
     * a line read from the original stream in order for it to match this 
     * filter. The returned object is "live" - in other words, changes made to 
     * the returned object are mirrored in the filter.
     */
    private final Vector getRegexps() {
        return regexps;
    }

    /**
     * Creates a new LineContainsRegExp using the passed in
     * Reader for instantiation.
     * 
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     * 
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public final Reader chain(final Reader rdr) {
        LineContainsRegExp newFilter = new LineContainsRegExp(rdr);
        newFilter.setRegexps(getRegexps());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Parses parameters to add user defined regular expressions.
     */
    private final void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (REGEXP_KEY.equals(params[i].getType())) {
                    String pattern = params[i].getValue();
                    RegularExpression regexp = new RegularExpression();
                    regexp.setPattern(pattern);
                    regexps.addElement(regexp);
                }
            }
        }
    }
}
