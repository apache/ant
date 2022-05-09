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

package org.apache.tools.ant.types.selectors;

import java.io.File;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Provides reusable path pattern matching. {@link TokenizedPattern}
 * is preferable to equivalent {@link SelectorUtils} methods if you need to
 * execute multiple matching with the same pattern because here the pattern
 * itself will be parsed only once.
 * @see SelectorUtils#matchPath(String, String)
 * @see SelectorUtils#matchPath(String, String, boolean)
 * @since 1.8.0
 */
public class TokenizedPattern {

    /**
     * Instance that holds no tokens at all.
     */
    public static final TokenizedPattern EMPTY_PATTERN =
        new TokenizedPattern("", new String[0]);

    private final String pattern;
    private final String[] tokenizedPattern;

    /**
     * Initialize the {@link TokenizedPattern} by parsing it.
     * @param pattern The pattern to match against. Must not be {@code null}.
     */
    public TokenizedPattern(String pattern) {
        this(pattern, SelectorUtils.tokenizePathAsArray(pattern));
    }

    TokenizedPattern(String pattern, String[] tokens) {
        this.pattern = pattern;
        this.tokenizedPattern = tokens;
    }

    /**
     * Tests whether a given path matches a given pattern.
     *
     * @param path    The path to match, as a {@link String}. Must not be
     *                {@code null}.
     * @param isCaseSensitive Whether matching should be performed
     *                        case-sensitively.
     *
     * @return {@code true} if the pattern matches against the string,
     *         or {@code false} otherwise.
     */
    public boolean matchPath(TokenizedPath path, boolean isCaseSensitive) {
        return SelectorUtils.matchPath(tokenizedPattern, path.getTokens(),
                                       isCaseSensitive);
    }

    /**
     * Tests whether this pattern matches the start of
     * a path.
     *
     * @param path {@link TokenizedPath}
     * @param caseSensitive {@code boolean}
     * @return {@code boolean}
     */
    public boolean matchStartOf(TokenizedPath path,
                                boolean caseSensitive) {
        return SelectorUtils.matchPatternStart(tokenizedPattern,
                                               path.getTokens(), caseSensitive);
    }

    /**
     * {@inheritDoc}
     *
     * @return The pattern {@link String}
     */
    public String toString() {
        return pattern;
    }

    /**
     * Get the pattern.
     *
     * @return {@link String}
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * {@inheritDoc}
     * {@code true} if the original patterns are equal.
     *
     * @param o {@link Object}
     */
    public boolean equals(Object o) {
        return o instanceof TokenizedPattern
            && pattern.equals(((TokenizedPattern) o).pattern);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return pattern.hashCode();
    }

    /**
     * Get the depth (or length) of a pattern.
     *
     * @return {@code int}
     */
    public int depth() {
        return tokenizedPattern.length;
    }

    /**
     * Does the tokenized pattern contain the given string?
     *
     * @param pat {@link String}
     * @return {@code boolean}
     */
    public boolean containsPattern(String pat) {
        return Stream.of(tokenizedPattern).anyMatch(Predicate.isEqual(pat));
    }

    /**
     * Returns a new {@link TokenizedPath} where all tokens of this pattern to
     * the right containing wildcards have been removed.
     *
     * @return the leftmost part of the pattern without wildcards
     */
    public TokenizedPath rtrimWildcardTokens() {
        StringBuilder sb = new StringBuilder();
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

    /**
     * Learn whether the last token equals the given string.
     *
     * @param s {@link String}
     * @return {@code boolean}
     */
    public boolean endsWith(String s) {
        return tokenizedPattern.length > 0
            && tokenizedPattern[tokenizedPattern.length - 1].equals(s);
    }

    /**
     * Returns a new pattern without the last token of this pattern.
     *
     * @return {@link TokenizedPattern}
     */
    public TokenizedPattern withoutLastToken() {
        if (tokenizedPattern.length == 0) {
            throw new IllegalStateException("can't strip a token from nothing");
        }
        if (tokenizedPattern.length == 1) {
            return EMPTY_PATTERN;
        }
        String toStrip = tokenizedPattern[tokenizedPattern.length - 1];
        int index = pattern.lastIndexOf(toStrip);
        String[] tokens = new String[tokenizedPattern.length - 1];
        System.arraycopy(tokenizedPattern, 0, tokens, 0,
                         tokenizedPattern.length - 1);
        return new TokenizedPattern(pattern.substring(0, index), tokens);
    }

}
