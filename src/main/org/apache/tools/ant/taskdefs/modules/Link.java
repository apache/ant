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

package org.apache.tools.ant.taskdefs.modules;

import java.io.File;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Properties;

import java.util.Collections;
import java.util.Objects;

import java.util.spi.ToolProvider;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.LogLevel;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.ResourceCollection;

import org.apache.tools.ant.util.CompositeMapper;
import org.apache.tools.ant.util.MergingMapper;

import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ResourceUtils;

/**
 * Assembles jmod files into an executable image.  Equivalent to the
 * JDK {@code jlink} command.
 * <p>
 * Supported attributes:
 * <dl>
 * <dt>{@code destDir}
 * <dd>Root directory of created image. (required)
 * <dt>{@code modulePath}
 * <dd>Path of modules.  Should be a list of .jmod files.  Required, unless
 *     nested module path or modulepathref is present.
 * <dt>{@code modulePathRef}
 * <dd>Reference to path of modules.  Referenced path should be
 *     a list of .jmod files.
 * <dt>{@code modules}
 * <dd>Comma-separated list of modules to assemble.  Required, unless
 *     one or more nested {@code <module>} elements are present.
 * <dt>{@code observableModules}
 * <dd>Comma-separated list of explicit modules that comprise
 *     "universe" visible to tool while linking.
 * <dt>{@code launchers}
 * <dd>Comma-separated list of commands, each of the form
 *     <var>name</var>{@code =}<var>module</var> or
 *     <var>name</var>{@code =}<var>module</var>{@code /}<var>mainclass</var>
 * <dt>{@code excludeFiles}
 * <dd>Comma-separated list of patterns specifying files to exclude from
 *     linked image.
 *     Each is either a <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29">standard PathMatcher pattern</a>
 *     or {@code @}<var>filename</var>.
 * <dt>{@code excludeResources}
 * <dd>Comma-separated list of patterns specifying resources to exclude from jmods.
 *     Each is either a <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29">standard PathMatcher pattern</a>
 *     or {@code @}<var>filename</var>.
 * <dt>{@code locales}
 * <dd>Comma-separated list of extra locales to include,
 *     requires {@code jdk.localedata} module
 * <dt>{@code resourceOrder}
 * <dt>Comma-separated list of patterns specifying resource search order.
 *     Each is either a <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29">standard PathMatcher pattern</a>
 *     or {@code @}<var>filename</var>.
 * <dt>{@code bindServices}
 * <dd>boolean, whether to link service providers; default is false
 * <dt>{@code ignoreSigning}
 * <dd>boolean, whether to allow signed jar files; default is false
 * <dt>{@code includeHeaders}
 * <dd>boolean, whether to include header files; default is true
 * <dt>{@code includeManPages}
 * <dd>boolean, whether to include man pages; default is true
 * <dt>{@code includeNativeCommands}
 * <dd>boolean, whether to include native executables normally generated
 *     for image; default is true
 * <dt>{@code debug}
 * <dd>boolean, whether to include debug information; default is true
 * <dt>{@code verboseLevel}
 * <dd>If set, jlink will produce verbose output, which will be logged at
 *     the specified Ant log level ({@code DEBUG}, {@code VERBOSE},
 *     {@code INFO}}, {@code WARN}, or {@code ERR}).
 * <dt>{@code compress}
 * <dd>compression level, one of:
 *     <dl>
 *     <dt>{@code 0}
 *     <dt>{@code none}
 *     <dd>no compression (default)
 *     <dt>{@code 1}
 *     <dt>{@code strings}
 *     <dd>constant string sharing
 *     <dt>{@code 2}
 *     <dt>{@code zip}
 *     <dd>zip compression
 *     </dl>
 * <dt>{@code endianness}
 * <dd>Must be {@code little} or {@code big}, default is native endianness
 * <dt>{@code checkDuplicateLegal}
 * <dd>Boolean.  When merging legal notices from different modules
 *     because they have the same name, verify that their contents
 *     are identical.  Default is false, which means any license files
 *     with the same name are assumed to have the same content, and no
 *     checking is done.
 * <dt>{@code vmType}
 * <dd>Hotspot VM in image, one of:
 *     <ul>
 *     <li>{@code client}
 *     <li>{@code server}
 *     <li>{@code minimal}
 *     <li>{@code all} (default)
 *     </ul>
 * </dl>
 *
 * <p>
 * Supported nested elements
 * <dl>
 * <dt>{@code <modulepath>}
 * <dd>path element
 * <dt>{@code <module>}
 * <dd>May be specified multiple times.
 *     Only attribute is required {@code name} attribute.
 * <dt>{@code <observableModule>}
 * <dd>May be specified multiple times.
 *     Only attribute is required {@code name} attribute.
 * <dt>{@code <launcher>}
 * <dd>May be specified multiple times.  Attributes:
 *     <ul>
 *     <li>{@code name} (required)
 *     <li>{@code module} (required)
 *     <li>{@code mainClass} (optional)
 *     </ul>
 * <dt>{@code <locale>}
 * <dd>May be specified multiple times.
 *     Only attribute is required {@code name} attribute.
 * <dt>{@code <resourceOrder>}
 * <dd>Explicit resource search order in image.  May be specified multiple
 *     times.  Exactly one of these attributes must be specified:
 *     <dl>
 *     <dt>{@code pattern}
 *     <dd>A <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29">standard PathMatcher pattern</a>
 *     <dt>{@code listFile}
 *     <dd>Text file containing list of resource names (not patterns),
 *         one per line
 *     </dl>
 *     If the {@code resourceOrder} attribute is also present on the task, its
 *     patterns are treated as if they occur before patterns in nested
 *     {@code <resourceOrder>} elements.
 * <dt>{@code <excludeFiles>}
 * <dd>Excludes files from linked image tree.  May be specified multiple times.
 *     Exactly one of these attributes is required:
 *     <dl>
 *     <dt>{@code pattern}
 *     <dd>A <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29">standard PathMatcher pattern</a>
 *     <dt>{@code listFile}
 *     <dd>Text file containing list of file names (not patterns),
 *         one per line
 *     </dl>
 * <dt>{@code <excludeResources>}
 * <dd>Excludes resources from jmods.  May be specified multiple times.
 *     Exactly one of these attributes is required:
 *     <dl>
 *     <dt>{@code pattern}
 *     <dd>A <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29">standard PathMatcher pattern</a>
 *     <dt>{@code listFile}
 *     <dd>Text file containing list of resource names (not patterns),
 *         one per line
 *     </dl>
 * <dt>{@code <compress>}
 * <dd>Must have {@code level} attribute, whose permitted values are the same
 *     as the {@code compress} task attribute described above.
 *     May also have a {@code files} attribute, which is a comma-separated
 *     list of patterns, and/or nested {@code <files>} elements, each with
 *     either a {@code pattern} attribute or {@code listFile} attribute.
 * <dt>{@code <releaseInfo>}
 * <dd>Replaces, augments, or trims the image's release info properties.
 *     This may specify any of the following:
 *     <ul>
 *     <li>A {@code file} attribute, pointing to a Java properties file
 *         containing new release info properties that will entirely replace
 *         the current ones.
 *     <li>A {@code delete} attribute, containing comma-separated property keys
 *         to remove from application's release info, and/or any number of
 *         nested {@code <delete>} elements, each with a required {@code key}
 *         attribute.
 *     <li>One or more nested {@code <add>} elements, containing either
 *         {@code key} and {@code value} attributes, or a {@code file}
 *         attribute and an optional {@code charset} attribute.
 *     </ul>
 * </dl>
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/11/tools/jlink.html"><code>jlink</code> tool reference</a>
 *
 * @since 1.10.6
 */
