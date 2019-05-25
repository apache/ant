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
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
 * available online at <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/extensions/versioning.html">
 * https://docs.oracle.com/javase/8/docs/technotes/guides/extensions/versioning.html</a>.</p>
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
    private final List<ExtensionSet> dependencies = new ArrayList<>();

    /**
     * ExtensionAdapter objects representing optional
     * dependencies required by library.
     */
    private final List<ExtensionSet> optionals = new ArrayList<>();

    /**
     * Extra attributes the user specifies for main section
     * in manifest.
     */
    private final List<ExtraAttribute> extraAttributes = new ArrayList<>();

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
            throw new BuildException(
                "Can not have multiple extensions defined in one library.");
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
    @Override
    public void execute() throws BuildException {
        validate();

        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();

        attributes.put(Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION);
        attributes.putValue(CREATED_BY, "Apache Ant "
                + getProject().getProperty(MagicNames.ANT_VERSION));

        appendExtraAttributes(attributes);

        if (null != extension) {
            Extension.addExtension(extension, attributes);
        }

        //Add all the dependency data to manifest for dependencies
        final List<Extension> depends = toExtensions(dependencies);
        appendExtensionList(attributes, Extension.EXTENSION_LIST, "lib", depends.size());
        appendLibraryList(attributes, "lib", depends);

        // Add all the dependency data to manifest for "optional"
        //dependencies
        final List<Extension> option = toExtensions(optionals);
        appendExtensionList(attributes, Extension.OPTIONAL_EXTENSION_LIST, "opt", option.size());
        appendLibraryList(attributes, "opt", option);

        try {
            log("Generating manifest " + destFile.getAbsoluteFile(), Project.MSG_INFO);
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
            throw new BuildException("Destfile attribute not specified.");
        }
        if (destFile.exists() && !destFile.isFile()) {
            throw new BuildException("%s is not a file.", destFile);
        }
    }

    /**
     * Add any extra attributes to the manifest.
     *
     * @param attributes the manifest section to write
     *        attributes to
     */
    private void appendExtraAttributes(final Attributes attributes) {
        for (ExtraAttribute attribute : extraAttributes) {
            attributes.putValue(attribute.getName(), attribute.getValue());
        }
    }

    /**
     * Write out manifest to destfile.
     *
     * @param manifest the manifest
     * @throws IOException if error writing file
     */
    private void writeManifest(final Manifest manifest) throws IOException {
        try (OutputStream output = Files.newOutputStream(destFile.toPath())) {
            manifest.write(output);
            output.flush();
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
    private void appendLibraryList(final Attributes attributes, final String listPrefix,
            final List<Extension> extensions) throws BuildException {
        final int size = extensions.size();
        for (int i = 0; i < size; i++) {
            Extension.addExtension(extensions.get(i), listPrefix + i + "-",
                attributes);
        }
    }

    /**
     * Append an attribute such as "Extension-List: lib0 lib1 lib2"
     * using specified prefix and counting up to specified size.
     * Also use specified extensionKey so that can generate list of
     * optional dependencies as well.
     *
     * @param size the number of libraries to list
     * @param listPrefix the prefix for all libraries
     * @param attributes the attributes to add key-value to
     * @param extensionKey the key to use
     */
    private void appendExtensionList(final Attributes attributes,
            final Attributes.Name extensionKey, final String listPrefix, final int size) {
        //add in something like
        //"Extension-List: javahelp java3d"
        attributes.put(extensionKey, IntStream.range(0, size)
            .mapToObj(i -> listPrefix + i).collect(Collectors.joining(" ")));
    }

    /**
     * Convert a list of ExtensionSet objects to extensions.
     *
     * @param extensionSets the list of ExtensionSets to add to list
     * @throws BuildException if an error occurs
     */
    private List<Extension> toExtensions(final List<ExtensionSet> extensionSets)
        throws BuildException {
        final Project prj = getProject();
        return extensionSets.stream().map(xset -> xset.toExtensions(prj))
            .flatMap(Stream::of).collect(Collectors.toList());
    }
}
