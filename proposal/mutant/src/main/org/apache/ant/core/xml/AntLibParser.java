/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.ant.core.xml;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.ant.core.support.*;
import org.apache.ant.core.execution.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses the TASK-INF/antlib.xml file of an ant library
 * component. An Ant library may contains tasks, apsects and
 * other ant plug in components
 */ 
public class AntLibParser {
    public static final String TASK_ELEMENT = "taskdef";
    public static final String CONVERTER_ELEMENT = "converter";
    
    /**
     * The factory used to create SAX parsers.
     */
    private SAXParserFactory parserFactory;

    /**
     * Parse the library definition
     *
     * @param libSource the URL from where the library XML is read.
     *
     * @throws SAXParseException if there is a problem parsing the task definitions
     */
    public AntLibrary parseAntLibrary(URL libSource, ClassLoader componentLoader) 
            throws ConfigException {
        try {
            parserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = parserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();

            AntLibRootHandler rootHandler = new AntLibRootHandler(libSource, xmlReader, componentLoader);
            saxParser.parse(libSource.toString(), rootHandler);
            return rootHandler.getAntLibrary();
        }
        catch (SAXParseException e) {
            throw new ConfigException(e.getMessage(), e, 
                                      new Location(libSource.toString(), 
                                                   e.getLineNumber(), e.getColumnNumber()));
        }
        catch (ParserConfigurationException e) {
            throw new ConfigException("Unable to parse Ant library component", e, 
                                      new Location(libSource.toString()));
        }
        catch (SAXException e) {
            throw new ConfigException("Unable to parse Ant library component", e, 
                                      new Location(libSource.toString()));
        }
        catch (IOException e) {
            throw new ConfigException("Unable to parse Ant library component", e, 
                                      new Location(libSource.toString()));
        }
    }
    
    /**
     * The root handler handles the antlib element. An ant lib may
     * contain a number of different types of elements
     * <ul>
     *    <li>taskdef</li>
     *    <li>aspect</li>
     *    <li>converter</li>
     * </ul>
     */
    private class AntLibRootHandler extends RootHandler {
        static private final int STATE_LOOKING_FOR_ROOT = 1;
        static private final int STATE_ROOT_SEEN = 2;
        static private final int STATE_FINISHED = 3;
        
        private int state = STATE_LOOKING_FOR_ROOT;
        
        /**
         * The AntLibrary that will be defined by parsing the library's definition
         * file.
         */
        private AntLibrary library = null;

        private ClassLoader componentLoader = null;

        /**
         * Create an Ant Library Root Handler.
         *
         * @param taskdefSource the URL from where the task definitions exist
         * @param reader the XML parser.
         */
        public AntLibRootHandler(URL taskdefSource, XMLReader reader, ClassLoader componentLoader) {
            super(taskdefSource, reader);
            this.componentLoader = componentLoader;
        }
    
        /**
         * Get the library which has been parsed.
         *
         * @return an AntLibary with the library definitions
         */
        public AntLibrary getAntLibrary() {
            return library;
        }

        /**
         * Start a new element in the root. This must be a taskdefs element
         * All other elements are invalid.
         *
         * @param uri The Namespace URI.
         * @param localName The local name (without prefix).
         * @param qualifiedName The qualified name (with prefix)
         * @param attributes The attributes attached to the element. 
         *
         * @throws SAXParseException if there is a parsing problem.
         */
        public void startElement(String uri, String localName, String qualifiedName,
                                 Attributes attributes) throws SAXParseException {
            switch (state) {
                case STATE_LOOKING_FOR_ROOT:
                    if (qualifiedName.equals("antlib")) {
                        state = STATE_ROOT_SEEN;
                        library = new AntLibrary();
                    }
                    else {
                        throw new SAXParseException("An Ant library component must start with an " +
                                                    "<antlib> element and not with <" + 
                                                     qualifiedName + ">", getLocator());
                    }
                    break;
                case STATE_ROOT_SEEN:                                                     
                    if (qualifiedName.equals(TASK_ELEMENT)) {
                        createTaskDef(attributes);
                    }
                    else if (qualifiedName.equals(CONVERTER_ELEMENT)) {
                        createConverterDef(attributes);
                    } 
                    else {
                        throw new SAXParseException("Unrecognized element <" + 
                                                     qualifiedName + "> in Ant library definition", getLocator());
                    }
                    break;
            }
        }
        

        public void createTaskDef(Attributes attributes) throws SAXParseException {
            Set validAttributes = new HashSet();
            validAttributes.add("name");
            validAttributes.add("classname");
            Map attributeValues 
                = AttributeValidator.validateAttributes(TASK_ELEMENT, attributes, 
                                                        validAttributes, getLocator());
            String taskName = (String)attributeValues.get("name");
            String className = (String)attributeValues.get("classname");
            if (taskName == null) {
                throw new SAXParseException("'name' attribute is required in a <" 
                                            + TASK_ELEMENT + "> element",
                                            getLocator());
            }
            if (className == null) {
                throw new SAXParseException("'classname' attribute is required in a " + 
                                            "<" + TASK_ELEMENT + "> element", getLocator());
            }
            
            System.out.println("Adding taskdef for " + taskName);
            TaskDefinition taskdef = new TaskDefinition(getSourceURL(), taskName, className, componentLoader);
            library.addTaskDefinition(taskdef);
        }                        
            
        public void createConverterDef(Attributes attributes) throws SAXParseException {
            Set validAttributes = new HashSet();
            validAttributes.add("target");
            validAttributes.add("classname");
            Map attributeValues 
                = AttributeValidator.validateAttributes("convert", attributes, 
                                                        validAttributes, getLocator());
            String targetClassName = (String)attributeValues.get("target");
            String className = (String)attributeValues.get("classname");
            if (targetClassName == null) {
                throw new SAXParseException("'target' attribute is required in a <" 
                                            + CONVERTER_ELEMENT + "> element",
                                            getLocator());
            }
            if (className == null) {
                throw new SAXParseException("'classname' attribute is required in a " + 
                                            "<" + CONVERTER_ELEMENT + "> element", getLocator());
            }
            
            ConverterDefinition converterDef 
                = new ConverterDefinition(getSourceURL(), className, targetClassName, componentLoader);
            library.addConverterDefinition(converterDef);
        }                        

        public void endElement(String namespaceURI, String localName, String qName) {
            if (state == STATE_ROOT_SEEN && qName.equals("antlib")) {
                state = STATE_FINISHED;
            }
        }
    }
}

