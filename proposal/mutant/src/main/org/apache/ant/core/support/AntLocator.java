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

package org.apache.ant.core.support;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 * The Ant Locator is used to find various Ant components without
 * requiring the user to maintain environment properties. 
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */ 
public class AntLocator {
    private AntLocator() {}
    
    /**
     * Get the URL for the given class's load location.
     *
     * @param theClass the class whose loadURL is desired.
     * @return a URL which identifies the component from which this class was loaded.
     *
     * @throws LocationException if the class' URL cannot be constructed.
     */
    static public URL getClassLocationURL(Class theClass) 
            throws LocationException {
        String className = theClass.getName().replace('.', '/') + ".class";
        URL classRawURL = theClass.getClassLoader().getResource(className);
        
        try {
            String fileComponent = classRawURL.getFile();
            if (classRawURL.getProtocol().equals("file")) {
                // Class comes from a directory of class files rather than
                // from a jar. 
                int classFileIndex = fileComponent.lastIndexOf(className);
                if (classFileIndex != -1) {
                    fileComponent = fileComponent.substring(0, classFileIndex);
                }
                
                return new URL("file:" + fileComponent);
            }
            else if (classRawURL.getProtocol().equals("jar")) {
                // Class is coming from a jar. The file component of the URL
                // is actually the URL of the jar file
                int classSeparatorIndex = fileComponent.lastIndexOf("!");
                if (classSeparatorIndex != -1) {
                    fileComponent = fileComponent.substring(0, classSeparatorIndex);
                }
                
                return new URL(fileComponent);
            }
            else {
                // its running out of something besides a jar. We just return the Raw
                // URL as a best guess
                return classRawURL;
            }
        }
        catch (MalformedURLException e) {
            throw new LocationException(e);
        }
    }


