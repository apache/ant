/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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
package org.apache.tools.ant.launch;

import java.net.MalformedURLException;

import java.net.URL;
import java.io.File;
import java.io.FilenameFilter;

/**
 * The Locator is a utility class which is used to find certain items
 * in the environment
 *
 * @author Conor MacNeill
 * @since Ant 1.6
 */
public class Locator {
    /**
     * Get the URL for the given class's load location.
     *
     * @param theClass the class whose load URL is desired.
     * @return a URL which identifies the component from which this class
     *      was loaded.
     * @throws MalformedURLException if the class' URL cannot be
     *      constructed.
     */
    public static URL getClassLocationURL(Class theClass)
         throws MalformedURLException {
        String className = theClass.getName().replace('.', '/') + ".class";
        URL classRawURL = theClass.getClassLoader().getResource(className);

        String fileComponent = classRawURL.getFile();
        if (classRawURL.getProtocol().equals("file")) {
            // Class comes from a directory of class files rather than
            // from a jar.
            int classFileIndex = fileComponent.lastIndexOf(className);
            if (classFileIndex != -1) {
                fileComponent = fileComponent.substring(0, classFileIndex);
            }

            return new URL("file:" + fileComponent);
        } else if (classRawURL.getProtocol().equals("jar")) {
            // Class is coming from a jar. The file component of the URL
            // is actually the URL of the jar file
            int classSeparatorIndex = fileComponent.lastIndexOf("!");
            if (classSeparatorIndex != -1) {
                fileComponent = fileComponent.substring(0, classSeparatorIndex);
            }

            return new URL(fileComponent);
        } else {
            // its running out of something besides a jar.
            // We just return the Raw URL as a best guess
            return classRawURL;
        }
    }

    /**
     * Get the URL necessary to load the Sun compiler tools. If the classes
     * are available to this class, then no additional URL is required and
     * null is returned. This may be because the classes are explcitly in the
     * class path or provided by the JVM directly
     *
     * @return the tools jar as a File if required, null otherwise
     */
    public static File getToolsJar() {
        // firstly check if the tols jar is alreayd n the classpath
        boolean toolsJarAvailable = false;

        try {
            // just check whether this throws an exception
            Class.forName("com.sun.tools.javac.Main");
            toolsJarAvailable = true;
        } catch (Exception e) {
            try {
                Class.forName("sun.tools.javac.Main");
                toolsJarAvailable = true;
            } catch (Exception e2) {
                // ignore
            }
        }

        if (toolsJarAvailable) {
            return null;
        }

        // couldn't find compiler - try to find tools.jar
        // based on java.home setting
        String javaHome = System.getProperty("java.home");
        if (javaHome.endsWith("jre")) {
            javaHome = javaHome.substring(0, javaHome.length() - 4);
        }
        File toolsJar = new File(javaHome + "/lib/tools.jar");
        if (!toolsJar.exists()) {
            System.out.println("Unable to locate tools.jar. "
                 + "Expected to find it in " + toolsJar.getPath());
            return null;
        }
        return toolsJar;
    }

    /**
     * Get an array or URLs representing all of the jar files in the
     * given location. If the location is a file, it is returned as the only
     * element of the array. If the location is a directory, it is scanned for
     * jar files
     *
     * @param location the location to scan for Jars
     *
     * @return an array of URLs for all jars in the given location.
     *
     * @exception MalformedURLException if the URLs for the jars cannot be
     *            formed
     */
    public static URL[] getLocationURLs(File location)
         throws MalformedURLException {
        return getLocationURLs(location, new String[]{".jar"});
    }

    /**
     * Get an array or URLs representing all of the files of a given set of
     * extensions in the given location. If the location is a file, it is
     * returned as the only element of the array. If the location is a
     * directory, it is scanned for matching files
     *
     * @param location the location to scan for files
     * @param extensions an array of extension that are to match in the
     *        directory search
     *
     * @return an array of URLs of matching files
     * @exception MalformedURLException if the URLs for the files cannot be
     *            formed
     */
    public static URL[] getLocationURLs(File location,
                                        final String[] extensions)
         throws MalformedURLException {
        URL[] urls = new URL[0];

        if (!location.exists()) {
            return urls;
        }

        if (!location.isDirectory()) {
            urls = new URL[1];
            String path = location.getPath();
            for (int i = 0; i < extensions.length; ++i) {
                if (path.endsWith(extensions[i])) {
                    urls[0] = location.toURL();
                    break;
                }
            }
            return urls;
        }

        File[] matches = location.listFiles(
            new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    for (int i = 0; i < extensions.length; ++i) {
                        if (name.endsWith(extensions[i])) {
                            return true;
                        }
                    }
                    return false;
                }
            });

        urls = new URL[matches.length];
        for (int i = 0; i < matches.length; ++i) {
            urls[i] = matches[i].toURL();
        }
        return urls;
    }
}

