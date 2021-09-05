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
package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Generates Javadoc documentation for a collection
 * of source code.
 *
 * <p>Current known limitations are:</p>
 *
 * <ul>
 *    <li>patterns must be of the form "xxx.*", every other pattern doesn't
 *        work.
 *    <li>there is no control on arguments sanity since they are left
 *        to the Javadoc implementation.
 * </ul>
 *
 * <p>If no <code>doclet</code> is set, then the <code>version</code> and
 * <code>author</code> are by default <code>"yes"</code>.</p>
 *
 * <p>Note: This task is run on another VM because the Javadoc code calls
 * <code>System.exit()</code> which would break Ant functionality.</p>
 *
 * @since Ant 1.1
 *
 * @ant.task category="java"
 */
public class Javadoc extends Task {

    private static final String LOAD_FRAME = "function loadFrames() {";
    private static final int LOAD_FRAME_LEN = LOAD_FRAME.length();

    /**
     * Inner class used to manage doclet parameters.
     */
    public class DocletParam {
        /** The parameter name */
        private String name;

        /** The parameter value */
        private String value;

        /**
         * Set the name of the parameter.
         *
         * @param name the name of the doclet parameter
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * Get the parameter name.
         *
         * @return the parameter's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Set the parameter value.
         *
         * Note that only string values are supported. No resolution of file
         * paths is performed.
         *
         * @param value the parameter value.
         */
        public void setValue(final String value) {
            this.value = value;
        }

        /**
         * Get the parameter value.
         *
         * @return the parameter value.
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * A project aware class used for Javadoc extensions which take a name
     * and a path such as doclet and taglet arguments.
     *
     */
    public static class ExtensionInfo extends ProjectComponent {
        /** The name of the extension */
        private String name;

        /** The optional path to use to load the extension */
        private Path path;

        /**
         * Set the name of the extension
         *
         * @param name the extension's name.
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * Get the name of the extension.
         *
         * @return the extension's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Set the path to use when loading the component.
         *
         * @param path a Path instance containing the classpath to use.
         */
        public void setPath(final Path path) {
            if (this.path == null) {
                this.path = path;
            } else {
                this.path.append(path);
            }
        }

        /**
         * Get the extension's path.
         *
         * @return the path to be used to load the extension.
         * May be <code>null</code>
         */
        public Path getPath() {
            return path;
        }

        /**
         * Create an empty nested path to be configured by Ant with the
         * classpath for the extension.
         *
         * @return a new Path instance to be configured.
         */
        public Path createPath() {
            if (path == null) {
                path = new Path(getProject());
            }
            return path.createPath();
        }

        /**
         * Adds a reference to a CLASSPATH defined elsewhere.
         *
         * @param r the reference containing the path.
         */
        public void setPathRef(final Reference r) {
            createPath().setRefid(r);
        }
    }

    /**
     * This class stores info about doclets.
     *
     */
    public class DocletInfo extends ExtensionInfo {

        /** Collection of doclet parameters. */
        private final List<DocletParam> params = new Vector<>();

        /**
         * Create a doclet parameter to be configured by Ant.
         *
         * @return a new DocletParam instance to be configured.
         */
        public DocletParam createParam() {
            final DocletParam param = new DocletParam();
            params.add(param);
            return param;
        }

        /**
         * Get the doclet's parameters.
         *
         * @return an Enumeration of DocletParam instances.
         */
        public Enumeration<DocletParam> getParams() {
            return Collections.enumeration(params);
        }
    }

    /**
     * Used to track info about the packages to be javadoc'd
     */
    public static class PackageName {
        /** The package name */
        private String name;

        /**
         * Set the name of the package
         *
         * @param name the package name.
         */
        public void setName(final String name) {
            this.name = name.trim();
        }

        /**
         * Get the package name.
         *
         * @return the package's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Return a string rep for this object.
         * @return the package name.
         */
        @Override
        public String toString() {
            return getName();
        }
    }

    /**
     * This class is used to manage the source files to be processed.
     */
    public static class SourceFile {
        /** The source file */
        private File file;

        /**
         * Default constructor
         */
        public SourceFile() {
            //empty
        }

        /**
         * Constructor specifying the source file directly
         *
         * @param file the source file
         */
        public SourceFile(final File file) {
            this.file = file;
        }

        /**
         * Set the source file.
         *
         * @param file the source file.
         */
        public void setFile(final File file) {
            this.file = file;
        }

        /**
         * Get the source file.
         *
         * @return the source file.
         */
        public File getFile() {
            return file;
        }
    }

    /**
     * An HTML element in the Javadoc.
     *
     * This class is used for those Javadoc elements which contain HTML such as
     * footers, headers, etc.
     */
    public static class Html {
        /** The text for the element */
        private final StringBuffer text = new StringBuffer();

        /**
         * Add text to the element.
         *
         * @param t the text to be added.
         */
        public void addText(final String t) {
            text.append(t);
        }

        /**
         * Get the current text for the element.
         *
         * @return the current text.
         */
        public String getText() {
            return text.substring(0);
        }
    }

    /**
     * EnumeratedAttribute implementation supporting the Javadoc scoping
     * values.
     */
    public static class AccessType extends EnumeratedAttribute {
        /**
         * @return the allowed values for the access type.
         */
        @Override
        public String[] getValues() {
            // Protected first so if any GUI tool offers a default
            // based on enum #0, it will be right.
            return new String[] {"protected", "public", "package", "private"};
        }
    }

    /**
     * Holds a collection of ResourceCollections.
     *
     * <p>A separate kind of container is needed since this task
     * contains special handling for FileSets that has to occur at
     * task runtime.</p>
     */
    public class ResourceCollectionContainer
        implements Iterable<ResourceCollection> {

        private final List<ResourceCollection> rcs = new ArrayList<>();

        /**
         * Add a resource collection to the container.
         * @param rc the collection to add.
         */
        public void add(final ResourceCollection rc) {
            rcs.add(rc);
        }

        /**
         * Get an iterator on the collection.
         * @return an iterator.
         */
        @Override
        public Iterator<ResourceCollection> iterator() {
            return rcs.iterator();
        }
    }

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** The command line built to execute Javadoc. */
    private final Commandline cmd = new Commandline();

    /**
     * Utility method to add an argument to the command line conditionally
     * based on the given flag.
     *
     * @param b the flag which controls if the argument is added.
     * @param arg the argument value.
     */
    private void addArgIf(final boolean b, final String arg) {
        if (b) {
            cmd.createArgument().setValue(arg);
        }
    }

    /**
     * Utility method to add a Javadoc argument.
     *
     * @param key the argument name.
     * @param value the argument value.
     */
    private void addArgIfNotEmpty(final String key, final String value) {
        if (value == null || value.isEmpty()) {
            log("Warning: Leaving out empty argument '" + key + "'",
                Project.MSG_WARN);
        } else {
            cmd.createArgument().setValue(key);
            cmd.createArgument().setValue(value);
        }
    }

    /**
     * Flag which indicates if the task should fail if there is a
     * Javadoc error.
     */
    private boolean failOnError = false;
    /**
     * Flag which indicates if the task should fail if there is a
     * Javadoc warning.
     */
    private boolean failOnWarning = false;
    private Path sourcePath = null;
    private File destDir = null;
    private final List<SourceFile> sourceFiles = new Vector<>();
    private final List<PackageName> packageNames = new Vector<>();
    private final List<PackageName> excludePackageNames = new Vector<>(1);
    private final List<PackageName> moduleNames = new ArrayList<>();
    private boolean author = true;
    private boolean version = true;
    private DocletInfo doclet = null;
    private Path classpath = null;
    private Path bootclasspath = null;
    private Path modulePath = null;
    private Path moduleSourcePath = null;
    private String group = null;
    private String packageList = null;
    private final List<LinkArgument> links = new Vector<>();
    private final List<GroupArgument> groups = new Vector<>();
    private final List<Object> tags = new Vector<>();
    private boolean useDefaultExcludes = true;
    private Html doctitle = null;
    private Html header = null;
    private Html footer = null;
    private Html bottom = null;
    private boolean useExternalFile = false;
    private String source = null;
    private boolean linksource = false;
    private boolean breakiterator = false;
    private String noqualifier;
    private boolean includeNoSourcePackages = false;
    private String executable = null;
    private boolean docFilesSubDirs = false;
    private String excludeDocFilesSubDir = null;
    private String docEncoding = null;
    private boolean postProcessGeneratedJavadocs = true;

