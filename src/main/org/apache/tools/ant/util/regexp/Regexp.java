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

import org.apache.tools.ant.BuildException;

/***
 * Interface which represents a regular expression, and the operations
 * that can be performed on it.
 *
 */
public interface Regexp extends RegexpMatcher {

    /**
     * Replace only the first occurrence of the regular expression
     */
    int REPLACE_FIRST          = 0x00000001;

    /**
     * Replace all occurrences of the regular expression
     */
    int REPLACE_ALL            = 0x00000010;

    /**
     * Perform a substitution on the regular expression.
     * @param input The string to substitute on
     * @param argument The string which defines the substitution
     * @param options The list of options for the match and replace. See the
     *                MATCH_ and REPLACE_ constants above.
     * @return the result of the operation
     * @throws BuildException on error
     */
    String substitute(String input, String argument, int options)
        throws BuildException;
}
