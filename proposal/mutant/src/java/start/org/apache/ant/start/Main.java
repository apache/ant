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
package org.apache.ant.start;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.ant.init.ClassLocator;
import org.apache.ant.init.InitUtils;
import org.apache.ant.init.InitConfig;
import org.apache.ant.init.InitException;
import org.apache.ant.init.LoaderUtils;

/**
 * This is the main startup class for the command line interface of Ant. It
 * establishes the classloaders used by the other components of Ant.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 9 January 2002
 */
public class Main {
    /** The actual class that implements the command line front end. */
    public final static String COMMANDLINE_CLASS
         = "org.apache.ant.cli.Commandline";


    /**
     * Get a URL to the Ant Library directory.
     *
     * @return the URL for the Ant library directory
     * @throws InitException if there is a problem constructing the library
     *      URL
     */
    public static URL getLibraryURL()
         throws InitException {
        try {
            URL cliURL = ClassLocator.getClassLocationURL(Main.class);

            if (cliURL.getProtocol().equals("file")
                 && cliURL.getFile().endsWith("/")) {
                // we are running from a set of classes. This should only
                // happen in an Ant build situation. We use some embedded
                // knowledge to locate the lib directory
                File classesDirectory = new File(cliURL.getFile());
                File libDirectory = new File(classesDirectory.getParent(),
                    "lib");
                if (!libDirectory.exists()) {
                    throw new RuntimeException("Ant library directory "
                         + libDirectory + " does not exist");
                }
                return InitUtils.getFileURL(libDirectory);
            } else {
                String cliURLString = cliURL.toString();
                int index = cliURLString.lastIndexOf("/");
                if (index != -1) {
                    cliURLString = cliURLString.substring(0, index + 1);
                }
                return new URL(cliURLString);
            }
        } catch (MalformedURLException e) {
            throw new InitException(e);
        }
    }


    /**
     * Entry point for starting command line Ant
     *
     * @param args commandline arguments
     */
    public static void main(String[] args) {
        Main main = new Main();
        main.start(args);
    }


    /**
     * Get the URLs necessary to load the Sun compiler tools. In some JVMs
     * this is available in the VM's system loader, in others we have to
     * find it ourselves
     *
     * @return the URL to the tools jar if available, null otherwise
     * @throws InitException if the URL to the tools jar cannot be formed.
     */
    private URL getToolsJarURL()
         throws InitException {
        try {
            // just check whether this throws an exception
            Class.forName("sun.tools.javac.Main");
            // tools jar is on system classpath - no need for URL
            return null;
        } catch (ClassNotFoundException cnfe) {
            try {
                // couldn't find compiler - try to find tools.jar
                // based on java.home setting
                String javaHome = System.getProperty("java.home");
                if (javaHome.endsWith("jre")) {
                    javaHome = javaHome.substring(0, javaHome.length() - 4);
                }
                File toolsjar = new File(javaHome + "/lib/tools.jar");
                if (!toolsjar.exists()) {
                    System.out.println("Unable to locate tools.jar. "
                         + "Expected to find it in " + toolsjar.getPath());
                    return null;
                }
                URL toolsJarURL = InitUtils.getFileURL(toolsjar);
                return toolsJarURL;
            } catch (MalformedURLException e) {
                throw new InitException(e);
            }
        }
    }


    /**
     * Get the location of AntHome
     *
     * @return the URL containing AntHome.
     * @throws InitException if Ant's home cannot be determined or properly
     *      contructed.
     */
    private URL getAntHome()
         throws InitException {
        try {
            URL libraryURL = getLibraryURL();
            if (libraryURL != null) {
                return new URL(libraryURL, "..");
            }
        } catch (MalformedURLException e) {
            throw new InitException(e);
        }
        throw new InitException("Unable to determine Ant Home");
    }


    /**
     * Internal start method used to initialise front end
     *
     * @param args commandline arguments
     */
    private void start(String[] args) {
        try {
            InitConfig config = new InitConfig();

            URL libraryURL = getLibraryURL();
            System.out.println("Library URL is " + libraryURL);
            config.setLibraryURL(libraryURL);

            URL antHome = getAntHome();
            config.setAntHome(antHome);
            if (antHome.getProtocol().equals("file")) {
                File systemConfigArea = new File(antHome.getFile(), "conf");
                config.setSystemConfigArea(systemConfigArea);
            }
            File userConfigArea
                 = new File(System.getProperty("user.home"), ".ant/conf");
            config.setUserConfigArea(userConfigArea);

            // set up the class loaders that will be used when running Ant
            ClassLoader systemLoader = getClass().getClassLoader();
            config.setSystemLoader(systemLoader);
            URL toolsJarURL = getToolsJarURL();
            config.setToolsJarURL(toolsJarURL);

            URL commonJarLib = new URL(libraryURL, "common/");
            ClassLoader commonLoader
                 = new URLClassLoader(LoaderUtils.getLocationURLs(commonJarLib,
                "common.jar"), systemLoader);
            config.setCommonLoader(commonLoader);

            // core needs XML parser for parsing various XML components.
            URL[] parserURLs
                 = LoaderUtils.getLocationURLs(new URL(libraryURL, "parser/"),
                "crimson.jar");
            config.setParserURLs(parserURLs);

            URL[] coreURLs
                 = LoaderUtils.getLocationURLs(new URL(libraryURL, "antcore/"),
                "antcore.jar");
            URL[] combinedURLs = new URL[parserURLs.length + coreURLs.length];
            System.arraycopy(coreURLs, 0, combinedURLs, 0, coreURLs.length);
            System.arraycopy(parserURLs, 0, combinedURLs, coreURLs.length,
                parserURLs.length);
            ClassLoader coreLoader = new URLClassLoader(combinedURLs,
                commonLoader);
            config.setCoreLoader(coreLoader);

            URL cliJarLib = new URL(libraryURL, "cli/");
            ClassLoader frontEndLoader
                 = new URLClassLoader(LoaderUtils.getLocationURLs(cliJarLib,
                "cli.jar"), coreLoader);

            // System.out.println("Front End Loader config");
            // LoaderUtils.dumpLoader(System.out, frontEndLoader);

            // Now start the front end by reflection.
            Class commandLineClass = Class.forName(COMMANDLINE_CLASS, true,
                frontEndLoader);

            final Class[] param = {Class.forName("[Ljava.lang.String;"),
                InitConfig.class};
            final Method startMethod
                 = commandLineClass.getMethod("start", param);
            final Object[] argument = {args, config};
            startMethod.invoke(null, argument);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