    private final ResourceCollectionContainer nestedSourceFiles
        = new ResourceCollectionContainer();
    private final List<DirSet> packageSets = new Vector<>();

    /**
     * Work around command line length limit by using an external file
     * for the sourcefiles.
     *
     * @param b true if an external file is to be used.
     */
    public void setUseExternalFile(final boolean b) {
        useExternalFile = b;
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *                           should be used, "false"|"off"|"no" when they
     *                           shouldn't be used.
     */
    public void setDefaultexcludes(final boolean useDefaultExcludes) {
        this.useDefaultExcludes = useDefaultExcludes;
    }

    /**
     * Set the maximum memory to be used by the javadoc process
     *
     * @param max a string indicating the maximum memory according to the
     *        JVM conventions (e.g. 128m is 128 Megabytes)
     */
    public void setMaxmemory(final String max) {
        cmd.createArgument().setValue("-J-Xmx" + max);
    }

    /**
     * Set an additional parameter on the command line
     *
     * @param add the additional command line parameter for the javadoc task.
     */
    public void setAdditionalparam(final String add) {
        cmd.createArgument().setLine(add);
    }

    /**
     * Adds a command-line argument.
     * @return a command-line argument to configure
     * @since Ant 1.6
     */
    public Commandline.Argument createArg() {
        return cmd.createArgument();
    }

    /**
     * Specify where to find source file
     *
     * @param src a Path instance containing the various source directories.
     */
    public void setSourcepath(final Path src) {
        if (sourcePath == null) {
            sourcePath = src;
        } else {
            sourcePath.append(src);
        }
    }

    /**
     * Create a path to be configured with the locations of the source
     * files.
     *
     * @return a new Path instance to be configured by the Ant core.
     */
    public Path createSourcepath() {
        if (sourcePath == null) {
            sourcePath = new Path(getProject());
        }
        return sourcePath.createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     *
     * @param r the reference containing the source path definition.
     */
    public void setSourcepathRef(final Reference r) {
        createSourcepath().setRefid(r);
    }

    /**
     * Specify where to find modules
     *
     * @param mp a Path instance containing the modules.
     *
     * @since Ant 1.10.6
     */
    public void setModulePath(final Path mp) {
        if (modulePath == null) {
            modulePath = mp;
        } else {
            modulePath.append(mp);
        }
    }

    /**
     * Create a path to be configured with the locations of the module
     * files.
     *
     * @return a new Path instance to be configured by the Ant core.
     *
     * @since Ant 1.10.6
     */
    public Path createModulePath() {
        if (modulePath == null) {
            modulePath = new Path(getProject());
        }
        return modulePath.createPath();
    }

    /**
     * Adds a reference to a path defined elsewhere that defines the module path.
     *
     * @param r the reference containing the module path definition.
     *
     * @since Ant 1.10.6
     */
    public void setModulePathref(final Reference r) {
        createModulePath().setRefid(r);
    }

    /**
     * Specify where to find sources for modules
     *
     * @param mp a Path instance containing the sources for modules.
     *
     * @since Ant 1.10.6
     */
    public void setModuleSourcePath(final Path mp) {
        if (moduleSourcePath == null) {
            moduleSourcePath = mp;
        } else {
            moduleSourcePath.append(mp);
        }
    }

    /**
     * Create a path to be configured with the locations of the module
     * source files.
     *
     * @return a new Path instance to be configured by the Ant core.
     *
     * @since Ant 1.10.6
     */
    public Path createModuleSourcePath() {
        if (moduleSourcePath == null) {
            moduleSourcePath = new Path(getProject());
        }
        return moduleSourcePath.createPath();
    }

    /**
     * Adds a reference to a path defined elsewhere that defines the module source path.
     *
     * @param r the reference containing the module source path definition.
     *
     * @since Ant 1.10.6
     */
    public void setModuleSourcePathref(final Reference r) {
        createModuleSourcePath().setRefid(r);
    }

    /**
     * Set the directory where the Javadoc output will be generated.
     *
     * @param dir the destination directory.
     */
    public void setDestdir(final File dir) {
        destDir = dir;
        cmd.createArgument().setValue("-d");
        cmd.createArgument().setFile(destDir);
    }

    /**
     * Set the list of source files to process.
     *
     * @param src a comma separated list of source files.
     */
    public void setSourcefiles(final String src) {
        final StringTokenizer tok = new StringTokenizer(src, ",");
        while (tok.hasMoreTokens()) {
            final String f = tok.nextToken();
            final SourceFile sf = new SourceFile();
            sf.setFile(getProject().resolveFile(f.trim()));
            addSource(sf);
        }
    }

    /**
     * Add a single source file.
     *
     * @param sf the source file to be processed.
     */
    public void addSource(final SourceFile sf) {
        sourceFiles.add(sf);
    }

    /**
     * Set the package names to be processed.
     *
     * @param packages a comma separated list of packages specs
     *        (may be wildcarded).
     *
     * @see #addPackage for wildcard information.
     */
    public void setPackagenames(final String packages) {
        final StringTokenizer tok = new StringTokenizer(packages, ",");
        while (tok.hasMoreTokens()) {
            final String p = tok.nextToken();
            final PackageName pn = new PackageName();
            pn.setName(p);
            addPackage(pn);
        }
    }

    /**
     * Set the module names to be processed.
     *
     * @param modules a comma separated list of module names
     *
     * @since Ant 1.10.6
     */
    public void setModulenames(final String modules) {
        for (String m : modules.split(",")) {
            final PackageName mn = new PackageName();
            mn.setName(m);
            addModule(mn);
        }
    }

    /**
     * Add a single package to be processed.
     *
     * If the package name ends with &quot;.*&quot; the Javadoc task
     * will find and process all subpackages.
     *
     * @param pn the package name, possibly wildcarded.
     */
    public void addPackage(final PackageName pn) {
        packageNames.add(pn);
    }

    /**
     * Add a single module to be processed.
     *
     * @param mn the module name
     *
     * @since Ant 1.10.6
     */
    public void addModule(final PackageName mn) {
        moduleNames.add(mn);
    }

    /**
     * Set the list of packages to be excluded.
     *
     * @param packages a comma separated list of packages to be excluded.
     *        This may not include wildcards.
     */
    public void setExcludePackageNames(final String packages) {
        final StringTokenizer tok = new StringTokenizer(packages, ",");
        while (tok.hasMoreTokens()) {
            final String p = tok.nextToken();
            final PackageName pn = new PackageName();
            pn.setName(p);
            addExcludePackage(pn);
        }
    }

    /**
     * Add a package to be excluded from the Javadoc run.
     *
     * @param pn the name of the package (wildcards are not permitted).
     */
    public void addExcludePackage(final PackageName pn) {
        excludePackageNames.add(pn);
    }

    /**
     * Specify the file containing the overview to be included in the generated
     * documentation.
     *
     * @param f the file containing the overview.
     */
    public void setOverview(final File f) {
        cmd.createArgument().setValue("-overview");
        cmd.createArgument().setFile(f);
    }

    /**
     * Indicate whether only public classes and members are to be included in
     * the scope processed
     *
     * @param b true if scope is to be public.
     */
    public void setPublic(final boolean b) {
        addArgIf(b, "-public");
    }

    /**
     * Indicate whether only protected and public classes and members are to
     * be included in the scope processed
     *
     * @param b true if scope is to be protected.
     */
    public void setProtected(final boolean b) {
        addArgIf(b, "-protected");
    }

    /**
     * Indicate whether only package, protected and public classes and
     * members are to be included in the scope processed
     *
     * @param b true if scope is to be package level.
     */
    public void setPackage(final boolean b) {
        addArgIf(b, "-package");
    }

    /**
     * Indicate whether all classes and
     * members are to be included in the scope processed
     *
     * @param b true if scope is to be private level.
     */
    public void setPrivate(final boolean b) {
        addArgIf(b, "-private");
    }

    /**
     * Set the scope to be processed. This is an alternative to the
     * use of the setPublic, setPrivate, etc methods. It gives better build
     * file control over what scope is processed.
     *
     * @param at the scope to be processed.
     */
    public void setAccess(final AccessType at) {
        cmd.createArgument().setValue("-" + at.getValue());
    }

    /**
     * Set the class that starts the doclet used in generating the
     * documentation.
     *
     * @param docletName the name of the doclet class.
     */
    public void setDoclet(final String docletName) {
        if (doclet == null) {
            doclet = new DocletInfo();
            doclet.setProject(getProject());
        }
        doclet.setName(docletName);
    }

    /**
     * Set the classpath used to find the doclet class.
     *
     * @param docletPath the doclet classpath.
     */
    public void setDocletPath(final Path docletPath) {
        if (doclet == null) {
            doclet = new DocletInfo();
            doclet.setProject(getProject());
        }
        doclet.setPath(docletPath);
    }

    /**
     * Set the classpath used to find the doclet class by reference.
     *
     * @param r the reference to the Path instance to use as the doclet
     *        classpath.
     */
    public void setDocletPathRef(final Reference r) {
        if (doclet == null) {
            doclet = new DocletInfo();
            doclet.setProject(getProject());
        }
        doclet.createPath().setRefid(r);
    }

    /**
     * Create a doclet to be used in the documentation generation.
     *
     * @return a new DocletInfo instance to be configured.
     */
    public DocletInfo createDoclet() {
        if (doclet == null) {
            doclet = new DocletInfo();
        }
        return doclet;
    }

    /**
     * Add a taglet
     *
     * @param tagletInfo information about the taglet.
     */
    public void addTaglet(final ExtensionInfo tagletInfo) {
        tags.add(tagletInfo);
    }

    /**
     * Indicate whether Javadoc should produce old style (JDK 1.1)
     * documentation.
     *
     * This is not supported by JDK 1.1 and has been phased out in JDK 1.4
     *
     * @param b if true attempt to generate old style documentation.
     */
    public void setOld(final boolean b) {
        log("Javadoc 1.4 doesn't support the -1.1 switch anymore",
            Project.MSG_WARN);
    }

    /**
     * Set the classpath to be used for this Javadoc run.
     *
     * @param path an Ant Path object containing the compilation
     *        classpath.
     */
    public void setClasspath(final Path path) {
        if (classpath == null) {
            classpath = path;
        } else {
            classpath.append(path);
        }
    }

    /**
     * Create a Path to be configured with the classpath to use
     *
     * @return a new Path instance to be configured with the classpath.
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     *
     * @param r the reference to an instance defining the classpath.
     */
    public void setClasspathRef(final Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Set the boot classpath to use.
     *
     * @param path the boot classpath.
     */
    public void setBootclasspath(final Path path) {
        if (bootclasspath == null) {
            bootclasspath = path;
        } else {
            bootclasspath.append(path);
        }
    }

    /**
     * Create a Path to be configured with the boot classpath
     *
     * @return a new Path instance to be configured with the boot classpath.
     */
    public Path createBootclasspath() {
        if (bootclasspath == null) {
            bootclasspath = new Path(getProject());
        }
        return bootclasspath.createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     *
     * @param r the reference to an instance defining the bootclasspath.
     */
    public void setBootClasspathRef(final Reference r) {
        createBootclasspath().setRefid(r);
    }

    /**
     * Set the location of the extensions directories.
     *
     * @param path the string version of the path.
     * @deprecated since 1.5.x.
     *             Use the {@link #setExtdirs(Path)} version.
     */
    @Deprecated
    public void setExtdirs(final String path) {
        cmd.createArgument().setValue("-extdirs");
        cmd.createArgument().setValue(path);
    }

    /**
     * Set the location of the extensions directories.
     *
     * @param path a path containing the extension directories.
     */
    public void setExtdirs(final Path path) {
        cmd.createArgument().setValue("-extdirs");
        cmd.createArgument().setPath(path);
    }

    /**
     * Run javadoc in verbose mode
     *
     * @param b true if operation is to be verbose.
     */
    public void setVerbose(final boolean b) {
        addArgIf(b, "-verbose");
    }

    /**
     * Set the local to use in documentation generation.
     *
     * @param locale the locale to use.
     */
    public void setLocale(final String locale) {
        // createArgument(true) is necessary to make sure -locale
        // is the first argument (required in 1.3+).
        cmd.createArgument(true).setValue(locale);
        cmd.createArgument(true).setValue("-locale");
    }

    /**
     * Set the encoding name of the source files,
     *
     * @param enc the name of the encoding for the source files.
     */
    public void setEncoding(final String enc) {
        cmd.createArgument().setValue("-encoding");
        cmd.createArgument().setValue(enc);
    }

    /**
     * Include the version tag in the generated documentation.
     *
     * @param b true if the version tag should be included.
     */
    public void setVersion(final boolean b) {
        this.version = b;
    }

    /**
     * Generate the &quot;use&quot; page for each package.
     *
     * @param b true if the use page should be generated.
     */
    public void setUse(final boolean b) {
        addArgIf(b, "-use");
    }


    /**
     * Include the author tag in the generated documentation.
     *
     * @param b true if the author tag should be included.
     */
    public void setAuthor(final boolean b) {
        author = b;
    }

    /**
     * Generate a split index
     *
     * @param b true if the index should be split into a file per letter.
     */
    public void setSplitindex(final boolean b) {
        addArgIf(b, "-splitindex");
    }

    /**
     * Set the title to be placed in the HTML &lt;title&gt; tag of the
     * generated documentation.
     *
     * @param title the window title to use.
     */
    public void setWindowtitle(final String title) {
        addArgIfNotEmpty("-windowtitle", title);
    }

    /**
     * Set the title of the generated overview page.
     *
     * @param doctitle the Document title.
     */
    public void setDoctitle(final String doctitle) {
        final Html h = new Html();
        h.addText(doctitle);
        addDoctitle(h);
    }

    /**
     * Add a document title to use for the overview page.
     *
     * @param text the HTML element containing the document title.
     */
    public void addDoctitle(final Html text) {
        doctitle = text;
    }

    /**
     * Set the header text to be placed at the top of each output file.
     *
     * @param header the header text
     */
    public void setHeader(final String header) {
        final Html h = new Html();
        h.addText(header);
        addHeader(h);
    }

    /**
     * Set the header text to be placed at the top of each output file.
     *
     * @param text the header text
     */
    public void addHeader(final Html text) {
        header = text;
    }

    /**
     * Set the footer text to be placed at the bottom of each output file.
     *
     * @param footer the footer text.
     */
    public void setFooter(final String footer) {
        final Html h = new Html();
        h.addText(footer);
        addFooter(h);
    }

    /**
     * Set the footer text to be placed at the bottom of each output file.
     *
     * @param text the footer text.
     */
    public void addFooter(final Html text) {
        footer = text;
    }

    /**
     * Set the text to be placed at the bottom of each output file.
     *
     * @param bottom the bottom text.
     */
    public void setBottom(final String bottom) {
        final Html h = new Html();
        h.addText(bottom);
        addBottom(h);
    }

    /**
     * Set the text to be placed at the bottom of each output file.
     *
     * @param text the bottom text.
     */
    public void addBottom(final Html text) {
        bottom = text;
    }

    /**
     * Link to docs at "url" using package list at "url2"
     * - separate the URLs by using a space character.
     *
     * @param src the offline link specification (url and package list)
     */
    public void setLinkoffline(final String src) {
        final LinkArgument le = createLink();
        le.setOffline(true);
        final String linkOfflineError = "The linkoffline attribute must include"
            + " a URL and a package-list file location separated by a"
            + " space";
        if (src.trim().isEmpty()) {
            throw new BuildException(linkOfflineError);
        }
        final StringTokenizer tok = new StringTokenizer(src, " ", false);
        le.setHref(tok.nextToken());

        if (!tok.hasMoreTokens()) {
            throw new BuildException(linkOfflineError);
        }
        le.setPackagelistLoc(getProject().resolveFile(tok.nextToken()));
    }

    /**
     * Group specified packages together in overview page.
     *
     * @param src the group packages - a command separated list of group specs,
     *        each one being a group name and package specification separated
     *        by a space.
     */
    public void setGroup(final String src) {
        group = src;
    }

    /**
     * Create links to Javadoc output at the given URL.
     * @param src the URL to link to
     */
    public void setLink(final String src) {
        createLink().setHref(src);
    }

    /**
     * Control deprecation information
     *
     * @param b If true, do not include deprecated information.
     */
    public void setNodeprecated(final boolean b) {
        addArgIf(b, "-nodeprecated");
    }

    /**
     * Control deprecated list generation
     *
     * @param b if true, do not generate deprecated list.
     */
    public void setNodeprecatedlist(final boolean b) {
        addArgIf(b, "-nodeprecatedlist");
    }

    /**
     * Control class tree generation.
     *
     * @param b if true, do not generate class hierarchy.
     */
    public void setNotree(final boolean b) {
        addArgIf(b, "-notree");
    }

    /**
     * Control generation of index.
     *
     * @param b if true, do not generate index.
     */
    public void setNoindex(final boolean b) {
        addArgIf(b, "-noindex");
    }

    /**
     * Control generation of help link.
     *
     * @param b if true, do not generate help link
     */
    public void setNohelp(final boolean b) {
        addArgIf(b, "-nohelp");
    }

    /**
     * Control generation of the navigation bar.
     *
     * @param b if true, do not generate navigation bar.
     */
    public void setNonavbar(final boolean b) {
        addArgIf(b, "-nonavbar");
    }

    /**
     * Control warnings about serial tag.
     *
     * @param b if true, generate warning about the serial tag.
     */
    public void setSerialwarn(final boolean b) {
        addArgIf(b, "-serialwarn");
    }

    /**
     * Specifies the CSS stylesheet file to use.
     *
     * @param f the file with the CSS to use.
     */
    public void setStylesheetfile(final File f) {
        cmd.createArgument().setValue("-stylesheetfile");
        cmd.createArgument().setFile(f);
    }

    /**
     * Specifies the HTML help file to use.
     *
     * @param f the file containing help content.
     */
    public void setHelpfile(final File f) {
        cmd.createArgument().setValue("-helpfile");
        cmd.createArgument().setFile(f);
    }

    /**
     * Output file encoding name.
     *
     * @param enc name of the encoding to use.
     */
    public void setDocencoding(final String enc) {
        cmd.createArgument().setValue("-docencoding");
        cmd.createArgument().setValue(enc);
        docEncoding = enc;
    }

    /**
     * The name of a file containing the packages to process.
     *
     * @param src the file containing the package list.
     */
    public void setPackageList(final String src) {
        packageList = src;
    }

    /**
     * Create link to Javadoc output at the given URL.
     *
     * @return link argument to configure
     */
    public LinkArgument createLink() {
        final LinkArgument la = new LinkArgument();
        links.add(la);
        return la;
    }

    /**
     * Represents a link triplet (href, whether link is offline,
     * location of the package list if off line)
     */
    public class LinkArgument {
        private String href;
        private boolean offline = false;
        private File packagelistLoc;
        private URL packagelistURL;
        private boolean resolveLink = false;

        /** Constructor for LinkArgument */
        public LinkArgument() {
            //empty
        }

        /**
         * Set the href attribute.
         * @param hr a <code>String</code> value
         */
        public void setHref(final String hr) {
            href = hr;
        }

        /**
         * Get the href attribute.
         * @return the href attribute.
         */
        public String getHref() {
            return href;
        }

        /**
         * Set the packetlist location attribute.
         * @param src a <code>File</code> value
         */
        public void setPackagelistLoc(final File src) {
            packagelistLoc = src;
        }

        /**
         * Get the packetList location attribute.
         * @return the packetList location attribute.
         */
        public File getPackagelistLoc() {
            return packagelistLoc;
        }

        /**
         * Set the packetlist location attribute.
         * @param src an <code>URL</code> value
         */
        public void setPackagelistURL(final URL src) {
            packagelistURL = src;
        }

        /**
         * Get the packetList location attribute.
         * @return the packetList location attribute.
         */
        public URL getPackagelistURL() {
            return packagelistURL;
        }

        /**
         * Set the offline attribute.
         * @param offline a <code>boolean</code> value
         */
        public void setOffline(final boolean offline) {
            this.offline = offline;
        }

        /**
         * Get the linkOffline attribute.
         * @return the linkOffline attribute.
         */
        public boolean isLinkOffline() {
            return offline;
        }

        /**
         * Sets whether Ant should resolve the link attribute relative
         * to the current basedir.
         * @param resolve a <code>boolean</code> value
         */
        public void setResolveLink(final boolean resolve) {
            this.resolveLink = resolve;
        }

        /**
         * should Ant resolve the link attribute relative to the
         * current basedir?
         * @return the resolveLink attribute.
         */
        public boolean shouldResolveLink() {
            return resolveLink;
        }

    }

    /**
     * Creates and adds a -tag argument. This is used to specify
     * custom tags. This argument is only available for Javadoc 1.4,
     * and will generate a verbose message (and then be ignored)
     * when run on Java versions below 1.4.
     * @return tag argument to be configured
     */
    public TagArgument createTag() {
        final TagArgument ta = new TagArgument();
        tags.add(ta);
        return ta;
    }

    /**
     * Scope element verbose names. (Defined here as fields
     * cannot be static in inner classes.) The first letter
     * from each element is used to build up the scope string.
     */
    static final String[] SCOPE_ELEMENTS = { //NOSONAR
        "overview", "packages", "types", "constructors",
        "methods", "fields"
    };

    /**
     * Class representing a -tag argument.
     */
    public class TagArgument extends FileSet {
        /** Name of the tag. */
        private String name = null;
        /** Whether or not the tag is enabled. */
        private boolean enabled = true;
        /**
         * Scope string of the tag. This will form the middle
         * argument of the -tag parameter when the tag is enabled
         * (with an X prepended for and is parsed from human-readable form.
         */
        private String scope = "a";

        /** Sole constructor. */
        public TagArgument() {
            //empty
        }

        /**
         * Sets the name of the tag.
         *
         * @param name The name of the tag.
         *             Must not be <code>null</code> or empty.
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * Sets the scope of the tag. This is in comma-separated
         * form, with each element being one of "all" (the default),
         * "overview", "packages", "types", "constructors", "methods",
         * "fields". The elements are treated in a case-insensitive
         * manner.
         *
         * @param verboseScope The scope of the tag.
         *                     Must not be <code>null</code>,
         *                     should not be empty.
         *
         * @exception BuildException if all is specified along with
         * other elements, if any elements are repeated, if no
         * elements are specified, or if any unrecognised elements are
         * specified.
         */
        public void setScope(String verboseScope) throws BuildException {
            verboseScope = verboseScope.toLowerCase(Locale.ENGLISH);

            final boolean[] elements = new boolean[SCOPE_ELEMENTS.length];

            boolean gotAll = false;
            boolean gotNotAll = false;

            // Go through the tokens one at a time, updating the
            // elements array and issuing warnings where appropriate.
            final StringTokenizer tok = new StringTokenizer(verboseScope, ",");
            while (tok.hasMoreTokens()) {
                final String next = tok.nextToken().trim();
                if ("all".equals(next)) {
                    if (gotAll) {
                        getProject().log("Repeated tag scope element: all",
                                          Project.MSG_VERBOSE);
                    }
                    gotAll = true;
                } else {
                    int i;
                    for (i = 0; i < SCOPE_ELEMENTS.length; i++) {
                        if (SCOPE_ELEMENTS[i].equals(next)) {
                            break;
                        }
                    }
                    if (i == SCOPE_ELEMENTS.length) {
                        throw new BuildException(
                            "Unrecognised scope element: %s", next);
                    }
                    if (elements[i]) {
                        getProject().log("Repeated tag scope element: " + next,
                            Project.MSG_VERBOSE);
                    }
                    elements[i] = true;
                    gotNotAll = true;
                }
            }

            if (gotNotAll && gotAll) {
                throw new BuildException(
                    "Mixture of \"all\" and other scope elements in tag parameter.");
            }
            if (!gotNotAll && !gotAll) {
                throw new BuildException(
                    "No scope elements specified in tag parameter.");
            }
            if (gotAll) {
                this.scope = "a";
            } else {
                final StringBuilder buff = new StringBuilder(elements.length);
                for (int i = 0; i < elements.length; i++) {
                    if (elements[i]) {
                        buff.append(SCOPE_ELEMENTS[i].charAt(0));
                    }
                }
                this.scope = buff.toString();
            }
        }

        /**
         * Sets whether or not the tag is enabled.
         *
         * @param enabled Whether or not this tag is enabled.
         */
        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the -tag parameter this argument represented.
         * @return the -tag parameter as a string
         * @exception BuildException if either the name or description
         *                           is <code>null</code> or empty.
         */
        public String getParameter() throws BuildException {
            if (name == null || name.isEmpty()) {
                throw new BuildException("No name specified for custom tag.");
            }
            if (getDescription() != null) {
                return name + ":" + (enabled ? "" : "X")
                    + scope + ":" + getDescription();
            }
            if (!enabled || !"a".equals(scope)) {
                return name + ":" + (enabled ? "" : "X") + scope;
            }
            return name;
        }
    }

    /**
     * Separates packages on the overview page into whatever
     * groups you specify, one group per table.
     * @return a group argument to be configured
     */
    public GroupArgument createGroup() {
        final GroupArgument ga = new GroupArgument();
        groups.add(ga);
        return ga;
    }

    /**
     * A class corresponding to the group nested element.
     */
    public class GroupArgument {
        private Html title;
        private final List<PackageName> packages = new Vector<>();

        /**
         * Set the title attribute using a string.
         * @param src a <code>String</code> value
         */
        public void setTitle(final String src) {
            final Html h = new Html();
            h.addText(src);
            addTitle(h);
        }

        /**
         * Set the title attribute using a nested Html value.
         * @param text a <code>Html</code> value
         */
        public void addTitle(final Html text) {
            title = text;
        }

        /**
         * Get the title.
         * @return the title
         */
        public String getTitle() {
            return title != null ? title.getText() : null;
        }

        /**
         * Set the packages to Javadoc on.
         * @param src a comma separated list of packages
         */
        public void setPackages(final String src) {
            final StringTokenizer tok = new StringTokenizer(src, ",");
            while (tok.hasMoreTokens()) {
                final String p = tok.nextToken();
                final PackageName pn = new PackageName();
                pn.setName(p);
                addPackage(pn);
            }
        }

        /**
         * Add a package nested element.
         * @param pn a nested element specifying the package.
         */
        public void addPackage(final PackageName pn) {
            packages.add(pn);
        }

        /**
         * Get the packages as a colon separated list.
         * @return the packages as a string
         */
        public String getPackages() {
            return packages.stream().map(Object::toString)
                .collect(Collectors.joining(":"));
        }
    }

    /**
     * Charset for cross-platform viewing of generated documentation.
     * @param src the name of the charset
     */
    public void setCharset(final String src) {
        this.addArgIfNotEmpty("-charset", src);
    }

    /**
     * Should the build process fail if Javadoc fails (as indicated by
     * a non zero return code)?
     *
     * <p>Default is false.</p>
     * @param b a <code>boolean</code> value
     */
    public void setFailonerror(final boolean b) {
        failOnError = b;
    }

    /**
     * Should the build process fail if Javadoc warns (as indicated by
     * the word "warning" on stdout)?
     *
     * <p>Default is false.</p>
     * @param b a <code>boolean</code> value
     * @since Ant 1.9.4
     */
    public void setFailonwarning(final boolean b) {
        failOnWarning = b;
    }

    /**
     * Enables the -source switch, will be ignored if Javadoc is not
     * the 1.4 version.
     * @param source a <code>String</code> value
     * @since Ant 1.5
     */
    public void setSource(final String source) {
        this.source = source;
    }

    /**
     * Sets the actual executable command to invoke, instead of the binary
     * <code>javadoc</code> found in Ant's JDK.
     * @param executable the command to invoke.
     * @since Ant 1.6.3
     */
    public void setExecutable(final String executable) {
        this.executable = executable;
    }

    /**
     * Adds a packageset.
     *
     * <p>All included directories will be translated into package
     * names be converting the directory separator into dots.</p>
     * @param packageSet a directory set
     * @since 1.5
     */
    public void addPackageset(final DirSet packageSet) {
        packageSets.add(packageSet);
    }

    /**
     * Adds a fileset.
     *
     * <p>All included files will be added as sourcefiles.  The task
     * will automatically add
     * <code>includes=&quot;**&#47;*.java&quot;</code> to the
     * fileset.</p>
     * @param fs a file set
     * @since 1.5
     */
    public void addFileset(final FileSet fs) {
        createSourceFiles().add(fs);
    }

    /**
     * Adds a container for resource collections.
     *
     * <p>All included files will be added as sourcefiles.</p>
     * @return the source files to configure.
     * @since 1.7
     */
    public ResourceCollectionContainer createSourceFiles() {
        return nestedSourceFiles;
    }

    /**
     * Enables the -linksource switch, will be ignored if Javadoc is not
     * the 1.4 version. Default is false
     * @param b a <code>String</code> value
     * @since Ant 1.6
     */
    public void setLinksource(final boolean b) {
        this.linksource = b;
    }

    /**
     * Enables the -linksource switch, will be ignored if Javadoc is not
     * the 1.4 version. Default is false
     * @param b a <code>String</code> value
     * @since Ant 1.6
     */
    public void setBreakiterator(final boolean b) {
        this.breakiterator = b;
    }

    /**
     * Enables the -noqualifier switch, will be ignored if Javadoc is not
     * the 1.4 version.
     * @param noqualifier the parameter to the -noqualifier switch
     * @since Ant 1.6
     */
    public void setNoqualifier(final String noqualifier) {
        this.noqualifier = noqualifier;
    }

    /**
     * If set to true, Ant will also accept packages that only hold
     * package.html files but no Java sources.
     * @param b a <code>boolean</code> value.
     * @since Ant 1.6.3
     */
    public void setIncludeNoSourcePackages(final boolean b) {
        this.includeNoSourcePackages = b;
    }

    /**
     * Enables deep-copying of <code>doc-files</code> directories.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setDocFilesSubDirs(final boolean b) {
        docFilesSubDirs = b;
    }

    /**
     * Colon-separated list of <code>doc-files</code> subdirectories
     * to skip if {@link #setDocFilesSubDirs docFilesSubDirs is true}.
     *
     * @param s String
     * @since Ant 1.8.0
     */
    public void setExcludeDocFilesSubDir(final String s) {
        excludeDocFilesSubDir = s;
    }

    /**
     * Whether to post-process the generated javadocs in order to mitigate CVE-2013-1571.
     *
     * @param b boolean
     * @since Ant 1.9.2
     */
    public void setPostProcessGeneratedJavadocs(final boolean b) {
        postProcessGeneratedJavadocs = b;
    }

    /**
     * Execute the task.
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        checkTaskName();

        final List<String> packagesToDoc = new Vector<>();
        final Path sourceDirs = new Path(getProject());

        checkPackageAndSourcePath();

        if (sourcePath != null) {
            sourceDirs.addExisting(sourcePath);
        }

        parsePackages(packagesToDoc, sourceDirs);
        checkPackages(packagesToDoc, sourceDirs);

        final List<SourceFile> sourceFilesToDoc = new ArrayList<>(sourceFiles);
        addSourceFiles(sourceFilesToDoc);

        checkPackagesToDoc(packagesToDoc, sourceFilesToDoc);

        log("Generating Javadoc", Project.MSG_INFO);

        final Commandline toExecute = (Commandline) cmd.clone();
        if (executable != null) {
            toExecute.setExecutable(executable);
        } else {
            toExecute.setExecutable(JavaEnvUtils.getJdkExecutable("javadoc"));
        }

        //  Javadoc arguments
        generalJavadocArguments(toExecute);  // general Javadoc arguments
        doSourcePath(toExecute, sourceDirs); // sourcepath
        doDoclet(toExecute);   // arguments for default doclet
        doBootPath(toExecute); // bootpath
        doLinks(toExecute);    // links arguments
        doGroup(toExecute);    // group attribute
        doGroups(toExecute);  // groups attribute
        doDocFilesSubDirs(toExecute); // docfilessubdir attribute
        doModuleArguments(toExecute);

        doTags(toExecute);
        doSource(toExecute);
        doLinkSource(toExecute);
        doNoqualifier(toExecute);
		
        if (breakiterator) {
            toExecute.createArgument().setValue("-breakiterator");
        }
        // If using an external file, write the command line options to it
        if (useExternalFile) {
            writeExternalArgs(toExecute);
        }

        File tmpList = null;
        FileWriter wr = null;
        try {
            /**
             * Write sourcefiles and package names to a temporary file
             * if requested.
             */
            BufferedWriter srcListWriter = null;
            if (useExternalFile) {
                tmpList = FILE_UTILS.createTempFile(getProject(), "javadoc", "", null, true, true);
                toExecute.createArgument()
                    .setValue("@" + tmpList.getAbsolutePath());
                wr = new FileWriter(tmpList.getAbsolutePath(), true);
                srcListWriter = new BufferedWriter(wr);
            }

            doSourceAndPackageNames(
                toExecute, packagesToDoc, sourceFilesToDoc,
                useExternalFile, tmpList, srcListWriter);

            if (useExternalFile) {
                srcListWriter.flush(); //NOSONAR
            }
        } catch (final IOException e) {
            if (tmpList != null) {
                tmpList.delete();
            }
            throw new BuildException("Error creating temporary file",
                                     e, getLocation());
        } finally {
            FileUtils.close(wr);
        }

        if (packageList != null) {
            toExecute.createArgument().setValue("@" + packageList);
        }
        log(toExecute.describeCommand(), Project.MSG_VERBOSE);

        log("Javadoc execution", Project.MSG_INFO);

        final JavadocOutputStream out = new JavadocOutputStream(Project.MSG_INFO);
        final JavadocOutputStream err = new JavadocOutputStream(Project.MSG_WARN);
        final Execute exe = new Execute(new PumpStreamHandler(out, err));
        exe.setAntRun(getProject());

        /*
         * No reason to change the working directory as all filenames and
         * path components have been resolved already.
         *
         * Avoid problems with command line length in some environments.
         */
        exe.setWorkingDirectory(null);
        try {
            exe.setCommandline(toExecute.getCommandline());
            final int ret = exe.execute();
            if (ret != 0 && failOnError) {
                throw new BuildException("Javadoc returned " + ret,
                                         getLocation());
            }
            if (failOnWarning && (out.sawWarnings() || err.sawWarnings())) {
                throw new BuildException("Javadoc issued warnings.",
                                         getLocation());
            }
            postProcessGeneratedJavadocs();
        } catch (final IOException e) {
            throw new BuildException("Javadoc failed: " + e, e, getLocation());
        } finally {
            if (tmpList != null) {
                tmpList.delete();
                tmpList = null;
            }

            out.logFlush();
            err.logFlush();
            FileUtils.close(out);
            FileUtils.close(err);
        }
    }

    private void checkTaskName() {
        if ("javadoc2".equals(getTaskType())) {
            log("Warning: the task name <javadoc2> is deprecated."
                + " Use <javadoc> instead.",
                Project.MSG_WARN);
        }
    }

    private void checkPackageAndSourcePath() {
        if (packageList != null && sourcePath == null) {
            final String msg = "sourcePath attribute must be set when "
                + "specifying packagelist.";
            throw new BuildException(msg);
        }
    }

    private void checkPackages(final List<String> packagesToDoc, final Path sourceDirs) {
        if (!packagesToDoc.isEmpty() && sourceDirs.isEmpty()) {
            throw new BuildException(
                "sourcePath attribute must be set when specifying package names.");
        }
    }

    private void checkPackagesToDoc(
        final List<String> packagesToDoc, final List<SourceFile> sourceFilesToDoc) {
        if (packageList == null && packagesToDoc.isEmpty()
            && sourceFilesToDoc.isEmpty() && moduleNames.isEmpty()) {
            throw new BuildException("No source files, no packages and no modules have "
                                     + "been specified.");
        }
    }

    private void doSourcePath(final Commandline toExecute, final Path sourceDirs) {
        if (!sourceDirs.isEmpty()) {
            toExecute.createArgument().setValue("-sourcepath");
            toExecute.createArgument().setPath(sourceDirs);
        }
    }

    private void generalJavadocArguments(final Commandline toExecute) {
        if (doctitle != null) {
            toExecute.createArgument().setValue("-doctitle");
            toExecute.createArgument().setValue(expand(doctitle.getText()));
        }
        if (header != null) {
            toExecute.createArgument().setValue("-header");
            toExecute.createArgument().setValue(expand(header.getText()));
        }
        if (footer != null) {
            toExecute.createArgument().setValue("-footer");
            toExecute.createArgument().setValue(expand(footer.getText()));
        }
        if (bottom != null) {
            toExecute.createArgument().setValue("-bottom");
            toExecute.createArgument().setValue(expand(bottom.getText()));
        }

        if (classpath == null) {
            classpath = new Path(getProject()).concatSystemClasspath("last");
        } else {
            classpath = classpath.concatSystemClasspath("ignore");
        }

        if (classpath.size() > 0) {
            toExecute.createArgument().setValue("-classpath");
            toExecute.createArgument().setPath(classpath);
        }

        if (version && doclet == null) {
            toExecute.createArgument().setValue("-version");
        }
        if (author && doclet == null) {
            toExecute.createArgument().setValue("-author");
        }

        if (doclet == null && destDir == null) {
            throw new BuildException("destdir attribute must be set!");
        }
    }

    private void doDoclet(final Commandline toExecute) {
        if (doclet != null) {
            if (doclet.getName() == null) {
                throw new BuildException("The doclet name must be specified.", getLocation());
            }
            toExecute.createArgument().setValue("-doclet");
            toExecute.createArgument().setValue(doclet.getName());
            if (doclet.getPath() != null) {
                final Path docletPath = doclet.getPath().concatSystemClasspath("ignore");
                if (docletPath.size() != 0) {
                    toExecute.createArgument().setValue("-docletpath");
                    toExecute.createArgument().setPath(docletPath);
                }
            }
            for (final DocletParam param : Collections.list(doclet.getParams())) {
                if (param.getName() == null) {
                    throw new BuildException("Doclet parameters must have a name");
                }
                toExecute.createArgument().setValue(param.getName());
                if (param.getValue() != null) {
                    toExecute.createArgument().setValue(param.getValue());
                }
            }
        }
    }

    private void writeExternalArgs(final Commandline toExecute) {
        // If using an external file, write the command line options to it
        File optionsTmpFile = null;
        try {
            optionsTmpFile = FILE_UTILS.createTempFile(
                getProject(), "javadocOptions", "", null, true, true);
            final String[] listOpt = toExecute.getArguments();
            toExecute.clearArgs();
            toExecute.createArgument().setValue(
                "@" + optionsTmpFile.getAbsolutePath());
            try (BufferedWriter optionsListWriter = new BufferedWriter(
                new FileWriter(optionsTmpFile.getAbsolutePath(), true))) {
                for (final String opt : listOpt) {
                    if (opt.startsWith("-J-")) {
                        toExecute.createArgument().setValue(opt);
                    } else if (opt.startsWith("-")) {
                        optionsListWriter.write(opt);
                        optionsListWriter.write(" ");
                    } else {
                        optionsListWriter.write(quoteString(opt));
                        optionsListWriter.newLine();
                    }
                }
            }
        } catch (final IOException ex) {
            if (optionsTmpFile != null) {
                optionsTmpFile.delete();
            }
            throw new BuildException(
                "Error creating or writing temporary file for javadoc options",
                ex, getLocation());
        }
    }

    private void doBootPath(final Commandline toExecute) {
        Path bcp = new Path(getProject());
        if (bootclasspath != null) {
            bcp.append(bootclasspath);
        }
        bcp = bcp.concatSystemBootClasspath("ignore");
        if (bcp.size() > 0) {
            toExecute.createArgument().setValue("-bootclasspath");
            toExecute.createArgument().setPath(bcp);
        }
    }

    private void doLinks(final Commandline toExecute) {
        for (final LinkArgument la : links) {
            if (la.getHref() == null || la.getHref().isEmpty()) {
                log("No href was given for the link - skipping",
                    Project.MSG_VERBOSE);
                continue;
            }
            String link = null;
            if (la.shouldResolveLink()) {
                final File hrefAsFile =
                    getProject().resolveFile(la.getHref());
                if (hrefAsFile.exists()) {
                    try {
                        link = FILE_UTILS.getFileURL(hrefAsFile)
                            .toExternalForm();
                    } catch (final MalformedURLException ex) {
                        // should be impossible
                        log("Warning: link location was invalid "
                            + hrefAsFile, Project.MSG_WARN);
                    }
                }
            }
            if (link == null) {
                // is the href a valid URL
                try {
                    final URL base = new URL("file://.");
                    // created for the side effect of throwing a MalformedURLException
                    new URL(base, la.getHref()); //NOSONAR
                    link = la.getHref();
                } catch (final MalformedURLException mue) {
                    // ok - just skip
                    log("Link href \"" + la.getHref()
                        + "\" is not a valid url - skipping link",
                        Project.MSG_WARN);
                    continue;
                }
            }

            if (la.isLinkOffline()) {
                final File packageListLocation = la.getPackagelistLoc();
                URL packageListURL = la.getPackagelistURL();
                if (packageListLocation == null
                    && packageListURL == null) {
                    throw new BuildException(
                        "The package list location for link " + la.getHref()
                            + " must be provided because the link is offline");
                }
                if (packageListLocation != null) {
                    final File packageListFile =
                        new File(packageListLocation, "package-list");
                    if (packageListFile.exists()) {
                        try {
                            packageListURL =
                                FILE_UTILS.getFileURL(packageListLocation);
                        } catch (final MalformedURLException ex) {
                            log("Warning: Package list location was "
                                + "invalid " + packageListLocation,
                                Project.MSG_WARN);
                        }
                    } else {
                        log("Warning: No package list was found at "
                            + packageListLocation, Project.MSG_VERBOSE);
                    }
                }
                if (packageListURL != null) {
                    toExecute.createArgument().setValue("-linkoffline");
                    toExecute.createArgument().setValue(link);
                    toExecute.createArgument()
                        .setValue(packageListURL.toExternalForm());
                }
            } else {
                toExecute.createArgument().setValue("-link");
                toExecute.createArgument().setValue(link);
            }
        }
    }

    private void doGroup(final Commandline toExecute) {
        // add the single group arguments
        // Javadoc 1.2 rules:
        //   Multiple -group args allowed.
        //   Each arg includes 3 strings: -group [name] [packagelist].
        //   Elements in [packagelist] are colon-delimited.
        //   An element in [packagelist] may end with the * wildcard.

        // Ant javadoc task rules for group attribute:
        //   Args are comma-delimited.
        //   Each arg is 2 space-delimited strings.
        //   E.g., group="XSLT_Packages org.apache.xalan.xslt*,
        //                XPath_Packages org.apache.xalan.xpath*"
        if (group != null) {
            final StringTokenizer tok = new StringTokenizer(group, ",", false);
            while (tok.hasMoreTokens()) {
                final String grp = tok.nextToken().trim();
                final int space = grp.indexOf(' ');
                if (space > 0) {
                    final String name = grp.substring(0, space);
                    final String pkgList = grp.substring(space + 1);
                    toExecute.createArgument().setValue("-group");
                    toExecute.createArgument().setValue(name);
                    toExecute.createArgument().setValue(pkgList);
                }
            }
        }
    }

    // add the group arguments
    private void doGroups(final Commandline toExecute) {
        for (final GroupArgument ga : groups) {
            final String title = ga.getTitle();
            final String packages = ga.getPackages();
            if (title == null || packages == null) {
                throw new BuildException(
                    "The title and packages must be specified for group elements.");
            }
            toExecute.createArgument().setValue("-group");
            toExecute.createArgument().setValue(expand(title));
            toExecute.createArgument().setValue(packages);
        }
    }

    private void doNoqualifier(final Commandline toExecute) {
        if (noqualifier != null && doclet == null) {
            toExecute.createArgument().setValue("-noqualifier");
            toExecute.createArgument().setValue(noqualifier);
        }
    }

    private void doLinkSource(final Commandline toExecute) {
        if (linksource && doclet == null) {
            toExecute.createArgument().setValue("-linksource");
        }
    }

    private void doSource(final Commandline toExecute) {
        final String sourceArg = source != null ? source : getProject().getProperty(MagicNames.BUILD_JAVAC_SOURCE);
        if (sourceArg != null) {
            toExecute.createArgument().setValue("-source");
            toExecute.createArgument().setValue(sourceArg);
        }
    }

    private void doTags(final Commandline toExecute) {
        for (final Object element : tags) {
            if (element instanceof TagArgument) {
                final TagArgument ta = (TagArgument) element;
                final File tagDir = ta.getDir(getProject());
                if (tagDir == null) {
                    // The tag element is not used as a fileset,
                    // but specifies the tag directly.
                    toExecute.createArgument().setValue("-tag");
                    toExecute.createArgument().setValue(ta.getParameter());
                } else {
                    // The tag element is used as a
                    // fileset. Parse all the files and create
                    // -tag arguments.
                    final DirectoryScanner tagDefScanner = ta.getDirectoryScanner(getProject());
                    for (String file : tagDefScanner.getIncludedFiles()) {
                        final File tagDefFile = new File(tagDir, file);
                        try (final BufferedReader in = new BufferedReader(new FileReader(tagDefFile))) {
                            in.lines().forEach(line -> {
                                toExecute.createArgument().setValue("-tag");
                                toExecute.createArgument().setValue(line);
                            });
                        } catch (final IOException ioe) {
                            throw new BuildException("Couldn't read tag file from " + tagDefFile.getAbsolutePath(),
                                    ioe);
                        }
                    }
                }
            } else {
                final ExtensionInfo tagletInfo = (ExtensionInfo) element;
                toExecute.createArgument().setValue("-taglet");
                toExecute.createArgument().setValue(tagletInfo.getName());
                if (tagletInfo.getPath() != null) {
                    final Path tagletPath = tagletInfo.getPath().concatSystemClasspath("ignore");
                    if (!tagletPath.isEmpty()) {
                        toExecute.createArgument().setValue("-tagletpath");
                        toExecute.createArgument().setPath(tagletPath);
                    }
                }
            }
        }
    }

    private void doDocFilesSubDirs(final Commandline toExecute) {
        if (docFilesSubDirs) {
            toExecute.createArgument().setValue("-docfilessubdirs");
            if (excludeDocFilesSubDir != null && !excludeDocFilesSubDir.trim().isEmpty()) {
                toExecute.createArgument().setValue("-excludedocfilessubdir");
                toExecute.createArgument().setValue(excludeDocFilesSubDir);
            }
        }
    }

    private void doSourceAndPackageNames(
        final Commandline toExecute,
        final List<String> packagesToDoc,
        final List<SourceFile> sourceFilesToDoc,
        final boolean useExternalFile,
        final File tmpList,
        final BufferedWriter srcListWriter)
        throws IOException {
        for (final String packageName : packagesToDoc) {
            if (useExternalFile) {
                srcListWriter.write(packageName);
                srcListWriter.newLine();
            } else {
                toExecute.createArgument().setValue(packageName);
            }
        }

        for (final SourceFile sf : sourceFilesToDoc) {
            final String sourceFileName = sf.getFile().getAbsolutePath();
            if (useExternalFile) {
                // TODO what is the following doing?
                //     should it run if !javadoc4 && executable != null?
                if (sourceFileName.contains(" ")) {
                    String name = sourceFileName;
                    if (File.separatorChar == '\\') {
                        name = sourceFileName.replace(File.separatorChar, '/');
                    }
                    srcListWriter.write("\"" + name + "\"");
                } else {
                    srcListWriter.write(sourceFileName);
                }
                srcListWriter.newLine();
            } else {
                toExecute.createArgument().setValue(sourceFileName);
            }
        }
    }

    /**
     * Quote a string to place in a @ file.
     * @param str the string to quote
     * @return the quoted string, if there is no need to quote the string,
     *         return the original string.
     */
    private String quoteString(final String str) {
        if (!containsWhitespace(str)
            && !str.contains("'") && !str.contains("\"")) {
            return str;
        }
        if (!str.contains("'")) {
            return quoteString(str, '\'');
        }
        return quoteString(str, '"');
    }

    private boolean containsWhitespace(final String s) {
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
    }

    private String quoteString(final String str, final char delim) {
        final StringBuilder buf = new StringBuilder(str.length() * 2);
        buf.append(delim);
        boolean lastCharWasCR = false;
        for (final char c : str.toCharArray()) {
            if (c == delim) { // can't put the non-constant delim into a case
                buf.append('\\').append(c);
                lastCharWasCR = false;
            } else {
                switch (c) {
                case '\\':
                    buf.append("\\\\");
                    lastCharWasCR = false;
                    break;
                case '\r':
                    // insert a line continuation marker
                    buf.append("\\\r");
                    lastCharWasCR = true;
                    break;
                case '\n':
                    // insert a line continuation marker unless this
                    // is a \r\n sequence in which case \r already has
                    // created the marker
                    if (!lastCharWasCR) {
                        buf.append("\\\n");
                    } else {
                        buf.append("\n");
                    }
                    lastCharWasCR = false;
                    break;
                default:
                    buf.append(c);
                    lastCharWasCR = false;
                    break;
                }
            }
        }
        buf.append(delim);
        return buf.toString();
    }

    /**
     * Add the files matched by the nested source files to the Vector
     * as SourceFile instances.
     *
     * @since 1.7
     */
    private void addSourceFiles(final List<SourceFile> sf) {
        for (ResourceCollection rc : nestedSourceFiles) {
            if (!rc.isFilesystemOnly()) {
                throw new BuildException(
                    "only file system based resources are supported by javadoc");
            }
            if (rc instanceof FileSet) {
                final FileSet fs = (FileSet) rc;
                if (!fs.hasPatterns() && !fs.hasSelectors()) {
                    final FileSet fs2 = (FileSet) fs.clone();
                    fs2.createInclude().setName("**/*.java");
                    if (includeNoSourcePackages) {
                        fs2.createInclude().setName("**/package.html");
                    }
                    rc = fs2;
                }
            }
            for (final Resource r : rc) {
                sf.add(new SourceFile(r.as(FileProvider.class).getFile()));
            }
        }
    }

    /**
     * Add the directories matched by the nested dirsets to the Vector
     * and the base directories of the dirsets to the Path.  It also
     * handles the packages and excludepackages attributes and
     * elements.
     *
     * @since 1.5
     */
    private void parsePackages(final List<String> pn, final Path sp) {
        final Set<String> addedPackages = new HashSet<>();
        final List<DirSet> dirSets = new ArrayList<>(packageSets);

        // for each sourcePath entry, add a directoryset with includes
        // taken from packagenames attribute and nested package
        // elements and excludes taken from excludepackages attribute
        // and nested excludepackage elements
        if (sourcePath != null) {
            final PatternSet ps = new PatternSet();
            ps.setProject(getProject());
            if (packageNames.isEmpty()) {
                ps.createInclude().setName("**");
            } else {
                packageNames.stream().map(PackageName::getName)
                    .map(s -> s.replace('.', '/').replaceFirst("\\*$", "**"))
                    .forEach(pkg -> ps.createInclude().setName(pkg));
            }

            excludePackageNames.stream().map(PackageName::getName)
                .map(s -> s.replace('.', '/').replaceFirst("\\*$", "**"))
                .forEach(pkg -> ps.createExclude().setName(pkg));

            for (String pathElement : sourcePath.list()) {
                final File dir = new File(pathElement);
                if (dir.isDirectory()) {
                    final DirSet ds = new DirSet();
                    ds.setProject(getProject());
                    ds.setDefaultexcludes(useDefaultExcludes);
                    ds.setDir(dir);
                    ds.createPatternSet().addConfiguredPatternset(ps);
                    dirSets.add(ds);
                } else {
                    log("Skipping " + pathElement
                        + " since it is no directory.", Project.MSG_WARN);
                }
            }
        }

        for (DirSet ds : dirSets) {
            final File baseDir = ds.getDir(getProject());
            log("scanning " + baseDir + " for packages.", Project.MSG_DEBUG);
            final DirectoryScanner dsc = ds.getDirectoryScanner(getProject());
            boolean containsPackages = false;
            for (String dir : dsc.getIncludedDirectories()) {
                // are there any java files in this directory?
                final File pd = new File(baseDir, dir);
                final String[] files = pd.list((directory,
                    name) -> name.endsWith(".java") || (includeNoSourcePackages
                        && name.equals("package.html")));

                if (files.length > 0) {
                    if (dir.isEmpty()) {
                        log(baseDir
                            + " contains source files in the default package, you must specify them as source files not packages.",
                            Project.MSG_WARN);
                    } else {
                        containsPackages = true;
                        final String packageName =
                                dir.replace(File.separatorChar, '.');
                        if (!addedPackages.contains(packageName)) {
                            addedPackages.add(packageName);
                            pn.add(packageName);
                        }
                    }
                }
            }
            if (containsPackages) {
                // We don't need to care for duplicates here,
                // Path.list does it for us.
                sp.createPathElement().setLocation(baseDir);
            } else {
                log(baseDir + " doesn't contain any packages, dropping it.",
                    Project.MSG_VERBOSE);
            }
        }
    }

    private void postProcessGeneratedJavadocs() throws IOException {
        if (!postProcessGeneratedJavadocs) {
            return;
        }
        if (destDir != null && !destDir.isDirectory()) {
            log("No javadoc created, no need to post-process anything",
                Project.MSG_VERBOSE);
            return;
        }
        final InputStream in = Javadoc.class
            .getResourceAsStream("javadoc-frame-injections-fix.txt");
        if (in == null) {
            throw new FileNotFoundException(
                "Missing resource 'javadoc-frame-injections-fix.txt' in classpath.");
        }
        final String fixData;
        try {
            fixData =
                fixLineFeeds(FileUtils
                             .readFully(new InputStreamReader(in, StandardCharsets.US_ASCII)))
                .trim();
        } finally {
            FileUtils.close(in);
        }

        final DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(destDir);
        ds.setCaseSensitive(false);
        ds.setIncludes(new String[] {
                "**/index.html", "**/index.htm", "**/toc.html", "**/toc.htm"
            });
        ds.addDefaultExcludes();
        ds.scan();
        int patched = 0;
        for (final String f : ds.getIncludedFiles()) {
            patched += postProcess(new File(destDir, f), fixData);
        }
        if (patched > 0) {
            log("Patched " + patched + " link injection vulnerable javadocs",
                Project.MSG_INFO);
        }
    }

    private int postProcess(final File file, final String fixData) throws IOException {
        final String enc = docEncoding != null ? docEncoding
            : FILE_UTILS.getDefaultEncoding();
        // we load the whole file as one String (toc/index files are
        // generally small, because they only contain frameset declaration):
        String fileContents;
        try (InputStreamReader reader =
            new InputStreamReader(Files.newInputStream(file.toPath()), enc)) {
            fileContents = fixLineFeeds(FileUtils.safeReadFully(reader));
        }

        // check if file may be vulnerable because it was not
        // patched with "validURL(url)":
        if (!fileContents.contains("function validURL(url) {")) {
            // we need to patch the file!
            final String patchedFileContents = patchContent(fileContents, fixData);
            if (!patchedFileContents.equals(fileContents)) {
                try (final OutputStreamWriter w =
                    new OutputStreamWriter(Files.newOutputStream(file.toPath()), enc)) {
                    w.write(patchedFileContents);
                    w.close();
                    return 1;
                }
            }
        }
        return 0;
    }

    private String fixLineFeeds(final String orig) {
        return orig.replace("\r\n", "\n")
            .replace("\n", System.lineSeparator());
    }

    private String patchContent(final String fileContents, final String fixData) {
        // using regexes here looks like overkill
        final int start = fileContents.indexOf(LOAD_FRAME);
        if (start >= 0) {
            return fileContents.substring(0, start) + fixData
                + fileContents.substring(start + LOAD_FRAME_LEN);
        }
        return fileContents;
    }

    private void doModuleArguments(Commandline toExecute) {
        if (!moduleNames.isEmpty()) {
            toExecute.createArgument().setValue("--module");
            toExecute.createArgument()
                .setValue(moduleNames.stream().map(PackageName::getName)
                          .collect(Collectors.joining(",")));
        }
        if (modulePath != null) {
            toExecute.createArgument().setValue("--module-path");
            toExecute.createArgument().setPath(modulePath);
        }
        if (moduleSourcePath != null) {
            toExecute.createArgument().setValue("--module-source-path");
            toExecute.createArgument().setPath(moduleSourcePath);
        }
    }

    private class JavadocOutputStream extends LogOutputStream {
        JavadocOutputStream(final int level) {
            super(Javadoc.this, level);
        }

        //
        // Override the logging of output in order to filter out Generating
        // messages.  Generating messages are set to a priority of VERBOSE
        // unless they appear after what could be an informational message.
        //
        private String queuedLine = null;
        private boolean sawWarnings = false;

        @Override
        protected void processLine(final String line, final int messageLevel) {
            if (line.matches("(\\d) warning[s]?$")) {
                sawWarnings = true;
            }
            if (messageLevel == Project.MSG_INFO
                && line.startsWith("Generating ")) {
                if (queuedLine != null) {
                    super.processLine(queuedLine, Project.MSG_VERBOSE);
                }
                queuedLine = line;
            } else {
                if (queuedLine != null) {
                    if (line.startsWith("Building ")) {
                        super.processLine(queuedLine, Project.MSG_VERBOSE);
                    } else {
                        super.processLine(queuedLine, Project.MSG_INFO);
                    }
                    queuedLine = null;
                }
                super.processLine(line, messageLevel);
            }
        }

        protected void logFlush() {
            if (queuedLine != null) {
                super.processLine(queuedLine, Project.MSG_VERBOSE);
                queuedLine = null;
            }
        }

        public boolean sawWarnings() {
            return sawWarnings;
        }
    }

    /**
     * Convenience method to expand properties.
     * @param content the string to expand
     * @return the converted string
     */
    protected String expand(final String content) {
        return getProject().replaceProperties(content);
    }

}
