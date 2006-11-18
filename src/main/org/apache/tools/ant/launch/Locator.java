/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.launch;

import java.net.MalformedURLException;

import java.net.URL;
import java.io.File;
import java.io.FilenameFilter;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;

/**
 * The Locator is a utility class which is used to find certain items
 * in the environment.
 *
 * @since Ant 1.6
 */
public final class Locator {
    /**
     * encoding used to represent URIs
     */
    public static final String URI_ENCODING = "UTF-8";
    // stolen from org.apache.xerces.impl.XMLEntityManager#getUserDir()
    // of the Xerces-J team
    // which ASCII characters need to be escaped
    private static boolean[] gNeedEscaping = new boolean[128];
    // the first hex character if a character needs to be escaped
    private static char[] gAfterEscaping1 = new char[128];
    // the second hex character if a character needs to be escaped
    private static char[] gAfterEscaping2 = new char[128];
    private static char[] gHexChs = {'0', '1', '2', '3', '4', '5', '6', '7',
                                     '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    // initialize the above 3 arrays
    static {
        for (int i = 0; i <= 0x1f; i++) {
            gNeedEscaping[i] = true;
            gAfterEscaping1[i] = gHexChs[i >> 4];
            gAfterEscaping2[i] = gHexChs[i & 0xf];
        }
        gNeedEscaping[0x7f] = true;
        gAfterEscaping1[0x7f] = '7';
        gAfterEscaping2[0x7f] = 'F';
        char[] escChs = {' ', '<', '>', '#', '%', '"', '{', '}',
                         '|', '\\', '^', '~', '[', ']', '`'};
        int len = escChs.length;
        char ch;
        for (int i = 0; i < len; i++) {
            ch = escChs[i];
            gNeedEscaping[ch] = true;
            gAfterEscaping1[ch] = gHexChs[ch >> 4];
            gAfterEscaping2[ch] = gHexChs[ch & 0xf];
        }
    }
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
     * Find the directory or jar a given resource has been loaded from.
     *
     * @param c the classloader to be consulted for the source.
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
        URL url = null;
        if (c == null) {
            url = ClassLoader.getSystemResource(resource);
        } else {
            url = c.getResource(resource);
        }
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
     * <p>Prior to Java 1.4,
     * swallows '%' that are not followed by two characters.</p>
     *
     * See <a href="http://www.w3.org/TR/xml11/#dt-sysid">dt-sysid</a>
     * which makes some mention of how
     * characters not supported by URI Reference syntax should be escaped.
     *
     * @param uri the URI designating a file in the local filesystem.
     * @return the local file system path for the file.
     * @throws IllegalArgumentException if the URI is malformed or not a legal file: URL
     * @since Ant 1.6
     */
    public static String fromURI(String uri) {
        // #8031: first try Java 1.4.
        Class uriClazz = null;
        try {
            uriClazz = Class.forName("java.net.URI");
        } catch (ClassNotFoundException cnfe) {
            // Fine, Java 1.3 or earlier, do it by hand.
        }
        // Also check for properly formed URIs. Ant formerly recommended using
        // nonsense URIs such as "file:./foo.xml" in XML includes. You shouldn't
        // do that (just "foo.xml" is correct) but for compatibility we special-case
        // things when the path is not absolute, and fall back to the old parsing behavior.
        if (uriClazz != null && uri.startsWith("file:/")) {
            try {
                java.lang.reflect.Method createMethod
                    = uriClazz.getMethod("create", new Class[] {String.class});
                Object uriObj = createMethod.invoke(null, new Object[] {uri});
                java.lang.reflect.Constructor fileConst
                    = File.class.getConstructor(new Class[] {uriClazz});
                File f = (File) fileConst.newInstance(new Object[] {uriObj});
                return f.getAbsolutePath();
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable e2 = e.getTargetException();
                if (e2 instanceof IllegalArgumentException) {
                    // Bad URI, pass this on.
                    throw (IllegalArgumentException) e2;
                } else {
                    // Unexpected target exception? Should not happen.
                    e2.printStackTrace();
                }
            } catch (Exception e) {
                // Reflection problems? Should not happen, debug.
                e.printStackTrace();
            }
        }

        // Fallback method for Java 1.3 or earlier.

        URL url = null;
        try {
            url = new URL(uri);
        } catch (MalformedURLException emYouEarlEx) {
            // Ignore malformed exception
        }
        if (url == null || !("file".equals(url.getProtocol()))) {
            throw new IllegalArgumentException("Can only handle valid file: URIs");
        }
        StringBuffer buf = new StringBuffer(url.getHost());
        if (buf.length() > 0) {
            buf.insert(0, File.separatorChar).insert(0, File.separatorChar);
        }
        String file = url.getFile();
        int queryPos = file.indexOf('?');
        buf.append((queryPos < 0) ? file : file.substring(0, queryPos));

        uri = buf.toString().replace('/', File.separatorChar);

        if (File.pathSeparatorChar == ';' && uri.startsWith("\\") && uri.length() > 2
            && Character.isLetter(uri.charAt(1)) && uri.lastIndexOf(':') > -1) {
            uri = uri.substring(1);
        }
        String path = null;
        try {
            path = decodeUri(uri);
            String cwd = System.getProperty("user.dir");
            int posi = cwd.indexOf(":");
            if ((posi > 0) && path.startsWith(File.separator)) {
               path = cwd.substring(0, posi + 1) + path;
            }
        } catch (UnsupportedEncodingException exc) {
            // not sure whether this is clean, but this method is
            // declared not to throw exceptions.
            throw new IllegalStateException("Could not convert URI to path: "
                                            + exc.getMessage());
        }
        return path;
    }

    /**
     * Decodes an Uri with % characters.
     * The URI is escaped
     * @param uri String with the uri possibly containing % characters.
     * @return The decoded Uri
     * @throws UnsupportedEncodingException if UTF-8 is not available
     * @since Ant 1.7
     */
    public static String decodeUri(String uri) throws UnsupportedEncodingException {
        if (uri.indexOf('%') == -1) {
            return uri;
        }
        ByteArrayOutputStream sb = new ByteArrayOutputStream(uri.length());
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
                        sb.write((char) ((i1 << 4) + i2));
                    }
                }
            } else {
                sb.write(c);
            }
        }
        return sb.toString(URI_ENCODING);
    }
    /**
     * Encodes an Uri with % characters.
     * The URI is escaped
     * @param path String to encode.
     * @return The encoded string, according to URI norms
     * @throws UnsupportedEncodingException if UTF-8 is not available
     * @since Ant 1.7
     */
    public static String encodeURI(String path) throws UnsupportedEncodingException {
        int i = 0;
        int len = path.length();
        int ch = 0;
        StringBuffer sb = null;
        for (; i < len; i++) {
            ch = path.charAt(i);
            // if it's not an ASCII character, break here, and use UTF-8 encoding
            if (ch >= 128) {
                break;
            }
            if (gNeedEscaping[ch]) {
                if (sb == null) {
                    sb = new StringBuffer(path.substring(0, i));
                }
                sb.append('%');
                sb.append(gAfterEscaping1[ch]);
                sb.append(gAfterEscaping2[ch]);
                // record the fact that it's escaped
            } else if (sb != null) {
                sb.append((char) ch);
            }
        }

        // we saw some non-ascii character
        if (i < len) {
            if (sb == null) {
                sb = new StringBuffer(path.substring(0, i));
            }
            // get UTF-8 bytes for the remaining sub-string
            byte[] bytes = null;
            byte b;
            bytes = path.substring(i).getBytes(URI_ENCODING);
            len = bytes.length;

            // for each byte
            for (i = 0; i < len; i++) {
                b = bytes[i];
                // for non-ascii character: make it positive, then escape
                if (b < 0) {
                    ch = b + 256;
                    sb.append('%');
                    sb.append(gHexChs[ch >> 4]);
                    sb.append(gHexChs[ch & 0xf]);
                } else if (gNeedEscaping[b]) {
                    sb.append('%');
                    sb.append(gAfterEscaping1[b]);
                    sb.append(gAfterEscaping2[b]);
                } else {
                    sb.append((char) b);
                }
            }
        }
        return sb == null ? path : sb.toString();
    }

    /**
     * Convert a File to a URL.
     * File.toURL() does not encode characters like #.
     * File.toURI() has been introduced in java 1.4, so
     * ANT cannot use it (except by reflection)
     * FileUtils.toURI() cannot be used by Locator.java
     * Implemented this way.
     * File.toURL() adds file: and changes '\' to '/' for dos OSes
     * encodeURI converts characters like ' ' and '#' to %DD
     * @param file the file to convert
     * @return URL the converted File
     * @throws MalformedURLException on error
     */
    public static URL fileToURL(File file)
        throws MalformedURLException {
        try {
            return new URL(encodeURI(file.toURL().toString()));
        } catch (UnsupportedEncodingException ex) {
            throw new MalformedURLException(ex.toString());
        }
    }

    /**
     * Get the File necessary to load the Sun compiler tools. If the classes
     * are available to this class, then no additional URL is required and
     * null is returned. This may be because the classes are explicitly in the
     * class path or provided by the JVM directly.
     *
     * @return the tools jar as a File if required, null otherwise.
     */
    public static File getToolsJar() {
        // firstly check if the tools jar is already in the classpath
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
        File toolsJar = new File(javaHome + "/lib/tools.jar");
        if (toolsJar.exists()) {
            // Found in java.home as given
            return toolsJar;
        }
        if (javaHome.toLowerCase(Locale.US).endsWith(File.separator + "jre")) {
            javaHome = javaHome.substring(0, javaHome.length() - 4);
            toolsJar = new File(javaHome + "/lib/tools.jar");
        }
        if (!toolsJar.exists()) {
            System.out.println("Unable to locate tools.jar. "
                 + "Expected to find it in " + toolsJar.getPath());
            return null;
        }
        return toolsJar;
    }

    /**
     * Get an array of URLs representing all of the jar files in the
     * given location. If the location is a file, it is returned as the only
     * element of the array. If the location is a directory, it is scanned for
     * jar files.
     *
     * @param location the location to scan for Jars.
     *
     * @return an array of URLs for all jars in the given location.
     *
     * @exception MalformedURLException if the URLs for the jars cannot be
     *            formed.
     */
    public static URL[] getLocationURLs(File location)
         throws MalformedURLException {
        return getLocationURLs(location, new String[]{".jar"});
    }

    /**
     * Get an array of URLs representing all of the files of a given set of
     * extensions in the given location. If the location is a file, it is
     * returned as the only element of the array. If the location is a
     * directory, it is scanned for matching files.
     *
     * @param location the location to scan for files.
     * @param extensions an array of extension that are to match in the
     *        directory search.
     *
     * @return an array of URLs of matching files.
     * @exception MalformedURLException if the URLs for the files cannot be
     *            formed.
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
                    urls[0] = fileToURL(location);
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
            urls[i] = fileToURL(matches[i]);
        }
        return urls;
    }
}