    /**
     * Get the location of AntHome
     *
     * @return the URL containing AntHome.
     *
     * @throws LocationException if Ant's home cannot be determined.
     */
    static public URL getAntHome() throws LocationException {
        try {
            URL libraryURL = getLibraryURL();
            if (libraryURL != null) {
                return new URL(libraryURL, "..");
            }
            else {
                return null;
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a URL to the Ant core jar. Other jars can be located 
     * from this as relative URLs
     *
     * @return a URL containing the Ant core or null if the core cannot be determined.
     *
     * @throws LocationException if the URL of the core.jar cannot be determined.
     */
    static public URL getCoreURL() throws LocationException {
        return getClassLocationURL(AntLocator.class);
    }
    
    /**
     * Get a URL to the Ant Library directory.
     *
     * @throws LocationException if the location of the Ant library directory cannot 
     *         be determined
     */
    static public URL getLibraryURL() throws LocationException {
        URL coreURL = getCoreURL();
        
        try {
            if (coreURL.getProtocol().equals("file") &&
                coreURL.getFile().endsWith("/")) {
                // we are running from a set of classes. This should only happen
                // in an Ant build situation. We use some embedded knowledge to
                // locate the lib directory
                File coreClassDirectory = new File(coreURL.getFile());
                File libDirectory = coreClassDirectory.getParentFile().getParentFile();
                if (!libDirectory.exists()) {
                    throw new LocationException("Ant library directory " + libDirectory + 
                                                   " does not exist");
                }
                return (new File(libDirectory, "lib")).toURL();
            }
            else {
                String coreURLString = coreURL.toString();
                int index = coreURLString.lastIndexOf("/");
                if (index != -1) {
                    coreURLString = coreURLString.substring(0, index+1);
                }
                return new URL(coreURLString);
            }
        }
        catch (MalformedURLException e) {
            throw new LocationException(e);
        }
    }
    
    /**
     * Get a classloader with which to load the SAX parser
     *
     * @return the classloader to use to load Ant's XML parser
     *
     * @throws LocationException if the location of the parser jars 
     *                              could not be determined.
     */
    static public ClassLoader getParserClassLoader(Properties properties) 
            throws LocationException {
        // we look for the parser directory based on a system property first
        String parserURLString = properties.getProperty(Constants.PropertyNames.PARSER_URL);
        URL parserURL = null;
        if (parserURLString != null) {
            try {
                parserURL = new URL(parserURLString);
            }
            catch (MalformedURLException e) {
                throw new LocationException("XML Parser URL " + parserURLString + 
                                               " is malformed.", e);
            }
        }
        else {
            try {
                parserURL = new URL(getLibraryURL(), "parser/");
            }
            catch (Exception e) {
                // ignore - we will just use the default class loader.
            }
        }

        if (parserURL != null) {
            try {
                URL[] parserURLs = null;
                if (parserURL.getProtocol().equals("file")) {
                    // build up the URLs for each jar file in the 
                    // parser directory
                    parserURLs = getDirectoryJarURLs(new File(parserURL.getFile()));
                }
                else {
                    // we can't search the URL so we look for a known parser relative to
                    // that URL
                    String defaultParser = properties.getProperty(Constants.PropertyNames.DEFAULT_PARSER);
                    if (defaultParser == null) {
                        defaultParser = Constants.Defaults.DEFAULT_PARSER;
                    }
                    
                    parserURLs = new URL[1];
                    parserURLs[0] = new URL(parserURL, defaultParser);
                    
                }
                
                return new AntClassLoader(parserURLs, "parser");
            }
            catch (MalformedURLException e) {
                throw new LocationException(e);
            }
        }
        
        return AntLocator.class.getClassLoader();
    }

    /**
     * Get an array of URLs for each file matching the given set of extensions
     *
     * @param directory the local directory
     * @param extensions the set of extensions to be returned
     *
     * @return an array of URLs for the file found in the directory.
     */
    static public URL[] getDirectoryURLs(File directory, final Set extensions) {
        URL[] urls = new URL[0];
        
        if (!directory.exists()) {
            return urls;
        }
        
        File[] jars = directory.listFiles(new FilenameFilter() {
                                                public boolean accept(File dir, String name) {
                                                    int extensionIndex = name.lastIndexOf(".");
                                                    if (extensionIndex == -1) {
                                                        return false;
                                                    }
                                                    String extension = name.substring(extensionIndex);
                                                    return extensions.contains(extension);
                                                }
                                            });
        urls = new URL[jars.length];
        for (int i = 0; i < jars.length; ++i) {
            try {
                urls[i] = jars[i].toURL();
            }
            catch (MalformedURLException e) {
                // just ignore
            }
        }
        return urls;
    }
    


    
    /**
     * Get an array of URLs for each jar file in a local directory.
     *
     * @param directory the local directory
     *
     * @return an array of URLs for the jars found in the directory.
     */
    static private URL[] getDirectoryJarURLs(File directory) {
        HashSet extensions = new HashSet();
        extensions.add(".jar");
        return getDirectoryURLs(directory, extensions);        
    }
    
    /**
     * Get the Core Class Loader. The core requires a SAX parser which must come from the
     * given classloader
     *
     * @throws LocationException if the location of the core ant classes could 
     *                              not be determined
     */
    static public AntClassLoader getCoreClassLoader(Properties properties) 
            throws LocationException {
        URL[] coreURL = new URL[1];
        coreURL[0] = getCoreURL();
        AntClassLoader coreLoader 
            = new AntClassLoader(coreURL, getParserClassLoader(properties), "core");
        URL libraryURL = getLibraryURL();
        if (libraryURL != null && libraryURL.getProtocol().equals("file")) {
            // we can search this

            URL[] optionalURLs = getDirectoryJarURLs(new File(libraryURL.getFile(), "optional"));
            for (int i = 0; i < optionalURLs.length; ++i) {
                coreLoader.addURL(optionalURLs[i]);
            }
            
        }
        
        return coreLoader;          
    }        
}

