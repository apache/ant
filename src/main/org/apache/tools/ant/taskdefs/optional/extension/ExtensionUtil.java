/*
 * Copyright  2002-2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * A set of useful methods relating to extensions.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ExtensionUtil {
    /**
     * Class is not meant to be instantiated.
     */
    private ExtensionUtil() {
    }

    /**
     * Convert a list of extensionAdapter objects to extensions.
     *
     * @param adapters the list of ExtensionAdapterss to add to convert
     * @throws BuildException if an error occurs
     */
    static ArrayList toExtensions(final ArrayList adapters)
        throws BuildException {
        final ArrayList results = new ArrayList();

        final int size = adapters.size();
        for (int i = 0; i < size; i++) {
            final ExtensionAdapter adapter =
                (ExtensionAdapter) adapters.get(i);
            final Extension extension = adapter.toExtension();
            results.add(extension);
        }

        return results;
    }

    /**
     * Generate a list of extensions from a specified fileset.
     *
     * @param librarys the list to add extensions to
     * @param fileset the filesets containing librarys
     * @throws BuildException if an error occurs
     */
    static void extractExtensions(final Project project,
                                   final ArrayList librarys,
                                   final ArrayList fileset)
        throws BuildException {
        if (!fileset.isEmpty()) {
            final Extension[] extensions = getExtensions(project,
                                                          fileset);
            for (int i = 0; i < extensions.length; i++) {
                librarys.add(extensions[ i ]);
            }
        }
    }

    /**
     * Retrieve extensions from the specified librarys.
     *
     * @param librarys the filesets for librarys
     * @return the extensions contained in librarys
     * @throws BuildException if failing to scan librarys
     */
    private static Extension[] getExtensions(final Project project,
                                              final ArrayList librarys)
        throws BuildException {
        final ArrayList extensions = new ArrayList();
        final Iterator iterator = librarys.iterator();
        while (iterator.hasNext()) {
            final FileSet fileSet = (FileSet) iterator.next();

            boolean includeImpl = true;
            boolean includeURL = true;

            if (fileSet instanceof LibFileSet) {
                LibFileSet libFileSet = (LibFileSet) fileSet;
                includeImpl = libFileSet.isIncludeImpl();
                includeURL = libFileSet.isIncludeURL();
            }

            final DirectoryScanner scanner = fileSet.getDirectoryScanner(project);
            final File basedir = scanner.getBasedir();
            final String[] files = scanner.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                final File file = new File(basedir, files[ i ]);
                loadExtensions(file, extensions, includeImpl, includeURL);
            }
        }
        return (Extension[]) extensions.toArray(new Extension[extensions.size()]);
    }

    /**
     * Load list of available extensions from specified file.
     *
     * @param file the file
     * @param extensionList the list to add available extensions to
     * @throws BuildException if there is an error
     */
    private static void loadExtensions(final File file,
                                        final ArrayList extensionList,
                                        final boolean includeImpl,
                                        final boolean includeURL)
        throws BuildException {
        try {
            final JarFile jarFile = new JarFile(file);
            final Extension[] extensions =
                Extension.getAvailable(jarFile.getManifest());
            for (int i = 0; i < extensions.length; i++) {
                final Extension extension = extensions[ i ];
                addExtension(extensionList, extension, includeImpl, includeURL);
            }
        } catch (final Exception e) {
            throw new BuildException(e.getMessage(), e);
        }
    }

    /**
     * Add extension to list.
     * If extension should not have implementation details but
     * does strip them. If extension should not have url but does
     * then strip it.
     *
     * @param extensionList the list of extensions to add to
     * @param originalExtension the extension
     * @param includeImpl false to exclude implementation details
     * @param includeURL false to exclude implementation URL
     */
    private static void addExtension(final ArrayList extensionList,
                                      final Extension originalExtension,
                                      final boolean includeImpl,
                                      final boolean includeURL) {
        Extension extension = originalExtension;
        if (!includeURL
            && null != extension.getImplementationURL()) {
            extension =
                new Extension(extension.getExtensionName(),
                               extension.getSpecificationVersion().toString(),
                               extension.getSpecificationVendor(),
                               extension.getImplementationVersion().toString(),
                               extension.getImplementationVendor(),
                               extension.getImplementationVendorID(),
                               null);
        }

        final boolean hasImplAttributes =
            null != extension.getImplementationURL()
            || null != extension.getImplementationVersion()
            || null != extension.getImplementationVendorID()
            || null != extension.getImplementationVendor();

        if (!includeImpl && hasImplAttributes) {
            extension =
                new Extension(extension.getExtensionName(),
                               extension.getSpecificationVersion().toString(),
                               extension.getSpecificationVendor(),
                               null,
                               null,
                               null,
                               extension.getImplementationURL());
        }

        extensionList.add(extension);
    }

    /**
     * retrieve manifest for specified file.
     *
     * @param file the file
     * @return the manifest
     * @throws BuildException if errror occurs (file not exist,
     *         file not a jar, manifest not exist in file)
     */
    static Manifest getManifest(final File file)
        throws BuildException {
        try {
            final JarFile jarFile = new JarFile(file);
            Manifest m = jarFile.getManifest();
            if (m == null) {
                throw new BuildException(file + " doesn't have a MANIFEST");
            }
            return m;
        } catch (final IOException ioe) {
            throw new BuildException(ioe.getMessage(), ioe);
        }
    }
}
