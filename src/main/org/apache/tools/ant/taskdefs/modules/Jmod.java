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
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;

import java.nio.file.Files;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import java.util.Map;
import java.util.LinkedHashMap;

import java.util.Collections;

import java.util.spi.ToolProvider;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ResourceUtils;

import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ModuleVersion;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;

/**
 * Creates a linkable .jmod file from a modular jar file, and optionally from
 * other resource files such as native libraries and documents.  Equivalent
 * to the JDK's
 * <a href="https://docs.oracle.com/en/java/javase/11/tools/jmod.html">jmod</a>
 * tool.
 * <p>
 * Supported attributes:
 * <dl>
 * <dt>{@code destFile}
 * <dd>Required, jmod file to create.
 * <dt>{@code classpath}
 * <dt>{@code classpathref}
 * <dd>Where to locate files to be placed in the jmod file.
 * <dt>{@code modulepath}
 * <dt>{@code modulepathref}
 * <dd>Where to locate dependencies.
 * <dt>{@code commandpath}
 * <dt>{@code commandpathref}
 * <dd>Directories containing native commands to include in jmod.
 * <dt>{@code headerpath}
 * <dt>{@code headerpathref}
 * <dd>Directories containing header files to include in jmod.
 * <dt>{@code configpath}
 * <dt>{@code configpathref}
 * <dd>Directories containing user-editable configuration files
 *     to include in jmod.
 * <dt>{@code legalpath}
 * <dt>{@code legalpathref}
 * <dd>Directories containing legal licenses and notices to include in jmod.
 * <dt>{@code nativelibpath}
 * <dt>{@code nativelibpathref}
 * <dd>Directories containing native libraries to include in jmod.
 * <dt>{@code manpath}
 * <dt>{@code manpathref}
 * <dd>Directories containing man pages to include in jmod.
 * <dt>{@code version}
 * <dd>Module <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/module/ModuleDescriptor.Version.html">version</a>.
 * <dt>{@code mainclass}
 * <dd>Main class of module.
 * <dt>{@code platform}
 * <dd>The target platform for the jmod.  A particular JDK's platform
 * can be seen by running
 * <code>jmod describe $JDK_HOME/jmods/java.base.jmod | grep -i platform</code>.
 * <dt>{@code hashModulesPattern}
 * <dd>Regular expression for names of modules in the module path
 *     which depend on the jmod being created, and which should have
 *     hashes generated for them and included in the new jmod.
 * <dt>{@code resolveByDefault}
 * <dd>Boolean indicating whether the jmod should be one of
 *     the default resolved modules in an application.  Default is true.
 * <dt>{@code moduleWarnings}
 * <dd>Whether to emit warnings when resolving modules which are
 *     not recommended for use.  Comma-separated list of one of more of
 *     the following:
 *     <dl>
 *     <dt>{@code deprecated}
 *     <dd>Warn if module is deprecated
 *     <dt>{@code leaving}
 *     <dd>Warn if module is deprecated for removal
 *     <dt>{@code incubating}
 *     <dd>Warn if module is an incubating (not yet official) module
 *     </dl>
 * </dl>
 *
 * <p>
 * Supported nested elements:
 * <dl>
 * <dt>{@code <classpath>}
 * <dd>Path indicating where to locate files to be placed in the jmod file.
 * <dt>{@code <modulepath>}
 * <dd>Path indicating where to locate dependencies.
 * <dt>{@code <commandpath>}
 * <dd>Path of directories containing native commands to include in jmod.
 * <dt>{@code <headerpath>}
 * <dd>Path of directories containing header files to include in jmod.
 * <dt>{@code <configpath>}
 * <dd>Path of directories containing user-editable configuration files
 *     to include in jmod.
 * <dt>{@code <legalpath>}
 * <dd>Path of directories containing legal notices to include in jmod.
 * <dt>{@code <nativelibpath>}
 * <dd>Path of directories containing native libraries to include in jmod.
 * <dt>{@code <manpath>}
 * <dd>Path of directories containing man pages to include in jmod.
 * <dt>{@code <version>}
 * <dd><a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/module/ModuleDescriptor.Version.html">Module version</a> of jmod.
 *     Must have a required {@code number} attribute.  May also have optional
 *     {@code preRelease} and {@code build} attributes.
 * <dt>{@code <moduleWarning>}
 * <dd>Has one required attribute, {@code reason}.  See {@code moduleWarnings}
 *     attribute above.  This element may be specified multiple times.
 * </dl>
 * <p>
 * destFile and classpath are required data.
 *
 * @since 1.10.6
 */
