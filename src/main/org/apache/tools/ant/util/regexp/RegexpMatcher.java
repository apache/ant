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

import org.apache.tools.ant.BuildException;

/**
 * Interface describing a regular expression matcher.
 *
 */
public interface RegexpMatcher {

    /***
     * Default Mask (case insensitive, neither multiline nor
     * singleline specified).
     */
    int MATCH_DEFAULT          = 0x00000000;

    /***
     * Perform a case insensitive match
     */
    int MATCH_CASE_INSENSITIVE = 0x00000100;

    /***
     * Treat the input as a multiline input
     */
    int MATCH_MULTILINE        = 0x00001000;

    /***
     * Treat the input as singleline input ('.' matches newline)
     */
    int MATCH_SINGLELINE       = 0x00010000;


    /**
     * Set the regexp pattern from the String description.
     * @param pattern the pattern to match
     * @throws BuildException on error
     */
    void setPattern(String pattern) throws BuildException;

    /**
     * Get a String representation of the regexp pattern
     * @return the pattern
     * @throws BuildException on error
     */
    String getPattern() throws BuildException;

    /**
     * Does the given argument match the pattern?
     * @param argument the string to match against
     * @return true if the pattern matches
     * @throws BuildException on error
     */
    boolean matches(String argument) throws BuildException;

    /**
     * Returns a Vector of matched groups found in the argument
     * using default options.
     *
     * <p>Group 0 will be the full match, the rest are the
     * parenthesized subexpressions</p>.
     *
     * @param argument the string to match against
     * @return the vector of groups
     * @throws BuildException on error
     */
    Vector<String> getGroups(String argument) throws BuildException;

    /***
     * Does this regular expression match the input, given
     * certain options
     * @param input The string to check for a match
     * @param options The list of options for the match. See the
     *                MATCH_ constants above.
     * @return true if the pattern matches
     * @throws BuildException on error
     */
    boolean matches(String input, int options) throws BuildException;

    /***
     * Get the match groups from this regular expression.  The return
     * type of the elements is always String.
     * @param input The string to check for a match
     * @param options The list of options for the match. See the
     *                MATCH_ constants above.
     * @return the vector of groups
     * @throws BuildException on error
     */
    Vector<String> getGroups(String input, int options) throws BuildException;

}
