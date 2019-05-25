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
package org.apache.tools.ant.util.regexp;

import org.apache.oro.text.regex.Perl5Substitution;
import org.apache.oro.text.regex.Substitution;
import org.apache.oro.text.regex.Util;
import org.apache.tools.ant.BuildException;

/***
 * Regular expression implementation using the Jakarta Oro package
 */
public class JakartaOroRegexp extends JakartaOroMatcher implements Regexp {

    private static final int DECIMAL = 10;

    /**
     * Perform a substitution on the regular expression.
     * @param input The string to substitute on
     * @param argument The string which defines the substitution
     * @param options The list of options for the match and replace.
     * @return the result of the operation
     * @throws BuildException on error
     */
    public String substitute(final String input, final String argument, final int options)
        throws BuildException {
        // translate \1 to $1 so that the Perl5Substitution will work
        final StringBuilder subst = new StringBuilder();
        for (int i = 0; i < argument.length(); i++) {
            char c = argument.charAt(i);
            if (c == '$') {
                subst.append('\\');
                subst.append('$');
            } else if (c == '\\') {
                if (++i < argument.length()) {
                    c = argument.charAt(i);
                    final int value = Character.digit(c, DECIMAL);
                    if (value > -1) {
                        subst.append('$').append(value);
                    } else {
                        subst.append(c);
                    }
                } else {
                    // TODO - should throw an exception instead?
                    subst.append('\\');
                }
            } else {
                subst.append(c);
            }
        }

        // Do the substitution
        final Substitution s =
            new Perl5Substitution(subst.toString(),
                                  Perl5Substitution.INTERPOLATE_ALL);
        return Util.substitute(matcher,
                               getCompiledPattern(options),
                               s,
                               input,
                               getSubsOptions(options));
    }

    /**
     * Convert ant regexp substitution option to oro options.
     *
     * @param options the ant regexp options
     * @return the oro substitution options
     */
    protected int getSubsOptions(final int options) {
        return RegexpUtil.hasFlag(options, REPLACE_ALL) ? Util.SUBSTITUTE_ALL
            : 1;
    }

}
