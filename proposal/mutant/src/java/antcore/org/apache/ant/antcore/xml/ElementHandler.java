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
package org.apache.ant.antcore.xml;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.ant.common.util.Location;
import org.apache.ant.common.util.AttributeCollection;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.ant.common.util.PropertyUtils;

/**
 * An Element Handler is a handler which handles a single element by
 * becoming the handler for the parser while processing the element. Any sub
 * elements must be delegated to separate handlers. When this element is
 * finished, control returns to the parent handler.
 *
 * @author Conor MacNeill
 * @created 9 January 2002
 */
public abstract class ElementHandler extends DefaultHandler {
    /** The parsing context for parsing this element */
    private ParseContext context;

    /**
     * Locator used to identify where in the build source particular
     * elements occur.
     */
    private Locator locator;

    /** The actual XML parser used to parse the build source */
    private XMLReader reader;

    /** The parent element handler */
    private ContentHandler parent;

    /** The URL from which we are reading source */
    private URL source;

    /** The name of this element */
    private String elementName;

    /** The attributes read from this element */
    private AttributeCollection elementAttributes;

    /**
     * This map contains a set of attribute collections for each namespace
     * encountered.
     */
    private Map namespaces;

    /** The content of this element */
    private String content;

    /**
     * Get the source which contains this element
     *
     * @return the URL from which this element is being read
     */
    public URL getElementSource() {
        return source;
    }

    /**
     * Get an interator over the namespace URIs encountered in the processing
     * of the element
     *
     * @return an iterator over the namespace URIs.
     */
    public Iterator getNamespaces() {
        return namespaces.keySet().iterator();
    }

    /**
     * Get the collection of namespace attributes for a given namespace.
     *
     * @param uri the URI of the namespace from which the attribute collection
     * is required.
     *
     * @return an attribute collection if any attributes of the requested
     *  namespace have beebn encountered - otherwise null.
     */
    public AttributeCollection getNamespaceAttributes(String uri) {
        return (AttributeCollection) namespaces.get(uri);
    }

    /**
     * Gets the attributeValue attribute of the ElementHandler object
     *
     * @param attributeName th name of the attribute
     * @return The corresponding attribute value or null if the attribute wa
     *      snot defined.
     */
    public String getAttribute(String attributeName) {
        return elementAttributes.getAttribute(attributeName);
    }

    /**
     * Get an attribute as a boolean value
     *
     * @param attributeName the name of the attribute
     * @return the attribute value as a boolean
     */
    protected boolean getBooleanAttribute(String attributeName) {
        return PropertyUtils.toBoolean(getAttribute(attributeName));
    }

    /**
     * Get an iterator to this elements attributes
     *
     * @return an iterator over the attribute names
     */
    public Iterator getAttributes() {
        return elementAttributes.getAttributeNames();
    }

    /**
     * Gets the content of the element
     *
     * @return The content value
     */
    public String getContent() {
        return content;
    }


    /**
     * Start this element handler.
     *
     * @param parent the element handler for the element which contains this
     *      one.
     * @param locator the locator is used to get location information from
     *      elements.
     * @param attributes the element's attributes.
     * @param source the URL from which the XML source is being parsed.
     * @param xmlReader the parser being used
     * @param context the parser context for this element
     * @param elementName the actual element Name for this element in the
     *      XML
     * @exception SAXParseException if there is a problem parsing the
     *      element
     */
    public final void start(ParseContext context, XMLReader xmlReader,
                            ContentHandler parent, Locator locator,
                            Attributes attributes, URL source,
                            String elementName)
         throws SAXParseException {
        this.context = context;
        this.reader = xmlReader;
        this.parent = parent;
        this.locator = locator;
        this.source = source;
        this.elementName = elementName;
        processAttributes(attributes);
        processElement(elementName);
        reader.setContentHandler(this);
    }


    /**
     * Process an element. This resolves any namespaces against
     * prefixes declared in the ParseContext.
     *
     * @param uri The Namespace URI.
     * @param localName The local name (without prefix).
     * @param qualifiedName The qualified name (with prefix)
     * @param attributes The attributes attached to the element.
     * @throws SAXParseException if there is a problem parsng the subelement
     */
    final public void startElement(String uri, String localName,
                                   String qualifiedName, Attributes attributes)
         throws SAXParseException {
        addNestedElement(uri, localName, qualifiedName, attributes);
    }

