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

package org.apache.tools.ant;

import java.util.Enumeration;
import java.util.Vector;
import org.xml.sax.AttributeList;
import org.xml.sax.helpers.AttributeListImpl;

/**
 * Wrapper class that holds the attributes of a Task (or elements
 * nested below that level) and takes care of configuring that element
 * at runtime.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class RuntimeConfigurable {

    private String elementTag = null;
    private Vector children = new Vector();
    private Object wrappedObject = null;
    private AttributeList attributes;
    private StringBuffer characters = new StringBuffer();

    /**
     * @param proxy The element to wrap.
     */
    public RuntimeConfigurable(Object proxy, String elementTag) {
        wrappedObject = proxy;
        this.elementTag = elementTag;
    }

    void setProxy(Object proxy) {
        wrappedObject = proxy;
    }

    /**
     * Set's the attributes for the wrapped element.
     */
    public void setAttributes(AttributeList attributes) {
        this.attributes = new AttributeListImpl(attributes);
    }

    /**
     * Returns the AttributeList of the wrapped element.
     */
    public AttributeList getAttributes() {
        return attributes;
    }

    /**
     * Adds child elements to the wrapped element.
     */
    public void addChild(RuntimeConfigurable child) {
        children.addElement(child);
    }

    /**
     * Returns the child with index <code>index</code>.
     */
    RuntimeConfigurable getChild(int index) {
        return (RuntimeConfigurable) children.elementAt(index);
    }

    /**
     * Add characters from #PCDATA areas to the wrapped element.
     */
    public void addText(String data) {
        characters.append(data);
    }

    /**
     * Add characters from #PCDATA areas to the wrapped element.
     */
    public void addText(char[] buf, int start, int end) {
        addText(new String(buf, start, end));
    }

    public String getElementTag() {
        return elementTag;
    }
    
    
    /**
     * Configure the wrapped element and all children.
     */
    public void maybeConfigure(Project p) throws BuildException {
	String id = null;
	
        if (attributes != null) {
            ProjectHelper.configure(wrappedObject, attributes, p);
            id = attributes.getValue("id");
            attributes = null;
        }
        if (characters.length() != 0) {
            ProjectHelper.addText(p, wrappedObject, characters.toString());
            characters.setLength(0);
        }
        Enumeration enum = children.elements();
        while (enum.hasMoreElements()) {
            RuntimeConfigurable child = (RuntimeConfigurable) enum.nextElement();
            child.maybeConfigure(p);
            ProjectHelper.storeChild(p, wrappedObject, child.wrappedObject, child.getElementTag().toLowerCase());
        }

        if (id != null) {
            p.addReference(id, wrappedObject);
        }
    }

}