public class Link
extends Task {
    /**
     * Error message for improperly formatted launcher attribute.
     */
    private static final String INVALID_LAUNCHER_STRING =
        "Launcher command must take the form name=module "
        + "or name=module/mainclass";

    /** Path of directories containing linkable modules. */
    private Path modulePath;

    /** Modules to include in linked image. */
    private final List<ModuleSpec> modules = new ArrayList<>();

    /** If non-empty, list of all modules linker is permitted to know about. */
    private final List<ModuleSpec> observableModules = new ArrayList<>();

    /**
     * Additional runnable programs which linker will place in image's
     * <code>bin</code> directory.
     */
    private final List<Launcher> launchers = new ArrayList<>();

    /**
     * Locales to explicitly include from {@code jdk.localdata} module.
     * If empty, all locales are included.
     */
    private final List<LocaleSpec> locales = new ArrayList<>();

    /** Resource ordering. */
    private final List<PatternListEntry> ordering = new ArrayList<>();

    /** Files to exclude from linked image. */
    private final List<PatternListEntry> excludedFiles = new ArrayList<>();

    /**
     * Resources in linked modules which should be excluded from linked image.
     */
    private final List<PatternListEntry> excludedResources = new ArrayList<>();

    /**
     * Whether to include all service provides in linked image which are
     * present in the module path and which are needed by modules explicitly
     * linked.
     */
    private boolean bindServices;

    /**
     * Whether to ignore signed jars (and jmods based on signed jars) when
     * linking, instead of emitting an error.
     */
    private boolean ignoreSigning;

    /** Whether to include header files from linked modules in image. */
    private boolean includeHeaders = true;

    /** Whether to include man pages from linked modules in image. */
    private boolean includeManPages = true;

    /** Whether to include native commands from linked modules in image. */
    private boolean includeNativeCommands = true;

    /** Whether to include classes' debug information or strip it. */
    private boolean debug = true;

    /**
     * The Ant logging level at which verbose output of linked should be
     * emitted.  If null, verbose output is disabled.
     */
    private LogLevel verboseLevel;

    /** Directory into which linked image will be placed. */
    private File outputDir;

    /** Endianness of some files (?) in linked image. */
    private Endianness endianness;

    /**
     * Simple compression level applied to linked image.
     * This or {@link #compression} may be set, but not both.
     */
    private CompressionLevel compressionLevel;

    /**
     * Describes which files in image to compress, and how to compress them.
     * This or {@link #compressionLevel} may be set, but not both.
     */
    private Compression compression;

    /**
     * Whether to check duplicate legal notices from different modules
     * actually have identical content, not just indentical names,
     * before merging them.
     * <a href="https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/src/jdk.jlink/share/classes/jdk/tools/jlink/internal/JlinkTask.java#L80">Forced to true as of Java 11.</a>
     */
    private boolean checkDuplicateLegal;

    /** Type of VM in linked image. */
    private VMType vmType;

    /** Changes to linked image's default release info. */
    private final List<ReleaseInfo> releaseInfo = new ArrayList<>();

    /**
     * Adds child {@code <modulePath>} element.
     *
     * @return new, empty child element
     *
     * @see #setModulePath(Path)
     */
    public Path createModulePath() {
        if (modulePath == null) {
            modulePath = new Path(getProject());
        }
        return modulePath.createPath();
    }

    /**
     * Attribute containing path of directories containing linkable modules.
     *
     * @return current module path, possibly {@code null}
     *
     * @see #setModulePath(Path)
     * @see #createModulePath()
     */
    public Path getModulePath() {
        return modulePath;
    }

    /**
     * Sets attribute containing path of directories containing
     * linkable modules.
     *
     * @param path new module path
     *
     * @see #getModulePath()
     * @see #setModulePathRef(Reference)
     * @see #createModulePath()
     */
    public void setModulePath(final Path path) {
        if (modulePath == null) {
            this.modulePath = path;
        } else {
            modulePath.append(path);
        }
    }

    /**
     * Sets module path as a reference.
     *
     * @param ref path reference
     *
     * @see #setModulePath(Path)
     * @see #createModulePath()
     */
    public void setModulePathRef(final Reference ref) {
        createModulePath().setRefid(ref);
    }

    /**
     * Adds child {@code <module>} element, specifying a module to link.
     *
     * @return new, unconfigured child element
     *
     * @see #setModules(String)
     */
    public ModuleSpec createModule() {
        ModuleSpec module = new ModuleSpec();
        modules.add(module);
        return module;
    }

    /**
     * Sets attribute containing list of modules to link.
     *
     * @param moduleList comma-separated list of module names
     */
    public void setModules(final String moduleList) {
        for (String moduleName : moduleList.split(",")) {
            modules.add(new ModuleSpec(moduleName));
        }
    }

    /**
     * Creates child {@code <observableModule>} element that represents
     * one of the modules the linker is permitted to know about.
     *
     * @return new, unconfigured child element
     */
    public ModuleSpec createObservableModule() {
        ModuleSpec module = new ModuleSpec();
        observableModules.add(module);
        return module;
    }

    /**
     * Sets attribute containing modules linker is permitted to know about.
     *
     * @param moduleList comma-separated list of module names
     */
    public void setObservableModules(final String moduleList) {
        for (String moduleName : moduleList.split(",")) {
            observableModules.add(new ModuleSpec(moduleName));
        }
    }

    /**
     * Creates child {@code <launcher>} element that can contain information
     * on additional executable in the linked image.
     *
     * @return new, unconfigured child element
     *
     * @see #setLaunchers(String)
     */
    public Launcher createLauncher() {
        Launcher command = new Launcher();
        launchers.add(command);
        return command;
    }

    /**
     * Sets attribute containing comma-separated list of information needed for
     * additional executables in the linked image.  Each item must be of the
     * form * <var>name</var>{@code =}<var>module</var> or
     * <var>name</var>{@code =}<var>module</var>{@code /}<var>mainclass</var>.
     *
     * @param launcherList comma-separated list of launcher data
     */
    public void setLaunchers(final String launcherList) {
        for (String launcherSpec : launcherList.split(",")) {
            launchers.add(new Launcher(launcherSpec));
        }
    }

    /**
     * Creates child {@code <locale>} element that specifies a Java locale,
     * or set of locales, to include from the {@code jdk.localedata} module
     * in the linked image.
     *
     * @return new, unconfigured child element
     */
    public LocaleSpec createLocale() {
        LocaleSpec locale = new LocaleSpec();
        locales.add(locale);
        return locale;
    }

    /**
     * Sets attribute containing a list of locale patterns, to specify
     * Java locales to include from {@code jdk.localedata} module in
     * linked image.  Asterisks ({@code *}) are permitted for wildcard
     * matches.
     *
     * @param localeList comma-separated list of locale patterns
     */
    public void setLocales(final String localeList) {
        for (String localeName : localeList.split(",")) {
            locales.add(new LocaleSpec(localeName));
        }
    }

    /**
     * Creates child {@code <excludeFiles>} element that specifies
     * files to exclude from linked modules when assembling linked image.
     *
     * @return new, unconfigured child element
     *
     * @see #setExcludeFiles(String)
     */
    public PatternListEntry createExcludeFiles() {
        PatternListEntry entry = new PatternListEntry();
        excludedFiles.add(entry);
        return entry;
    }

    /**
     * Sets attribute containing a list of patterns denoting files
     * to exclude from linked modules when assembling linked image.
     *
     * @param patternList comman-separated list of patterns
     *
     * @see Link.PatternListEntry
     */
    public void setExcludeFiles(String patternList) {
        for (String pattern : patternList.split(",")) {
            excludedFiles.add(new PatternListEntry(pattern));
        }
    }

    /**
     * Creates child {@code <excludeResources>} element that specifies
     * resources in linked modules that will be excluded from linked image.
     *
     * @return new, unconfigured child element
     *
     * @see #setExcludeResources(String)
     */
    public PatternListEntry createExcludeResources() {
        PatternListEntry entry = new PatternListEntry();
        excludedResources.add(entry);
        return entry;
    }

    /**
     * Sets attribute containing a list of patterns denoting resources
     * to exclude from linked modules in linked image.
     *
     * @param patternList comma-separated list of patterns
     *
     * @see #createExcludeResources()
     * @see Link.PatternListEntry
     */
    public void setExcludeResources(String patternList) {
        for (String pattern : patternList.split(",")) {
            excludedResources.add(new PatternListEntry(pattern));
        }
    }

    /**
     * Creates child {@code <resourceOrder} element that specifies
     * explicit ordering of resources in linked image.
     *
     * @return new, unconfigured child element
     *
     * @see #setResourceOrder(String)
     */
    public PatternListEntry createResourceOrder() {
        PatternListEntry order = new PatternListEntry();
        ordering.add(order);
        return order;
    }

    /**
     * Sets attribute containing a list of patterns that explicitly
     * order resources in the linked image.  Any patterns specified here
     * will be placed before any patterns specified as
     * {@linkplain #createResourceOrder() child elements}.
     *
     * @param patternList comma-separated list of patterns
     *
     * @see #createResourceOrder()
     * @see Link.PatternListEntry
     */
    public void setResourceOrder(final String patternList) {
        List<PatternListEntry> orderList = new ArrayList<>();

        for (String pattern : patternList.split(",")) {
            orderList.add(new PatternListEntry(pattern));
        }

        // Attribute value comes before nested elements.
        ordering.addAll(0, orderList);
    }

    /**
     * Attribute indicating whether linked image should pull in providers
     * in the module path of services used by explicitly linked modules.
     *
     * @return true if linked will pull in service provides, false if not
     *
     * @see #setBindServices(boolean)
     */
    public boolean getBindServices() {
        return bindServices;
    }

    /**
     * Sets attribute indicating whether linked image should pull in providers
     * in the module path of services used by explicitly linked modules.
     *
     * @param bind whether to include service providers
     *
     * @see #getBindServices()
     */
    public void setBindServices(final boolean bind) {
        this.bindServices = bind;
    }

    /**
     * Attribute indicating whether linker should allow modules made from
     * signed jars.
     *
     * @return true if signed jars are allowed, false if modules based on
     *         signed jars cause an error
     *
     * @see #setIgnoreSigning(boolean)
     */
    public boolean getIgnoreSigning() {
        return ignoreSigning;
    }

    /**
     * Sets attribute indicating whether linker should allow modules made from
     * signed jars.
     * <p>
     * Note: As of Java 11, this attribute is internally forced to true.  See
     * <a href="https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/src/jdk.jlink/share/classes/jdk/tools/jlink/internal/JlinkTask.java#L80">the source</a>.
     *
     * @param ignore true to have linker allow signed jars,
     *               false to have linker emit an error for signed jars
     *
     *
     * @see #getIgnoreSigning()
     */
    public void setIgnoreSigning(final boolean ignore) {
        this.ignoreSigning = ignore;
    }

    /**
     * Attribute indicating whether to include header files from linked modules
     * in image.
     *
     * @return true if header files should be included, false to exclude them
     *
     * @see #setIncludeHeaders(boolean)
     */
    public boolean getIncludeHeaders() {
        return includeHeaders;
    }

    /**
     * Sets attribute indicating whether to include header files from
     * linked modules in image.
     *
     * @param include true if header files should be included,
     *                false to exclude them
     *
     * @see #getIncludeHeaders()
     */
    public void setIncludeHeaders(final boolean include) {
        this.includeHeaders = include;
    }

    /**
     * Attribute indicating whether to include man pages from linked modules
     * in image.
     *
     * @return true if man pages should be included, false to exclude them
     *
     * @see #setIncludeManPages(boolean)
     */
    public boolean getIncludeManPages() {
        return includeManPages;
    }

    /**
     * Sets attribute indicating whether to include man pages from
     * linked modules in image.
     *
     * @param include true if man pages should be included,
     *                false to exclude them
     *
     * @see #getIncludeManPages()
     */
    public void setIncludeManPages(final boolean include) {
        this.includeManPages = include;
    }

    /**
     * Attribute indicating whether to include generated native commands,
     * and native commands from linked modules, in image.
     *
     * @return true if native commands should be included, false to exclude them
     *
     * @see #setIncludeNativeCommands(boolean)
     */
    public boolean getIncludeNativeCommands() {
        return includeNativeCommands;
    }

    /**
     * Sets attribute indicating whether to include generated native commands,
     * and native commands from linked modules, in image.
     *
     * @param include true if native commands should be included,
     *                false to exclude them
     *
     * @see #getIncludeNativeCommands()
     */
    public void setIncludeNativeCommands(final boolean include) {
        this.includeNativeCommands = include;
    }

    /**
     * Attribute indicating whether linker should keep or strip
     * debug information in classes.
     *
     * @return true if debug information will be retained,
     *         false if it will be stripped
     *
     * @see #setDebug(boolean)
     */
    public boolean getDebug() {
        return debug;
    }

    /**
     * Sets attribute indicating whether linker should keep or strip
     * debug information in classes.
     *
     * @param debug true if debug information should be retained,
     *              false if it should be stripped
     *
     * @see #getDebug()
     */
    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    /**
     * Attribute indicating whether linker should produce verbose output,
     * and at what logging level that output should be shown.
     *
     * @return logging level at which to show linker's verbose output,
     *         or {@code null} to disable verbose output
     *
     * @see #setVerboseLevel(LogLevel)
     */
    public LogLevel getVerboseLevel() {
        return verboseLevel;
    }

    /**
     * Sets attribute indicating whether linker should produce verbose output,
     * and at what logging level that output should be shown.
     *
     * @param level level logging level at which to show linker's
     *              verbose output, or {@code null} to disable verbose output
     *
     * @see #getVerboseLevel()
     */
    public void setVerboseLevel(final LogLevel level) {
        this.verboseLevel = level;
    }

    /**
     * Required attribute containing directory where linked image will be
     * created.
     *
     * @return directory where linked image will reside
     *
     * @see #setDestDir(File)
     */
    public File getDestDir() {
        return outputDir;
    }

    /**
     * Sets attribute indicating directory where linked image will be created.
     *
     * @param dir directory in which image will be created by linker
     *
     * @see #getDestDir()
     */
    public void setDestDir(final File dir) {
        this.outputDir = dir;
    }

    /**
     * Attribute indicating level of compression linker will apply to image.
     * This is exclusive with regard to {@link #createCompress()}:  only one
     * of the two may be specified.
     *
     * @return compression level to apply, or {@code null} for none
     *
     * @see #setCompress(Link.CompressionLevel)
     * @see #createCompress()
     */
    public CompressionLevel getCompress() {
        return compressionLevel;
    }

    /**
     * Sets attribute indicating level of compression linker will apply
     * to image. This is exclusive with regard to {@link #createCompress()}:
     * only one of the two may be specified.
     *
     * @param level compression level to apply, or {@code null} for none
     *
     * @see #getCompress()
     * @see #createCompress()
     */
    public void setCompress(final CompressionLevel level) {
        this.compressionLevel = level;
    }

    /**
     * Creates child {@code <compress>} element that specifies the level of
     * compression the linker will apply, and optionally, which files in the
     * image will be compressed.  This is exclusive with regard to the
     * {@link #setCompress compress} attribute:  only one of the two may be
     * specified.
     *
     * @return new, unconfigured child element
     *
     * @see #setCompress(Link.CompressionLevel)
     */
    public Compression createCompress() {
        if (compression != null) {
            throw new BuildException(
                "Only one nested compression element is permitted.",
                getLocation());
        }
        compression = new Compression();
        return compression;
    }

    /**
     * Attribute which indicates whether certain files in the linked image
     * will be big-endian or little-endian.  If {@code null}, the underlying
     * platform's endianness is used.
     *
     * @return endianness to apply, or {@code null} to platform default
     *
     * @see #setEndianness(Link.Endianness)
     */
    public Endianness getEndianness() {
        return endianness;
    }

    /**
     * Sets attribute which indicates whether certain files in the linked image
     * will be big-endian or little-endian.  If {@code null}, the underlying
     * platform's endianness is used.
     *
     * @param endianness endianness to apply, or {@code null} to use
     *                   platform default
     *
     * @see #getEndianness()
     */
    public void setEndianness(final Endianness endianness) {
        this.endianness = endianness;
    }

    /**
     * Attribute indicating whether linker should check legal notices with
     * duplicate names, and refuse to merge them (usually using symbolic links)
     * if their respective content is not identical.
     *
     * @return true if legal notice files with same name should be checked
     *         for identical content, false to suppress check
     *
     * @see #setCheckDuplicateLegal(boolean)
     */
    public boolean getCheckDuplicateLegal() {
        return checkDuplicateLegal;
    }

    /**
     * Sets attribute indicating whether linker should check legal notices with
     * duplicate names, and refuse to merge them (usually using symbolic links)
     * if their respective content is not identical.
     *
     * @param check true if legal notice files with same name should be checked
     *         for identical content, false to suppress check
     *
     * @see #getCheckDuplicateLegal()
     */
    public void setCheckDuplicateLegal(final boolean check) {
        this.checkDuplicateLegal = check;
    }

    /**
     * Attribute indicating what type of JVM the linked image should have.
     * If {@code null}, all JVM types are included.
     *
     * @return type of JVM linked image will have
     *
     * @see #setVmType(Link.VMType)
     */
    public VMType getVmType() {
        return vmType;
    }

    /**
     * Set attribute indicating what type of JVM the linked image should have.
     * If {@code null}, all JVM types are included.
     *
     * @param type type of JVM linked image will have
     *
     * @see #getVmType()
     */
    public void setVmType(final VMType type) {
        this.vmType = type;
    }

    /**
     * Creates child {@code <releaseInfo>} element that modifies the default
     * release properties of the linked image.
     *
     * @return new, unconfigured child element
     */
    public ReleaseInfo createReleaseInfo() {
        ReleaseInfo info = new ReleaseInfo();
        releaseInfo.add(info);
        return info;
    }

    /**
     * Child element that explicitly names a Java module.
     */
    public class ModuleSpec {
        /** Module's name.  Required. */
        private String name;

        /** Creates an unconfigured element. */
        public ModuleSpec() {
            // Deliberately empty.
        }

        /**
         * Creates an element with the given module name.
         *
         * @param name module's name
         */
        public ModuleSpec(final String name) {
            setName(name);
        }

        /**
         * Attribute containing name of module this element represents.
         *
         * @return name of module
         */
        public String getName() {
            return name;
        }

        /**
         * Sets attribute representing the name of this module this element
         * represents.
         *
         * @param name module's name
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * Verifies this element's state.
         *
         * @throws BuildException if name is not set
         */
        public void validate() {
            if (name == null) {
                throw new BuildException("name is required for module.",
                    getLocation());
            }
        }
    }

    /**
     * Child element that contains a pattern matching Java locales.
     */
    public class LocaleSpec {
        /** Pattern of locale names to match. */
        private String name;

        /** Creates an unconfigured element. */
        public LocaleSpec() {
            // Deliberately empty.
        }

        /**
         * Creates an element with the given name pattern.
         *
         * @param name pattern of locale names to match
         */
        public LocaleSpec(final String name) {
            setName(name);
        }

        /**
         * Attribute containing a pattern which matches Java locale names.
         * May be an explicit Java locale, or may contain an asterisk
         * ({@code *)} for wildcard matching.
         *
         * @return this element's locale name pattern
         */
        public String getName() {
            return name;
        }

        /**
         * Sets attribute containing a pattern which matches Java locale names.
         * May be an explicit Java locale, or may contain an asterisk
         * ({@code *)} for wildcard matching.
         *
         * @param name new locale name or pattern matching locale names
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * Verifies this element's state.
         *
         * @throws BuildException if name is not set
         */
        public void validate() {
            if (name == null) {
                throw new BuildException("name is required for locale.",
                    getLocation());
            }
        }
    }

    /**
     * Child element type which specifies a jlink files pattern.  Each
     * instance may specify a string
     * <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29">PathMatcher pattern</a>
     * or a text file containing a list of such patterns, one per line.
     */
    public class PatternListEntry {
        /** PathMatcher pattern of files to match. */
        private String pattern;

        /** Plain text list file with one PathMatcher pattern per line. */
        private File file;

        /** Creates an unconfigured element. */
        public PatternListEntry() {
            // Deliberately empty.
        }

        /**
         * Creates a new element from either a pattern or listing file.
         * If the argument starts with "{@code @}", the remainder of it
         * is assumed to be a listing file;  otherwise, it is treated as
         * a PathMatcher pattern.
         *
         * @param pattern a PathMatcher pattern or {@code @}-filename
         */
        public PatternListEntry(final String pattern) {
            if (pattern.startsWith("@")) {
                setListFile(new File(pattern.substring(1)));
            } else {
                setPattern(pattern);
            }
        }

        /**
         * Returns this element's PathMatcher pattern attribute, if set.
         *
         * @return this element's files pattern
         */
        public String getPattern() {
            return pattern;
        }

        /**
         * Sets this element's
         * <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29">PathMatcher pattern</a>
         * attribute for matching files.
         *
         * @param pattern new files pattern
         */
        public void setPattern(final String pattern) {
            this.pattern = pattern;
        }

        /**
         * Returns this element's list file attribute, if set.
         *
         * @return this element's list file
         *
         * @see #setListFile(File)
         */
        public File getListFile() {
            return file;
        }

        /**
         * Sets this element's list file attribute.  The file must be a
         * plain text file with one PathMatcher pattern per line.
         *
         * @param file list file containing patterns
         *
         * @see #getListFile()
         */
        public void setListFile(final File file) {
            this.file = file;
        }

        /**
         * Verifies this element's state.
         *
         * @throws BuildException if both pattern and file are set
         * @throws BuildException if neither pattern nor file is set
         */
        public void validate() {
            if ((pattern == null && file == null)
                || (pattern != null && file != null)) {

                throw new BuildException(
                    "Each entry in a pattern list must specify "
                    + "exactly one of pattern or file.", getLocation());
            }
        }

        /**
         * Converts this element to a jlink command line attribute,
         * either this element's bare pattern, or its list file
         * preceded by "{@code @}".
         *
         * @return this element's information converted to a command line value
         */
        public String toOptionValue() {
            return pattern != null ? pattern : ("@" + file);
        }
    }

    /**
     * Child element representing a custom launcher command in a linked image.
     * A launcher has a name, which is typically used as a file name for an
     * executable file, a Java module name, and optionally a class within
     * that module which can act as a standard Java main class.
     */
    public class Launcher {
        /** This launcher's name, usually used to create an executable file. */
        private String name;

        /** The name of the Java module this launcher launches. */
        private String module;

        /**
         * The class within this element's {@link #module} to run.
         * Optional if the Java module specifies its own main class.
         */
        private String mainClass;

        /** Creates a new, unconfigured element. */
        public Launcher() {
            // Deliberately empty.
        }

        /**
         * Creates a new element from a {@code jlink}-compatible string
         * specifier, which must take the form
         * <var>name</var>{@code =}<var>module</var> or
         * <var>name</var>{@code =}<var>module</var>{@code /}<var>mainclass</var>.
         *
         * @param textSpec name, module, and optional main class, as described
         *                 above
         *
         * @throws NullPointerException if argument is {@code null}
         * @throws BuildException if argument does not conform to above
         *                        requirements
         */
        public Launcher(final String textSpec) {
            Objects.requireNonNull(textSpec, "Text cannot be null");

            int equals = textSpec.lastIndexOf('=');
            if (equals < 1) {
                throw new BuildException(INVALID_LAUNCHER_STRING);
            }

            setName(textSpec.substring(0, equals));

            int slash = textSpec.indexOf('/', equals);
            if (slash < 0) {
                setModule(textSpec.substring(equals + 1));
            } else if (slash > equals + 1 && slash < textSpec.length() - 1) {
                setModule(textSpec.substring(equals + 1, slash));
                setMainClass(textSpec.substring(slash + 1));
            } else {
                throw new BuildException(INVALID_LAUNCHER_STRING);
            }
        }

        /**
         * Returns this element's name attribute, typically used as the basis
         * of an executable file name.
         *
         * @return this element's name
         *
         * @see #setName(String)
         */
        public String getName() {
            return name;
        }

        /**
         * Sets this element's name attribute, which is typically used by the
         * linker to create an executable file with a similar name.  Thus,
         * the name should contain only characters safe for file names.
         *
         * @param name name of launcher
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * Returns the attribute of this element which contains the
         * name of the Java module to execute.
         *
         * @return this element's module name
         */
        public String getModule() {
            return module;
        }

        /**
         * Sets the attribute of this element which contains the name of
         * a Java module to execute.
         *
         * @param module name of module to execute
         */
        public void setModule(final String module) {
            this.module = module;
        }

        /**
         * Returns the attribute of this element which contains the main class
         * to execute in this element's {@linkplain #getModule() module}, if
         * that module doesn't define its main class.
         *
         * @return name of main class to execute
         */
        public String getMainClass() {
            return mainClass;
        }

        /**
         * Sets the attribute which contains the main class to execute in
         * this element's {@linkplain #getModule() module}, if that module
         * doesn't define its main class.
         *
         * @param className name of class to execute
         */
        public void setMainClass(final String className) {
            this.mainClass = className;
        }

        /**
         * Verifies this element's state.
         *
         * @throws BuildException if name or module is not set
         */
        public void validate() {
            if (name == null || name.isEmpty()) {
                throw new BuildException("Launcher must have a name",
                    getLocation());
            }
            if (module == null || module.isEmpty()) {
                throw new BuildException("Launcher must have specify a module",
                    getLocation());
            }
        }

        /**
         * Returns this element's information in jlink launcher format:
         * <var>name</var>{@code =}<var>module</var> or
         * <var>name</var>{@code =}<var>module</var>{@code /}<var>mainclass</var>.
         *
         * @return name, module and optional main class in jlink format
         */
        @Override
        public String toString() {
            if (mainClass != null) {
                return name + "=" + module + "/" + mainClass;
            } else {
                return name + "=" + module;
            }
        }
    }

    /**
     * Possible values for linked image endianness:
     * {@code little} and {@code big}.
     */
    public static class Endianness
    extends EnumeratedAttribute {
        @Override
        public String[] getValues() {
            return new String[] {
                "little", "big"
            };
        }
    }

    /**
     * Possible values for JVM type in linked image:
     * {@code client}, {@code server}, {@code minimal}, or {@code all}.
     */
    public static class VMType
    extends EnumeratedAttribute {
        @Override
        public String[] getValues() {
            return new String[] {
                "client", "server", "minimal", "all"
            };
        }
    }

    /**
     * Possible attribute values for compression level of a linked image:
     * <dl>
     * <dt>{@code 0}
     * <dt>{@code none}
     * <dd>no compression (default)
     * <dt>{@code 1}
     * <dt>{@code strings}
     * <dd>constant string sharing
     * <dt>{@code 2}
     * <dt>{@code zip}
     * <dd>zip compression
     * </dl>
     */
    public static class CompressionLevel
    extends EnumeratedAttribute {
        private static final Map<String, String> KEYWORDS;

        static {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("0", "0");
            map.put("1", "1");
            map.put("2", "2");
            map.put("none", "0");
            map.put("strings", "1");
            map.put("zip", "2");

            KEYWORDS = Collections.unmodifiableMap(map);
        }

        @Override
        public String[] getValues() {
            return KEYWORDS.keySet().toArray(new String[0]);
        }

        /**
         * Converts this value to a string suitable for use in a
         * jlink command.
         *
         * @return jlink keyword corresponding to this value
         */
        String toCommandLineOption() {
            return KEYWORDS.get(getValue());
        }
    }

    /**
     * Child element fully describing compression of a linked image.
     * This includes the level, and optionally, the names of files to compress.
     */
    public class Compression {
        /** Compression level.  Required attribute. */
        private CompressionLevel level;

        /**
         * Patterns specifying files to compress.  If empty, all files are
         * compressed.
         */
        private final List<PatternListEntry> patterns = new ArrayList<>();

        /**
         * Required attribute containing level of compression.
         *
         * @return compression level
         */
        public CompressionLevel getLevel() {
            return level;
        }

        /**
         * Sets attribute indicating level of compression.
         *
         * @param level type of compression to apply to linked image
         */
        public void setLevel(final CompressionLevel level) {
            this.level = level;
        }

        /**
         * Creates a nested element which can specify a pattern of files
         * to compress.
         *
         * @return new, unconfigured child element
         */
        public PatternListEntry createFiles() {
            PatternListEntry pattern = new PatternListEntry();
            patterns.add(pattern);
            return pattern;
        }

        /**
         * Sets an attribute that represents a list of file patterns to
         * compress in the linked image, as a comma-separated list of
         * PathMatcher patterns or pattern list files.
         *
         * @param patternList comma-separated list of patterns and/or file names
         *
         * @see Link.PatternListEntry
         */
        public void setFiles(final String patternList) {
            patterns.clear();
            for (String pattern : patternList.split(",")) {
                patterns.add(new PatternListEntry(pattern));
            }
        }

        /**
         * Verifies this element's state.
         *
         * @throws BuildException if compression level is not set
         * @throws BuildException if any nested patterns are invalid
         */
        public void validate() {
            if (level == null) {
                throw new BuildException("Compression level must be specified.",
                     getLocation());
            }
            patterns.forEach(PatternListEntry::validate);
        }

        /**
         * Converts this element to a single jlink option value.
         *
         * @return command line option representing this element's state
         */
        public String toCommandLineOption() {
            StringBuilder option =
                new StringBuilder(level.toCommandLineOption());

            if (!patterns.isEmpty()) {
                String separator = ":filter=";
                for (PatternListEntry entry : patterns) {
                    option.append(separator).append(entry.toOptionValue());
                    separator = ",";
                }
            }

            return option.toString();
        }
    }

    /**
     * Grandchild element representing deletable key in a linked image's
     * release properties.
     */
    public class ReleaseInfoKey {
        /** Required attribute holding property key to delete. */
        private String key;

        /** Creates a new, unconfigured element. */
        public ReleaseInfoKey() {
            // Deliberately empty.
        }

        /**
         * Creates a new element with the specified key.
         *
         * @param key property key to delete from release info
         */
        public ReleaseInfoKey(final String key) {
            setKey(key);
        }

        /**
         * Attribute holding the release info property key to delete.
         *
         * @return property key to be deleted
         */
        public String getKey() {
            return key;
        }

        /**
         * Sets attribute containing property key to delete from
         * linked image's release info.
         *
         * @param key propert key to be deleted
         */
        public void setKey(final String key) {
            this.key = key;
        }

        /**
         * Verifies this element's state is valid.
         *
         * @throws BuildException if key is not set
         */
        public void validate() {
            if (key == null) {
                throw new BuildException(
                    "Release info key must define a 'key' attribute.",
                    getLocation());
            }
        }
    }

    /**
     * Grandchild element describing additional release info properties for a
     * linked image.  To be valid, an instance must have either a non-null
     * key and value, or a non-null file.
     */
    public class ReleaseInfoEntry {
        /** New release property's key. */
        private String key;

        /** New release property's value. */
        private String value;

        /** File containing additional release properties. */
        private File file;

        /** Charset of {@link #file}. */
        private String charset = StandardCharsets.ISO_8859_1.name();

        /** Creates a new, unconfigured element. */
        public ReleaseInfoEntry() {
            // Deliberately empty.
        }

        /**
         * Creates a new element which specifies a single additional property.
         *
         * @param key new property's key
         * @param value new property's value
         */
        public ReleaseInfoEntry(final String key,
                                final String value) {
            setKey(key);
            setValue(value);
        }

        /**
         * Attribute containing the key of this element's additional property.
         *
         * @return additional property's key
         *
         * @see #getValue()
         */
        public String getKey() {
            return key;
        }

        /**
         * Sets attribute containing the key of this element's
         * additional property.
         *
         * @param key additional property's key
         *
         * @see #setValue(String)
         */
        public void setKey(final String key) {
            this.key = key;
        }

        /**
         * Attribute containing the value of this element's additional property.
         *
         * @return additional property's value
         *
         * @see #getKey()
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets attributes containing the value of this element's
         * additional property.
         *
         * @param value additional property's value
         *
         * @see #setKey(String)
         */
        public void setValue(final String value) {
            this.value = value;
        }

        /**
         * Attribute containing a Java properties file which contains
         * additional release info properties.  This is exclusive with
         * respect to the {@linkplain #getKey() key} and
         * {@linkplain #getValue() value} of this instance:  either the
         * file must be set, or the key and value must be set.
         *
         * @return this element's properties file
         */
        public File getFile() {
            return file;
        }

        /**
         * Sets attribute containing a Java properties file which contains
         * additional release info properties.  This is exclusive with
         * respect to the {@linkplain #setKey(String) key} and
         * {@linkplain #setValue(String) value} of this instance:  either the
         * file must be set, or the key and value must be set.
         *
         * @param file this element's properties file
         */
        public void setFile(final File file) {
            this.file = file;
        }

        /**
         * Attribute containing the character set of this object's
         * {@linkplain #getFile() file}.  This is {@code ISO_8859_1}
         * by default, in accordance with the java.util.Properties default.
         *
         * @return character set of this element's file
         */
        public String getCharset() {
            return charset;
        }

        /**
         * Sets attribute containing the character set of this object's
         * {@linkplain #setFile(File) file}.  If not set, this is
         * {@code ISO_8859_1} by default, in accordance with the
         * java.util.Properties default.
         *
         * @param charset character set of this element's file
         */
        public void setCharset(final String charset) {
            this.charset = charset;
        }

        /**
         * Verifies the state of this element.
         *
         * @throws BuildException if file is set, and key and/or value are set
         * @throws BuildException if file is not set, and key and value are not both set
         * @throws BuildException if charset is not a valid Java Charset name
         */
        public void validate() {
            if (file == null && (key == null || value == null)) {
                throw new BuildException(
                    "Release info must define 'key' and 'value' attributes, "
                    + "or a 'file' attribute.", getLocation());
            }
            if (file != null && (key != null || value != null)) {
                throw new BuildException(
                    "Release info cannot define both a file attribute and "
                    + "key/value attributes.", getLocation());
            }

            // This can't happen from a build file, but can theoretically
            // happen if called from Java code.
            if (charset == null) {
                throw new BuildException("Charset cannot be null.",
                    getLocation());
            }

            try {
                Charset.forName(charset);
            } catch (IllegalArgumentException e) {
                throw new BuildException(e, getLocation());
            }
        }

        /**
         * Converts this element to a Java properties object containing
         * the additional properties this element represents.  If this
         * element's file is set, it is read;  otherwise, a Properties
         * object containing just one property, consisting of this element's
         * key and value, is returned.
         *
         * @return new Properties object obtained from this element's file or
         *         its key and value
         *
         * @throws BuildException if file is set, but cannot be read
         */
        public Properties toProperties() {
            Properties props = new Properties();
            if (file != null) {
                try (Reader reader = Files.newBufferedReader(
                    file.toPath(), Charset.forName(charset))) {

                    props.load(reader);
                } catch (IOException e) {
                    throw new BuildException(
                        "Cannot read release info file \"" + file + "\": " + e,
                        e, getLocation());
                }
            } else {
                props.setProperty(key, value);
            }

            return props;
        }
    }

    /**
     * Child element describing changes to the default release properties
     * of a linked image.
     */
    public class ReleaseInfo {
        /**
         * File that contains replacement release properties for linked image.
         */
        private File file;

        /**
         * Properties to add to default release properties of linked image.
         */
        private final List<ReleaseInfoEntry> propertiesToAdd = new ArrayList<>();

        /**
         * Property keys to remove from release properties of linked image.
         */
        private final List<ReleaseInfoKey> propertiesToDelete = new ArrayList<>();

        /**
         * Attribute specifying Java properties file which will replace the
         * default release info properties for the linked image.
         *
         * @return release properties file
         */
        public File getFile() {
            return file;
        }

        /**
         * Sets attribute specifying Java properties file which will replace
         * the default release info properties for the linked image.
         *
         * @param file replacement release properties file
         */
        public void setFile(final File file) {
            this.file = file;
        }

        /**
         * Creates an uninitialized child element which can represent properties
         * to add to the default release properties of a linked image.
         *
         * @return new, unconfigured child element
         */
        public ReleaseInfoEntry createAdd() {
            ReleaseInfoEntry property = new ReleaseInfoEntry();
            propertiesToAdd.add(property);
            return property;
        }

        /**
         * Creates an uninitialized child element which can represent
         * a property key to delete from the release properties of
         * a linked image.
         *
         * @return new, unconfigured child element
         */
        public ReleaseInfoKey createDelete() {
            ReleaseInfoKey key = new ReleaseInfoKey();
            propertiesToDelete.add(key);
            return key;
        }

        /**
         * Sets attribute which contains a comma-separated list of
         * property keys to delete from the release properties of
         * a linked image.
         *
         * @param keyList comma-separated list of property keys
         *
         * @see #createDelete()
         */
        public void setDelete(final String keyList) {
            for (String key : keyList.split(",")) {
                propertiesToDelete.add(new ReleaseInfoKey(key));
            }
        }

        /**
         * Verifies the state of this element.
         *
         * @throws BuildException if any child element is invalid
         *
         * @see Link.ReleaseInfoEntry#validate()
         * @see Link.ReleaseInfoKey#validate()
         */
        public void validate() {
            propertiesToAdd.forEach(ReleaseInfoEntry::validate);
            propertiesToDelete.forEach(ReleaseInfoKey::validate);
        }

        /**
         * Converts all of this element's state to a series of
         * <code>jlink</code> options.
         *
         * @return new collection of jlink options based on this element's
         *         attributes and child elements
         */
        public Collection<String> toCommandLineOptions() {
            Collection<String> options = new ArrayList<>();

            if (file != null) {
                options.add("--release-info=" + file);
            }
            if (!propertiesToAdd.isEmpty()) {
                StringBuilder option = new StringBuilder("--release-info=add");

                for (ReleaseInfoEntry entry : propertiesToAdd) {
                    Properties props = entry.toProperties();
                    for (String key : props.stringPropertyNames()) {
                        option.append(":").append(key).append("=");
                        option.append(props.getProperty(key));
                    }
                }

                options.add(option.toString());
            }
            if (!propertiesToDelete.isEmpty()) {
                StringBuilder option =
                    new StringBuilder("--release-info=del:keys=");

                String separator = "";
                for (ReleaseInfoKey key : propertiesToDelete) {
                    option.append(separator).append(key.getKey());
                    // jlink docs aren't clear on whether property keys
                    // to delete should be separated by commas or colons.
                    separator = ",";
                }

                options.add(option.toString());
            }

            return options;
        }
    }

    /**
     * Invokes the jlink tool to create a new linked image, unless the
     * output directory exists and all of its files are files are newer
     * than all files in the module path.
     *
     * @throws BuildException if destDir is not set
     * @throws BuildException if module path is unset or empty
     * @throws BuildException if module list is empty
     * @throws BuildException if compressionLevel attribute and compression
     *                        child element are both specified
     */
    @Override
    public void execute()
    throws BuildException {
        if (outputDir == null) {
            throw new BuildException("Destination directory is required.",
                getLocation());
        }

        if (modulePath == null || modulePath.isEmpty()) {
            throw new BuildException("Module path is required.", getLocation());
        }

        if (modules.isEmpty()) {
            throw new BuildException("At least one module must be specified.",
                getLocation());
        }

        if (outputDir.exists()) {
            CompositeMapper imageMapper = new CompositeMapper();
            try (Stream<java.nio.file.Path> imageTree =
                Files.walk(outputDir.toPath())) {

                /*
                 * Is this sufficient?  What if part of the image tree was
                 * deleted or altered?  Should we check for standard
                 * files and directories, like 'bin', 'lib', 'conf', 'legal',
                 * and 'release'?  (Some, like 'include', may not be present,
                 * if the image was previously built with options that
                 * omitted them.)
                 */
                imageTree.forEach(
                    p -> imageMapper.add(new MergingMapper(p.toString())));

                ResourceCollection outOfDate =
                    ResourceUtils.selectOutOfDateSources(this, modulePath,
                        imageMapper, getProject(),
                        FileUtils.getFileUtils().getFileTimestampGranularity());
                if (outOfDate.isEmpty()) {
                    log("Skipping image creation, since "
                        + "\"" + outputDir + "\" is already newer than "
                        + "all constituent modules.", Project.MSG_VERBOSE);
                    return;
                }
            } catch (IOException e) {
                throw new BuildException(
                    "Could not scan \"" + outputDir + "\" "
                    + "for being up-to-date: " + e, e, getLocation());
            }
        }

        modules.forEach(ModuleSpec::validate);
        observableModules.forEach(ModuleSpec::validate);
        launchers.forEach(Launcher::validate);
        locales.forEach(LocaleSpec::validate);
        ordering.forEach(PatternListEntry::validate);
        excludedFiles.forEach(PatternListEntry::validate);
        excludedResources.forEach(PatternListEntry::validate);

        Collection<String> args = buildJlinkArgs();

        ToolProvider jlink = ToolProvider.findFirst("jlink").orElseThrow(
            () -> new BuildException("jlink tool not found in JDK.",
                getLocation()));

        if (outputDir.exists()) {
            log("Deleting existing " + outputDir, Project.MSG_VERBOSE);
            deleteTree(outputDir.toPath());
        }

        log("Executing: jlink " + String.join(" ", args), Project.MSG_VERBOSE);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode;
        try (PrintStream out = new PrintStream(stdout);
             PrintStream err = new PrintStream(stderr)) {

            exitCode = jlink.run(out, err, args.toArray(new String[0]));
        }

        if (exitCode != 0) {
            StringBuilder message = new StringBuilder();
            message.append("jlink failed (exit code ").append(exitCode).append(")");
            if (stdout.size() > 0) {
                message.append(", output is: ").append(stdout);
            }
            if (stderr.size() > 0) {
                message.append(", error output is: ").append(stderr);
            }

            throw new BuildException(message.toString(), getLocation());
        }

        if (verboseLevel != null) {
            int level = verboseLevel.getLevel();

            if (stdout.size() > 0) {
                log(stdout.toString(), level);
            }
            if (stderr.size() > 0) {
                log(stderr.toString(), level);
            }
        }

        log("Created " + outputDir.getAbsolutePath(), Project.MSG_INFO);
    }

    /**
     * Recursively deletes a file tree.
     *
     * @param dir root of tree to delete
     *
     * @throws BuildException if deletion fails
     */
    private void deleteTree(java.nio.file.Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<java.nio.file.Path>() {
                @Override
                public FileVisitResult visitFile(final java.nio.file.Path file,
                                                 final BasicFileAttributes attr)
                throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final java.nio.file.Path dir,
                                                          IOException e)
                throws IOException {
                    if (e == null) {
                        Files.delete(dir);
                    }
                    return super.postVisitDirectory(dir, e);
                }
            });
        } catch (IOException e) {
            throw new BuildException(
                "Could not delete \"" + dir + "\": " + e, e, getLocation());
        }
    }

    /**
     * Creates list of arguments to <code>jlink</code> tool, based on this
     * instance's current state.
     *
     * @return new list of <code>jlink</code> arguments
     *
     * @throws BuildException if any inconsistencies attributes/elements
     *                        is found
     */
    private Collection<String> buildJlinkArgs() {
        Collection<String> args = new ArrayList<>();

        args.add("--output");
        args.add(outputDir.toString());

        args.add("--module-path");
        args.add(modulePath.toString());

        args.add("--add-modules");
        args.add(modules.stream().map(ModuleSpec::getName).collect(
            Collectors.joining(",")));

        if (!observableModules.isEmpty()) {
            args.add("--limit-modules");
            args.add(observableModules.stream().map(ModuleSpec::getName).collect(
                Collectors.joining(",")));
        }

        if (!locales.isEmpty()) {
            args.add("--include-locales="
                + locales.stream().map(LocaleSpec::getName).collect(
                    Collectors.joining(",")));
        }

        for (Launcher launcher : launchers) {
            args.add("--launcher");
            args.add(launcher.toString());
        }

        if (!ordering.isEmpty()) {
            args.add("--order-resources="
                + ordering.stream().map(PatternListEntry::toOptionValue).collect(
                    Collectors.joining(",")));
        }
        if (!excludedFiles.isEmpty()) {
            args.add("--exclude-files="
                + excludedFiles.stream().map(PatternListEntry::toOptionValue).collect(
                    Collectors.joining(",")));
        }
        if (!excludedResources.isEmpty()) {
            args.add("--exclude-resources="
                + excludedResources.stream().map(PatternListEntry::toOptionValue).collect(
                    Collectors.joining(",")));
        }

        if (bindServices) {
            args.add("--bind-services");
        }
        if (ignoreSigning) {
            args.add("--ignore-signing-information");
        }
        if (!includeHeaders) {
            args.add("--no-header-files");
        }
        if (!includeManPages) {
            args.add("--no-man-pages");
        }
        if (!includeNativeCommands) {
            args.add("--strip-native-commands");
        }
        if (!debug) {
            args.add("--strip-debug");
        }
        if (verboseLevel != null) {
            args.add("--verbose");
        }

        if (endianness != null) {
            args.add("--endian");
            args.add(endianness.getValue());
        }

        if (compressionLevel != null) {
            if (compression != null) {
                throw new BuildException("compressionLevel attribute "
                    + "and <compression> child element cannot both be present.",
                    getLocation());
            }
            args.add("--compress=" + compressionLevel.toCommandLineOption());
        }
        if (compression != null) {
            compression.validate();
            args.add("--compress=" + compression.toCommandLineOption());
        }
        if (vmType != null) {
            args.add("--vm=" + vmType.getValue());
        }
        if (checkDuplicateLegal) {
            args.add("--dedup-legal-notices=error-if-not-same-content");
        }
        for (ReleaseInfo info : releaseInfo) {
            info.validate();
            args.addAll(info.toCommandLineOptions());
        }

        return args;
    }
}
