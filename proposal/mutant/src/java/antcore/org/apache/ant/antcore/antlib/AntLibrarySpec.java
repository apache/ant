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
package org.apache.ant.antcore.antlib;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ant.common.util.ConfigException;

/**
 * This class represents the specification of an Ant library. It is merely
 * the internal representation of the antlib XML definition. An instance of
 * this class is used to contruct an AntLibrary instance.
 *
 * @author Conor MacNeill
 * @created 13 January 2002
 */
public class AntLibrarySpec {
    /**
     * This is the globally unique name of this library. It uses the same
     * conventions as the Java package space - i.e. reverse order DNS names
     * This name is used when importing tasks from this library
     */
    private String libraryId;

    /**
     * This string identifies the location where the library is maintained.
     * It is usually a URL to the location from which the library may be
     * downloaded or purchased
     */
    private String libraryHome;

    /** The list of converter classnames defined in this library */
    private List converterClassNames = new ArrayList();

    /** The list of aspect classnames defined in this library */
    private List aspectClassNames = new ArrayList();

    /** The name of the factory class for this library */
    private String factoryClassName;

    /**
     * This is the optional id of another Ant library upon which this
     * library depends.
     */
    private String extendsLibraryId;

    /** This is the URL from which this library has been loaded */
    private URL libraryURL;

    /** This is the list of definitions */
    private Map definitions = new HashMap();

    /** Indicates if each Task Instance should use its own classloader */
    private boolean isolated = false;

    /** Flag which indicates if tools.jar is required */
    private boolean toolsJarRequired = false;

    /**
     * This flag indicates that this task processes XML and wishes to use
     * the XML parser packaged with Ant
     */
    private boolean requiresAntXMLParser = false;

    /**
     * Set the library that this library extends, if any
     *
     * @param extendsLibraryId The new ExtendsLibraryId value
     */
    public void setExtendsLibraryId(String extendsLibraryId) {
        this.extendsLibraryId = extendsLibraryId;
    }

    /**
     * Sets the name of the factory class of the AntLibrarySpec
     *
     * @param className the new factory classname
     */
    public void setFactory(String className) {
        this.factoryClassName = className;
    }

    /**
     * Indicate that this library requires a separate classloader per task
     * instance
     *
     * @param isolated The new Isolated value
     */
    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
    }

    /**
     * Sets the home of the AntLibrary
     *
     * @param home The new home value
     */
    public void setHome(String home) {
        this.libraryHome = libraryHome;
    }

    /**
     * Sets the libraryId of the AntLibrary
     *
     * @param libraryId The new libraryId value
     */
    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
    }


    /**
     * Indicate that this library uses the Sun tools.jar
     *
     * @param toolsJarRequired The new ToolsJarRequired value
     */
    public void setToolsJarRequired(boolean toolsJarRequired) {
        this.toolsJarRequired = toolsJarRequired;
    }

    /**
     * Sets the libraryURL of the AntLibrary
     *
     * @param libraryURL The new libraryURL value
     */
    public void setLibraryURL(URL libraryURL) {
        this.libraryURL = libraryURL;
    }

    /**
     * Indicates that this library uses Ant's XML parser libraries
     *
     * @param requiresAntXMLParser true if this library uses Ant's XML
     *      parser libraries
     */
    public void setAntXML(boolean requiresAntXMLParser) {
        this.requiresAntXMLParser = requiresAntXMLParser;
    }

    /**
     * Get the list of converter classnames defined in this library spec
     *
     * @return the converter classnames list
     */
    public List getConverters() {
        return converterClassNames;
    }


    /**
     * Get the list of aspect classnames defined in this library spec
     *
     * @return the aspect classnames list
     */
    public List getAspects() {
        return aspectClassNames;
    }

    /**
     * Gets the factory classname of the AntLibrarySpec
     *
     * @return the factory classname
     */
    public String getFactory() {
        return factoryClassName;
    }

    /**
     * Indicate whether this AntLibrary requires the Sun tools.jar
     *
     * @return The ToolsJarRequired value
     */
    public boolean isToolsJarRequired() {
        return toolsJarRequired;
    }

    /**
     * Get the id of the library that this library extends if any.
     *
     * @return The ExtendsLibraryId value
     */
    public String getExtendsLibraryId() {
        return extendsLibraryId;
    }

    /**
     * Indicate if this library required an classloader per instance
     *
     * @return true if a separate classloader should be used per instance.
     */
    public boolean isIsolated() {
        return isolated;
    }


    /**
     * Gets the libraryId of the AntLibrary
     *
     * @return The libraryId value
     */
    public String getLibraryId() {
        return libraryId;
    }

    /**
     * Gets the libraryURL of the AntLibrary
     *
     * @return The libraryURL value
     */
    public URL getLibraryURL() {
        return libraryURL;
    }


    /**
     * Gets the definitions of the AntLibrarySpec
     *
     * @return the definitions map
     */
    public Map getDefinitions() {
        return definitions;
    }

    /**
     * Add a converter to this library spec
     *
     * @param className the name of the converter class
     */
    public void addConverter(String className) {
        converterClassNames.add(className);
    }

    /**
     * Add an aspect to this the library spec
     *
     * @param className the name of the aspect class
     */
    public void addAspect(String className) {
        aspectClassNames.add(className);
    }
    
    /**
     * Indicates if this library requires Ant's XML parser
     *
     * @return true if this library requires Ant's XML parser
     */
    public boolean usesAntXML() {
        return requiresAntXMLParser;
    }

    /**
     * Adds a definition to the Ant Library
     *
     * @param name the name of the library definition
     * @param classname the name of the class implementing the element
     * @param definitionTypeName the name of the definition type. This is
     *      converted to its symbolic value
     * @exception ConfigException if the definition has already been defined
     */
    public void addDefinition(String definitionTypeName, String name,
                              String classname)
         throws ConfigException {
        if (definitions.containsKey(name)) {
            throw new ConfigException("More than one definition "
                 + "in library for " + name);
        }
        int definitionType = 0;

        if (definitionTypeName.equals("typedef")) {
            definitionType = AntLibrary.TYPEDEF;
        } else if (definitionTypeName.equals("taskdef")) {
            definitionType = AntLibrary.TASKDEF;
        } else {
            throw new ConfigException("Unknown type of definition "
                 + definitionTypeName);
        }
        definitions.put(name,
            new AntLibDefinition(definitionType, name, classname));
    }

}

