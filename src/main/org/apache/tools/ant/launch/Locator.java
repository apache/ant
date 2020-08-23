/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.launch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;
import java.util.stream.Stream;

// CheckStyle:LineLengthCheck OFF - urls are long!
/**
 * The Locator is a utility class which is used to find certain items
 * in the environment.
 * <p>
 * It is used at boot time in the launcher, and cannot make use of any of Ant's other classes.
 * <p>
 * This is a surprisingly brittle piece of code, and has had lots of bugs filed against it:
 * <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=42275">running ant off a network share can cause Ant to fail</a>,
 * <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=8031">use File.toURI().toURL().toExternalForm()</a>,
 * <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=42222">Locator implementation not encoding URI strings properly: spaces in paths</a>.
 * It also breaks Eclipse 3.3 Betas:
 * <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=183283">Exception if installation path has spaces</a>.
 * <p>
 * Be very careful when making changes to this class, as a break will upset a lot of people.
 * @since Ant 1.6
 */
// CheckStyle:LineLengthCheck ON - urls are long!
public final class Locator {

    private static final int NIBBLE = 4;
    private static final int NIBBLE_MASK   = 0xF;

    private static final int ASCII_SIZE = 128;

    private static final int BYTE_SIZE = 256;

    private static final int WORD = 16;

    private static final int SPACE = 0x20;
    private static final int DEL = 0x7F;

