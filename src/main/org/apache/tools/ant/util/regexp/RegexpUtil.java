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

// CheckStyle:HideUtilityClassConstructorCheck OFF - bc

/***
 * Regular expression utilities class which handles flag operations.
 *
 */
public class RegexpUtil {

    /**
     * Check the options has a particular flag set.
     *
     * @param options an <code>int</code> value
     * @param flag an <code>int</code> value
     * @return true if the flag is set
     */
    public static boolean hasFlag(int options, int flag) {
        return (options & flag) > 0;
    }

    /**
     * Remove a particular flag from an int value contains the option flags.
     *
     * @param options an <code>int</code> value
     * @param flag an <code>int</code> value
     * @return the options with the flag unset
     */
    public static int removeFlag(int options, int flag) {
        return options & (0xFFFFFFFF - flag);
    }

    /**
     * convert regex option flag characters to regex options
     * <dl>
     *   <dt>g</dt><dd>Regexp.REPLACE_ALL</dd>
     *   <dt>i</dt><dd>RegexpMatcher.MATCH_CASE_INSENSITIVE</dd>
     *   <dt>m</dt><dd>RegexpMatcher.MATCH_MULTILINE</dd>
     *   <dt>s</dt><dd>RegexpMatcher.MATCH_SINGLELINE</dd>
     * </dl>
     * @param flags the string containing the flags
     * @return the Regexp option bits
     * @since Ant 1.8.2
     */
    public static int asOptions(String flags) {
        int options = RegexpMatcher.MATCH_DEFAULT;
        if (flags != null) {
            options = asOptions(!flags.contains("i"),
                    flags.contains("m"),
                    flags.contains("s"));
            if (flags.contains("g")) {
                options |= Regexp.REPLACE_ALL;
            }
        }
        return options;
    }

    /**
     * Convert flag to regex options.
     *
     * @param caseSensitive opposite of RegexpMatcher.MATCH_CASE_INSENSITIVE
     * @return the Regexp option bits
     * @since Ant 1.8.2
     */
    public static int asOptions(boolean caseSensitive) {
        return asOptions(caseSensitive, false, false);
    }

    /**
     * Convert flags to regex options.
     *
     * @param caseSensitive opposite of RegexpMatcher.MATCH_CASE_INSENSITIVE
     * @param multiLine RegexpMatcher.MATCH_MULTILINE
     * @param singleLine RegexpMatcher.MATCH_SINGLELINE
     * @return the Regexp option bits
     * @since Ant 1.8.2
     */
    public static int asOptions(boolean caseSensitive, boolean multiLine,
                                boolean singleLine) {
        int options = RegexpMatcher.MATCH_DEFAULT;
        if (!caseSensitive) {
            options |= RegexpMatcher.MATCH_CASE_INSENSITIVE;
        }
        if (multiLine) {
            options |= RegexpMatcher.MATCH_MULTILINE;
        }
        if (singleLine) {
            options |= RegexpMatcher.MATCH_SINGLELINE;
        }
        return options;
    }
}
