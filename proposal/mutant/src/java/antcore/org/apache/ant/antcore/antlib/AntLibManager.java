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
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.ant.antcore.xml.ParseContext;
import org.apache.ant.antcore.xml.XMLParseException;
import org.apache.ant.common.util.CircularDependencyChecker;
import org.apache.ant.common.util.CircularDependencyException;
import org.apache.ant.common.util.ExecutionException;
import org.apache.ant.init.InitConfig;
import org.apache.ant.init.InitUtils;
import org.apache.ant.init.LoaderUtils;

/**
 * This class manages the configuration of Ant Libraries
 *
 * @author Conor MacNeill
 * @created 29 January 2002
 */
public class AntLibManager {

    /** The list of extensions which are examined for ant libraries */
    public static final String[] ANTLIB_EXTENSIONS
         = new String[]{".tsk", ".jar", ".zip"};

    /** Flag which indicates whether non-file URLS are used */
    private boolean remoteAllowed;

    /**
     * Constructor for the AntLibManager object
     *
     * @param remoteAllowed true if remote libraries can be used and
     *      configured
     */
    public AntLibManager(boolean remoteAllowed) {
        this.remoteAllowed = remoteAllowed;
    }

    /**
     * Add all the Ant libraries that can be found at the given URL
     *
     * @param librarySpecs A map to which additional library specifications
     *      are added.
     * @param libURL the URL from which Ant libraries are to be loaded
     * @exception MalformedURLException if the URL for the individual
     *      library components cannot be formed
     * @exception ExecutionException if the library specs cannot be parsed
     */
    public void addAntLibraries(Map librarySpecs, URL libURL)
         throws MalformedURLException, ExecutionException {
        URL[] libURLs = LoaderUtils.getLocationURLs(libURL, libURL.toString(),
            ANTLIB_EXTENSIONS);

        if (libURLs == null) {
            return;
        }

        // parse each task library to get its library definition
        for (int i = 0; i < libURLs.length; ++i) {
            URL antLibraryURL = new URL("jar:" + libURLs[i]
                 + "!/META-INF/antlib.xml");
            try {
                AntLibrarySpec antLibrarySpec = parseLibraryDef(antLibraryURL);
                if (antLibrarySpec != null) {
                    String libraryId = antLibrarySpec.getLibraryId();
                    if (librarySpecs.containsKey(libraryId)) {
                        AntLibrarySpec currentSpec 
                            = (AntLibrarySpec) librarySpecs.get(libraryId);
                        throw new ExecutionException("Found more than one "
                             + "copy of library with id = " + libraryId 
                             + " (" + libURLs[i] + ") + existing library at ("
                             + currentSpec.getLibraryURL() + ")");
                    }
                    antLibrarySpec.setLibraryURL(libURLs[i]);
                    librarySpecs.put(libraryId, antLibrarySpec);
                }
            } catch (XMLParseException e) {
                Throwable t = e.getCause();
                // ignore file not found exceptions - means the
                // jar does not provide META-INF/antlib.xml
                if (!(t instanceof FileNotFoundException)) {
                    throw new ExecutionException("Unable to parse Ant library "
                         + libURLs[i], e);
                }
            }
        }
    }

    /**
     * Configures the Ant Libraries. Configuration of an Ant Library
     * involves resolving any dependencies between libraries and then
     * creating the class loaders for the library
     *
     * @param librarySpecs the loaded specifications of the Ant libraries
     * @param initConfig the Ant initialization configuration
     * @param libraries the collection of libraries already configured
     * @param libPathsMap a map of lists of library paths for each library
     * @exception ExecutionException if a library cannot be configured from
     *      the given specification
     */
    public void configLibraries(InitConfig initConfig, Map librarySpecs,
                                Map libraries, Map libPathsMap)
         throws ExecutionException {

        // check if any already defined
        for (Iterator i = librarySpecs.keySet().iterator(); i.hasNext();) {
            String libraryId = (String) i.next();
            if (libraries.containsKey(libraryId)) {
                AntLibrary currentVersion
                     = (AntLibrary) libraries.get(libraryId);
                // same location?
                AntLibrarySpec spec 
                    = (AntLibrarySpec) librarySpecs.get(libraryId); 
                URL specURL = spec.getLibraryURL();
                if (!specURL.equals(currentVersion.getDefinitionURL())) {
                    throw new ExecutionException("Ant Library \"" + libraryId
                         + "\" is already loaded from "
                         + currentVersion.getDefinitionURL() 
                         + " new version found at " 
                         + specURL);
                }
            }
        }

        CircularDependencyChecker configuring
             = new CircularDependencyChecker("configuring Ant libraries");
        for (Iterator i = librarySpecs.keySet().iterator(); i.hasNext();) {
            String libraryId = (String) i.next();
            if (!libraries.containsKey(libraryId)) {
                configLibrary(initConfig, librarySpecs, libraryId,
                    configuring, libraries, libPathsMap);
            }
        }
    }

