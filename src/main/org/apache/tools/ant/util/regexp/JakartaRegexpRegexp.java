/*
 * Copyright  2001-2002,2004 Apache Software Foundation
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
import org.apache.regexp.RE;
import org.apache.tools.ant.BuildException;

/***
 * Regular expression implementation using the Jakarta Regexp package
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">mattinger@mindless.com</a>
 */
public class JakartaRegexpRegexp extends JakartaRegexpMatcher
    implements Regexp {

    public JakartaRegexpRegexp() {
        super();
    }

    protected int getSubsOptions(int options) {
        int subsOptions = RE.REPLACE_FIRSTONLY;
        if (RegexpUtil.hasFlag(options, REPLACE_ALL)) {
            subsOptions = RE.REPLACE_ALL;
        }
        return subsOptions;
    }

    public String substitute(String input, String argument, int options)
        throws BuildException {
        Vector v = getGroups(input, options);

        // replace \1 with the corresponding group
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < argument.length(); i++) {
            char c = argument.charAt(i);
            if (c == '\\') {
                if (++i < argument.length()) {
                    c = argument.charAt(i);
                    int value = Character.digit(c, 10);
                    if (value > -1) {
                        result.append((String) v.elementAt(value));
                    } else {
                        result.append(c);
                    }
                } else {
                    // XXX - should throw an exception instead?
                    result.append('\\');
                }
            } else {
                result.append(c);
            }
        }
        argument = result.toString();

        RE reg = getCompiledPattern(options);
        int sOptions = getSubsOptions(options);
        return reg.subst(input, argument, sOptions);
    }
}
