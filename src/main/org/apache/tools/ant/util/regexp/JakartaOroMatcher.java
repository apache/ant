/*
 * Copyright  2000-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.apache.tools.ant.util.regexp;

import java.util.Vector;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.tools.ant.BuildException;

/**
 * Implementation of RegexpMatcher for Jakarta-ORO.
 *
 * @author Stefan Bodewig
 * @author <a href="mailto:mattinger@mindless.com">Matthew Inger</a>
 */
public class JakartaOroMatcher implements RegexpMatcher {

    private String pattern;
    protected final Perl5Compiler compiler = new Perl5Compiler();
    protected final Perl5Matcher matcher = new Perl5Matcher();

    public JakartaOroMatcher() {
    }

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
        return this.pattern;
    }

    /**
     * Get a compiled representation of the regexp pattern
     */
    protected Pattern getCompiledPattern(int options)
        throws BuildException {
        try {
            // compute the compiler options based on the input options first
            Pattern p = compiler.compile(pattern, getCompilerOptions(options));
            return p;
        } catch (Exception e) {
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
        Pattern p = getCompiledPattern(options);
        return matcher.contains(input, p);
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
        if (!matches(input, options)) {
            return null;
        }
        Vector v = new Vector();
        MatchResult mr = matcher.getMatch();
        int cnt = mr.groups();
        for (int i = 0; i < cnt; i++) {
            String match = mr.group(i);
            // treat non-matching groups as empty matches
            if (match == null) {
                match = "";
            }
            v.addElement(match);
        }
        return v;
    }

    protected int getCompilerOptions(int options) {
        int cOptions = Perl5Compiler.DEFAULT_MASK;

        if (RegexpUtil.hasFlag(options, MATCH_CASE_INSENSITIVE)) {
            cOptions |= Perl5Compiler.CASE_INSENSITIVE_MASK;
        }
        if (RegexpUtil.hasFlag(options, MATCH_MULTILINE)) {
            cOptions |= Perl5Compiler.MULTILINE_MASK;
        }
        if (RegexpUtil.hasFlag(options, MATCH_SINGLELINE)) {
            cOptions |= Perl5Compiler.SINGLELINE_MASK;
        }

        return cOptions;
    }

}
