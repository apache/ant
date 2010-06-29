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

import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests for org.apache.tools.ant.util.DOMElementWriter.
 *
 */
public class DOMElementWriterTest extends TestCase {

    private DOMElementWriter w = new DOMElementWriter();

    public DOMElementWriterTest(String name) {
        super(name);
    }

    public void testIsReference() {
        assertTrue("&#20;", w.isReference("&#20;"));
        assertTrue("&#x20;", w.isReference("&#x20;"));
        assertTrue("&#xA0;", w.isReference("&#xA0;"));
        assertTrue("&#A0;", !w.isReference("&#A0;"));
        assertTrue("20;", !w.isReference("20;"));
        assertTrue("&#20", !w.isReference("&#20"));
        assertTrue("&quot;", w.isReference("&quot;"));
        assertTrue("&apos;", w.isReference("&apos;"));
        assertTrue("&gt;", w.isReference("&gt;"));
        assertTrue("&lt;", w.isReference("&lt;"));
        assertTrue("&amp;", w.isReference("&amp;"));
    }

    public void testEncode() {
        assertEquals("&amp;#20;", w.encode("&#20;"));
        assertEquals("&amp;#x20;", w.encode("&#x20;"));
        assertEquals("&amp;#xA0;", w.encode("&#xA0;"));
        assertEquals("&amp;#A0;", w.encode("&#A0;"));
        assertEquals("20;", w.encode("20;"));
        assertEquals("&amp;#20", w.encode("&#20"));
        assertEquals("&amp;quot;", w.encode("&quot;"));
        assertEquals("&amp;apos;", w.encode("&apos;"));
        assertEquals("&amp;gt;", w.encode("&gt;"));
        assertEquals("&amp;lt;", w.encode("&lt;"));
        assertEquals("&amp;amp;", w.encode("&amp;"));
        assertEquals("&quot;", w.encode("\""));
        assertEquals("&lt;", w.encode("<"));
        assertEquals("&amp;", w.encode("&"));
        assertEquals("", w.encode("\u0017"));
        assertEquals("\r\n\t", w.encode("\r\n\t"));
    }

    public void testEncodeAttributeValue() {
        assertEquals("&amp;#20;", w.encodeAttributeValue("&#20;"));
        assertEquals("&amp;#x20;", w.encodeAttributeValue("&#x20;"));
        assertEquals("&amp;#xA0;", w.encodeAttributeValue("&#xA0;"));
        assertEquals("&amp;#A0;", w.encodeAttributeValue("&#A0;"));
        assertEquals("20;", w.encodeAttributeValue("20;"));
        assertEquals("&amp;#20", w.encodeAttributeValue("&#20"));
        assertEquals("&amp;quot;", w.encodeAttributeValue("&quot;"));
        assertEquals("&amp;apos;", w.encodeAttributeValue("&apos;"));
        assertEquals("&amp;gt;", w.encodeAttributeValue("&gt;"));
        assertEquals("&amp;lt;", w.encodeAttributeValue("&lt;"));
        assertEquals("&amp;amp;", w.encodeAttributeValue("&amp;"));
        assertEquals("&quot;", w.encodeAttributeValue("\""));
        assertEquals("&lt;", w.encodeAttributeValue("<"));
        assertEquals("&amp;", w.encodeAttributeValue("&"));
        assertEquals("", w.encodeAttributeValue("\u0017"));
        assertEquals("&#xd;&#xa;&#x9;", w.encodeAttributeValue("\r\n\t"));
    }

    public void testAttributeWithWhitespace() throws IOException {
        Document d = DOMUtils.newDocument();
        Element root = d.createElement("root");
        root.setAttribute("foo", "bar\nbaz");
        StringWriter sw = new StringWriter();
        DOMElementWriter w = new DOMElementWriter();
        w.write(root, sw, 0, "  ");
        assertEquals("<root foo=\"bar&#xa;baz\" />" + StringUtils.LINE_SEP,
                     sw.toString());
    }

    public void testEncodeData() {
        assertEquals("&#20;\"20;&", w.encodedata("&#20;\"20;&"));
        assertEquals("", w.encodedata("\u0017"));
    }

