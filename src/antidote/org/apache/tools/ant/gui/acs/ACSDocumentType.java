/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.gui.acs;
import java.io.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import com.sun.xml.parser.Parser;
import com.sun.xml.parser.DtdEventListener;
import com.sun.xml.parser.ValidatingParser;
import com.sun.xml.tree.*;
import com.sun.xml.parser.Resolver;

/**
 * Reads the ANT DTD and provides information about it.
 *
 * @version $Revision$
 * @author Nick Davis<a href="mailto:nick_home_account@yahoo.com">nick_home_account@yahoo.com</a>
 */
public class ACSDocumentType extends java.lang.Object {
    /** True if the DTD has been loaded */
    private boolean isInit = false;
    /** Hold the DTD elements */
    private HashMap elementMap = new HashMap();
    /** XML document used to load the DTD */
    final static String XMLDOC = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<!DOCTYPE project SYSTEM \"file:/project.dtd\">" +
        "<project name=\"sample-project\">" + 
        "</project>";

    /**
     * Standard ctor.
     */
    public ACSDocumentType() {
    }
    
    /**
     * Loads the DTD if not already loaded.
     */
    public void init() {
        // Return if already inited. 
        if (isInit) {
            return;
        }
        
        try {
            // Setup the parser
            Parser p = new Parser();
            p.setEntityResolver(new ACSResolver());

            // Setup the builder
            XmlDocumentBuilder builder = new XmlDocumentBuilder();
            SimpleElementFactory fact = new SimpleElementFactory();
            fact.addMapping(new Properties(),
                ACSDocumentType.class.getClassLoader());
            builder.setElementFactory(fact);
            builder.setParser(p);

            DtdHandler dtdh = new DtdHandler();
            p.setDTDHandler(dtdh);

            // Create the default xml file
            InputSource xmldoc = new InputSource(
                new ByteArrayInputStream(XMLDOC.getBytes()));
            
            // Parse the document
            p.parse(xmldoc);
            
            isInit = true;
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    /**
     * Returns the dtd element.
     *
     * @param name the element name
     */
    public DtdElement findElement(String name) {
        return (DtdElement) elementMap.get(name);
    }
    
    /**
     * Class which represents a DTD element.
     */
    static class DtdElement {
        private String _name;
        private String[] _contentModel;
        private DtdAttributes _map = new DtdAttributes();
        
        public String getName() {
            return _name;
        }
        public void setName(String name) {
            _name = name;
        }
        public String[] getContentModel() {
            return _contentModel;
        }
        public void setContentModel(String[] model) {
            _contentModel = model;
        }
        public DtdAttributes getAttributes() {
            return _map;
        }
    }
    
    /**
     * Class which represents a DTD attribute.
     */
    static class DtdAttribute {
        private String _name;
        private String _type;
        private String[] _options;
        private String _defaultValue;
        private boolean _isFixed;
        private boolean _isRequired;
        
        public String getName() {
            return _name;
        }
        public void setName(String name) {
            _name = name;
        }
        public String getType() {
            return _type;
        }
        public void setType(String type) {
            _type = type;
        }
        public String getDefaultValue() {
            return _defaultValue;
        }
        public void setDefaultValue(String value) {
            _defaultValue = value;
        }
        public String[] getOptions() {
            return _options;
        }
        public void setOptions(String[] s) {
            _options = s;
        }
        public boolean isFixed() {
            return _isFixed;
        }
        public void setFixed(boolean value) {
            _isFixed = value;
        }
        public boolean isRequired() {
            return _isRequired;
        }
        public void setRequired(boolean value) {
            _isRequired = value;
        }
    }

    /**
     * Class which represents a collection of DTD attributes.
     */
    public static class DtdAttributes extends HashMap {
        /**
         * Default constructor
         */
        public DtdAttributes() {
        }

        /**
         * Adds the Attribute
         *
         * @param attribute new attribute
         */
        public void addAttribute(DtdAttribute attribute) {
            put(attribute.getName(), attribute);
        }

        /**
         * Return the requested attribute
         *
         * @param name attribute name
         * @returns the requested attribute
         */
        public DtdAttribute getAttribute(String name) {
            return (DtdAttribute) get(name);
        }

        /**
         * @returns an array of the optional attribute names
         */
        public String[] getOptionalAttributes() {
            ArrayList list = new ArrayList();
            Iterator i = values().iterator();
            while(i.hasNext()) {
                DtdAttribute a = (DtdAttribute)i.next();
                if (a.isRequired()) {
                    list.add(a.getName());
                }
            }
            String[] result = new String[list.size()];
            list.toArray(result);
            return result;
        }

        /**
         * @returns an array of the required attribute names
         */
        public String[] getRequiredAttributes() {
            ArrayList list = new ArrayList();
            Iterator i = values().iterator();
            while(i.hasNext()) {
                DtdAttribute a = (DtdAttribute)i.next();
                if (!a.isRequired()) {
                    list.add(a.getName());
                }
            }
            String[] result = new String[list.size()];
            list.toArray(result);
            return result;
        }
        /**
         * @returns an array of the all attribute names
         */
        public String[] getAttributes() {
            ArrayList list = new ArrayList();
            Iterator i = values().iterator();
            while(i.hasNext()) {
                DtdAttribute a = (DtdAttribute)i.next();
                list.add(a.getName());
            }
            String[] result = new String[list.size()];
            list.toArray(result);
            return result;
        }
    }
    
    /**
     * When parsing XML documents, DTD related events are signaled through
     * this interface. 
     */
    class DtdHandler implements DtdEventListener {
        public void externalDtdDecl (
            String publicId,
            String systemId)
            throws SAXException { }
        
        public void internalDtdDecl (
            String internalSubset)
            throws SAXException { }
        
        public void internalEntityDecl (
            String name,
            String value)
            throws SAXException { }
        
        public void externalEntityDecl (
            String name,
            String publicId,
            String systemId)
            throws SAXException { }
        
        public void endDtd ()
            throws SAXException { }
        
        public void notationDecl (
            String name,
            String publicId,
            String systemId)
            throws SAXException { }
        
        public void unparsedEntityDecl (
            String name,
            String publicId,
            String systemId,
            String notationName)
            throws SAXException { }
            
        public void startDtd (
            String rootName
        ) throws SAXException
        {
            elementMap.clear();
        }
        
        /**
         * Reports an attribute declaration found within the DTD.
         *
         * @param elementName The name of the element to which the attribute
         *	applies; this includes a namespace prefix if one was used within
         *	the DTD.
         * @param attributeName The name of the attribute being declared; this
         *	includes a namespace prefix if one was used within the DTD.
         * @param attributeType The type of the attribute, either CDATA, NMTOKEN,
         *	NMTOKENS, ENTITY, ENTITIES, NOTATION, ID, IDREF, or IDREFS as
         *	defined in the XML specification; or null for enumerations.
         * @param options When attributeType is null or NOTATION, this is an
         *	array of the values which are permitted; it is otherwise null.
         * @param defaultValue When not null, this provides the default value
         *	of this attribute.
         * @param isFixed When true, the defaultValue is the only legal value.
         *	(Precludes isRequired.)
         * @param isRequired When true, the attribute value must be provided
         *	for each element of the named type.  (Precludes isFixed.)
         */
        public void attributeDecl (
            String		elementName,
            String		attributeName,
            String		attributeType,
            String		options [],
            String		defaultValue,
            boolean		isFixed,
            boolean		isRequired
        ) throws SAXException
        {
            // Try to find the element.
            DtdElement e = (DtdElement) elementMap.get(elementName);
            if (e == null) {
                throw new SAXException("element " + elementName +
                " not declared before attributes");
            }
            
            // Update the element's attribute.
            DtdAttribute attrib = new DtdAttribute();
            attrib.setName(attributeName);
            attrib.setType(attributeType);
            attrib.setFixed(isFixed);
            attrib.setRequired(isRequired);
            attrib.setDefaultValue(defaultValue);
            attrib.setOptions(options);
            e.getAttributes().addAttribute(attrib);
        }

        /**
         * Reports an element declaration found within the DTD.  The content
         * model will be a string such as "ANY", "EMPTY", "(#PCDATA|foo)*",
         * or "(caption?,tr*)".
         *
         * @param elementName The name of the element; this includes a namespace
         *	prefix if one was used within the DTD.
         * @param contentModel The content model as defined in the DTD, with
         *	any whitespace removed.
         */
        public void elementDecl (
            String elementName,
            String contentModel
        ) throws SAXException
        {
            DtdElement e = new DtdElement();
            e.setName(elementName);

            // Break the contentModel string into pieces.
            ArrayList list = new ArrayList();
            StringTokenizer st = new StringTokenizer(contentModel, "|()*");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if ( s.length() > 0 && !"EMPTY".equals(s) ) {
                    list.add(s);
                }
            }            
            String[] array = new String[list.size()];
            list.toArray(array);
            e.setContentModel(array);
            
            // Update the map
            elementMap.put(e.getName(), e);
        }
    }
    
    /**
     * We provide the location for the ant dtds.
     */
    class ACSResolver implements org.xml.sax.EntityResolver {
        
        /**
         * We process the project.dtd and project-ext.dtd.
         *
         * @param name Used to find alternate copies of the entity, when
         *	this value is non-null; this is the XML "public ID".
         * @param uri Used when no alternate copy of the entity is found;
         *	this is the XML "system ID", normally a URI.
         */
        public InputSource resolveEntity (
            String publicId,
            String systemId)
            throws SAXException, IOException {
                
            final String PROJECT = "project.dtd";
            final String PROJECTEXT = "project-ext.dtd";
            InputStream result = null;
            
            // Is it the project.dtd?
            if (systemId.indexOf(PROJECT) != -1) {
                try {
                    // Look for it as a resource
                    result = getClass().getResourceAsStream(PROJECT);
                } catch (Exception e) {}
            }
            // Is it the project-ext.dtd?
            if (systemId.indexOf(PROJECTEXT) != -1) {
                try {
                    // Look for it as a resource
                    result = getClass().getResourceAsStream(PROJECTEXT);
                } catch (Exception e) {}
            }
            if (result != null) {
                return new InputSource(result);
            }

            // Otherwise, use the default impl.
            com.sun.xml.parser.Resolver r = new com.sun.xml.parser.Resolver();
            return r.resolveEntity(publicId, systemId);
        }
    }
}
