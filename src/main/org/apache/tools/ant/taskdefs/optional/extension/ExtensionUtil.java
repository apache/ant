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
