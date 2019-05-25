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
import java.util.Vector;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.util.regexp.Regexp;
import org.apache.tools.ant.util.regexp.RegexpUtil;

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
 */
public final class LineContainsRegExp
    extends BaseParamFilterReader
    implements ChainableReader {
    /** Parameter name for the regular expression to filter on. */
    private static final String REGEXP_KEY = "regexp";

    /** Parameter name for the negate attribute. */
    private static final String NEGATE_KEY = "negate";

    /** Parameter name for the casesensitive attribute. */
    private static final String CS_KEY = "casesensitive";

    /** Vector that holds the expressions that input lines must contain. */
    private Vector<RegularExpression> regexps = new Vector<>();

    /**
     * Remaining line to be read from this filter, or <code>null</code> if
     * the next call to <code>read()</code> should read the original stream
     * to find the next matching line.
     */
    private String line = null;

    private boolean negate = false;
    private int regexpOptions = Regexp.MATCH_DEFAULT;

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
    public int read() throws IOException {
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
            for (line = readLine(); line != null; line = readLine()) {
                boolean matches = true;
                for (RegularExpression regexp : regexps) {
                    if (!regexp.getRegexp(getProject()).matches(line, regexpOptions)) {
                        matches = false;
                        break;
                    }
                }
                if (matches ^ isNegated()) {
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
    public void addConfiguredRegexp(final RegularExpression regExp) {
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
    private void setRegexps(final Vector<RegularExpression> regexps) {
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
    private Vector<RegularExpression> getRegexps() {
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
    public Reader chain(final Reader rdr) {
        LineContainsRegExp newFilter = new LineContainsRegExp(rdr);
        newFilter.setRegexps(getRegexps());
        newFilter.setNegate(isNegated());
        newFilter.setCaseSensitive(!RegexpUtil.hasFlag(regexpOptions,
                Regexp.MATCH_CASE_INSENSITIVE));
        return newFilter;
    }

    /**
     * Set the negation mode.  Default false (no negation).
     * @param b the boolean negation mode to set.
     */
    public void setNegate(boolean b) {
        negate = b;
    }

    /**
     * Whether to match casesensitively.
     * @param b boolean
     * @since Ant 1.8.2
     */
    public void setCaseSensitive(boolean b) {
        regexpOptions = RegexpUtil.asOptions(b);
    }

    /**
     * Find out whether we have been negated.
     * @return boolean negation flag.
     */
    public boolean isNegated() {
        return negate;
    }

    /**
     * Set the regular expression as an attribute.
     * @param pattern String
     * @since Ant 1.10.2
     */
    public void setRegexp(String pattern) {
        RegularExpression regexp = new RegularExpression();
        regexp.setPattern(pattern);
        regexps.addElement(regexp);
    }

    /**
     * Parses parameters to add user defined regular expressions.
     */
    private void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (Parameter param : params) {
                if (REGEXP_KEY.equals(param.getType())) {
                    setRegexp(param.getValue());
                } else if (NEGATE_KEY.equals(param.getType())) {
                    setNegate(Project.toBoolean(param.getValue()));
                } else if (CS_KEY.equals(param.getType())) {
                    setCaseSensitive(Project.toBoolean(param.getValue()));
                }
            }
        }
    }
}
