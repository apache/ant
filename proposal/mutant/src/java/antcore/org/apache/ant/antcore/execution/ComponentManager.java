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
import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.ant.antcore.antlib.AntLibDefinition;
import org.apache.ant.antcore.antlib.AntLibManager;
import org.apache.ant.antcore.antlib.AntLibrary;
import org.apache.ant.antcore.modelparser.XMLProjectParser;
import org.apache.ant.antcore.xml.XMLParseException;
import org.apache.ant.common.antlib.AntLibFactory;
import org.apache.ant.common.antlib.Converter;
import org.apache.ant.common.antlib.StandardLibFactory;
import org.apache.ant.common.model.Project;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.util.ExecutionException;
import org.apache.ant.init.InitUtils;

/**
 * The instance of the ComponentServices made available by the core to the
 * ant libraries.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 27 January 2002
 */
public class ComponentManager implements ComponentService {
    /** The prefix for library ids that are automatically imported */
    public final static String ANT_LIB_PREFIX = "ant.";

    /**
     * Type converters for this executionFrame. Converters are used when
     * configuring Tasks to handle special type conversions.
     */
    private Map converters = new HashMap();

    /** The factory objects for each library, indexed by the library Id */
    private Map libFactories = new HashMap();

    /** The ExecutionFrame this service instance is working for */
    private ExecutionFrame frame;

    /** The library manager instance used to configure libraries. */
    private AntLibManager libManager;

    /**
     * These are AntLibraries which have been loaded into this component
     * manager
     */
    private Map antLibraries;
    /** The definitions which have been imported into this frame. */
    private Map definitions = new HashMap();

    /**
     * Constructor
     *
     * @param executionFrame the frame containing this context
     * @param allowRemoteLibs true if remote libraries can be loaded though
     *      this service.
     */
    protected ComponentManager(ExecutionFrame executionFrame,
                               boolean allowRemoteLibs) {
        this.frame = executionFrame;
        libManager = new AntLibManager(allowRemoteLibs);
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
            libManager.loadLib(librarySpecs, libLocation);
            libManager.configLibraries(frame.getInitConfig(), librarySpecs,
                antLibraries);

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
     * Run a sub-build.
     *
     * @param antFile the file containing the XML description of the model
     * @param targets A list of targets to be run
     * @param properties the initiali properties to be used in the build
     * @exception ExecutionException if the subbuild cannot be run
     */
    public void runBuild(File antFile, Map properties, List targets)
         throws ExecutionException {
        try {
            // Parse the build file into a project
            XMLProjectParser parser = new XMLProjectParser();
            Project project
                 = parser.parseBuildFile(InitUtils.getFileURL(antFile));
            runBuild(project, properties, targets);
        } catch (MalformedURLException e) {
            throw new ExecutionException(e);
        } catch (XMLParseException e) {
            throw new ExecutionException(e);
        }
    }

    /**
     * Run a sub-build.
     *
     * @param model the project model to be used for the build
     * @param targets A list of targets to be run
     * @param properties the initiali properties to be used in the build
     * @exception ExecutionException if the subbuild cannot be run
     */
    public void runBuild(Project model, Map properties, List targets)
         throws ExecutionException {
        ExecutionFrame newFrame = frame.createFrame(model);
        newFrame.setInitialProperties(properties);
        newFrame.runBuild(targets);
    }

    /**
     * Run a sub-build using the current frame's project model
     *
     * @param targets A list of targets to be run
     * @param properties the initiali properties to be used in the build
     * @exception ExecutionException if the subbuild cannot be run
     */
    public void callTarget(Map properties, List targets)
         throws ExecutionException {
        runBuild(frame.getProject(), properties, targets);
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
     * Get the collection of Ant Libraries defined for this frame
     *
     * @return a map of Ant Libraries indexed by thier library Id
     */
    protected Map getAntLibraries() {
        return antLibraries;
    }

    /**
     * Gets the factory object for the given library
     *
     * @param antLibrary the library for which the factory is required
     * @return the library's factory object
     * @exception ExecutionException if the factory cannot be initialised
     */
    protected AntLibFactory getLibFactory(AntLibrary antLibrary)
         throws ExecutionException {
        String libraryId = antLibrary.getLibraryId();
        if (libFactories.containsKey(libraryId)) {
            return (AntLibFactory)libFactories.get(libraryId);
        }
        AntLibFactory libFactory = antLibrary.getFactory();
        if (libFactory == null) {
            libFactory = new StandardLibFactory();
        }
        libFactories.put(libraryId, libFactory);
        libFactory.init(new ExecutionContext(frame));
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
     * Import a complete library into this frame
     *
     * @param libraryId The id of the library to be imported
     * @exception ExecutionException if the library cannot be imported
     */
    protected void importLibrary(String libraryId) throws ExecutionException {
        AntLibrary library = (AntLibrary)antLibraries.get(libraryId);
        if (library == null) {
            throw new ExecutionException("Unable to import library " + libraryId
                 + " as it has not been loaded");
        }
        Map libDefs = library.getDefinitions();
        for (Iterator i = libDefs.keySet().iterator(); i.hasNext(); ) {
            String defName = (String)i.next();
            AntLibDefinition libdef
                 = (AntLibDefinition)libDefs.get(defName);
            definitions.put(defName, new ImportInfo(library, libdef));
        }
        addLibraryConverters(library);
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
        if (!library.hasConverters()) {
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

