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
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ant.common.util.AntException;
import org.apache.ant.antcore.antlib.AntLibHandler;
import org.apache.ant.antcore.antlib.AntLibrary;
import org.apache.ant.antcore.antlib.AntLibrarySpec;
import org.apache.ant.antcore.config.AntConfig;
import org.apache.ant.antcore.config.AntConfigHandler;
import org.apache.ant.antcore.event.BuildEventSupport;
import org.apache.ant.antcore.event.BuildListener;
import org.apache.ant.antcore.model.Project;
import org.apache.ant.antcore.util.CircularDependencyChecker;
import org.apache.ant.antcore.util.CircularDependencyException;
import org.apache.ant.antcore.util.ConfigException;
import org.apache.ant.antcore.xml.ParseContext;
import org.apache.ant.antcore.xml.XMLParseException;
import org.apache.ant.init.InitUtils;
import org.apache.ant.init.InitConfig;
import org.apache.ant.init.LoaderUtils;

/**
 * The ExecutionManager is used to manage the execution of a build. The
 * Execution manager is responsible for loading the Ant task libraries,
 * creating ExecutionFrames for each project that is part of the build and
 * then executing the tasks within those Execution Frames.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 12 January 2002
 */
public class ExecutionManager {
    /** The AntLibraries built from Ant's Populated Task Libraries. */
    private Map antLibraries = new HashMap();

    /** BuildEvent support used to fire events and manage listeners */
    private BuildEventSupport eventSupport = new BuildEventSupport();

    /** The Execution Frame for the top level project being executed */
    private ExecutionFrame mainFrame;

    /**
     * Create an ExecutionManager. When an ExecutionManager is created, it
     * loads the ant libraries which are installed in the Ant lib/task
     * directory.
     *
     * @param initConfig Ant's configuration - classloaders etc
     * @exception ConfigException if there is a problem with one of Ant's
     *      tasks
     */
    public ExecutionManager(InitConfig initConfig)
         throws ConfigException {
        Map librarySpecs = new HashMap();

        AntConfig userConfig = getAntConfig(initConfig.getUserConfigArea());
        AntConfig systemConfig = getAntConfig(initConfig.getSystemConfigArea());

        AntConfig config = systemConfig;
        if (config == null) {
            config = userConfig;
        } else if (userConfig != null) {
            config.merge(userConfig);
        }

        try {
            // start by loading the task libraries
            URL taskBaseURL = new URL(initConfig.getLibraryURL(), "antlibs");
            addAntLibraries(librarySpecs, taskBaseURL);

            if (config != null) {
                // Now add in any found in the dirs specified in
                // the config files
                for (Iterator i = config.getTaskDirLocations(); i.hasNext(); ) {
                    // try file first
                    String taskDirString = (String)i.next();
                    File taskDir = new File(taskDirString);
                    if (!taskDir.exists()) {
                        URL taskDirURL = new URL(taskDirString);
                        addAntLibraries(librarySpecs, taskDirURL);
                    } else {
                        addAntLibraries(librarySpecs,
                            InitUtils.getFileURL(taskDir));
                    }
                }
            }

            configLibraries(initConfig, librarySpecs);

            if (config != null) {
                // now add any additional library Paths specified by the config
                for (Iterator i = config.getLibraryIds(); i.hasNext(); ) {
                    String libraryId = (String)i.next();
                    if (antLibraries.containsKey(libraryId)) {
                        AntLibrary antLib 
                            = (AntLibrary)antLibraries.get(libraryId);
                        List pathList = config.getLibraryPathList(libraryId);
                        for (Iterator j = pathList.iterator(); j.hasNext(); ) {
                            URL pathElementURL = (URL)j.next();
                            antLib.addLibraryURL(pathElementURL);
                        }
                    }
                }
            }

            mainFrame = new ExecutionFrame(antLibraries);
        } catch (MalformedURLException e) {
            throw new ConfigException("Unable to load Ant libraries", e);
        }
    }

    /**
     * Run a build, executing each of the targets on the given project
     *
     * @param project The project model on which to run the build
     * @param targets The list of target names
     */
    public void runBuild(Project project, List targets) {
        Throwable buildFailureCause = null;
        try {
            // start by validating the project we have been given.
            project.validate(null);
            mainFrame.setProject(project);

            eventSupport.fireBuildStarted(project);
            mainFrame.runBuild(targets);
        } catch (RuntimeException e) {
            buildFailureCause = e;
            throw e;
        } catch (AntException e) {
            buildFailureCause = e;
        } finally {
            eventSupport.fireBuildFinished(project, buildFailureCause);
        }
    }

    /**
     * Add a build listener to the build
     *
     * @param listener the listener to be added to the build
     */
    public void addBuildListener(BuildListener listener) {
        eventSupport.addBuildListener(listener);
        mainFrame.addBuildListener(listener);
    }

    /**
     * Remove a build listener from the execution
     *
     * @param listener the listener to be removed
     */
    public void removeBuildListener(BuildListener listener) {
        eventSupport.removeBuildListener(listener);
        mainFrame.removeBuildListener(listener);
    }

