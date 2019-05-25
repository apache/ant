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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TokenizedPatternTest {
    private static final String DOT_SVN_PATTERN =
        SelectorUtils.DEEP_TREE_MATCH + File.separator + ".svn"
        + File.separator + SelectorUtils.DEEP_TREE_MATCH;

    @Test
    public void testTokenization() {
        TokenizedPattern pat = new TokenizedPattern(DOT_SVN_PATTERN);
        assertEquals(3, pat.depth());
        assertEquals(DOT_SVN_PATTERN, pat.getPattern());
        assertTrue(pat.containsPattern(SelectorUtils.DEEP_TREE_MATCH));
        assertTrue(pat.containsPattern(".svn"));
    }

    @Test
    public void testEndsWith() {
        assertTrue(new TokenizedPattern(DOT_SVN_PATTERN)
                   .endsWith(SelectorUtils.DEEP_TREE_MATCH));
    }

    @Test
    public void testWithoutLastToken() {
        assertEquals(SelectorUtils.DEEP_TREE_MATCH + File.separatorChar
                     + ".svn" + File.separator,
                     new TokenizedPattern(DOT_SVN_PATTERN)
                     .withoutLastToken().getPattern());
    }

    @Test
    public void testMatchPath() {
        File f = new File(".svn");
        TokenizedPath p = new TokenizedPath(f.getAbsolutePath());
        assertTrue(new TokenizedPattern(DOT_SVN_PATTERN).matchPath(p, true));
        assertTrue(new TokenizedPattern(DOT_SVN_PATTERN)
                   .withoutLastToken().matchPath(p, true));
    }

    /**
     * this test illustrates the behavior described in bugzilla 59114
     * meaning that the pattern "**" matches the empty path
     * but the pattern "*" does not
     */
    @Test
    public void testEmptyFolderWithStarStar() {
        TokenizedPath p = TokenizedPath.EMPTY_PATH;
        assertTrue(new TokenizedPattern(SelectorUtils.DEEP_TREE_MATCH).matchPath(p, true));
        assertFalse(new TokenizedPattern("*").matchPath(p, true));
    }

}
