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
package org.apache.ant.common.model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ant.common.util.Location;

/**
 * A BuildElement is a holder of configuration information for an element of
 * the build. BuildElements may be grouped into a hierarchy to capture any
 * level of element nesting.
 *
 * @author Conor MacNeill
 * @created 20 January 2002
 */
public class BuildElement extends ModelElement {
    /** The attributes of this build element */
    private Map attributes = new HashMap();

    /** The element's name or type  */
    private String type;

    /** The nested task elements that make up this task element.  */
    private List nestedElements = new ArrayList();

    /** The content (text) of this element */
    private String text = "";

    /**
     * Create a Build Element of the given type
     *
     * @param location the location of the element
     * @param type the element's type
     */
    public BuildElement(Location location, String type) {
        super(location);
        this.type = type;
    }

    /**
     * Get the text of this element
     *
     * @return the elements's text.
     */
    public String getText() {
        return text;
    }

    /**
     * Get an iterator over this element's nested elements
     *
     * @return an iterator which provides BuildElement instances
     */
    public Iterator getNestedElements() {
        return nestedElements.iterator();
    }

    /**
     * Get the type of this element
     *
     * @return the element's type
     */
    public String getType() {
        return type;
    }

    /**
     * Get an iterator over the elements's attributes
     *
     * @return an iterator which provide's attribute names
     */
    public Iterator getAttributeNames() {
        return attributes.keySet().iterator();
    }

    /**
     * Get the value of an attribute.
     *
     * @param attributeName the name of the attribute
     * @return the value of the attribute or null if there is no such
     *      attribute.
     */
    public String getAttributeValue(String attributeName) {
        return (String) attributes.get(attributeName);
    }

    /**
     * Add text to this element.
     *
     * @param text the element text to add.
     */
    public void addText(String text) {
        this.text += text;
    }

    /**
     * Add a nested element to this element
     *
     * @param nestedElement the build element to be added.
     */
    public void addNestedElement(BuildElement nestedElement) {
        nestedElements.add(nestedElement);
    }

    /**
     * Add an attribute to this element
     *
     * @param attributeName the name of the attribute
     * @param attributeValue the attribute's value.
     */
    public void addAttribute(String attributeName, String attributeValue) {
        attributes.put(attributeName, attributeValue);
    }
}