    /**
     * Get the AntConfig from the given config area if it is available
     *
     * @param configArea the config area from which the config may be read
     * @return the AntConfig instance representing the config info read in
     *      from the config area. May be null if the AntConfig is not
     *      present
     * @exception ConfigException if the URL for the config file cannotbe
     *      formed.
     */
    private AntConfig getAntConfig(URL configArea) throws ConfigException {
        try {
            URL configFileURL = new URL(configArea, "antconfig.xml");

            ParseContext context = new ParseContext();
            AntConfigHandler configHandler = new AntConfigHandler();

            context.parse(configFileURL, "antconfig", configHandler);

            return configHandler.getAntConfig();
        } catch (MalformedURLException e) {
            throw new ConfigException("Unable to form URL to read config from "
                 + configArea, e);
        } catch (XMLParseException e) {
            if (!(e.getCause() instanceof FileNotFoundException)) {
                throw new ConfigException("Unable to parse config file from "
                     + configArea, e);
            }
            // ignore missing config files
            return null;
        }
    }

    /**
     * Add all the Ant libraries that can be found at the given URL
     *
     * @param librarySpecs A map to which additional library specifications
     *      are added.
     * @param taskBaseURL the URL from which Ant libraries are to be loaded
     * @exception MalformedURLException if the URL for the individual
     *      library components cannot be formed
     * @exception ConfigException if the library specs cannot be parsed
     */
    private void addAntLibraries(Map librarySpecs, URL taskBaseURL)
         throws MalformedURLException, ConfigException {
        URL[] taskURLs = LoaderUtils.getLoaderURLs(taskBaseURL, null,
            new String[]{".tsk", ".jar", ".zip"});

        if (taskURLs == null) {
            return;
        }

        // parse each task library to get its library definition
        for (int i = 0; i < taskURLs.length; ++i) {
            URL libURL = new URL("jar:" + taskURLs[i]
                 + "!/META-INF/antlib.xml");
            try {
                AntLibrarySpec antLibrarySpec = parseLibraryDef(libURL);
                if (antLibrarySpec != null) {
                    String libraryId = antLibrarySpec.getLibraryId();
                    if (librarySpecs.containsKey(libraryId)) {
                        throw new ConfigException("Found more than one "
                             + "copy of library with id = " + libraryId +
                            " (" + taskURLs[i] + ")");
                    }
                    antLibrarySpec.setLibraryURL(taskURLs[i]);
                    librarySpecs.put(libraryId, antLibrarySpec);
                }
            } catch (XMLParseException e) {
                Throwable t = e.getCause();
                // ignore file not found exceptions - means the
                // jar does not provide META-INF/antlib.xml
                if (!(t instanceof FileNotFoundException)) {
                    throw new ConfigException("Unable to parse Ant library "
                         + taskURLs[i], e);
                }
            }
        }
    }

    /**
     * Configures the Ant Libraries. Configuration of an Ant Library
     * involves resolving any dependencies between libraries and then
     * creating the class loaders for the library
     *
     * @param initConfig the Ant initialized config
     * @param librarySpecs the loaded specifications of the Ant libraries
     * @exception ConfigException if a library cannot be configured from the
     *      given specification
     */
    private void configLibraries(InitConfig initConfig, Map librarySpecs)
         throws ConfigException {
        Set configured = new HashSet();
        CircularDependencyChecker configuring
             = new CircularDependencyChecker("configuring Ant libraries");
        for (Iterator i = librarySpecs.keySet().iterator(); i.hasNext(); ) {
            String libraryId = (String)i.next();
            if (!configured.contains(libraryId)) {
                configLibrary(initConfig, librarySpecs, libraryId,
                    configured, configuring);
            }
        }
    }

    /**
     * Configure a library from a specification and the Ant init config.
     *
     * @param initConfig Ant's init config passed in from the front end.
     * @param librarySpecs the library specs from which this library is to
     *      be configured.
     * @param libraryId the global identifier for the library
     * @param configured the set of libraries which have been configured
     *      already
     * @param configuring A circualr dependency chcker for library
     *      dependencies.
     * @exception ConfigException if the library cannot be configured.
     */
    private void configLibrary(InitConfig initConfig, Map librarySpecs,
                               String libraryId, Set configured,
                               CircularDependencyChecker configuring)
         throws ConfigException {

        try {

            configuring.visitNode(libraryId);

            AntLibrarySpec librarySpec
                 = (AntLibrarySpec)librarySpecs.get(libraryId);
            String extendsId = librarySpec.getExtendsLibraryId();
            if (extendsId != null) {
                if (!configured.contains(extendsId)) {
                    if (!librarySpecs.containsKey(extendsId)) {
                        throw new ConfigException("Could not find library, "
                             + extendsId + ", upon which library "
                             + libraryId + " depends");
                    }
                    configLibrary(initConfig, librarySpecs, extendsId,
                        configured, configuring);
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

            URL[] parserURLs = initConfig.getParserURLs();
            if (librarySpec.usesAntXML()) {
                for (int i = 0; i < parserURLs.length; ++i) {
                    urlsList.add(parserURLs[i]);
                }
            }

            for (Iterator i = urlsList.iterator(); i.hasNext(); ) {
                antLibrary.addLibraryURL((URL)i.next());
            }
            if (extendsId != null) {
                AntLibrary extendsLibrary
                     = (AntLibrary)antLibraries.get(extendsId);
                antLibrary.setExtendsLibrary(extendsLibrary);
            }
            antLibrary.setParentLoader(initConfig.getCommonLoader());
            antLibraries.put(libraryId, antLibrary);
            configuring.leaveNode(libraryId);
        } catch (CircularDependencyException e) {
            throw new ConfigException(e);
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

