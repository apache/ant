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
package org.apache.ant.antcore.execution;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ant.antcore.antlib.AntLibDefinition;
import org.apache.ant.antcore.antlib.AntLibManager;
import org.apache.ant.antcore.antlib.AntLibrary;
import org.apache.ant.antcore.antlib.ComponentLibrary;
import org.apache.ant.antcore.antlib.DynamicLibrary;
import org.apache.ant.common.antlib.AntLibFactory;
import org.apache.ant.common.antlib.Converter;
import org.apache.ant.common.antlib.StandardLibFactory;
import org.apache.ant.common.event.MessageLevel;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.util.ExecutionException;

/**
 * The instance of the ComponentServices made available by the core to the
 * ant libraries.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 27 January 2002
 */
public class ComponentManager implements ComponentService {
    /** The prefix for library ids that are automatically imported */
    public static final String ANT_LIB_PREFIX = "ant.";

    /**
     * Type converters for this frame. Converters are used when configuring
     * Tasks to handle special type conversions.
     */
    private Map converters = new HashMap();

    /** This is the set of libraries whose converters have been loaded */
    private Set loadedConverters = new HashSet();

    /** The factory objects for each library, indexed by the library Id */
    private Map libFactories = new HashMap();

    /** The Frame this service instance is working for */
    private Frame frame;

    /** The library manager instance used to configure libraries. */
    private AntLibManager libManager;

    /**
     * These are AntLibraries which have been loaded into this component
     * manager
     */
    private Map antLibraries;

    /** dynamic libraries which have been defined */
    private Map dynamicLibraries;

    /** The definitions which have been imported into this frame. */
    private Map definitions = new HashMap();

    /**
     * This map stores a list of additional paths for each library indexed
     * by the libraryId
     */
    private Map libPathsMap;

    /**
     * Constructor
     *
     * @param frame the frame containing this context
     * @param allowRemoteLibs true if remote libraries can be loaded though
     *      this service.
     * @param configLibPaths the additional library paths specified in the
     *      configuration
     */
    protected ComponentManager(Frame frame, boolean allowRemoteLibs,
                               Map configLibPaths) {
        this.frame = frame;
        libManager = new AntLibManager(allowRemoteLibs);
        dynamicLibraries = new HashMap();
        libPathsMap = new HashMap(configLibPaths);
    }

    /**
     * Load a library or set of libraries from a location making them
     * available for use
     *
     * @param libLocation the file or URL of the library location
     * @param importAll if true all tasks are imported as the library is
     *      loaded
     * @exception ExecutionException if the library cannot be loaded
     */
    public void loadLib(String libLocation, boolean importAll)
         throws ExecutionException {
        try {
            Map librarySpecs = new HashMap();
            libManager.loadLibs(librarySpecs, libLocation);
            libManager.configLibraries(frame.getInitConfig(), librarySpecs,
                antLibraries, libPathsMap);

            if (importAll) {
                Iterator i = librarySpecs.keySet().iterator();
                while (i.hasNext()) {
                    String libraryId = (String)i.next();
                    importLibrary(libraryId);
                }
            }
        } catch (MalformedURLException e) {
            throw new ExecutionException("Unable to load libraries from "
                 + libLocation, e);
        }
    }

    /**
     * Experimental - define a new task
     *
     * @param taskName the name by which this task will be referred
     * @param factory the library factory object to create the task
     *      instances
     * @param loader the class loader to use to create the particular tasks
     * @param className the name of the class implementing the task
     * @exception ExecutionException if the task cannot be defined
     */
    public void taskdef(AntLibFactory factory, ClassLoader loader,
                        String taskName, String className)
         throws ExecutionException {
        defineComponent(factory, loader, ComponentLibrary.TASKDEF,
            taskName, className);
    }

    /**
     * Experimental - define a new type
     *
     * @param typeName the name by which this type will be referred
     * @param factory the library factory object to create the type
     *      instances
     * @param loader the class loader to use to create the particular types
     * @param className the name of the class implementing the type
     * @exception ExecutionException if the type cannot be defined
     */
    public void typedef(AntLibFactory factory, ClassLoader loader,
                        String typeName, String className)
         throws ExecutionException {
        defineComponent(factory, loader, ComponentLibrary.TYPEDEF,
            typeName, className);
    }