    // stolen from org.apache.xerces.impl.XMLEntityManager#getUserDir()
    // of the Xerces-J team
    // which ASCII characters need to be escaped
    private static boolean[] gNeedEscaping = new boolean[ASCII_SIZE];
    // the first hex character if a character needs to be escaped
    private static char[] gAfterEscaping1 = new char[ASCII_SIZE];
    // the second hex character if a character needs to be escaped
    private static char[] gAfterEscaping2 = new char[ASCII_SIZE];
    private static char[] gHexChs = {'0', '1', '2', '3', '4', '5', '6', '7',
                                     '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    /** Error string used when an invalid uri is seen */
    public static final String ERROR_NOT_FILE_URI
        = "Can only handle valid file: URIs, not ";

    // initialize the above 3 arrays
    static {
        for (int i = 0; i < SPACE; i++) {
            gNeedEscaping[i] = true;
            gAfterEscaping1[i] = gHexChs[i >> NIBBLE];
            gAfterEscaping2[i] = gHexChs[i & NIBBLE_MASK];
        }
        gNeedEscaping[DEL] = true;
        gAfterEscaping1[DEL] = '7';
        gAfterEscaping2[DEL] = 'F';
        char[] escChs = {' ', '<', '>', '#', '%', '"', '{', '}',
                         '|', '\\', '^', '~', '[', ']', '`'};
        for (char ch : escChs) {
            gNeedEscaping[ch] = true;
            gAfterEscaping1[ch] = gHexChs[ch >> NIBBLE];
            gAfterEscaping2[ch] = gHexChs[ch & NIBBLE_MASK];
        }
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
    public static File getClassSource(Class<?> c) {
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
        URL url;
        if (c == null) {
            url = ClassLoader.getSystemResource(resource);
        } else {
            url = c.getResource(resource);
        }
        if (url != null) {
            String u = url.toString();
            try {
                if (u.startsWith("jar:file:")) {
                    return new File(fromJarURI(u));
                }
                if (u.startsWith("file:")) {
                    int tail = u.indexOf(resource);
                    String dirName = u.substring(0, tail);
                    return new File(fromURI(dirName));
                }
            } catch (IllegalArgumentException e) {
                //unable to determine the URI for reasons unknown.
            }
        }
        return null;
    }

    /**
     * Constructs a file path from a <code>file:</code> URI.
     *
     * <p>Will be an absolute path if the given URI is absolute.</p>
     *
     * <p>Prior to Java 1.4,<!-- TODO is JDK version actually relevant? -->
     * swallows '%' that are not followed by two characters.</p>
     *
     * See <a href="https://www.w3.org/TR/xml11/#dt-sysid">dt-sysid</a>
     * which makes some mention of how
     * characters not supported by URI Reference syntax should be escaped.
     *
     * @param uri the URI designating a file in the local filesystem.
     * @return the local file system path for the file.
     * @throws IllegalArgumentException if the URI is malformed or not a legal file: URL
     * @since Ant 1.6
     */
    public static String fromURI(String uri) {
        URL url = null;
        try {
            url = new URL(uri);
        } catch (MalformedURLException emYouEarlEx) {
            // Ignore malformed exception
        }
        if (url == null || !("file".equals(url.getProtocol()))) {
            throw new IllegalArgumentException(ERROR_NOT_FILE_URI + uri);
        }
        StringBuilder buf = new StringBuilder(url.getHost());
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
            //consider adding the current directory. This is not done when
            //the path is a UNC name
            String cwd = System.getProperty("user.dir");
            int posi = cwd.indexOf(':');
            boolean pathStartsWithFileSeparator = path.startsWith(File.separator);
            boolean pathStartsWithUNC = path.startsWith("" + File.separator + File.separator);
            if (posi > 0 && pathStartsWithFileSeparator && !pathStartsWithUNC) {
                path = cwd.substring(0, posi + 1) + path;
            }
        } catch (UnsupportedEncodingException exc) {
            // not sure whether this is clean, but this method is
            // declared not to throw exceptions.
            throw new IllegalStateException(
                "Could not convert URI " + uri + " to path: "
                + exc.getMessage());
        }
        return path;
    }

    /**
     * Crack a JAR URI.
     * This method is public for testing; we may delete it without any warning -it is not part of Ant's stable API.
     * @param uri uri to expand; contains jar: somewhere in it
     * @return the decoded URI
     * @since Ant1.7.1
     */
    public static String fromJarURI(String uri) {
        int pling = uri.indexOf("!/");
        String jarName = uri.substring("jar:".length(), pling);
        return fromURI(jarName);
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
        if (!uri.contains("%")) {
            return uri;
        }
        ByteArrayOutputStream sb = new ByteArrayOutputStream(uri.length());
        CharacterIterator iter = new StringCharacterIterator(uri);
        for (char c = iter.first(); c != CharacterIterator.DONE;
             c = iter.next()) {
            if (c == '%') {
                char c1 = iter.next();
                if (c1 != CharacterIterator.DONE) {
                    int i1 = Character.digit(c1, WORD);
                    char c2 = iter.next();
                    if (c2 != CharacterIterator.DONE) {
                        int i2 = Character.digit(c2, WORD);
                        sb.write((char) ((i1 << NIBBLE) + i2));
                    }
                }
            } else if (c >= 0x0000 && c < 0x0080) {
                sb.write(c);
            } else { // #50543
                byte[] bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                sb.write(bytes, 0, bytes.length);
            }
        }
        return sb.toString(StandardCharsets.UTF_8.name());
    }

    /**
     * Encodes an Uri with % characters.
     * The URI is escaped
     * @param path String to encode.
     * @return The encoded string, according to URI norms
     * @since Ant 1.7
     */
    public static String encodeURI(String path) {
        int i = 0;
        int len = path.length();
        int ch = 0;
        StringBuilder sb = null;
        for (; i < len; i++) {
            ch = path.charAt(i);
            // if it's not an ASCII character, break here, and use UTF-8 encoding
            if (ch >= ASCII_SIZE) {
                break;
            }
            if (gNeedEscaping[ch]) {
                if (sb == null) {
                    sb = new StringBuilder(path.substring(0, i));
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
                sb = new StringBuilder(path.substring(0, i));
            }
            // get UTF-8 bytes for the remaining sub-string
            byte[] bytes = path.substring(i).getBytes(StandardCharsets.UTF_8);
            len = bytes.length;

            // for each byte
            for (i = 0; i < len; i++) {
                byte b = bytes[i];
                // for non-ascii character: make it positive, then escape
                if (b < 0) {
                    ch = b + BYTE_SIZE;
                    sb.append('%');
                    sb.append(gHexChs[ch >> NIBBLE]);
                    sb.append(gHexChs[ch & NIBBLE_MASK]);
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
     * Ant cannot use it (except by reflection) <!-- TODO no longer true -->
     * File.toURI() cannot be used by Locator.java
     * Implemented this way.
     * File.toURL() adds file: and changes '\' to '/' for dos OSes
     * encodeURI converts characters like ' ' and '#' to %DD
     * @param file the file to convert
     * @return URL the converted File
     * @throws MalformedURLException on error
     * @deprecated since 1.9, use <code>FileUtils.getFileURL(File)</code>
     */
    @Deprecated
    public static URL fileToURL(File file) throws MalformedURLException {
        return new URL(file.toURI().toASCIIString());
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
        String libToolsJar
            = File.separator + "lib" + File.separator + "tools.jar";
        String javaHome = System.getProperty("java.home");
        File toolsJar = new File(javaHome + libToolsJar);
        if (toolsJar.exists()) {
            // Found in java.home as given
            return toolsJar;
        }
        if (javaHome.toLowerCase(Locale.ENGLISH).endsWith(File.separator + "jre")) {
            javaHome = javaHome.substring(
                0, javaHome.length() - "/jre".length());
            toolsJar = new File(javaHome + libToolsJar);
        }
        if (!toolsJar.exists()) {
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
        return getLocationURLs(location, ".jar");
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
                                        final String... extensions)
            throws MalformedURLException {
        URL[] urls = new URL[0];

        if (!location.exists()) {
            return urls;
        }
        if (!location.isDirectory()) {
            urls = new URL[1];
            String path = location.getPath();
            String littlePath = path.toLowerCase(Locale.ENGLISH);
            for (String extension : extensions) {
                if (littlePath.endsWith(extension)) {
                    urls[0] = fileToURL(location);
                    break;
                }
            }
            return urls;
        }
        File[] matches = location.listFiles((dir, name) -> {
            String littleName = name.toLowerCase(Locale.ENGLISH);
            return Stream.of(extensions).anyMatch(littleName::endsWith);
        });
        urls = new URL[matches.length];
        for (int i = 0; i < matches.length; ++i) {
            urls[i] = fileToURL(matches[i]);
        }
        return urls;
    }

    /**
     * Not instantiable
     */
    private Locator() {
    }

}
