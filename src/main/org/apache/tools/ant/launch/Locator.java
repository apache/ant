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
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * The Locator is a utility class which is used to find certain items
 * in the environment
 *
 * @author Conor MacNeill
 * @since Ant 1.6
 */
public final class Locator {
    /**
     * Not instantiable
     */
    private Locator() {
    }

    /**
     * Find the directory or jar file the class has been loaded from.
     *
     * @param c the class whose location is required.
     * @return the file or jar with the class or null if we cannot
     *         determine the location.
     *
     * @since Ant 1.6
     */
    public static File getClassSource(Class c) {
        String classResource = c.getName().replace('.', '/') + ".class";
        return getResourceSource(c.getClassLoader(), classResource);
    }

    /**
     * Find the directory or jar a give resource has been loaded from.
     *
     * @param c the classloader to be consulted for the source
     * @param resource the resource whose location is required.
     *
     * @return the file with the resource source or null if
     *         we cannot determine the location.
     *
     * @since Ant 1.6
     */
    public static File getResourceSource(ClassLoader c, String resource) {
        if (c == null) {
            c = Locator.class.getClassLoader();
        }

        URL url = c.getResource(resource);
        if (url != null) {
            String u = url.toString();
            if (u.startsWith("jar:file:")) {
                int pling = u.indexOf("!");
                String jarName = u.substring(4, pling);
                return new File(fromURI(jarName));
            } else if (u.startsWith("file:")) {
                int tail = u.indexOf(resource);
                String dirName = u.substring(0, tail);
                return new File(fromURI(dirName));
            }
        }
        return null;
    }

    /**
     * Constructs a file path from a <code>file:</code> URI.
     *
     * <p>Will be an absolute path if the given URI is absolute.</p>
     *
     * <p>Swallows '%' that are not followed by two characters,
     * doesn't deal with non-ASCII characters.</p>
     *
     * @param uri the URI designating a file in the local filesystem.
     * @return the local file system path for the file.
     * @since Ant 1.6
     */
    public static String fromURI(String uri) {
        if (!uri.startsWith("file:")) {
            throw new IllegalArgumentException("Can only handle file: URIs");
        }
        if (uri.startsWith("file://")) {
            uri = uri.substring(7);
        } else {
            uri = uri.substring(5);
        }

        uri = uri.replace('/', File.separatorChar);
        if (File.pathSeparatorChar == ';' && uri.startsWith("\\") && uri.length() > 2
            && Character.isLetter(uri.charAt(1)) && uri.lastIndexOf(':') > -1) {
            uri = uri.substring(1);
        }

        StringBuffer sb = new StringBuffer();
        CharacterIterator iter = new StringCharacterIterator(uri);
        for (char c = iter.first(); c != CharacterIterator.DONE;
             c = iter.next()) {
            if (c == '%') {
                char c1 = iter.next();
                if (c1 != CharacterIterator.DONE) {
                    int i1 = Character.digit(c1, 16);
                    char c2 = iter.next();
                    if (c2 != CharacterIterator.DONE) {
                        int i2 = Character.digit(c2, 16);
                        sb.append((char) ((i1 << 4) + i2));
                    }
                }
            } else {
                sb.append(c);
            }
        }

        String path = sb.toString();
        return path;
    }


    /**
     * Get the File necessary to load the Sun compiler tools. If the classes
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
                if (path.toLowerCase().endsWith(extensions[i])) {
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
                        if (name.toLowerCase().endsWith(extensions[i])) {
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

