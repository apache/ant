/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

import java.util.Hashtable;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;

import junit.framework.TestCase;

/**
 * Tests for org.apache.tools.ant.util.CollectionUtils.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @author <a href="mailto:jtulley@novell.com">Jeff Tulley</a> 
 */
public class CollectionUtilsTest extends TestCase {

    public CollectionUtilsTest(String name) {
        super(name);
    }

    public void testVectorEquals() {
        assertTrue(!CollectionUtils.equals(null, new Vector()));
        assertTrue(!CollectionUtils.equals(new Vector(), null));
        assertTrue(CollectionUtils.equals(new Vector(), new Vector()));
        Vector v1 = new Vector();
        Stack s2 = new Stack();
        v1.addElement("foo");
        s2.push("foo");
        assertTrue(CollectionUtils.equals(v1, s2));
        assertTrue(CollectionUtils.equals(s2, v1));
        v1.addElement("bar");
        assertTrue(!CollectionUtils.equals(v1, s2));
        assertTrue(!CollectionUtils.equals(s2, v1));
        s2.push("bar");
        assertTrue(CollectionUtils.equals(v1, s2));
        assertTrue(CollectionUtils.equals(s2, v1));
        s2.push("baz");
        assertTrue(!CollectionUtils.equals(v1, s2));
        assertTrue(!CollectionUtils.equals(s2, v1));
        v1.addElement("baz");
        assertTrue(CollectionUtils.equals(v1, s2));
        assertTrue(CollectionUtils.equals(s2, v1));
        v1.addElement("zyzzy");
        s2.push("zyzzy2");
        assertTrue(!CollectionUtils.equals(v1, s2));
        assertTrue(!CollectionUtils.equals(s2, v1));
    }

    public void testDictionaryEquals() {
        assertTrue(!CollectionUtils.equals(null, new Hashtable()));
        assertTrue(!CollectionUtils.equals(new Hashtable(), null));
        assertTrue(CollectionUtils.equals(new Hashtable(), new Properties()));
        Hashtable h1 = new Hashtable();
        Properties p2 = new Properties();
        h1.put("foo", "");
        p2.put("foo", "");
        assertTrue(CollectionUtils.equals(h1, p2));
        assertTrue(CollectionUtils.equals(p2, h1));
        h1.put("bar", "");
        assertTrue(!CollectionUtils.equals(h1, p2));
        assertTrue(!CollectionUtils.equals(p2, h1));
        p2.put("bar", "");
        assertTrue(CollectionUtils.equals(h1, p2));
        assertTrue(CollectionUtils.equals(p2, h1));
        p2.put("baz", "");
        assertTrue(!CollectionUtils.equals(h1, p2));
        assertTrue(!CollectionUtils.equals(p2, h1));
        h1.put("baz", "");
        assertTrue(CollectionUtils.equals(h1, p2));
        assertTrue(CollectionUtils.equals(p2, h1));
        h1.put("zyzzy", "");
        p2.put("zyzzy2", "");
        assertTrue(!CollectionUtils.equals(h1, p2));
        assertTrue(!CollectionUtils.equals(p2, h1));
        p2.put("zyzzy", "");
        h1.put("zyzzy2", "");
        assertTrue(CollectionUtils.equals(h1, p2));
        assertTrue(CollectionUtils.equals(p2, h1));
        h1.put("dada", "1");
        p2.put("dada", "2");
        assertTrue(!CollectionUtils.equals(h1, p2));
        assertTrue(!CollectionUtils.equals(p2, h1));
    }
}
