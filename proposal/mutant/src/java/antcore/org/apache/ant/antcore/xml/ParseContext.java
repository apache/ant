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

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.ant.common.util.Location;
import org.apache.ant.common.util.CircularDependencyChecker;
import org.apache.ant.common.util.CircularDependencyException;
import org.apache.ant.common.util.AntException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Holds the current parsing context.
 *
 * @author Conor MacNeill
 * @created 9 January 2002
 */
public class ParseContext {
    /** These are namespace to URIs which need not be declared in the XML */
    private Map knownNamespaces = new HashMap();

    /**
     * Used to check if we are trying to parse a build file within its own
     * context.
     */
    private CircularDependencyChecker checker
         = new CircularDependencyChecker("parsing XML");

    /** The factory used to create SAX parsers. */
    private SAXParserFactory parserFactory = SAXParserFactory.newInstance();


    /**
     * Parse a URL using the given root handler
     *
     * @param source The URL to the source to be parsed
     * @param rootElementName The required root element name
     * @param rootElementHandler The handler for the root element
     * @exception XMLParseException if the element cannot be parsed
     */
    public void parse(URL source, String rootElementName,
                      ElementHandler rootElementHandler)
         throws XMLParseException {
        parse(source, new String[]{rootElementName}, rootElementHandler);
    }


    /**
     * Parse a URL using the given root handler
     *
     * @param source The URL to the source to be parsed
     * @param rootElementNames The allowable root element names
     * @param rootElementHandler The handler for the root element
     * @exception XMLParseException if the element cannot be parsed
     */
    public void parse(URL source, String[] rootElementNames,
                      ElementHandler rootElementHandler)
         throws XMLParseException {
        try {
            checker.visitNode(source);

            // create a parser for this source
            SAXParser saxParser = parserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();

            // create a root handler for this
            RootHandler rootHandler = new RootHandler(this, source, xmlReader,
                rootElementNames, rootElementHandler);
            saxParser.parse(source.toString(), rootHandler);

            checker.leaveNode(source);
        } catch (ParserConfigurationException e) {
            throw new XMLParseException(e);
        } catch (SAXParseException e) {
            Location location = new Location(e.getSystemId(),
                e.getLineNumber(), e.getColumnNumber());
            if (e.getException() != null) {
                Throwable nestedException = e.getException();
                if (nestedException instanceof AntException) {
                    location = ((AntException) nestedException).getLocation();
                }
                throw new XMLParseException(nestedException, location);
            } else {
                throw new XMLParseException(e, location);
            }
        } catch (SAXException e) {
            throw new XMLParseException(e);
        } catch (IOException e) {
            throw new XMLParseException(e);
        } catch (CircularDependencyException e) {
            throw new XMLParseException(e);
        }
    }

    /**
     * Given an XML qName, this method tries to resolve a name into a URI
     * using the map of well known namespaces.
     *
     * @param qName the XML qName
     * @return the namespace URI for the given name. If the namespace
     *   prefix is unknown the prefix is returned.
     */
    public String resolveNamespace(String qName) {
        String namespaceId = qName.substring(0, qName.indexOf(":"));
        String namespaceURI = (String) knownNamespaces.get(namespaceId);
        return namespaceURI == null ? namespaceId : namespaceURI;
    }

    /**
     * Declare a namespace
     *
     * @param prefix the prefix that is used in the XML for the namespace.
     * @param uri the namespace's unique URI.
     */
    public void declareNamespace(String prefix, String uri) {
        knownNamespaces.put(prefix, uri);
    }
}

