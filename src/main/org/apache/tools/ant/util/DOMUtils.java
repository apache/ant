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

package org.apache.tools.ant.util;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

// CheckStyle:HideUtilityClassConstructorCheck OFF - bc

/**
 * Some utility methods for common tasks when building DOM trees in memory.
 *
 * <p>In this documentation <code>&lt;a&gt;</code> means an {@link
 * org.w3c.dom.Element Element} instance with name <code>a</code>.</p>
 *
 * @since Ant 1.6.3
 */
public class DOMUtils {

    /**
     * Get a new Document instance,
     * @return the document.
     * @since Ant 1.6.3
     */
    public static Document newDocument() {
        return JAXPUtils.getDocumentBuilder().newDocument();
    }

    /**
     * Creates a named Element and appends it to the given element,
     * returns it.
     *
     * <p>This means
     * <pre>createChildElement(&lt;a&gt;, "b")</pre>
     * creates
     * <pre>
     * &lt;a&gt;
     *   &lt;b/&gt;
     * &lt;/a&gt;
     * </pre>
     * and returns <code>&lt;b&gt;</code>.
     *
     * @param parent element that will receive the new element as child.
     * @param name name of the new element.
     * @return the new element.
     *
     * @since Ant 1.6.3
     */
    public static Element createChildElement(Element parent, String name) {
        Document doc = parent.getOwnerDocument();
        Element e = doc.createElement(name);
        parent.appendChild(e);
        return e;
    }

    /**
     * Adds nested text.
     *
     * <p>This means
     * <pre>appendText(&lt;a&gt;, "b")</pre>
     * creates
     * <pre>
     * &lt;a&gt;b&lt;/a&gt;
     * </pre>
     *
     * @param parent element that will receive the new element as child.
     * @param content text content.
     *
     * @since Ant 1.6.3
     */
    public static void appendText(Element parent, String content) {
        Document doc = parent.getOwnerDocument();
        Text t = doc.createTextNode(content);
        parent.appendChild(t);
    }

    /**
     * Adds a nested CDATA section.
     *
     * <p>This means
     * <pre>appendCDATA(&lt;a&gt;, "b")</pre>
     * creates
     * <pre>
     * &lt;a&gt;&lt;[!CDATA[b]]&gt;&lt;/a&gt;
     * </pre>
     *
     * @param parent element that will receive the new element as child.
     * @param content text content.
     *
     * @since Ant 1.6.3
     */
    public static void appendCDATA(Element parent, String content) {
        Document doc = parent.getOwnerDocument();
        CDATASection c = doc.createCDATASection(content);
        parent.appendChild(c);
    }

    /**
     * Adds nested text in a new child element.
     *
     * <p>This means
     * <pre>appendTextElement(&lt;a&gt;, "b", "c")</pre>
     * creates
     * <pre>
     * &lt;a&gt;
     *   &lt;b&gt;c&lt;/b&gt;
     * &lt;/a&gt;
     * </pre>
     *
     * @param parent element that will receive the new element as child.
     * @param name of the child element.
     * @param content text content.
     *
     * @since Ant 1.6.3
     */
    public static void appendTextElement(Element parent, String name,
                                         String content) {
        Element e = createChildElement(parent, name);
        appendText(e, content);
    }

    /**
     * Adds a nested CDATA section in a new child element.
     *
     * <p>This means
     * <pre>appendCDATAElement(&lt;a&gt;, "b", "c")</pre>
     * creates
     * <pre>
     * &lt;a&gt;
     *   &lt;b&gt;&lt;![CDATA[c]]&gt;&lt;/b&gt;
     * &lt;/a&gt;
     * </pre>
     *
     * @param parent element that will receive the new element as child.
     * @param name of the child element.
     * @param content text content.
     *
     * @since Ant 1.6.3
     */
    public static void appendCDATAElement(Element parent, String name,
                                          String content) {
        Element e = createChildElement(parent, name);
        appendCDATA(e, content);
    }

    private DOMUtils() {
    }
}