    /**
     * Load either a set of libraries or a single library.
     *
     * @param libLocationURL URL where libraries can be found
     * @param librarySpecs A collection of library specs which will be
     *      populated with the libraries found
     * @exception ExecutionException if the libraries cannot be loaded
     * @exception MalformedURLException if the library's location cannot be
     *      formed
     */
    public void loadLibs(Map librarySpecs, URL libLocationURL)
         throws ExecutionException, MalformedURLException {
        if (!libLocationURL.getProtocol().equals("file")
             && !remoteAllowed) {
            throw new ExecutionException("The config library "
                 + "location \"" + libLocationURL
                 + "\" cannot be used because config does "
                 + "not allow remote libraries");
        }
        addAntLibraries(librarySpecs, libLocationURL);
    }
    
    /**
     * Load either a set of libraries or a single library.
     *
     * @param libLocationString URL or file where libraries can be found
     * @param librarySpecs A collection of library specs which will be
     *      populated with the libraries found
     * @exception ExecutionException if the libraries cannot be loaded
     * @exception MalformedURLException if the library's location cannot be
     *      formed
     */
    public void loadLibs(Map librarySpecs, String libLocationString)
         throws ExecutionException, MalformedURLException {

        File libLocation = new File(libLocationString);
        if (!libLocation.exists()) {
            try {
                loadLibs(librarySpecs, new URL(libLocationString));
            } catch (MalformedURLException e) {
                // XXX
            }
        } else {
            addAntLibraries(librarySpecs, InitUtils.getFileURL(libLocation));
        }
    }

    /**
     * Add a library path to the given library
     *
     * @param antLibrary the library to which the path is to be added
     * @param path the path to be added
     * @exception ExecutionException if remote paths are not allowed by
     *      configuration
     */
    public void addLibPath(AntLibrary antLibrary, URL path)
         throws ExecutionException {
        if (!path.getProtocol().equals("file")
             && !remoteAllowed) {
            throw new ExecutionException("Remote libpaths are not"
                 + " allowed: " + path);
        }
        antLibrary.addLibraryURL(path);
    }

    /**
     * Configure a library from a specification and the Ant init config.
     *
     * @param initConfig Ant's init config passed in from the front end.
     * @param librarySpecs the library specs from which this library is to
     *      be configured.
     * @param libraryId the global identifier for the library
     * @param configuring A circualr dependency chcker for library
     *      dependencies.
     * @param libraries the collection of libraries which have already been
     *      configured
     * @param libPathsMap a map of lists of library patsh fro each library
     * @exception ExecutionException if the library cannot be configured.
     */
    private void configLibrary(InitConfig initConfig, Map librarySpecs,
                               String libraryId,
                               CircularDependencyChecker configuring,
                               Map libraries, Map libPathsMap)
         throws ExecutionException {

        try {
            configuring.visitNode(libraryId);

            AntLibrarySpec librarySpec
                 = (AntLibrarySpec) librarySpecs.get(libraryId);
            String extendsId = librarySpec.getExtendsLibraryId();
            if (extendsId != null) {
                if (!libraries.containsKey(extendsId)) {
                    if (!librarySpecs.containsKey(extendsId)) {
                        throw new ExecutionException("Could not find library, "
                             + extendsId + ", upon which library "
                             + libraryId + " depends");
                    }
                    configLibrary(initConfig, librarySpecs, extendsId,
                        configuring, libraries, libPathsMap);
                }
            }

            // now create the library for the specification
            AntLibrary antLibrary = new AntLibrary(librarySpec);

            // determine the URLs required for this task. These are the
            // task URL itself, the XML parser URLs if required, the
            // tools jar URL if required
            List urlsList = new ArrayList();

            if (librarySpec.getLibraryURL() != null) {
                urlsList.add(librarySpec.getLibraryURL());
            }
            if (librarySpec.isToolsJarRequired()
                 && initConfig.getToolsJarURL() != null) {
                urlsList.add(initConfig.getToolsJarURL());
            }

            if (librarySpec.usesAntXML()) {
                URL[] parserURLs = initConfig.getParserURLs();
                for (int i = 0; i < parserURLs.length; ++i) {
                    urlsList.add(parserURLs[i]);
                }
            }

            for (Iterator i = urlsList.iterator(); i.hasNext();) {
                antLibrary.addLibraryURL((URL) i.next());
            }
            if (extendsId != null) {
                AntLibrary extendsLibrary
                     = (AntLibrary) libraries.get(extendsId);
                antLibrary.setExtendsLibrary(extendsLibrary);
            }
            antLibrary.setParentLoader(initConfig.getCommonLoader());
            libraries.put(libraryId, antLibrary);

            if (libPathsMap != null) {
                List libPaths = (List) libPathsMap.get(libraryId);
                if (libPaths != null) {
                    for (Iterator j = libPaths.iterator(); j.hasNext();) {
                        URL pathURL = (URL) j.next();
                        addLibPath(antLibrary, pathURL);
                    }
                }
            }

            configuring.leaveNode(libraryId);
        } catch (CircularDependencyException e) {
            throw new ExecutionException(e);
        }
    }


    /**
     * Read an Ant library definition from a URL
     *
     * @param antlibURL the URL of the library definition
     * @return the AntLibrary specification read from the library XML
     *      definition
     * @exception XMLParseException if the library cannot be parsed
     */
    private AntLibrarySpec parseLibraryDef(URL antlibURL)
         throws XMLParseException {
        ParseContext context = new ParseContext();
        AntLibHandler libHandler = new AntLibHandler();

        context.parse(antlibURL, "antlib", libHandler);

        return libHandler.getAntLibrarySpec();
    }

}

