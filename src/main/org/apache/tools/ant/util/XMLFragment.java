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

import org.apache.tools.ant.DynamicConfiguratorNS;
import org.apache.tools.ant.DynamicElementNS;
import org.apache.tools.ant.ProjectComponent;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Use this class as a nested element if you want to get a literal DOM
 * fragment of something nested into your task/type.
 *
 * <p>This is useful for tasks that want to deal with the "real" XML
 * from the build file instead of objects.</p>
 *
 * <p>Code heavily influenced by code written by Dominique Devienne.</p>
 *
 * @since Ant 1.7
 */
public class XMLFragment extends ProjectComponent implements DynamicElementNS {

    private Document doc;
    private DocumentFragment fragment;

    /**
     * Constructor for XMLFragment object.
     */
    public XMLFragment() {
        doc = JAXPUtils.getDocumentBuilder().newDocument();
        fragment = doc.createDocumentFragment();
    }

    /**
     * @return the DocumentFragment that corresponds to the nested
     *          structure.
     */
    public DocumentFragment getFragment() {
        return fragment;
    }

    /**
     * Add nested text, expanding properties as we go
     * @param s the text to add
     */
    public void addText(String s) {
        addText(fragment, s);
    }

    /**
     * Creates a nested element.
     * @param uri the uri of the nested element
     * @param name the localname of the nested element
     * @param qName the qualified name of the nested element
     * @return an object that the element is applied to
     */
    @Override
    public Object createDynamicElement(String uri, String name, String qName) {
        Element e;
        if (uri.isEmpty()) {
            e = doc.createElement(name);
        } else {
            e = doc.createElementNS(uri, qName);
        }
        fragment.appendChild(e);
        return new Child(e);
    }

    /**
     * Add text to a node.
     * @param n node
     * @param s value
     */
    private void addText(Node n, String s) {
        s = getProject().replaceProperties(s);
        //only text nodes that are non null after property expansion are added
        if (s != null && !s.trim().isEmpty()) {
            Text t = doc.createTextNode(s.trim());
            n.appendChild(t);
        }
    }

    /**
     * An object to handle (recursively) nested elements.
     */
    public class Child implements DynamicConfiguratorNS {
        private Element e;

        Child(Element e) {
            this.e = e;
        }

        /**
         * Add nested text.
         * @param s the text to add
         */
        public void addText(String s) {
            XMLFragment.this.addText(e, s);
        }

        /**
         * Sets the attribute
         * @param uri the uri of the attribute
         * @param name the localname of the attribute
         * @param qName the qualified name of the attribute
         * @param value the value of the attribute
         */
        @Override
        public void setDynamicAttribute(
            String uri, String name, String qName, String value) {
            if (uri.isEmpty()) {
                e.setAttribute(name, value);
            } else {
                e.setAttributeNS(uri, qName, value);
            }
        }

        /**
         * Creates a nested element.
         * @param uri the uri of the nested element
         * @param name the localname of the nested element
         * @param qName the qualified name of the nested element
         * @return an object that the element is applied to
         */
        @Override
        public Object createDynamicElement(String uri, String name, String qName) {
            Element e2 = null;
            if (uri.isEmpty()) {
                e2 = doc.createElement(name);
            } else {
                e2 = doc.createElementNS(uri, qName);
            }
            e.appendChild(e2);
            return new Child(e2);
        }
    }

}