    /**
     * Add a library path for the given library
     *
     * @param libraryId the unique id of the library for which an additional
     *      path is being defined
     * @param libPath the library path (usually a jar)
     * @exception ExecutionException if the path cannot be specified
     */
    public void addLibPath(String libraryId, URL libPath)
         throws ExecutionException {
        List libPaths = (List)libPathsMap.get(libraryId);
        if (libPaths == null) {
            libPaths = new ArrayList();
            libPathsMap.put(libraryId, libPaths);
        }
        libPaths.add(libPath);

        // If this library already exists give it the new path now
        AntLibrary library = (AntLibrary)antLibraries.get(libraryId);
        if (library != null) {
            libManager.addLibPath(library, libPath);
        }
    }

    /**
     * Import a complete library into the current execution frame
     *
     * @param libraryId The id of the library to be imported
     * @exception ExecutionException if the library cannot be imported
     */
    public void importLibrary(String libraryId) throws ExecutionException {
        AntLibrary library = (AntLibrary)antLibraries.get(libraryId);
        if (library == null) {
            throw new ExecutionException("Unable to import library " + libraryId
                 + " as it has not been loaded");
        }
        for (Iterator i = library.getDefinitionNames(); i.hasNext(); ) {
            String defName = (String)i.next();
            importLibraryDef(library, defName, null);
        }
        addLibraryConverters(library);
    }

    /**
     * Import a single component from a library, optionally aliasing it to a
     * new name
     *
     * @param libraryId the unique id of the library from which the
     *      component is being imported
     * @param defName the name of the component within its library
     * @param alias the name under which this component will be used in the
     *      build scripts. If this is null, the components default name is
     *      used.
     * @exception ExecutionException if the component cannot be imported
     */
    public void importComponent(String libraryId, String defName,
                                String alias) throws ExecutionException {
        AntLibrary library = (AntLibrary)antLibraries.get(libraryId);
        if (library == null) {
            throw new ExecutionException("Unable to import component from "
                 + "library \"" + libraryId + "\" as it has not been loaded");
        }
        importLibraryDef(library, defName, alias);
        addLibraryConverters(library);
    }

    /**
     * Imports a component defined in a nother frame.
     *
     * @param relativeName the qualified name of the component relative to
     *      this execution frame
     * @param alias the name under which this component will be used in the
     *      build scripts. If this is null, the components default name is
     *      used.
     * @exception ExecutionException if the component cannot be imported
     */
    public void importFrameComponent(String relativeName, String alias)
         throws ExecutionException {
        ImportInfo definition 
            = frame.getReferencedDefinition(relativeName);

        if (definition == null) {
            throw new ExecutionException("The reference \"relativeName\" does" 
                + " not refer to a defined component");
        }
            
        String label = alias;
        if (label == null) {
            label = frame.getNameInFrame(relativeName);
        }

        frame.log("Adding referenced component <" + definition.getLocalName() 
             + "> as <" + label + "> from library \"" 
             + definition.getComponentLibrary().getLibraryId() + "\", class: "
             + definition.getClassName(), MessageLevel.MSG_DEBUG);
        definitions.put(label, definition);
    }

    /**
     * Set the standard libraries (i.e. those which are independent of the
     * build files) to be used in this component manager
     *
     * @param standardLibs A collection of AntLibrary objects indexed by
     *      their libraryId
     * @exception ExecutionException if the components cannot be imported
     *      form the libraries fro which such importing is automatic.
     */
    protected void setStandardLibraries(Map standardLibs)
         throws ExecutionException {

        antLibraries = new HashMap(standardLibs);

        // go through the libraries and import all standard ant libraries
        for (Iterator i = antLibraries.keySet().iterator(); i.hasNext(); ) {
            String libraryId = (String)i.next();
            if (libraryId.startsWith(ANT_LIB_PREFIX)) {
                // standard library - import whole library
                importLibrary(libraryId);
            }
        }
    }

    /**
     * Get the collection ov converters currently configured
     *
     * @return A map of converter instances indexed on the class they can
     *      convert
     */
    protected Map getConverters() {
        return converters;
    }