    public void testIsLegalCharacter() {
        assertTrue("0x00", !w.isLegalCharacter('\u0000'));
        assertTrue("0x09", w.isLegalCharacter('\t'));
        assertTrue("0x0A", w.isLegalCharacter('\n'));
        assertTrue("0x0C", w.isLegalCharacter('\r'));
        assertTrue("0x1F", !w.isLegalCharacter('\u001F'));
        assertTrue("0x20", w.isLegalCharacter('\u0020'));
        assertTrue("0xD7FF", w.isLegalCharacter('\uD7FF'));
        assertTrue("0xD800", !w.isLegalCharacter('\uD800'));
        assertTrue("0xDFFF", !w.isLegalCharacter('\uDFFF'));
        assertTrue("0xE000", w.isLegalCharacter('\uE000'));
        assertTrue("0xFFFD", w.isLegalCharacter('\uFFFD'));
        assertTrue("0xFFFE", !w.isLegalCharacter('\uFFFE'));
    }

    public void testCDATAEndEncoding() {
        assertEquals("]>", w.encodedata("]>"));
        assertEquals("]]", w.encodedata("]]"));
        assertEquals("]]]]><![CDATA[>", w.encodedata("]]>"));
        assertEquals("]]]]><![CDATA[>A", w.encodedata("]]>A"));
        assertEquals("A]]]]><![CDATA[>", w.encodedata("A]]>"));
        assertEquals("A]]]]><![CDATA[>A", w.encodedata("A]]>A"));
        assertEquals("A]]]]><![CDATA[>B]]]]><![CDATA[>C",
                     w.encodedata("A]]>B]]>C"));
    }

    public void testNoAdditionalWhiteSpaceForText() throws IOException {
        Document d = DOMUtils.newDocument();
        Element root = d.createElement("root");
        DOMUtils.appendTextElement(root, "textElement", "content");

        StringWriter sw = new StringWriter();
        DOMElementWriter w = new DOMElementWriter();
        w.write(root, sw, 0, "  ");
        assertEquals("<root>" + StringUtils.LINE_SEP
                     + "  <textElement>content</textElement>"
                     + StringUtils.LINE_SEP
                     + "</root>" + StringUtils.LINE_SEP,
                     sw.toString());
    }

    public void testNoAdditionalWhiteSpaceForCDATA() throws IOException {
        Document d = DOMUtils.newDocument();
        Element root = d.createElement("root");
        DOMUtils.appendCDATAElement(root, "cdataElement", "content");

        StringWriter sw = new StringWriter();
        DOMElementWriter w = new DOMElementWriter();
        w.write(root, sw, 0, "  ");
        assertEquals("<root>" + StringUtils.LINE_SEP
                     + "  <cdataElement><![CDATA[content]]></cdataElement>"
                     + StringUtils.LINE_SEP
                     + "</root>" + StringUtils.LINE_SEP,
                     sw.toString());
    }

    public void testNoAdditionalWhiteSpaceForEmptyElement() throws IOException {
        Document d = DOMUtils.newDocument();
        Element root = d.createElement("root");
        DOMUtils.createChildElement(root, "emptyElement");

        StringWriter sw = new StringWriter();
        DOMElementWriter w = new DOMElementWriter();
        w.write(root, sw, 0, "  ");
        assertEquals("<root>" + StringUtils.LINE_SEP
                     //                     + "  <emptyElement></emptyElement>"
                     + "  <emptyElement />"
                     + StringUtils.LINE_SEP
                     + "</root>" + StringUtils.LINE_SEP,
                     sw.toString());
    }

    public void testNoNSPrefixByDefault() throws IOException {
        Document d = DOMUtils.newDocument();
        Element root = d.createElementNS("urn:foo", "root");
        root.setAttributeNS("urn:foo2", "bar", "baz");

        StringWriter sw = new StringWriter();
        DOMElementWriter w = new DOMElementWriter();
        w.write(root, sw, 0, "  ");
        assertEquals("<root bar=\"baz\" />"
                     + StringUtils.LINE_SEP, sw.toString());
    }

