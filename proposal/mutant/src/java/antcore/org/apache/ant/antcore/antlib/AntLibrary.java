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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.antlib.AntLibFactory;
import org.apache.ant.common.util.ExecutionException;

/**
 * This class represents the Ant library.
 *
 * @author Conor MacNeill
 * @created 14 January 2002
 */
public class AntLibrary implements ComponentLibrary {
    /**
     * This is the globally unique name of this library. It uses the same
     * conventions as the Java package space - i.e. reverse order DNS names
     * This name is used when importing tasks from this library
     */
    private String libraryId;

    /** THe URL of the antlib.xml library spec which defines this library */
    private URL definitionURL;

    /**
     * The URLs to use when contructing a classloader for the components in
     * this library.
     */
    private List libraryURLs = new ArrayList();

    /** The list of converter classnames defined in this library */
    private List converterClassNames = new ArrayList();

    /** The list of aspect classnames defined in this library */
    private List aspectClassNames = new ArrayList();

    /** The class name of this library's factory class, if any */
    private String factoryClassName;

    /** The parent classloader to use when contructing classloaders */
    private ClassLoader parentLoader;

    /** The library which this library extends, if any */
    private AntLibrary extendsLibrary;

    /** Indicates if each Task Instance should use its own classloader */
    private boolean isolated = false;

    /** The classloader for this library if it can be reused */
    private ClassLoader loader = null;

    /** The definitions in the library */
    private Map definitions = null;

    /**
     * Constructor for the AntLibrary object
     *
     * @param spec the specification from which this library is created.
     */
    public AntLibrary(AntLibrarySpec spec) {
        this.libraryId = spec.getLibraryId();
        this.definitions = spec.getDefinitions();
        this.isolated = spec.isIsolated();
        this.converterClassNames.addAll(spec.getConverters());
        this.aspectClassNames.addAll(spec.getAspects());
        this.factoryClassName = spec.getFactory();
        this.definitionURL = spec.getLibraryURL();
    }

    /**
     * Sets the Library which this library extends
     *
     * @param extendsLibrary The new ExtendsLibrary value
     */
    public void setExtendsLibrary(AntLibrary extendsLibrary) {
        this.extendsLibrary = extendsLibrary;
    }

    /**
     * Sets the ParentLoader of the AntLibrary
     *
     * @param parentLoader The new ParentLoader value
     */
    public void setParentLoader(ClassLoader parentLoader) {
        this.parentLoader = parentLoader;
    }

    /**
     * Gets the libraryId of the AntLibrary
     *
     * @return the libraryId value
     */
    public String getLibraryId() {
        return libraryId;
    }

    /**
     * Gets the ClassLoader of the AntLibrary
     *
     * @return The ClassLoader value
     */
    public ClassLoader getClassLoader() {
        if (isolated) {
            return createLoader();
        } else if (loader == null) {
            loader = createLoader();
        }
        return loader;
    }

    /**
     * Gets the definitions (taskdefs and typedefs) of the AntLibrary
     *
     * @return an iterator over the definition names
     */
    public Iterator getDefinitionNames() {
        return definitions.keySet().iterator();
    }

    /**
     * Get the definition of a particular component
     *
     * @param definitionName the name of the component within the library
     * @return an AntLibDefinition instance with information about the
     *      component's definition
     */
    public AntLibDefinition getDefinition(String definitionName) {
        return (AntLibDefinition) definitions.get(definitionName);
    }

    /**
     * Gets the converter class names of the AntLibrary
     *
     * @return an iterator over a list of String class names
     */
    public Iterator getConverterClassNames() {
        return converterClassNames.iterator();
    }

    /**
     * Gets the aspect class names of the AntLibrary
     *
     * @return an iterator over a list of String class names
     */
    public Iterator getAspectClassNames() {
        return aspectClassNames.iterator();
    }

    /**
     * Get the URL to where the library was loaded from
     *
     * @return the library's URL
     */
    public URL getDefinitionURL() {
        return definitionURL;
    }


    /**
     * Gat an instance of a factory object for creating objects in this
     * library.
     *
     * @param context the context to use for the factory creation if
     *      required
     * @return an instance of the factory, or null if this library does not
     *      support a factory
     * @exception ExecutionException if the factory cannot be created
     */
    public AntLibFactory getFactory(AntContext context)
         throws ExecutionException {
        try {
            AntLibFactory libFactory = null;
            if (factoryClassName != null) {
                Class factoryClass = Class.forName(factoryClassName,
                    true, getClassLoader());
                libFactory
                     = (AntLibFactory) factoryClass.newInstance();
                libFactory.init(context);
            }
            return libFactory;
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("Unable to create factory "
                 + factoryClassName + " for the \"" + libraryId
                 + "\" Ant library", e);
        } catch (NoClassDefFoundError e) {
            throw new ExecutionException("Could not load a dependent class ("
                 + e.getMessage() + ") to create the factory "
                 + factoryClassName + " for the \"" + libraryId
                 + "\" Ant library", e);
        } catch (InstantiationException e) {
            throw new ExecutionException("Unable to instantiate factory "
                 + factoryClassName + " for the \"" + libraryId
                 + "\" Ant library", e);
        } catch (IllegalAccessException e) {
            throw new ExecutionException("Unable to access factory "
                 + factoryClassName + " for the \"" + libraryId
                 + "\" Ant library", e);
        }
    }

    /**
     * Indicate whether this library has any converters defined
     *
     * @return true if any converters have been defined
     */
    public boolean hasConverters() {
        return !converterClassNames.isEmpty();
    }

    /**
     * Indicate whether this library has any aspects defined
     *
     * @return true if any aspects have been defined
     */
    public boolean hasAspects() {
        return !aspectClassNames.isEmpty();
    }

    /**
     * Add a library to path to this AntLibrary definition
     *
     * @param libraryURL the URL to the library to be added
     */
    public void addLibraryURL(URL libraryURL) {
        libraryURLs.add(libraryURL);
    }


    /**
     * Create classloader which can be used to load the classes of this ant
     * library
     *
     * @return the classloader for this ant library
     */
    private ClassLoader createLoader() {
        ClassLoader ourParent
             = extendsLibrary == null ? parentLoader
             : extendsLibrary.getClassLoader();
        return new URLClassLoader((URL[]) libraryURLs.toArray(new URL[0]),
            ourParent);
    }

}