public class Jmod
extends Task {
    /** Location of jmod file to be created. */
    private File jmodFile;

    /**
     * Path of files (usually jar files or directories containing
     * compiled classes) from which to create jmod.
     */
    private Path classpath;

    /**
     * Path of directories containing modules on which the modules
     * in the classpath depend.
     */
    private Path modulePath;

    /**
     * Path of directories containing executable files to bundle in the
     * created jmod.
     */
    private Path commandPath;

    /**
     * Path of directories containing configuration files to bundle in the
     * created jmod.
     */
    private Path configPath;

    /**
     * Path of directories containing includable header files (such as for
     * other languages) to bundle in the created jmod.
     */
    private Path headerPath;

    /**
     * Path of directories containing legal license files to bundle
     * in the created jmod.
     */
    private Path legalPath;

    /**
     * Path of directories containing native libraries needed by classes
     * in the modules comprising the created jmod.
     */
    private Path nativeLibPath;

    /**
     * Path of directories containing manual pages to bundle
     * in the created jmod.
     */
    private Path manPath;

    /**
     * Module version of jmod.  Either this or {@link #moduleVersion}
     * may be set.
     */
    private String version;

    /** Module version of jmod.  Either this or {@link #version} may be set. */
    private ModuleVersion moduleVersion;

    /**
     * Main class to execute, if Java attempts to execute jmod's module
     * without specifying a main class explicitly.
     */
    private String mainClass;

    /**
     * Target platform of created jmod.  Examples are {@code windows-amd64}
     * and {@code linux-amd64}.  Target platform is an attribute
     * of each JDK, which can be seen by executing
     * <code>jmod describe $JDK_HOME/jmods/java.base.jmod</code> and
     * searching the output for a line starting with {@code platform}.
     */
    private String platform;

    /**
     * Regular expression matching names of modules which depend on the
     * the created jmod's module, for which hashes should be added to the
     * created jmod.
     */
    private String hashModulesPattern;

    /**
     * Whether the created jmod should be seen by Java when present in a
     * module path, even if not explicitly named.  Normally true.
     */
    private boolean resolveByDefault = true;

    /**
     * Reasons why module resolution during jmod creation may emit warnings.
     */
    private final List<ResolutionWarningSpec> moduleWarnings =
        new ArrayList<>();

    /**
     * Attribute containing the location of the jmod file to create.
     *
     * @return location of jmod file
     *
     * @see #setDestFile(File)
     */
    public File getDestFile() {
        return jmodFile;
    }

    /**
     * Sets attribute containing the location of the jmod file to create.
     * This value is required.
     *
     * @param file location where jmod file will be created.
     */
    public void setDestFile(final File file) {
        this.jmodFile = file;
    }

    /**
     * Adds an unconfigured {@code <classpath>} child element which can
     * specify the files which will comprise the created jmod.
     *
     * @return new, unconfigured child element
     *
     * @see #setClasspath(Path)
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    /**
     * Attribute which specifies the files (usually modular .jar files)
     * which will comprise the created jmod file.
     *
     * @return path of constituent files
     *
     * @see #setClasspath(Path)
     */
    public Path getClasspath() {
        return classpath;
    }

    /**
     * Sets attribute specifying the files that will comprise the created jmod
     * file.  Usually this contains a single modular .jar file.
     * <p>
     * The classpath is required and must not be empty.
     *
     * @param path path of files that will comprise jmod
     *
     * @see #createClasspath()
     */
    public void setClasspath(final Path path) {
        if (classpath == null) {
            this.classpath = path;
        } else {
            classpath.append(path);
        }
    }

    /**
     * Sets {@linkplain #setClasspath(Path) classpath attribute} from a
     * path reference.
     *
     * @param ref reference to path which will act as classpath
     */
    public void setClasspathRef(final Reference ref) {
        createClasspath().setRefid(ref);
    }

    /**
     * Creates a child {@code <modulePath>} element which can contain a
     * path of directories containing modules upon which modules in the
     * {@linkplain #setClasspath(Path) classpath} depend.
     *
     * @return new, unconfigured child element
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
     * Attribute containing path of directories which contain modules on which
     * the created jmod's {@linkplain #setClasspath(Path) constituent modules}
     * depend.
     *
     * @return path of directories containing modules needed by
     *         classpath modules
     *
     * @see #setModulePath(Path)
     */
    public Path getModulePath() {
        return modulePath;
    }

    /**
     * Sets attribute containing path of directories which contain modules
     * on which the created jmod's
     * {@linkplain #setClasspath(Path) constituent modules} depend.
     *
     * @param path path of directories containing modules needed by
     *             classpath modules
     *
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
     * Sets {@linkplain #setModulePath(Path) module path}
     * from a path reference.
     *
     * @param ref reference to path which will act as module path
     */
    public void setModulePathRef(final Reference ref) {
        createModulePath().setRefid(ref);
    }

    /**
     * Creates a child element which can contain a list of directories
     * containing native executable files to include in the created jmod.
     *
     * @return new, unconfigured child element
     *
     * @see #setCommandPath(Path)
     */
    public Path createCommandPath() {
        if (commandPath == null) {
            commandPath = new Path(getProject());
        }
        return commandPath.createPath();
    }

    /**
     * Attribute containing path of directories which contain native
     * executable files to include in the created jmod.
     *
     * @return list of directories containing native executables
     *
     * @see #setCommandPath(Path)
     */
    public Path getCommandPath() {
        return commandPath;
    }

    /**
     * Sets attribute containing path of directories which contain native
     * executable files to include in the created jmod.
     *
     * @param path list of directories containing native executables
     *
     * @see #createCommandPath()
     */
    public void setCommandPath(final Path path) {
        if (commandPath == null) {
            this.commandPath = path;
        } else {
            commandPath.append(path);
        }
    }

    /**
     * Sets {@linkplain #setCommandPath(Path) command path}
     * from a path reference.
     *
     * @param ref reference to path which will act as command path
     */
    public void setCommandPathRef(final Reference ref) {
        createCommandPath().setRefid(ref);
    }

    /**
     * Creates a child element which can contain a list of directories
     * containing user configuration files to include in the created jmod.
     *
     * @return new, unconfigured child element
     *
     * @see #setConfigPath(Path)
     */
    public Path createConfigPath() {
        if (configPath == null) {
            configPath = new Path(getProject());
        }
        return configPath.createPath();
    }

    /**
     * Attribute containing list of directories which contain
     * user configuration files.
     *
     * @return list of directories containing user configuration files
     *
     * @see #setConfigPath(Path)
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Sets attribute containing list of directories which contain
     * user configuration files.
     *
     * @param path list of directories containing user configuration files
     *
     * @see #createConfigPath()
     */
    public void setConfigPath(final Path path) {
        if (configPath == null) {
            this.configPath = path;
        } else {
            configPath.append(path);
        }
    }

    /**
     * Sets {@linkplain #setConfigPath(Path) configuration file path}
     * from a path reference.
     *
     * @param ref reference to path which will act as configuration file path
     */
    public void setConfigPathRef(final Reference ref) {
        createConfigPath().setRefid(ref);
    }

    /**
     * Creates a child element which can contain a list of directories
     * containing compile-time header files for third party use, to include
     * in the created jmod.
     *
     * @return new, unconfigured child element
     *
     * @see #setHeaderPath(Path)
     */
    public Path createHeaderPath() {
        if (headerPath == null) {
            headerPath = new Path(getProject());
        }
        return headerPath.createPath();
    }

    /**
     * Attribute containing a path of directories which hold compile-time
     * header files for third party use, all of which will be included in the
     * created jmod.
     *
     * @return path of directories containing header files
     */
    public Path getHeaderPath() {
        return headerPath;
    }

    /**
     * Sets attribute containing a path of directories which hold compile-time
     * header files for third party use, all of which will be included in the
     * created jmod.
     *
     * @param path path of directories containing header files
     *
     * @see #createHeaderPath()
     */
    public void setHeaderPath(final Path path) {
        if (headerPath == null) {
            this.headerPath = path;
        } else {
            headerPath.append(path);
        }
    }

    /**
     * Sets {@linkplain #setHeaderPath(Path) header path}
     * from a path reference.
     *
     * @param ref reference to path which will act as header path
     */
    public void setHeaderPathRef(final Reference ref) {
        createHeaderPath().setRefid(ref);
    }

    /**
     * Creates a child element which can contain a list of directories
     * containing license files to include in the created jmod.
     *
     * @return new, unconfigured child element
     *
     * @see #setLegalPath(Path)
     */
    public Path createLegalPath() {
        if (legalPath == null) {
            legalPath = new Path(getProject());
        }
        return legalPath.createPath();
    }

    /**
     * Attribute containing list of directories which hold license files
     * to include in the created jmod.
     *
     * @return path containing directories which hold license files
     */
    public Path getLegalPath() {
        return legalPath;
    }

    /**
     * Sets attribute containing list of directories which hold license files
     * to include in the created jmod.
     *
     * @param path path containing directories which hold license files
     *
     * @see #createLegalPath()
     */
    public void setLegalPath(final Path path) {
        if (legalPath == null) {
            this.legalPath = path;
        } else {
            legalPath.append(path);
        }
    }

    /**
     * Sets {@linkplain #setLegalPath(Path) legal licenses path}
     * from a path reference.
     *
     * @param ref reference to path which will act as legal path
     */
    public void setLegalPathRef(final Reference ref) {
        createLegalPath().setRefid(ref);
    }

    /**
     * Creates a child element which can contain a list of directories
     * containing native libraries to include in the created jmod.
     *
     * @return new, unconfigured child element
     *
     * @see #setNativeLibPath(Path)
     */
    public Path createNativeLibPath() {
        if (nativeLibPath == null) {
            nativeLibPath = new Path(getProject());
        }
        return nativeLibPath.createPath();
    }

    /**
     * Attribute containing list of directories which hold native libraries
     * to include in the created jmod.
     *
     * @return path of directories containing native libraries
     */
    public Path getNativeLibPath() {
        return nativeLibPath;
    }

    /**
     * Sets attribute containing list of directories which hold native libraries
     * to include in the created jmod.
     *
     * @param path path of directories containing native libraries
     *
     * @see #createNativeLibPath()
     */
    public void setNativeLibPath(final Path path) {
        if (nativeLibPath == null) {
            this.nativeLibPath = path;
        } else {
            nativeLibPath.append(path);
        }
    }

    /**
     * Sets {@linkplain #setNativeLibPath(Path) native library path}
     * from a path reference.
     *
     * @param ref reference to path which will act as native library path
     */
    public void setNativeLibPathRef(final Reference ref) {
        createNativeLibPath().setRefid(ref);
    }

    /**
     * Creates a child element which can contain a list of directories
     * containing man pages (program manuals, typically in troff format)
     * to include in the created jmod.
     *
     * @return new, unconfigured child element
     *
     * @see #setManPath(Path)
     */
    public Path createManPath() {
        if (manPath == null) {
            manPath = new Path(getProject());
        }
        return manPath.createPath();
    }

    /**
     * Attribute containing list of directories containing man pages
     * to include in created jmod.  Man pages are textual program manuals,
     * typically in troff format.
     *
     * @return path containing directories which hold man pages to include
     *         in jmod
     */
    public Path getManPath() {
        return manPath;
    }

    /**
     * Sets attribute containing list of directories containing man pages
     * to include in created jmod.  Man pages are textual program manuals,
     * typically in troff format.
     *
     * @param path path containing directories which hold man pages to include
     *             in jmod
     *
     * @see #createManPath()
     */
    public void setManPath(final Path path) {
        if (manPath == null) {
            this.manPath = path;
        } else {
            manPath.append(path);
        }
    }

    /**
     * Sets {@linkplain #setManPath(Path) man pages path}
     * from a path reference.
     *
     * @param ref reference to path which will act as module path
     */
    public void setManPathRef(final Reference ref) {
        createManPath().setRefid(ref);
    }

    /**
     * Creates an uninitialized child element representing the version of
     * the module represented by the created jmod.
     *
     * @return new, unconfigured child element
     *
     * @see #setVersion(String)
     */
    public ModuleVersion createVersion() {
        if (moduleVersion != null) {
            throw new BuildException(
                "No more than one <moduleVersion> element is allowed.",
                getLocation());
        }
        moduleVersion = new ModuleVersion();
        return moduleVersion;
    }

    /**
     * Attribute which specifies
     * a <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/module/ModuleDescriptor.Version.html">module version</a>
     * for created jmod.
     *
     * @return module version for created jmod
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/module/ModuleDescriptor.Version.html">module version</a>
     * for the created jmod.
     *
     * @param version module version of created jmod
     *
     * @see #createVersion()
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Attribute containing the class that acts as the executable entry point
     * of the created jmod.
     *
     * @return fully-qualified name of jmod's main class
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Sets attribute containing the class that acts as the
     * executable entry point of the created jmod.
     *
     * @param className fully-qualified name of jmod's main class
     */
    public void setMainClass(final String className) {
        this.mainClass = className;
    }

    /**
     * Attribute containing the platform for which the jmod
     * will be built.  Platform values are defined in the
     * {@code java.base.jmod} of JDKs, and usually take the form
     * <var>OS</var>{@code -}<var>architecture</var>.  If unset,
     * current platform is used.
     *
     * @return OS and architecture for which jmod will be built, or {@code null}
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * Sets attribute containing the platform for which the jmod
     * will be built.  Platform values are defined in the
     * {@code java.base.jmod} of JDKs, and usually take the form
     * <var>OS</var>{@code -}<var>architecture</var>.  If unset,
     * current platform is used.
     * <p>
     * A JDK's platform can be viewed with a command like:
     * <code>jmod describe $JDK_HOME/jmods/java.base.jmod | grep -i platform</code>.
o    *
     * @param platform platform for which jmod will be created, or {@code null}
     */
    public void setPlatform(final String platform) {
        this.platform = platform;
    }

    /**
     * Attribute containing a regular expression which specifies which
     * of the modules that depend on the jmod being created should have
     * hashes generated and added to the jmod.
     *
     * @return regex specifying which dependent modules should have
     *         their generated hashes included
     */
    public String getHashModulesPattern() {
        return hashModulesPattern;
    }

    /**
     * Sets attribute containing a regular expression which specifies which
     * of the modules that depend on the jmod being created should have
     * hashes generated and added to the jmod.
     *
     * @param pattern regex specifying which dependent modules should have
     *         their generated hashes included
     */
    public void setHashModulesPattern(final String pattern) {
        this.hashModulesPattern = pattern;
    }

    /**
     * Attribute indicating whether the created jmod should be visible
     * in a module path, even when not specified explicitly.  True by default.
     *
     * @return whether jmod should be visible in module paths
     */
    public boolean getResolveByDefault() {
        return resolveByDefault;
    }

    /**
     * Sets attribute indicating whether the created jmod should be visible
     * in a module path, even when not specified explicitly.  True by default.
     *
     * @param resolve whether jmod should be visible in module paths
     */
    public void setResolveByDefault(final boolean resolve) {
        this.resolveByDefault = resolve;
    }

    /**
     * Creates a child element which can specify the circumstances
     * under which jmod creation emits warnings.
     *
     * @return new, unconfigured child element
     *
     * @see #setModuleWarnings(String)
     */
    public ResolutionWarningSpec createModuleWarning() {
        ResolutionWarningSpec warningSpec = new ResolutionWarningSpec();
        moduleWarnings.add(warningSpec);
        return warningSpec;
    }

    /**
     * Sets attribute containing a comma-separated list of reasons for
     * jmod creation to emit warnings.  Valid values in list are:
     * {@code deprecated}, {@code leaving}, {@code incubating}.
     *
     * @param warningList list containing one or more of the above values,
     *                    separated by commas
     *
     * @see #createModuleWarning()
     * @see Jmod.ResolutionWarningReason
     */
    public void setModuleWarnings(final String warningList) {
        for (String warning : warningList.split(",")) {
            moduleWarnings.add(new ResolutionWarningSpec(warning));
        }
    }

    /**
     * Permissible reasons for jmod creation to emit warnings.
     */
    public static class ResolutionWarningReason
    extends EnumeratedAttribute {
        /**
         * String value indicating warnings are emitted for modules
         * marked as deprecated (but not deprecated for removal).
         */
        public static final String DEPRECATED = "deprecated";

        /**
         * String value indicating warnings are emitted for modules
         * marked as deprecated for removal.
         */
        public static final String LEAVING = "leaving";

        /**
         * String value indicating warnings are emitted for modules
         * designated as "incubating" in the JDK.
         */
        public static final String INCUBATING = "incubating";

        /** Maps Ant task values to jmod option values. */
        private static final Map<String, String> VALUES_TO_OPTIONS;

        static {
            Map<String, String> map = new LinkedHashMap<>();
            map.put(DEPRECATED, "deprecated");
            map.put(LEAVING,    "deprecated-for-removal");
            map.put(INCUBATING, "incubating");

            VALUES_TO_OPTIONS = Collections.unmodifiableMap(map);
        }

        @Override
        public String[] getValues() {
            return VALUES_TO_OPTIONS.keySet().toArray(new String[0]);
        }

        /**
         * Converts this object's current value to a jmod tool
         * option value.
         *
         * @return jmod option value
         */
        String toCommandLineOption() {
            return VALUES_TO_OPTIONS.get(getValue());
        }

        /**
         * Converts a string to a {@code ResolutionWarningReason} instance.
         *
         * @param s string to convert
         *
         * @return {@code ResolutionWarningReason} instance corresponding to
         *         string argument
         *
         * @throws BuildException if argument is not a valid
         *                        {@code ResolutionWarningReason} value
         */
        public static ResolutionWarningReason valueOf(String s) {
            return (ResolutionWarningReason)
                getInstance(ResolutionWarningReason.class, s);
        }
    }

    /**
     * Child element which enables jmod tool warnings.  'reason' attribute
     * is required.
     */
    public class ResolutionWarningSpec {
        /** Condition which should trigger jmod warning output. */
        private ResolutionWarningReason reason;

        /**
         * Creates an uninitialized element.
         */
        public ResolutionWarningSpec() {
            // Deliberately empty.
        }

        /**
         * Creates an element with the given reason attribute.
         *
         * @param reason non{@code null} {@link Jmod.ResolutionWarningReason}
         *               value
         *
         * @throws BuildException if argument is not a valid
         *                        {@code ResolutionWarningReason}
         */
        public ResolutionWarningSpec(String reason) {
            setReason(ResolutionWarningReason.valueOf(reason));
        }

        /**
         * Required attribute containing reason for emitting jmod warnings.
         *
         * @return condition which triggers jmod warnings
         */
        public ResolutionWarningReason getReason() {
            return reason;
        }

        /**
         * Sets attribute containing reason for emitting jmod warnings.
         *
         * @param reason condition which triggers jmod warnings
         */
        public void setReason(ResolutionWarningReason reason) {
            this.reason = reason;
        }

        /**
         * Verifies this object's state.
         *
         * @throws BuildException if this object's reason is {@code null}
         */
        public void validate() {
            if (reason == null) {
                throw new BuildException("reason attribute is required",
                    getLocation());
            }
        }
    }

    /**
     * Checks whether a resource is a directory.  Used for checking validity
     * of jmod path arguments which have to be directories.
     *
     * @param resource resource to check
     *
     * @return true if resource exists and is not a directory,
     *         false if it is a directory or does not exist
     */
    private static boolean isRegularFile(Resource resource) {
        return resource.isExists() && !resource.isDirectory();
    }

    /**
     * Checks that all paths which are required to be directories only,
     * refer only to directories.
     *
     * @throws BuildException if any path has an existing file
     *                        which is a non-directory
     */
    private void checkDirPaths() {
        if (modulePath != null
            && modulePath.stream().anyMatch(Jmod::isRegularFile)) {

            throw new BuildException(
                "ModulePath must contain only directories.", getLocation());
        }
        if (commandPath != null
            && commandPath.stream().anyMatch(Jmod::isRegularFile)) {

            throw new BuildException(
                "CommandPath must contain only directories.", getLocation());
        }
        if (configPath != null
            && configPath.stream().anyMatch(Jmod::isRegularFile)) {

            throw new BuildException(
                "ConfigPath must contain only directories.", getLocation());
        }
        if (headerPath != null
            && headerPath.stream().anyMatch(Jmod::isRegularFile)) {

            throw new BuildException(
                "HeaderPath must contain only directories.", getLocation());
        }
        if (legalPath != null
            && legalPath.stream().anyMatch(Jmod::isRegularFile)) {

            throw new BuildException(
                "LegalPath must contain only directories.", getLocation());
        }
        if (nativeLibPath != null
            && nativeLibPath.stream().anyMatch(Jmod::isRegularFile)) {

            throw new BuildException(
                "NativeLibPath must contain only directories.", getLocation());
        }
        if (manPath != null
            && manPath.stream().anyMatch(Jmod::isRegularFile)) {

            throw new BuildException(
                "ManPath must contain only directories.", getLocation());
        }
    }

    /**
     * Creates a jmod file according to this task's properties
     * and child elements.
     *
     * @throws BuildException if destFile is not set
     * @throws BuildException if classpath is not set or is empty
     * @throws BuildException if any path other than classpath refers to an
     *                        existing file which is not a directory
     * @throws BuildException if both {@code version} attribute and
     *                        {@code <version>} child element are present
     * @throws BuildException if {@code hashModulesPattern} is set, but
     *                        module path is not defined
     */
    @Override
    public void execute()
    throws BuildException {

        if (jmodFile == null) {
            throw new BuildException("Destination file is required.",
                getLocation());
        }

        if (classpath == null) {
            throw new BuildException("Classpath is required.",
                getLocation());
        }

        if (classpath.stream().noneMatch(Resource::isExists)) {
            throw new BuildException(
                "Classpath must contain at least one entry which exists.",
                getLocation());
        }

        if (version != null && moduleVersion != null) {
            throw new BuildException(
                "version attribute and nested <version> element "
                + "cannot both be present.",
                getLocation());
        }

        if (hashModulesPattern != null && !hashModulesPattern.isEmpty()
            && modulePath == null) {

            throw new BuildException(
                "hashModulesPattern requires a module path, since "
                + "it will generate hashes of the other modules which depend "
                + "on the module being created.",
                getLocation());
        }

        checkDirPaths();

        Path[] dependentPaths = {
            classpath,
            modulePath,
            commandPath,
            configPath,
            headerPath,
            legalPath,
            nativeLibPath,
            manPath,
        };
        Union allResources = new Union(getProject());
        for (Path path : dependentPaths) {
            if (path != null) {
                for (String entry : path.list()) {
                    File entryFile = new File(entry);
                    if (entryFile.isDirectory()) {
                        log("Will compare timestamp of all files in "
                            + "\"" + entryFile + "\" with timestamp of "
                            + jmodFile, Project.MSG_VERBOSE);
                        FileSet fileSet = new FileSet();
                        fileSet.setDir(entryFile);
                        allResources.add(fileSet);
                    } else {
                        log("Will compare timestamp of \"" + entryFile + "\" "
                            + "with timestamp of " + jmodFile,
                            Project.MSG_VERBOSE);
                        allResources.add(new FileResource(entryFile));
                    }
                }
            }
        }

        ResourceCollection outOfDate =
            ResourceUtils.selectOutOfDateSources(this, allResources,
                new MergingMapper(jmodFile.toString()),
                getProject(),
                FileUtils.getFileUtils().getFileTimestampGranularity());

        if (outOfDate.isEmpty()) {
            log("Skipping jmod creation, since \"" + jmodFile + "\" "
                + "is already newer than all files in paths.",
                Project.MSG_VERBOSE);
            return;
        }

        Collection<String> args = buildJmodArgs();

        try {
            log("Deleting " + jmodFile + " if it exists.", Project.MSG_VERBOSE);
            Files.deleteIfExists(jmodFile.toPath());
        } catch (IOException e) {
            throw new BuildException(
                "Could not remove old file \"" + jmodFile + "\": " + e, e,
                getLocation());
        }

        ToolProvider jmod = ToolProvider.findFirst("jmod").orElseThrow(
            () -> new BuildException("jmod tool not found in JDK.",
                getLocation()));

        log("Executing: jmod " + String.join(" ", args), Project.MSG_VERBOSE);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode;
        try (PrintStream out = new PrintStream(stdout);
             PrintStream err = new PrintStream(stderr)) {

            exitCode = jmod.run(out, err, args.toArray(new String[0]));
        }

        if (exitCode != 0) {
            StringBuilder message = new StringBuilder();
            message.append("jmod failed (exit code ").append(exitCode).append(")");
            if (stdout.size() > 0) {
                message.append(", output is: ").append(stdout);
            }
            if (stderr.size() > 0) {
                message.append(", error output is: ").append(stderr);
            }

            throw new BuildException(message.toString(), getLocation());
        }

        log("Created " + jmodFile.getAbsolutePath(), Project.MSG_INFO);
    }

    /**
     * Creates list of arguments to <code>jmod</code> tool, based on this
     * instance's current state.
     *
     * @return new list of <code>jmod</code> arguments
     */
    private Collection<String> buildJmodArgs() {
        Collection<String> args = new ArrayList<>();

        args.add("create");

        args.add("--class-path");
        args.add(classpath.toString());

        // Paths

        if (modulePath != null && !modulePath.isEmpty()) {
            args.add("--module-path");
            args.add(modulePath.toString());
        }
        if (commandPath != null && !commandPath.isEmpty()) {
            args.add("--cmds");
            args.add(commandPath.toString());
        }
        if (configPath != null && !configPath.isEmpty()) {
            args.add("--config");
            args.add(configPath.toString());
        }
        if (headerPath != null && !headerPath.isEmpty()) {
            args.add("--header-files");
            args.add(headerPath.toString());
        }
        if (legalPath != null && !legalPath.isEmpty()) {
            args.add("--legal-notices");
            args.add(legalPath.toString());
        }
        if (nativeLibPath != null && !nativeLibPath.isEmpty()) {
            args.add("--libs");
            args.add(nativeLibPath.toString());
        }
        if (manPath != null && !manPath.isEmpty()) {
            args.add("--man-pages");
            args.add(manPath.toString());
        }

        // Strings

        String versionStr =
            (moduleVersion != null ? moduleVersion.toModuleVersionString() : version);
        if (versionStr != null && !versionStr.isEmpty()) {
            args.add("--module-version");
            args.add(versionStr);
        }

        if (mainClass != null && !mainClass.isEmpty()) {
            args.add("--main-class");
            args.add(mainClass);
        }
        if (platform != null && !platform.isEmpty()) {
            args.add("--target-platform");
            args.add(platform);
        }
        if (hashModulesPattern != null && !hashModulesPattern.isEmpty()) {
            args.add("--hash-modules");
            args.add(hashModulesPattern);
        }

        // booleans

        if (!resolveByDefault) {
            args.add("--do-not-resolve-by-default");
        }
        for (ResolutionWarningSpec moduleWarning : moduleWarnings) {
            moduleWarning.validate();
            args.add("--warn-if-resolved");
            args.add(moduleWarning.getReason().toCommandLineOption());
        }

        // Destination file

        args.add(jmodFile.toString());

        return args;
    }
}
