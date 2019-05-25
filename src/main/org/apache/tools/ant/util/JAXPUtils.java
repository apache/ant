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

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

// CheckStyle:HideUtilityClassConstructorCheck OFF - bc

/**
 * Collection of helper methods that retrieve a ParserFactory or
 * Parsers and Readers.
 *
 * <p>This class will create only a single factory instance.</p>
 *
 * @since Ant 1.5
 */
public class JAXPUtils {

    /**
     * Helper for systemId.
     *
     * @since Ant 1.6
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Parser factory to use to create parsers.
     * @see #getParserFactory
     *
     * @since Ant 1.5
     */
    private static SAXParserFactory parserFactory = null;

    /**
     * Parser Factory to create Namespace aware parsers.
     *
     * @since Ant 1.6
     */
    private static SAXParserFactory nsParserFactory = null;

    /**
     * Parser factory to use to create document builders.
     *
     * @since Ant 1.6
     */
    private static DocumentBuilderFactory builderFactory = null;

    /**
     * Returns the parser factory to use. Only one parser factory is
     * ever created by this method and is then cached for future use.
     *
     * @return a SAXParserFactory to use.
     * @throws BuildException on error.
     *
     * @since Ant 1.5
     */
    public static synchronized SAXParserFactory getParserFactory()
        throws BuildException {

        if (parserFactory == null) {
            parserFactory = newParserFactory();
        }
        return parserFactory;
    }

    /**
     * Returns the parser factory to use to create namespace aware parsers.
     *
     * @return a SAXParserFactory to use which supports manufacture of
     * namespace aware parsers.
     * @throws BuildException on error.
     *
     * @since Ant 1.6
     */
    public static synchronized SAXParserFactory getNSParserFactory()
        throws BuildException {

        if (nsParserFactory == null) {
            nsParserFactory = newParserFactory();
            nsParserFactory.setNamespaceAware(true);
        }
        return nsParserFactory;
    }

    /**
     * Returns a new  parser factory instance.
     *
     * @return the parser factory.
     * @throws BuildException on error.
     * @since Ant 1.5
     */
    public static SAXParserFactory newParserFactory() throws BuildException {

        try {
            return SAXParserFactory.newInstance();
        } catch (FactoryConfigurationError e) {
            throw new BuildException("XML parser factory has not been "
                                     + "configured correctly: "
                                     + e.getMessage(), e);
        }
    }

    /**
     * Returns a newly created SAX 1 Parser, using the default parser
     * factory.
     *
     * @return a SAX 1 Parser.
     * @throws BuildException on error.
     * @see #getParserFactory
     * @since Ant 1.5
     */
    public static Parser getParser() throws BuildException {
        try {
            return newSAXParser(getParserFactory()).getParser();
        } catch (SAXException e) {
            throw convertToBuildException(e);
        }
    }

    /**
     * Returns a newly created SAX 2 XMLReader, using the default parser
     * factory.
     *
     * @return a SAX 2 XMLReader.
     * @throws BuildException on error.
     * @see #getParserFactory
     * @since Ant 1.5
     */
    public static XMLReader getXMLReader() throws BuildException {
        try {
            return newSAXParser(getParserFactory()).getXMLReader();
        } catch (SAXException e) {
            throw convertToBuildException(e);
        }
    }

    /**
     * Returns a newly created SAX 2 XMLReader, which is namespace aware
     *
     * @return a SAX 2 XMLReader.
     * @throws BuildException on error.
     * @see #getParserFactory
     * @since Ant 1.6
     */
    public static XMLReader getNamespaceXMLReader() throws BuildException {
        try {
            return newSAXParser(getNSParserFactory()).getXMLReader();
        } catch (SAXException e) {
            throw convertToBuildException(e);
        }
    }

    /**
     * This is a best attempt to provide a URL.toExternalForm() from
     * a file URL. Some parsers like Crimson choke on uri that are made of
     * backslashed paths (ie windows) as it is does not conform
     * URI specifications.
     * @param file the file to create the system id from.
     * @return the systemid corresponding to the given file.
     * @since Ant 1.5.2
     */
    public static String getSystemId(File file) {
        return FILE_UTILS.toURI(file.getAbsolutePath());
    }

    /**
     * Returns a newly created DocumentBuilder.
     *
     * @return a DocumentBuilder.
     * @throws BuildException on error.
     * @since Ant 1.6
     */
    public static DocumentBuilder getDocumentBuilder() throws BuildException {
        try {
            return getDocumentBuilderFactory().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new BuildException(e);
        }
    }

    /**
     * @return a new SAXParser instance as helper for getParser and
     * getXMLReader.
     *
     * @since Ant 1.5
     */
    private static SAXParser newSAXParser(SAXParserFactory factory)
         throws BuildException {
        try {
            return factory.newSAXParser();
        } catch (ParserConfigurationException e) {
            throw new BuildException("Cannot create parser for the given "
                                     + "configuration: " + e.getMessage(), e);
        } catch (SAXException e) {
            throw convertToBuildException(e);
        }
    }

    /**
     * Translate a SAXException into a BuildException
     *
     * @since Ant 1.5
     */
    private static BuildException convertToBuildException(SAXException e) {
        Exception nested = e.getException();
        if (nested != null) {
            return new BuildException(nested);
        }
        return new BuildException(e);
    }

    /**
     * Obtains the default builder factory if not already.
     *
     * @since Ant 1.6
     */
    private static synchronized
        DocumentBuilderFactory getDocumentBuilderFactory()
        throws BuildException {
        if (builderFactory == null) {
            try {
                builderFactory = DocumentBuilderFactory.newInstance();
            } catch (FactoryConfigurationError e) {
                throw new BuildException("Document builder factory has not "
                                         + "been configured correctly: "
                                         + e.getMessage(), e);
            }
        }
        return builderFactory;
    }

}
