/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights 
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

package org.apache.tools.ant.util.regexp;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.tools.ant.BuildException;

/**
 * Implementation of RegexpMatcher for the built-in regexp matcher of
 * JDK 1.4. UNIX_LINES option is enabled as a default.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">mattinger@mindless.com</a>
 */
public class Jdk14RegexpMatcher implements RegexpMatcher {

    private String pattern;

    public Jdk14RegexpMatcher() {}

    /**
     * Set the regexp pattern from the String description.
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * Get a String representation of the regexp pattern
     */
    public String getPattern() {
        return pattern;
    }

    protected Pattern getCompiledPattern(int options)
        throws BuildException {
        int cOptions = getCompilerOptions(options);
        try {
            Pattern p = Pattern.compile(this.pattern, cOptions);
            return p;
        } catch (PatternSyntaxException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Does the given argument match the pattern?
     */
    public boolean matches(String argument) throws BuildException {
        return matches(argument, MATCH_DEFAULT);
    }

    /**
     * Does the given argument match the pattern?
     */
    public boolean matches(String input, int options)
        throws BuildException {
        try {
            Pattern p = getCompiledPattern(options);
            return p.matcher(input).find();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Returns a Vector of matched groups found in the argument.
     *
     * <p>Group 0 will be the full match, the rest are the
     * parenthesized subexpressions</p>.
     */
    public Vector getGroups(String argument) throws BuildException {
        return getGroups(argument, MATCH_DEFAULT);
    }

    /**
     * Returns a Vector of matched groups found in the argument.
     *
     * <p>Group 0 will be the full match, the rest are the
     * parenthesized subexpressions</p>.
     */
    public Vector getGroups(String input, int options)
        throws BuildException {
        Pattern p = getCompiledPattern(options);
        Matcher matcher = p.matcher(input);
        if (!matcher.find()) {
            return null;
        }
        Vector v = new Vector();
        int cnt = matcher.groupCount();
        for (int i = 0; i <= cnt; i++) {
            v.addElement(matcher.group(i));
        }
        return v;
    }

    protected int getCompilerOptions(int options) {
        // be strict about line separator
        int cOptions = Pattern.UNIX_LINES;

        if (RegexpUtil.hasFlag(options, MATCH_CASE_INSENSITIVE)) {
            cOptions |= Pattern.CASE_INSENSITIVE;
        }
        if (RegexpUtil.hasFlag(options, MATCH_MULTILINE)) {
            cOptions |= Pattern.MULTILINE;
        }
        if (RegexpUtil.hasFlag(options, MATCH_SINGLELINE)) {
            cOptions |= Pattern.DOTALL;
        }

        return cOptions;
    }

}
