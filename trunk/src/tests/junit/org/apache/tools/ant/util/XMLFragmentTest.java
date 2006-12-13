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
