/*
 * Copyright  2003-2004 The Apache Software Foundation
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

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicConfiguratorNS;
import org.apache.tools.ant.ProjectHelper;

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
public class XMLFragment implements DynamicConfiguratorNS {

    private Document doc;
    private DocumentFragment fragment;

    public XMLFragment() {
        doc = JAXPUtils.getDocumentBuilder().newDocument();
        fragment = doc.createDocumentFragment();
    }

    /**
     * Return the DocumentFragment that corresponds to the nested
     * structure.
     */
    public DocumentFragment getFragment() {
        return fragment;
    }

    /**
     * Add nested text.
     */
    public void addText(String s) {
        addText(fragment, s);
    }

    /**
     * No attributes for the wrapping element.
     */
    public void setDynamicAttribute(String uri, String name, String qName, String value)
        throws BuildException {
        throw new BuildException("Attribute " + name + " is not supported.");
    }

    /**
     * Creates a nested element.
     */
    public Object createDynamicElement(String uri, String name, String qName) {
        Element e = doc.createElementNS(uri, qName);
        fragment.appendChild(e);
        return new Child(e);
    }

    private void addText(Node n, String s) {
        if (s != null && !s.trim().equals("")) {
            Text t = doc.createTextNode(s);
            n.appendChild(t);
        }
    }

    public class Child implements DynamicConfiguratorNS {
        private Element e;

        Child(Element e) {
            this.e = e;
        }

        /**
         * Add nested text.
         */
        public void addText(String s) {
            XMLFragment.this.addText(e, s);
        }

        /**
         * Sets the attribute
         */
        public void setDynamicAttribute(
            String uri, String name, String qName, String value) {
            if (uri.equals("")) {
                e.setAttribute(name, value);
            } else {
                e.setAttributeNS(uri, qName, value);
            }
        }

        /**
         * Creates a nested element.
         */
        public Object createDynamicElement(String uri, String name, String qName) {
            Element e2 = null;
            if (uri.equals("")) {
                e2 = doc.createElement(name);
            } else {
                e2 = doc.createElementNS(uri, qName);
            }
            e.appendChild(e2);
            return new Child(e2);
        }
    }

}
