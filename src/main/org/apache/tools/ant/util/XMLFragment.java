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

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicConfigurator;
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
public class XMLFragment implements DynamicConfigurator {

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
    public void setDynamicAttribute(String name, String value)
        throws BuildException {
        throw new BuildException("Attribute " + name + " is not supported.");
    }

    /**
     * Creates a nested element.
     */
    public Object createDynamicElement(String name) {
        /* I don't get the namespace prefix here
        Element e = doc
            .createElementNS(ProjectHelper.extractUriFromComponentName(name),
                             ProjectHelper.extractNameFromComponentName(name));
        */
        Element e = doc.createElement(name);
        fragment.appendChild(e);
        return new Child(e);
    }

    private void addText(Node n, String s) {
        if (s != null && !s.trim().equals("")) {
            Text t = doc.createTextNode(s);
            n.appendChild(t);
        }
    }

    public class Child implements DynamicConfigurator {
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
        public void setDynamicAttribute(String name, String value) {
            e.setAttribute(name, value);
        }

        /**
         * Creates a nested element.
         */
        public Object createDynamicElement(String name) {
            /*
            Element e2 = doc
                .createElementNS(ProjectHelper
                                 .extractUriFromComponentName(name),
                                 ProjectHelper
                                 .extractNameFromComponentName(name));
            */
            Element e2 = doc.createElement(name);
            e.appendChild(e2);
            return new Child(e2);
        }
    }

}