    /**
     * By default an element handler does not support nested elements. This
     * method will always throw an exception. Subclasses should override
     * this method to support their own nested elements
     *
     * @param uri The Namespace URI.
     * @param localName The local name (without prefix).
     * @param qualifiedName The qualified name (with prefix)
     * @param attributes The attributes attached to the element.
     * @throws SAXParseException if there is a problem parsng the subelement
     */
    protected void addNestedElement(String uri, String localName,
                                    String qualifiedName,
                                    Attributes attributes)
         throws SAXParseException {
        throw new SAXParseException("<" + elementName + "> does not support a <"
             + qualifiedName + "> nested element", getLocator());
    }


    /**
     * Handle the end of this element by making the parent element handler
     * the current content handler
     *
     * @param localName The local name (without prefix).
     * @param namespaceURI The Namespace URI.
     * @param qName the qualified name of the element
     */
    public final void endElement(String namespaceURI, String localName,
                                 String qName) {
        finish();
        reader.setContentHandler(parent);
    }

    /**
     * Record content of this element
     *
     * @param buf the buffer containing the content to be added
     * @param start start position in the buffer
     * @param end end position in the buffer
     * @exception SAXParseException if there is a parsing error.
     * @see org.xml.sax.ContentHandler.characters()
     */
    public void characters(char[] buf, int start, int end)
         throws SAXParseException {
        if (content == null) {
            content = "";
        }
        content += new String(buf, start, end);
    }


    /**
     * Get the current parsing location
     *
     * @return a location instance representing the current parse position
     */
    protected Location getLocation() {
        return new Location(locator.getSystemId(), locator.getLineNumber(),
            locator.getColumnNumber());
    }


    /**
     * Get the XML Reader being used to parse the XML.
     *
     * @return the XML Reader.
     */
    protected XMLReader getXMLReader() {
        return reader;
    }


    /**
     * Get the parsing context
     *
     * @return the parsing context of this element
     */
    protected ParseContext getParseContext() {
        return context;
    }


    /**
     * Get the locator used to locate elements in the XML source as they are
     * parsed.
     *
     * @return the locator object which can be used to determine an elements
     *      location within the XML source
     */
    protected Locator getLocator() {
        return locator;
    }


    /**
     * Process the element.
     *
     * @param elementName the name of the element
     * @exception SAXParseException if there is a problem parsing the
     *      element
     */
    protected abstract void processElement(String elementName)
         throws SAXParseException;

    /**
     * Process all of the attributes of the element into maps, one for
     * aspects and one for other attributes
     *
     * @param attributes The SAX attributes collection for the element
     * @exception SAXParseException if there is a problem reading the
     *      attributes
     */
    protected final void processAttributes(Attributes attributes)
         throws SAXParseException {
        namespaces = new HashMap();
        elementAttributes = new AttributeCollection();
        int length = attributes.getLength();
        for (int i = 0; i < length; ++i) {
            String localName = attributes.getLocalName(i);
            String qName = attributes.getQName(i);
            String uri = attributes.getURI(i);
            if (uri != null && uri.trim().length() == 0) {
                uri = null;
            }


            if (uri == null) {
                if (qName.indexOf(":") != -1) {
                    // try to resolve through known namespaces
                    uri = context.resolveNamespace(qName);
                    localName = qName.substring(qName.indexOf(":") + 1);
                } else {
                    localName = qName;
                }
            }

            String attributeValue = attributes.getValue(i);
            if (uri != null) {
                AttributeCollection namespaceAttributes
                    = (AttributeCollection) namespaces.get(uri);
                if (namespaceAttributes == null) {
                    namespaceAttributes = new AttributeCollection();
                    namespaces.put(uri, namespaceAttributes);
                }

                namespaceAttributes.putAttribute(localName, attributeValue);
            } else {
                validateAttribute(localName, attributeValue);
                elementAttributes.putAttribute(localName, attributeValue);
            }
        }
    }

    /**
     * Validate that the given attribute and value are valid. By default all
     * attributes are considered invalid. This method should be overrider by
     * subclasses to allow specific attributes
     *
     * @param attributeName The name of the attributes
     * @param attributeValue The value of the attributes
     * @exception SAXParseException if the attribute is not allowed on the
     *      element.
     */
    protected void validateAttribute(String attributeName,
                                     String attributeValue)
         throws SAXParseException {
        throwInvalidAttribute(attributeName);
    }

    /**
     * Throws an invalid attribute exception
     *
     * @param attributeName The name of the invalid attribute
     * @exception SAXParseException always - indicating attribute is invalid
     */
    protected final void throwInvalidAttribute(String attributeName)
         throws SAXParseException {
        throw new SAXParseException("The attribute '" + attributeName
             + "' is not " + "supported by the <" + elementName
             + "> element", getLocator());
    }


    /**
     * This method is called when this element is finished being processed.
     * This is a template method allowing subclasses to complete any
     * necessary processing.
     */
    protected void finish() {
    }
}

