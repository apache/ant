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

package org.apache.tools.ant.util.regexp;

import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for all implementations of the RegexpMatcher interface.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public abstract class RegexpMatcherTest extends TestCase {

    public abstract RegexpMatcher getImplementation();

    public RegexpMatcherTest(String name) {
        super(name);
    }

    public void testMatches() {
        RegexpMatcher reg = getImplementation();
        reg.setPattern("aaaa");
        assert("aaaa should match itself", reg.matches("aaaa"));
        assert("aaaa should match xaaaa", reg.matches("xaaaa"));
        assert("aaaa shouldn\'t match xaaa", !reg.matches("xaaa"));
        reg.setPattern("^aaaa");
        assert("^aaaa shouldn\'t match xaaaa", !reg.matches("xaaaa"));
        assert("^aaaa should match aaaax", reg.matches("aaaax"));
        reg.setPattern("aaaa$");
        assert("aaaa$ shouldn\'t match aaaax", !reg.matches("aaaax"));
        assert("aaaa$ should match xaaaa", reg.matches("xaaaa"));
        reg.setPattern("[0-9]+");
        assert("[0-9]+ should match 123", reg.matches("123"));
        assert("[0-9]+ should match 1", reg.matches("1"));
        assert("[0-9]+ shouldn\'t match \'\'", !reg.matches(""));
        assert("[0-9]+ shouldn\'t match a", !reg.matches("a"));
        reg.setPattern("[0-9]*");
        assert("[0-9]* should match 123", reg.matches("123"));
        assert("[0-9]* should match 1", reg.matches("1"));
        assert("[0-9]* should match \'\'", reg.matches(""));
        assert("[0-9]* should match a", reg.matches("a"));
        reg.setPattern("([0-9]+)=\\1");
        assert("([0-9]+)=\\1 should match 1=1", reg.matches("1=1"));
        assert("([0-9]+)=\\1 shouldn\'t match 1=2", !reg.matches("1=2"));
    }

    public void testGroups() {
        RegexpMatcher reg = getImplementation();
        reg.setPattern("aaaa");
        Vector v = reg.getGroups("xaaaa");
        assertEquals("No parens -> no extra groups", 1, v.size());
        assertEquals("Trivial match with no parens", "aaaa", 
                     (String) v.elementAt(0));

        reg.setPattern("(aaaa)");
        v = reg.getGroups("xaaaa");
        assertEquals("Trivial match with single paren", 2, v.size());
        assertEquals("Trivial match with single paren, full match", "aaaa", 
                     (String) v.elementAt(0));
        assertEquals("Trivial match with single paren, matched paren", "aaaa", 
                     (String) v.elementAt(0));

        reg.setPattern("(a+)b(b+)");
        v = reg.getGroups("xaabb");
        assertEquals(3, v.size());
        assertEquals("aabb", (String) v.elementAt(0));
        assertEquals("aa", (String) v.elementAt(1));
        assertEquals("b", (String) v.elementAt(2));
    }
}
