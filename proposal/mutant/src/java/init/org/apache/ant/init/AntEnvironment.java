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
package org.apache.ant.init;
import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * AntEnvironment describes the environment in which Ant is operating. It
 * provides the locations of a number of key Ant components.
 *
 * @author Conor MacNeill
 * @created 9 January 2002
 */
public class AntEnvironment {

    /**
     * How to navigate from a URL retrieved from a core class to
     * the Ant Home area
     */
    public static final String CORECLASS_TO_ANTHOME = "../../..";

    /** The configuration directory */
    public static final String SYSTEM_CONFDIR = "conf";

    /** The User's configuration directory */
    public static final String USER_CONFDIR = ".ant/conf";

    /** The library diurectory */
    public static final String LIB_DIR = "lib/";

    /** Common libraries directory */
    public static final String COMMON_DIR = "core/common/";

    /** Parser library directory */
    public static final String PARSER_DIR = "core/parser/";

    /** Core libraries directory */
    public static final String ANTCORE_DIR = "core/antcore/";

    /** System Ant libraries directory */
    public static final String SYSLIBS_DIR = "core/syslibs/";

    /** Standard Ant Libraries directory */
    public static final String ANTLIBS_DIR = "antlibs/";

    /** The Ant Home property */
    public static final String ANTHOME_PROPERTY = "ant.home";

    /** The default name of the jar containing the XML parser */
    public static final String DEFAULT_PARSER_JAR = "crimson.jar";

    /** The system classloader */
    private ClassLoader systemLoader;

    /**
     * The common class loader loads components which are common to tasks and
     * the core
     */
    private ClassLoader commonLoader;

    /**
     * The core loader is the loader which loads classes which are exclusively
     * used by the Ant core
     */
    private ClassLoader coreLoader;

    /**
     * The tools.jar URL is available for things which require the Sun tools
     * jar
     */
    private URL toolsJarURL;

    /**
     * The URLs to the Ant XML parser. These are available to allow tasks
     * which require XML support to use the standard parser rather than having
     * to supply their own
     */
    private URL[] parserURLs;

    /**
     * The location of the Ant library directory from which tasks may be
     * loaded
     */
    private URL libraryURL;

    /** The location of the system configuration file */
    private File systemConfigArea;

    /** The location of ANT_HOME */
    private URL antHome;

    /** The location of the user config file */
    private File userConfigArea;

    /**
     * Create an unconfigured AntEnvironment which will be configured manually
     * by the user
     */
    public AntEnvironment() {
    }

    /**
     * Create and automatically configure the Ant Environment
     *
     * @param coreClass - a core Ant class
     * @exception InitException if the configuration cannot be initialized
     */
    public AntEnvironment(Class coreClass) throws InitException {
        try {
            // is Ant Home set?
            String antHomeProperty = System.getProperty(ANTHOME_PROPERTY);
            if (antHomeProperty == null) {
                URL classURL = getAntLibURL(coreClass);
                antHome = new URL(classURL, CORECLASS_TO_ANTHOME);
            } else {
                try {
                    antHome = new URL(antHomeProperty);
                } catch (MalformedURLException e) {
                    // try as a file
                    File antHomeDir = new File(antHomeProperty);
                    if (!antHomeDir.exists()) {
                        throw new InitException("ant.home value \""
                             + antHomeProperty + "\" is not valid.");
                    }
                    antHome = InitUtils.getFileURL(antHomeDir);
                }
            }

            setLibraryURL(new URL(antHome, LIB_DIR));

            if (antHome.getProtocol().equals("file")) {
                File systemConfigArea
                     = new File(antHome.getFile(), SYSTEM_CONFDIR);
                setSystemConfigArea(systemConfigArea);
            }
            File userConfigArea
                 = new File(System.getProperty("user.home"), USER_CONFDIR);
            setUserConfigArea(userConfigArea);

            // set up the class loaders that will be used when running Ant
            ClassLoader systemLoader = getClass().getClassLoader();
            setSystemLoader(systemLoader);
            URL toolsJarURL = ClassLocator.getToolsJarURL();
            setToolsJarURL(toolsJarURL);

            URL commonJarLib = new URL(libraryURL, COMMON_DIR);
            ClassLoader commonLoader
                 = new URLClassLoader(LoaderUtils.getLocationURLs(commonJarLib,
                "common.jar"), systemLoader);
            setCommonLoader(commonLoader);

            // core needs XML parser for parsing various XML components.
            URL parserBase = new URL(libraryURL, PARSER_DIR);
            URL[] parserURLs
                 = LoaderUtils.getLocationURLs(parserBase, DEFAULT_PARSER_JAR);
            setParserURLs(parserURLs);

            URL antcoreBase = new URL(libraryURL, ANTCORE_DIR);
            URL[] coreURLs
                 = LoaderUtils.getLocationURLs(antcoreBase, "antcore.jar");
            URL[] combinedURLs = new URL[parserURLs.length + coreURLs.length];
            System.arraycopy(coreURLs, 0, combinedURLs, 0, coreURLs.length);
            System.arraycopy(parserURLs, 0, combinedURLs, coreURLs.length,
                parserURLs.length);
            ClassLoader coreLoader = new URLClassLoader(combinedURLs,
                commonLoader);
            setCoreLoader(coreLoader);
        } catch (MalformedURLException e) {
            throw new InitException(e);
        }
    }