    public void testNSOnElement() throws IOException {
        Document d = DOMUtils.newDocument();
        Element root = d.createElementNS("urn:foo", "root");
        root.setAttributeNS("urn:foo2", "bar", "baz");

        StringWriter sw = new StringWriter();
        DOMElementWriter w =
            new DOMElementWriter(false,
                                 DOMElementWriter.XmlNamespacePolicy
                                 .ONLY_QUALIFY_ELEMENTS);
        w.write(root, sw, 0, "  ");
        assertEquals("<root bar=\"baz\" xmlns=\"urn:foo\" />"
                     + StringUtils.LINE_SEP, sw.toString());
    }

    public void testNSPrefixOnAttribute() throws IOException {
        Document d = DOMUtils.newDocument();
        Element root = d.createElementNS("urn:foo", "root");
        root.setAttributeNS("urn:foo2", "bar", "baz");

        StringWriter sw = new StringWriter();
        DOMElementWriter w =
            new DOMElementWriter(false,
                                 DOMElementWriter.XmlNamespacePolicy
                                 .QUALIFY_ALL);
        w.write(root, sw, 0, "  ");
        assertEquals("<root ns0:bar=\"baz\" xmlns=\"urn:foo\""
                     + " xmlns:ns0=\"urn:foo2\" />"
                     + StringUtils.LINE_SEP, sw.toString());
    }

    public void testNSPrefixOnAttributeEvenWithoutElement() throws IOException {
        Document d = DOMUtils.newDocument();
        Element root = d.createElementNS("urn:foo", "root");
        root.setAttributeNS("urn:foo2", "bar", "baz");

        StringWriter sw = new StringWriter();
        DOMElementWriter w =
            new DOMElementWriter(false,
                                 new DOMElementWriter.XmlNamespacePolicy(false,
                                                                         true)
                                 );
        w.write(root, sw, 0, "  ");
        assertEquals("<root ns0:bar=\"baz\" xmlns:ns0=\"urn:foo2\" />"
                     + StringUtils.LINE_SEP, sw.toString());
    }

    public void testNSGetsReused() throws IOException {
        Document d = DOMUtils.newDocument();
        Element root = d.createElementNS("urn:foo", "root");
        Element child = d.createElementNS("urn:foo", "child");
        root.appendChild(child);
        StringWriter sw = new StringWriter();
        DOMElementWriter w =
            new DOMElementWriter(false,
                                 DOMElementWriter.XmlNamespacePolicy
                                 .ONLY_QUALIFY_ELEMENTS);
        w.write(root, sw, 0, "  ");
        assertEquals("<root xmlns=\"urn:foo\">"
                     + StringUtils.LINE_SEP
                     + "  <child />"
                     + StringUtils.LINE_SEP
                     + "</root>"
                     + StringUtils.LINE_SEP, sw.toString());
    }

    public void testNSGoesOutOfScope() throws IOException {
        Document d = DOMUtils.newDocument();
        Element root = d.createElementNS("urn:foo", "root");
        Element child = d.createElementNS("urn:foo2", "child");
        root.appendChild(child);
        Element child2 = d.createElementNS("urn:foo2", "child");
        root.appendChild(child2);
        Element grandChild = d.createElementNS("urn:foo2", "grandchild");
        child2.appendChild(grandChild);
        Element child3 = d.createElementNS("urn:foo2", "child");
        root.appendChild(child3);
        StringWriter sw = new StringWriter();
        DOMElementWriter w =
            new DOMElementWriter(false,
                                 DOMElementWriter.XmlNamespacePolicy
                                 .ONLY_QUALIFY_ELEMENTS);
        w.write(root, sw, 0, "  ");
        assertEquals("<root xmlns=\"urn:foo\">"
                     + StringUtils.LINE_SEP
                     + "  <ns0:child xmlns:ns0=\"urn:foo2\" />"
                     + StringUtils.LINE_SEP
                     + "  <ns1:child xmlns:ns1=\"urn:foo2\">"
                     + StringUtils.LINE_SEP
                     + "    <ns1:grandchild />"
                     + StringUtils.LINE_SEP
                     + "  </ns1:child>"
                     + StringUtils.LINE_SEP
                     + "  <ns2:child xmlns:ns2=\"urn:foo2\" />"
                     + StringUtils.LINE_SEP
                      + "</root>"
                     + StringUtils.LINE_SEP, sw.toString());
    }
}
