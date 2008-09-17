/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.types.selectors;

import java.io.File;

/**
 * Provides reusable path pattern matching.  PathPattern is preferable
 * to equivalent SelectorUtils methods if you need to execute multiple
 * matching with the same pattern because here the pattern itself will
 * be parsed only once.
 * @see SelectorUtils#matchPath(String, String)
 * @see SelectorUtils#matchPath(String, String, boolean)
 * @since 1.8.0
 */
public class TokenizedPattern {

    private final String pattern;
    private final String tokenizedPattern[];

    /**
    * Initialize the PathPattern by parsing it. 
    * @param pattern The pattern to match against. Must not be
    *                <code>null</code>.
    */
    public TokenizedPattern(String pattern) {
        this(pattern, SelectorUtils.tokenizePathAsArray(pattern));
    }
    
    private TokenizedPattern(String pattern, String[] tokens) {
        this.pattern = pattern;
        this.tokenizedPattern = tokens;
    }

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     *                
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    public boolean matchPath(String str) {
        return SelectorUtils.matchPath(tokenizedPattern, str, true);
    }
    
    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    public boolean matchPath(String str, boolean isCaseSensitive) {
        return SelectorUtils.matchPath(tokenizedPattern, str, isCaseSensitive);
    }
    
    /**
     * Tests whether or not this pattern matches the start of
     * a path.
     */
    public boolean matchStartOf(TokenizedPath path,
                                boolean caseSensitive) {
        return SelectorUtils.matchPatternStart(tokenizedPattern,
                                               path.getTokens(), caseSensitive);
    }

    /**
     * @return The pattern String
     */
    public String toString() {
        return pattern;
    }
    
    public String getPattern() {
        return pattern;
    }

    /**
     * true if the original patterns are equal.
     */
    public boolean equals(Object o) {
        return o instanceof TokenizedPattern
            && pattern.equals(((TokenizedPattern) o).pattern);
    }

    public int hashCode() {
        return pattern.hashCode();
    }

    /**
     * The depth (or length) of a pattern.
     */
    public int depth() {
        return tokenizedPattern.length;
    }

    /**
     * Does the tokenized pattern contain the given string?
     */
    public boolean containsPattern(String pat) {
        for (int i = 0; i < tokenizedPattern.length; i++) {
            if (tokenizedPattern[i].equals(pat)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a new TokenizedPath where all tokens of this pattern to
     * the right containing wildcards have been removed
     * @return the leftmost part of the pattern without wildcards
     */
    public TokenizedPath rtrimWildcardTokens() {
        StringBuffer sb = new StringBuffer();
        int newLen = 0;
        for (; newLen < tokenizedPattern.length; newLen++) {
            if (SelectorUtils.hasWildcards(tokenizedPattern[newLen])) {
                break;
            }
            if (newLen > 0
                && sb.charAt(sb.length() - 1) != File.separatorChar) {
                sb.append(File.separator);
            }
            sb.append(tokenizedPattern[newLen]);
        }
        if (newLen == 0) {
            return TokenizedPath.EMPTY_PATH;
        }
        String[] newPats = new String[newLen];
        System.arraycopy(tokenizedPattern, 0, newPats, 0, newLen);
        return new TokenizedPath(sb.toString(), newPats);
    }
}
