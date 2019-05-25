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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;

/***
 * Regular expression implementation using the JDK 1.4 regular expression package
 */
public class Jdk14RegexpRegexp extends Jdk14RegexpMatcher implements Regexp {

    private static final int DECIMAL = 10;

    /**
     * Convert ant regexp substitution option to jdk1.4 options.
     *
     * @param options the ant regexp options
     * @return the jdk14 substitution options
     */
    protected int getSubsOptions(int options) {
        int subsOptions = REPLACE_FIRST;
        if (RegexpUtil.hasFlag(options, REPLACE_ALL)) {
            subsOptions = REPLACE_ALL;
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
        // translate \1 to $(1) so that the Matcher will work
        StringBuilder subst = new StringBuilder();
        for (int i = 0; i < argument.length(); i++) {
            char c = argument.charAt(i);
            if (c == '$') {
                subst.append('\\');
                subst.append('$');
            } else if (c == '\\') {
                if (++i < argument.length()) {
                    c = argument.charAt(i);
                    int value = Character.digit(c, DECIMAL);
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

        int sOptions = getSubsOptions(options);
        Pattern p = getCompiledPattern(options);
        StringBuffer sb = new StringBuffer();

        Matcher m = p.matcher(input);
        if (RegexpUtil.hasFlag(sOptions, REPLACE_ALL)) {
            sb.append(m.replaceAll(subst.toString()));
        } else if (m.find()) {
            m.appendReplacement(sb, subst.toString());
            m.appendTail(sb);
        } else {
            sb.append(input);
        }
        return sb.toString();
    }
}
