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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.ant.common.util.AttributeCollection;

/**
 * The NamespaceValueCollection holds namespace values for a range of
 * namespaces. Values can be retrieved for a particular namespace attribute
 * or all attributes of a given namespace.
 *
 * @author Conor MacNeill
 * @created 11 January 2002
 */
public class NamespaceValueCollection {
    /**
     * The namespaces defined for this collection. Each entry is an attribute
     * collection keyed by the namespace URI.
     */
    private Map namespaces = new HashMap();

    /**
     * Add the attributes of a given namespace to this collection.
     *
     * @param uri the namespace's URI.
     * @param attributes the collection of attributes for the given namespace.
     */
    public void addAttributes(String uri, AttributeCollection attributes) {
        AttributeCollection currentCollection
            = (AttributeCollection) namespaces.get(uri);
        if (currentCollection == null) {
            currentCollection = new AttributeCollection();
            namespaces.put(uri, currentCollection);
        }
        for (Iterator i = attributes.getAttributeNames(); i.hasNext();) {
            String attributeName = (String) i.next();
            currentCollection.putAttribute(attributeName,
                attributes.getAttribute(attributeName));
        }
    }

    /**
     * Get an iterator on the namespaces which have been given values on this
     * collection
     *
     * @return an iterator of Strings, being the namespaces which have been
     *      given values on this element.
     */
    public Iterator getNames() {
        return namespaces.keySet().iterator();
    }

    /**
     * Get the set of attribute values related to the given namespace
     *
     * @param namespaceURI the namespace URI
     * @return an attribute collection
     */
    public AttributeCollection getAttributes(String namespaceURI) {
        return (AttributeCollection) namespaces.get(namespaceURI);
    }

    /**
     * Get the value of a single namespace attribute
     *
     * @param namespaceURI the namespace URI
     * @param keyName the attribute name
     * @return the namespace attribute value
     */
    public String getAttributeValue(String namespaceURI, String keyName) {
        AttributeCollection namespaceAttributes = getAttributes(namespaceURI);
        if (namespaceAttributes == null) {
            return null;
        }
        return namespaceAttributes.getAttribute(keyName);
    }
}

