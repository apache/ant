/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for org.apache.tools.ant.util.DOMElementWriter.
 *
 * @author Stefan Bodewig
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
        assertEquals("&#20;", w.encode("&#20;"));
        assertEquals("&#x20;", w.encode("&#x20;"));
        assertEquals("&#xA0;", w.encode("&#xA0;"));
        assertEquals("&amp;#A0;", w.encode("&#A0;"));
        assertEquals("20;", w.encode("20;"));
        assertEquals("&amp;#20", w.encode("&#20"));
        assertEquals("&quot;", w.encode("&quot;"));
        assertEquals("&apos;", w.encode("&apos;"));
        assertEquals("&gt;", w.encode("&gt;"));
        assertEquals("&lt;", w.encode("&lt;"));
        assertEquals("&amp;", w.encode("&amp;"));
        assertEquals("&quot;", w.encode("\""));
        assertEquals("&lt;", w.encode("<"));
        assertEquals("&amp;", w.encode("&"));
        assertEquals("", w.encode("\u0017"));
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
        assertEquals("&#x5d;&#x5d;&gt;", w.encodedata("]]>"));
        assertEquals("&#x5d;&#x5d;&gt;A", w.encodedata("]]>A"));
        assertEquals("A&#x5d;&#x5d;&gt;", w.encodedata("A]]>"));
        assertEquals("A&#x5d;&#x5d;&gt;A", w.encodedata("A]]>A"));
        assertEquals("A&#x5d;&#x5d;&gt;B&#x5d;&#x5d;&gt;C",
                     w.encodedata("A]]>B]]>C"));
    }
}
