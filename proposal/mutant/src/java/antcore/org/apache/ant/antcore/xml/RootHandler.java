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
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handle the root of an XML parse. This class recognizes the root document
 * element and then passes control to the handler for that root element.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 9 January 2002
 */
public class RootHandler extends DefaultHandler {
    /** The parsing context for this document */
    private ParseContext context;

    /**
     * Locator used to identify where in the build source particular
     * elements occur.
     */
    private Locator locator;

    /** The actual XML parser used to parse the build source */
    private XMLReader reader;

    /** The URL from which the XML source is being read. */
    private URL sourceURL;

    /** The allowed names of the root element in this document */
    private String[] allowedRootNames;

    /** The handler for the root element */
    private ElementHandler rootElementHandler;


    /**
     * Handler to handle the document root.
     *
     * @param context The Parser context for this parse operation
     * @param sourceURL URL of the source containing the XML definition
     * @param reader XML parser
     * @param allowedRootNames An array of allowed element names
     * @param rootElementHandler The element handler for the root element
     */
    public RootHandler(ParseContext context, URL sourceURL, XMLReader reader,
                       String[] allowedRootNames, 
                       ElementHandler rootElementHandler) {
        this.context = context;
        this.sourceURL = sourceURL;
        this.reader = reader;
        this.allowedRootNames = allowedRootNames;
        this.rootElementHandler = rootElementHandler;
    }


    /**
     * Set the locator to use when parsing elements. This is passed onto
     * child elements.
     *
     * @param locator the locator for locating elements in the build source.
     */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }


    /**
     * Start a new element in the root. This must be a project element All
     * other elements are invalid.
     *
     * @param uri The Namespace URI.
     * @param localName The local name (without prefix).
     * @param qualifiedName The qualified name (with prefix)
     * @param attributes The attributes attached to the element.
     * @throws SAXParseException if there is a parsing problem.
     */
    public void startElement(String uri, String localName, String qualifiedName,
                             Attributes attributes)
         throws SAXParseException {
        boolean allowed = false;
        for (int i = 0; i < allowedRootNames.length; ++i) {
            if (qualifiedName.equals(allowedRootNames[i])) {
                allowed = true;
                break;
            }
        }

        if (allowed) {
            rootElementHandler.start(context, reader, this,
                locator, attributes, sourceURL, qualifiedName);
        } else {
            throw new SAXParseException("<" + qualifiedName
                 + "> element was not expected as the root element", locator);
        }
    }
}

