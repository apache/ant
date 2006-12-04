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
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Generates a manifest that declares all the dependencies.
 * The dependencies are determined by looking in the
 * specified path and searching for Extension / "Optional Package"
 * specifications in the manifests of the jars.
 *
 * <p>Prior to JDK1.3, an "Optional Package" was known as an Extension.
 * The specification for this mechanism is available in the JDK1.3
 * documentation in the directory
 * $JDK_HOME/docs/guide/extensions/versioning.html. Alternatively it is
 * available online at <a href="http://java.sun.com/j2se/1.3/docs/guide/extensions/versioning.html">
 * http://java.sun.com/j2se/1.3/docs/guide/extensions/versioning.html</a>.</p>
 *
 * @ant.task name="jarlib-manifest"
 */
public final class JarLibManifestTask extends Task {
    /**
     * Version of manifest spec that task generates.
     */
    private static final String MANIFEST_VERSION = "1.0";

    /**
     * "Created-By" string used when creating manifest.
     */
    private static final String CREATED_BY = "Created-By";

    /**
     * The library to display information about.
     */
    private File destFile;

    /**
     * The extension supported by this library (if any).
     */
    private Extension extension;

    /**
     * ExtensionAdapter objects representing
     * dependencies required by library.
     */
    private final ArrayList dependencies = new ArrayList();

    /**
     * ExtensionAdapter objects representing optional
     * dependencies required by library.
     */
    private final ArrayList optionals = new ArrayList();

    /**
     * Extra attributes the user specifies for main section
     * in manifest.
     */
    private final ArrayList extraAttributes = new ArrayList();

    /**
     * The location where generated manifest is placed.
     *
     * @param destFile The location where generated manifest is placed.
     */
    public void setDestfile(final File destFile) {
        this.destFile = destFile;
    }

    /**
     * Adds an extension that this library implements.
     *
     * @param extensionAdapter an extension that this library implements.
     *
     * @throws BuildException if there is multiple extensions detected
     *         in the library.
     */
    public void addConfiguredExtension(final ExtensionAdapter extensionAdapter)
        throws BuildException {
        if (null != extension) {
            final String message =
                "Can not have multiple extensions defined in one library.";
            throw new BuildException(message);
        }
        extension = extensionAdapter.toExtension();
    }

    /**
     * Adds a set of extensions that this library requires.
     *
     * @param extensionSet a set of extensions that this library requires.
     */
    public void addConfiguredDepends(final ExtensionSet extensionSet) {
        dependencies.add(extensionSet);
    }

    /**
     * Adds a set of extensions that this library optionally requires.
     *
     * @param extensionSet a set of extensions that this library optionally requires.
     */
    public void addConfiguredOptions(final ExtensionSet extensionSet) {
        optionals.add(extensionSet);
    }

    /**
     * Adds an attribute that is to be put in main section of manifest.
     *
     * @param attribute an attribute that is to be put in main section of manifest.
     */
    public void addConfiguredAttribute(final ExtraAttribute attribute) {
        extraAttributes.add(attribute);
    }

    /**
     * Execute the task.
     *
     * @throws BuildException if the task fails.
     */
    public void execute() throws BuildException {
        validate();

        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();

        attributes.put(Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION);
        final String createdBy = "Apache Ant " + getProject().getProperty(MagicNames.ANT_VERSION);
        attributes.putValue(CREATED_BY, createdBy);

        appendExtraAttributes(attributes);

        if (null != extension) {
            Extension.addExtension(extension, attributes);
        }

        //Add all the dependency data to manifest for dependencies
        final ArrayList depends = toExtensions(dependencies);
        appendExtensionList(attributes,
                             Extension.EXTENSION_LIST,
                             "lib",
                             depends.size());
        appendLibraryList(attributes, "lib", depends);

        //Add all the dependency data to manifest for "optional"
        //dependencies
        final ArrayList option = toExtensions(optionals);
        appendExtensionList(attributes,
                             Extension.OPTIONAL_EXTENSION_LIST,
                             "opt",
                             option.size());
        appendLibraryList(attributes, "opt", option);

        try {
            final String message = "Generating manifest " + destFile.getAbsoluteFile();
            log(message, Project.MSG_INFO);
            writeManifest(manifest);
        } catch (final IOException ioe) {
            throw new BuildException(ioe.getMessage(), ioe);
        }
    }

    /**
     * Validate the tasks parameters.
     *
     * @throws BuildException if invalid parameters found
     */
    private void validate() throws BuildException {
        if (null == destFile) {
            final String message = "Destfile attribute not specified.";
            throw new BuildException(message);
        }
        if (destFile.exists() && !destFile.isFile()) {
            final String message = destFile + " is not a file.";
            throw new BuildException(message);
        }
    }

    /**
     * Add any extra attributes to the manifest.
     *
     * @param attributes the manifest section to write
     *        attributes to
     */
    private void appendExtraAttributes(final Attributes attributes) {
        final Iterator iterator = extraAttributes.iterator();
        while (iterator.hasNext()) {
            final ExtraAttribute attribute =
                (ExtraAttribute) iterator.next();
            attributes.putValue(attribute.getName(),
                                 attribute.getValue());
        }
    }

    /**
     * Write out manifest to destfile.
     *
     * @param manifest the manifest
     * @throws IOException if error writing file
     */
    private void writeManifest(final Manifest manifest)
        throws IOException {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(destFile);
            manifest.write(output);
            output.flush();
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Append specified extensions to specified attributes.
     * Use the extensionKey to list the extensions, usually "Extension-List:"
     * for required dependencies and "Optional-Extension-List:" for optional
     * dependencies. NOTE: "Optional" dependencies are not part of the
     * specification.
     *
     * @param attributes the attributes to add extensions to
     * @param extensions the list of extensions
     * @throws BuildException if an error occurs
     */
    private void appendLibraryList(final Attributes attributes,
                                    final String listPrefix,
                                    final ArrayList extensions)
        throws BuildException {
        final int size = extensions.size();
        for (int i = 0; i < size; i++) {
            final Extension ext = (Extension) extensions.get(i);
            final String prefix = listPrefix + i + "-";
            Extension.addExtension(ext, prefix, attributes);
        }
    }

    /**
     * Append an attribute such as "Extension-List: lib0 lib1 lib2"
     * using specified prefix and counting up to specified size.
     * Also use specified extensionKey so that can generate list of
     * optional dependencies aswell.
     *
     * @param size the number of librarys to list
     * @param listPrefix the prefix for all librarys
     * @param attributes the attributes to add key-value to
     * @param extensionKey the key to use
     */
    private void appendExtensionList(final Attributes attributes,
                                      final Attributes.Name extensionKey,
                                      final String listPrefix,
                                      final int size) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < size; i++) {
            sb.append(listPrefix);
            sb.append(i);
            sb.append(' ');
        }

        //add in something like
        //"Extension-List: javahelp java3d"
        attributes.put(extensionKey, sb.toString());
    }

    /**
     * Convert a list of ExtensionSet objects to extensions.
     *
     * @param extensionSets the list of ExtensionSets to add to list
     * @throws BuildException if an error occurs
     */
    private ArrayList toExtensions(final ArrayList extensionSets)
        throws BuildException {
        final ArrayList results = new ArrayList();

        final int size = extensionSets.size();
        for (int i = 0; i < size; i++) {
            final ExtensionSet set = (ExtensionSet) extensionSets.get(i);
            final Extension[] extensions = set.toExtensions(getProject());
            for (int j = 0; j < extensions.length; j++) {
                results.add(extensions[ j ]);
            }
        }

        return results;
    }
}