    /**
     * Get the collection of Ant Libraries defined for this frame Gets the
     * factory object for the given library
     *
     * @param componentLibrary the compnent library for which a factory
     *      objetc is required
     * @return the library's factory object
     * @exception ExecutionException if the factory cannot be created
     */
    protected AntLibFactory getLibFactory(ComponentLibrary componentLibrary)
         throws ExecutionException {
        String libraryId = componentLibrary.getLibraryId();
        if (libFactories.containsKey(libraryId)) {
            return (AntLibFactory)libFactories.get(libraryId);
        }
        AntLibFactory libFactory
             = componentLibrary.getFactory(new ExecutionContext(frame));
        if (libFactory == null) {
            libFactory = new StandardLibFactory();
        }
        libFactories.put(libraryId, libFactory);
        return libFactory;
    }

    /**
     * Get an imported definition from the component manager
     *
     * @param name the name under which the component has been imported
     * @return the ImportInfo object detailing the import's library and
     *      other details
     */
    protected ImportInfo getDefinition(String name) {
        return (ImportInfo)definitions.get(name);
    }

    /**
     * Import a single component from the given library
     *
     * @param library the library which provides the component
     * @param defName the name of the component in the library
     * @param alias the name to be used for the component in build files. If
     *      this is null, the component's name within its library is used.
     */
    protected void importLibraryDef(ComponentLibrary library, String defName,
                                    String alias) {
        String label = alias;
        if (label == null) {
            label = defName;
        }

        AntLibDefinition libDef = library.getDefinition(defName);
        frame.log("Adding component <" + defName + "> as <" + label
             + "> from library \"" + library.getLibraryId() + "\", class: "
             + libDef.getClassName(), MessageLevel.MSG_DEBUG);
        definitions.put(label, new ImportInfo(library, libDef));
    }

    /**
     * Define a new component
     *
     * @param componentName the name this component will take
     * @param defType the type of component being defined
     * @param factory the library factory object to create the component
     *      instances
     * @param loader the class loader to use to create the particular
     *      components
     * @param className the name of the class implementing the component
     * @exception ExecutionException if the component cannot be defined
     */
    private void defineComponent(AntLibFactory factory, ClassLoader loader,
                                 int defType, String componentName,
                                 String className)
         throws ExecutionException {
        DynamicLibrary dynamicLibrary
             = new DynamicLibrary(factory, loader);
        dynamicLibrary.addComponent(defType, componentName, className);
        dynamicLibraries.put(dynamicLibrary.getLibraryId(), dynamicLibrary);
        importLibraryDef(dynamicLibrary, componentName, null);
    }


    /**
     * Add the converters from the given library to those managed by this
     * frame.
     *
     * @param library the library from which the converters are required
     * @exception ExecutionException if a converter defined in the library
     *      cannot be instantiated
     */
    private void addLibraryConverters(AntLibrary library)
         throws ExecutionException {
        if (!library.hasConverters()
             || loadedConverters.contains(library.getLibraryId())) {
            return;
        }

        String className = null;
        try {
            AntLibFactory libFactory = getLibFactory(library);
            ClassLoader converterLoader = library.getClassLoader();
            for (Iterator i = library.getConverterClassNames(); i.hasNext(); ) {
                className = (String)i.next();
                Class converterClass
                     = Class.forName(className, true, converterLoader);
                if (!Converter.class.isAssignableFrom(converterClass)) {
                    throw new ExecutionException("In Ant library \""
                         + library.getLibraryId() + "\" the converter class "
                         + converterClass.getName()
                         + " does not implement the Converter interface");
                }
                Converter converter
                     = libFactory.createConverter(converterClass);
                ExecutionContext context
                     = new ExecutionContext(frame);
                converter.init(context);
                Class[] converterTypes = converter.getTypes();
                for (int j = 0; j < converterTypes.length; ++j) {
                    converters.put(converterTypes[j], converter);
                }
            }
            loadedConverters.add(library.getLibraryId());
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId() + "\" converter class "
                 + className + " was not found", e);
        } catch (NoClassDefFoundError e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId()
                 + "\" could not load a dependent class ("
                 + e.getMessage() + ") for converter " + className);
        } catch (InstantiationException e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId()
                 + "\" unable to instantiate converter class "
                 + className, e);
        } catch (IllegalAccessException e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId()
                 + "\" unable to access converter class "
                 + className, e);
        }
    }

}

