/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.BuildFileTest;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLFragmentTest extends BuildFileTest {
    
    public XMLFragmentTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/types/xmlfragment.xml");
    }

    public void testNestedText() {
        XMLFragment x = (XMLFragment) getProject().getReference("nested-text");
        assertNotNull(x);
        Node n = x.getFragment();
        assertTrue("No attributes", !n.hasAttributes());
        NodeList nl = n.getChildNodes();
        assertEquals(1, nl.getLength());
        assertEquals(Node.TEXT_NODE, nl.item(0).getNodeType());
        assertEquals("foo", nl.item(0).getNodeValue());
    }

    public void testNestedChildren() {
        XMLFragment x = 
            (XMLFragment) getProject().getReference("with-children");
        assertNotNull(x);
        Node n = x.getFragment();
        assertTrue("No attributes", !n.hasAttributes());
        NodeList nl = n.getChildNodes();
        assertEquals(3, nl.getLength());

        assertEquals(Node.ELEMENT_NODE, nl.item(0).getNodeType());
        Element child1 = (Element) nl.item(0);
        assertEquals("child1", child1.getTagName());
        assertTrue(!child1.hasAttributes());
        NodeList nl2 = child1.getChildNodes();
        assertEquals(1, nl2.getLength());
        assertEquals(Node.TEXT_NODE, nl2.item(0).getNodeType());
        assertEquals("foo", nl2.item(0).getNodeValue());

        assertEquals(Node.ELEMENT_NODE, nl.item(1).getNodeType());
        Element child2 = (Element) nl.item(1);
        assertEquals("child2", child2.getTagName());
        assertTrue(child2.hasAttributes());
        nl2 = child2.getChildNodes();
        assertEquals(0, nl2.getLength());
        assertEquals("bar", child2.getAttribute("foo"));

        assertEquals(Node.ELEMENT_NODE, nl.item(2).getNodeType());
        Element child3 = (Element) nl.item(2);
        assertEquals("child3", child3.getTagName());
        assertTrue(!child3.hasAttributes());
        nl2 = child3.getChildNodes();
        assertEquals(1, nl2.getLength());
        assertEquals(Node.ELEMENT_NODE, nl2.item(0).getNodeType());
        assertEquals("child4", ((Element) nl2.item(0)).getTagName());
    }
}