    /**
     * Sets the location of the user configuration files
     *
     * @param userConfigArea the new user config area
     */
    public void setUserConfigArea(File userConfigArea) {
        this.userConfigArea = userConfigArea;
    }

    /**
     * Set the location of ANT_HOME
     *
     * @param antHome the new value of ANT_HOME
     */
    public void setAntHome(URL antHome) {
        this.antHome = antHome;
    }

    /**
     * Sets the location of the system configuration files
     *
     * @param systemConfigArea the new system config area
     */
    public void setSystemConfigArea(File systemConfigArea) {
        this.systemConfigArea = systemConfigArea;
    }

    /**
     * Sets the systemLoader of the AntEnvironment
     *
     * @param systemLoader the new systemLoader value
     */
    public void setSystemLoader(ClassLoader systemLoader) {
        this.systemLoader = systemLoader;
    }

    /**
     * Sets the commonLoader of the AntEnvironment
     *
     * @param commonLoader the new commonLoader value
     */
    public void setCommonLoader(ClassLoader commonLoader) {
        this.commonLoader = commonLoader;
    }

    /**
     * Sets the coreLoader of the AntEnvironment
     *
     * @param coreLoader the new coreLoader value
     */
    public void setCoreLoader(ClassLoader coreLoader) {
        this.coreLoader = coreLoader;
    }

    /**
     * Sets the toolsJarURL of the AntEnvironment
     *
     * @param toolsJarURL the new toolsJarURL value
     */
    public void setToolsJarURL(URL toolsJarURL) {
        this.toolsJarURL = toolsJarURL;
    }

    /**
     * Sets the parserURLs of the AntEnvironment
     *
     * @param parserURLs the new parserURLs value
     */
    public void setParserURLs(URL[] parserURLs) {
        this.parserURLs = parserURLs;
    }

    /**
     * Sets the libraryURL of the AntEnvironment
     *
     * @param libraryURL the new libraryURL value
     */
    public void setLibraryURL(URL libraryURL) {
        this.libraryURL = libraryURL;
    }

    /**
     * Get the location of the user's config files
     *
     * @return the location of the user's Ant config files
     */
    public File getUserConfigArea() {
        return userConfigArea;
    }

    /**
     * Get the location of Ant's home area
     *
     * @return the location of ANT_HOME
     */
    public URL getAntHome() {
        return antHome;
    }

    /**
     * Get the location of the system config files
     *
     * @return the location of the system Ant config files
     */
    public File getSystemConfigArea() {
        return systemConfigArea;
    }

    /**
     * Gets the systemLoader of the AntEnvironment
     *
     * @return the systemLoader value
     */
    public ClassLoader getSystemLoader() {
        return systemLoader;
    }

    /**
     * Gets the commonLoader of the AntEnvironment
     *
     * @return the commonLoader value
     */
    public ClassLoader getCommonLoader() {
        return commonLoader;
    }

    /**
     * Gets the coreLoader of the AntEnvironment
     *
     * @return the coreLoader value
     */
    public ClassLoader getCoreLoader() {
        return coreLoader;
    }

    /**
     * Gets the toolsJarURL of the AntEnvironment
     *
     * @return the toolsJarURL value
     */
    public URL getToolsJarURL() {
        return toolsJarURL;
    }

    /**
     * Gets the parserURLs of the AntEnvironment
     *
     * @return the parserURLs value
     */
    public URL[] getParserURLs() {
        return parserURLs;
    }

    /**
     * Gets the libraryURL of the AntEnvironment
     *
     * @return the libraryURL value
     */
    public URL getLibraryURL() {
        return libraryURL;
    }

    /**
     * Get the location of the antlibs directory
     *
     * @return a URL giving the location of the antlibs directory
     * @exception MalformedURLException if the URL cannot be formed.
     */
    public URL getSyslibsURL() throws MalformedURLException {
        return new URL(libraryURL, SYSLIBS_DIR);
    }

    /**
     * Get the location of the syslibs directory
     *
     * @return a URL giving the location of the syslibs directory
     * @exception MalformedURLException if the URL cannot be formed.
     */
    public URL getAntlibsURL() throws MalformedURLException {
        return new URL(libraryURL, ANTLIBS_DIR);
    }

    /**
     * Get a URL to the Ant Library directory.
     *
     * @param libraryClass - a class loaded from the Ant library area.
     * @return the URL for the Ant library directory
     * @throws MalformedURLException if there is a problem constructing the
     *      library URL
     */
    private URL getAntLibURL(Class libraryClass) throws MalformedURLException {
        URL initClassURL = ClassLocator.getClassLocationURL(libraryClass);

        String initURLString = initClassURL.toString();
        int index = initURLString.lastIndexOf("/");
        if (index != -1) {
            initURLString = initURLString.substring(0, index + 1);
        }

        return new URL(initURLString);
    }
}

