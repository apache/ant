/*
 * Copyright  2003-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.zip.ZipEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.tools.ant.util.FileUtils;

/**
 * An implementation of the AntClassLoader suitable for use on post JDK 1.1
 * platforms
 *
 */
public class AntClassLoader2 extends AntClassLoader {
    /** Instance of a utility class to use for file operations. */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** Static map of jar file/time to manifiest class-path entries */
    private static Map pathMap = Collections.synchronizedMap(new HashMap());

    /**
     * Constructor
     */
    public AntClassLoader2() {
    }

    /**
     * Define a class given its bytes
     *
     * @param container the container from which the class data has been read
     *                  may be a directory or a jar/zip file.
     *
     * @param classData the bytecode data for the class
     * @param className the name of the class
     *
     * @return the Class instance created from the given data
     *
     * @throws IOException if the class data cannot be read.
     */
    protected Class defineClassFromData(File container, byte[] classData,
                                        String className) throws IOException {

        definePackage(container, className);
        return defineClass(className, classData, 0, classData.length,
                           Project.class.getProtectionDomain());

    }

    /**
     * Get the manifest from the given jar, if it is indeed a jar and it has a
     * manifest
     *
     * @param container the File from which a manifest is required.
     *
     * @return the jar's manifest or null is the container is not a jar or it
     *         has no manifest.
     *
     * @exception IOException if the manifest cannot be read.
     */
    private Manifest getJarManifest(File container) throws IOException {
        if (container.isDirectory()) {
            return null;
        }
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(container);
            return jarFile.getManifest();
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
    }

    /**
     * Define the package information associated with a class.
     *
     * @param container the file containing the class definition.
     * @param className the class name of for which the package information
     *        is to be determined.
     *
     * @exception IOException if the package information cannot be read from the
     *            container.
     */
    protected void definePackage(File container, String className)
        throws IOException {
        int classIndex = className.lastIndexOf('.');
        if (classIndex == -1) {
            return;
        }

        String packageName = className.substring(0, classIndex);
        if (getPackage(packageName) != null) {
            // already defined
            return;
        }

        // define the package now
        Manifest manifest = getJarManifest(container);

        if (manifest == null) {
            definePackage(packageName, null, null, null, null, null,
                          null, null);
        } else {
            definePackage(container, packageName, manifest);
        }
    }

    /**
     * Define the package information when the class comes from a
     * jar with a manifest
     *
     * @param container the jar file containing the manifest
     * @param packageName the name of the package being defined.
     * @param manifest the jar's manifest
     */
    protected void definePackage(File container, String packageName,
                                 Manifest manifest) {
        String sectionName = packageName.replace('.', '/') + "/";

        String specificationTitle = null;
        String specificationVendor = null;
        String specificationVersion = null;
        String implementationTitle = null;
        String implementationVendor = null;
        String implementationVersion = null;
        String sealedString = null;
        URL sealBase = null;

        Attributes sectionAttributes = manifest.getAttributes(sectionName);
        if (sectionAttributes != null) {
            specificationTitle
                = sectionAttributes.getValue(Name.SPECIFICATION_TITLE);
            specificationVendor
                = sectionAttributes.getValue(Name.SPECIFICATION_VENDOR);
            specificationVersion
                = sectionAttributes.getValue(Name.SPECIFICATION_VERSION);
            implementationTitle
                = sectionAttributes.getValue(Name.IMPLEMENTATION_TITLE);
            implementationVendor
                = sectionAttributes.getValue(Name.IMPLEMENTATION_VENDOR);
            implementationVersion
                = sectionAttributes.getValue(Name.IMPLEMENTATION_VERSION);
            sealedString
                = sectionAttributes.getValue(Name.SEALED);
        }

        Attributes mainAttributes = manifest.getMainAttributes();
        if (mainAttributes != null) {
            if (specificationTitle == null) {
                specificationTitle
                    = mainAttributes.getValue(Name.SPECIFICATION_TITLE);
            }
            if (specificationVendor == null) {
                specificationVendor
                    = mainAttributes.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (specificationVersion == null) {
                specificationVersion
                    = mainAttributes.getValue(Name.SPECIFICATION_VERSION);
            }
            if (implementationTitle == null) {
                implementationTitle
                    = mainAttributes.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (implementationVendor == null) {
                implementationVendor
                    = mainAttributes.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (implementationVersion == null) {
                implementationVersion
                    = mainAttributes.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (sealedString == null) {
                sealedString
                    = mainAttributes.getValue(Name.SEALED);
            }
        }

        if (sealedString != null && sealedString.equalsIgnoreCase("true")) {
            try {
                sealBase = new URL("file:" + container.getPath());
            } catch (MalformedURLException e) {
                // ignore
            }
        }

        definePackage(packageName, specificationTitle, specificationVersion,
                      specificationVendor, implementationTitle,
                      implementationVersion, implementationVendor, sealBase);
    }


    /**
     * Add a file to the path. This classloader reads the manifest, if
     * available, and adds any additional class path jars specified in the
     * manifest.
     *
     * @param pathComponent the file which is to be added to the path for
     *                      this class loader
     *
     * @throws IOException if data needed from the file cannot be read.
     */
    protected void addPathFile(File pathComponent) throws IOException {
        super.addPathFile(pathComponent);

        if (pathComponent.isDirectory()) {
            return;
        }

        String absPathPlusTimeAndLength =
            pathComponent.getAbsolutePath() + pathComponent.lastModified() + "-"
            + pathComponent.length();
        String classpath = (String) pathMap.get(absPathPlusTimeAndLength);
        if (classpath == null) {
            ZipFile jarFile = null;
            InputStream manifestStream = null;
            try {
                jarFile = new ZipFile(pathComponent);
                manifestStream
                    = jarFile.getInputStream(new ZipEntry("META-INF/MANIFEST.MF"));

                if (manifestStream == null) {
                    return;
                }
                Reader manifestReader
                    = new InputStreamReader(manifestStream, "UTF-8");
                org.apache.tools.ant.taskdefs.Manifest manifest
                    = new org.apache.tools.ant.taskdefs.Manifest(manifestReader);
                classpath
                    = manifest.getMainSection().getAttributeValue("Class-Path");

            } catch (org.apache.tools.ant.taskdefs.ManifestException e) {
                // ignore
            } finally {
                if (manifestStream != null) {
                    manifestStream.close();
                }
                if (jarFile != null) {
                    jarFile.close();
                }
            }
            if (classpath == null) {
                classpath = "";
            }
            pathMap.put(absPathPlusTimeAndLength, classpath);
        }

        if (!"".equals(classpath)) {
            URL baseURL = FILE_UTILS.getFileURL(pathComponent);
            StringTokenizer st = new StringTokenizer(classpath);
            while (st.hasMoreTokens()) {
                String classpathElement = st.nextToken();
                URL libraryURL = new URL(baseURL, classpathElement);
                if (!libraryURL.getProtocol().equals("file")) {
                    log("Skipping jar library " + classpathElement
                        + " since only relative URLs are supported by this"
                        + " loader", Project.MSG_VERBOSE);
                    continue;
                }
                File libraryFile = new File(libraryURL.getFile());
                if (libraryFile.exists() && !isInPath(libraryFile)) {
                    addPathFile(libraryFile);
                }
            }
        }
    }
}

