/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for org.apache.tools.ant.util;GlobPatternMapper.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public class GlobPatternMapperTest extends TestCase {

    public GlobPatternMapperTest(String name) {
        super(name);
    }

    public void testNoPatternAtAll() {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("foobar");
        m.setTo("baz");
        assertNull("Shouldn\'t match foobar", m.mapFileName("plonk"));
        String[] result = m.mapFileName("foobar");
        assertNotNull("Should match foobar", result);
        assertEquals("only one result for foobar", 1, result.length);
        assertEquals("baz", result[0]);
    }

    public void testPostfixOnly() {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("*foo");
        m.setTo("*plonk");
        assertNull("Shouldn\'t match *foo", m.mapFileName("bar.baz"));
        String[] result = m.mapFileName("bar.foo");
        assertNotNull("Should match *.foo", result);
        assertEquals("only one result for bar.foo", 1, result.length);
        assertEquals("bar.plonk", result[0]);

        // Try a silly case
        m.setTo("foo*");
        result = m.mapFileName("bar.foo");
        assertEquals("foobar.", result[0]);
    }

    public void testPrefixOnly() {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("foo*");
        m.setTo("plonk*");
        assertNull("Shouldn\'t match foo*", m.mapFileName("bar.baz"));
        String[] result = m.mapFileName("foo.bar");
        assertNotNull("Should match foo*", result);
        assertEquals("only one result for foo.bar", 1, result.length);
        assertEquals("plonk.bar", result[0]);

        // Try a silly case
        m.setTo("*foo");
        result = m.mapFileName("foo.bar");
        assertEquals(".barfoo", result[0]);
    }

    public void testPreAndPostfix() {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("foo*bar");
        m.setTo("plonk*pling");
        assertNull("Shouldn\'t match foo*bar", m.mapFileName("bar.baz"));
        String[] result = m.mapFileName("foo.bar");
        assertNotNull("Should match foo*bar", result);
        assertEquals("only one result for foo.bar", 1, result.length);
        assertEquals("plonk.pling", result[0]);

        // and a little longer
        result = m.mapFileName("foo.baz.bar");
        assertNotNull("Should match foo*bar", result);
        assertEquals("only one result for foo.baz.bar", 1, result.length);
        assertEquals("plonk.baz.pling", result[0]);

        // and a little shorter
        result = m.mapFileName("foobar");
        assertNotNull("Should match foo*bar", result);
        assertEquals("only one result for foobar", 1, result.length);
        assertEquals("plonkpling", result[0]);
    }
}
