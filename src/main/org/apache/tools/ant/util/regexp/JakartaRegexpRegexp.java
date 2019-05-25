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

import java.util.Vector;
import org.apache.regexp.RE;
import org.apache.tools.ant.BuildException;

/***
 * Regular expression implementation using the Jakarta Regexp package
 */
public class JakartaRegexpRegexp extends JakartaRegexpMatcher
    implements Regexp {

    private static final int DECIMAL = 10;

    /**
     * Convert ant regexp substitution option to apache regex options.
     *
     * @param options the ant regexp options
     * @return the apache regex substitution options
     */
    protected int getSubsOptions(int options) {
        int subsOptions = RE.REPLACE_FIRSTONLY;
        if (RegexpUtil.hasFlag(options, REPLACE_ALL)) {
            subsOptions = RE.REPLACE_ALL;
        }
        return subsOptions;
    }

    /**
     * Perform a substitution on the regular expression.
     * @param input The string to substitute on
     * @param argument The string which defines the substitution
     * @param options The list of options for the match and replace.
     * @return the result of the operation
     * @throws BuildException on error
     */
    @Override
    public String substitute(String input, String argument, int options)
        throws BuildException {
        Vector<String> v = getGroups(input, options);

        // replace \1 with the corresponding group
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < argument.length(); i++) {
            char c = argument.charAt(i);
            if (c == '\\') {
                if (++i < argument.length()) {
                    c = argument.charAt(i);
                    int value = Character.digit(c, DECIMAL);
                    if (value > -1) {
                        result.append(v.elementAt(value));
                    } else {
                        result.append(c);
                    }
                } else {
                    // TODO - should throw an exception instead?
                    result.append('\\');
                }
            } else {
                result.append(c);
            }
        }
        RE reg = getCompiledPattern(options);
        int sOptions = getSubsOptions(options);
        return reg.subst(input, result.toString(), sOptions);
    }
}
