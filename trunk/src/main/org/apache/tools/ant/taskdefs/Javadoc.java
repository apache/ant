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
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;
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
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Generates Javadoc documentation for a collection
 * of source code.
 *
 * <p>Current known limitations are:
 *
 * <p><ul>
 *    <li>patterns must be of the form "xxx.*", every other pattern doesn't
 *        work.
 *    <li>there is no control on arguments sanity since they are left
 *        to the Javadoc implementation.
 * </ul>
 *
 * <p>If no <code>doclet</code> is set, then the <code>version</code> and
 * <code>author</code> are by default <code>"yes"</code>.
 *
 * <p>Note: This task is run on another VM because the Javadoc code calls
 * <code>System.exit()</code> which would break Ant functionality.
 *
 * @since Ant 1.1
 *
 * @ant.task category="java"
 */
public class Javadoc extends Task {
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
        public void setName(String name) {
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
        public void setValue(String value) {
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
        public void setName(String name) {
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
        public void setPath(Path path) {
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
        public void setPathRef(Reference r) {
            createPath().setRefid(r);
        }
    }

    /**
     * This class stores info about doclets.
     *
     */
    public class DocletInfo extends ExtensionInfo {

        /** Collection of doclet parameters. */
        private Vector params = new Vector();

        /**
         * Create a doclet parameter to be configured by Ant.
         *
         * @return a new DocletParam instance to be configured.
         */
        public DocletParam createParam() {
            DocletParam param = new DocletParam();
            params.addElement(param);

            return param;
        }

        /**
         * Get the doclet's parameters.
         *
         * @return an Enumeration of DocletParam instances.
         */
        public Enumeration getParams() {
            return params.elements();
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
        public void setName(String name) {
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
        public SourceFile(File file) {
            this.file = file;
        }

        /**
         * Set the source file.
         *
         * @param file the source file.
         */
        public void setFile(File file) {
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
        private StringBuffer text = new StringBuffer();

        /**
         * Add text to the element.
         *
         * @param t the text to be added.
         */
        public void addText(String t) {
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
    public class ResourceCollectionContainer {
        private ArrayList rcs = new ArrayList();
        /**
         * Add a resource collection to the container.
         * @param rc the collection to add.
         */
        public void add(ResourceCollection rc) {
            rcs.add(rc);
        }

        /**
         * Get an iterator on the collection.
         * @return an iterator.
         */
        private Iterator iterator() {
            return rcs.iterator();
        }
    }

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** The command line built to execute Javadoc. */
    private Commandline cmd = new Commandline();

    /**
     * Utility method to add an argument to the command line conditionally
     * based on the given flag.
     *
     * @param b the flag which controls if the argument is added.
     * @param arg the argument value.
     */
    private void addArgIf(boolean b, String arg) {
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
    private void addArgIfNotEmpty(String key, String value) {
        if (value != null && value.length() != 0) {
            cmd.createArgument().setValue(key);
            cmd.createArgument().setValue(value);
        } else {
            log("Warning: Leaving out empty argument '" + key + "'",
                Project.MSG_WARN);
        }
    }

    /**
     * Flag which indicates if the task should fail if there is a
     * Javadoc error.
     */
    private boolean failOnError = false;
    private Path sourcePath = null;
    private File destDir = null;
    private Vector sourceFiles = new Vector();
    private Vector packageNames = new Vector();
    private Vector excludePackageNames = new Vector(1);
    private boolean author = true;
    private boolean version = true;
    private DocletInfo doclet = null;
    private Path classpath = null;
    private Path bootclasspath = null;
    private String group = null;
    private String packageList = null;
    private Vector links = new Vector();
    private Vector groups = new Vector();
    private Vector tags = new Vector();
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
    private boolean old = false;
    private String executable = null;

    private ResourceCollectionContainer nestedSourceFiles
        = new ResourceCollectionContainer();
    private Vector packageSets = new Vector();

    /**
     * Work around command line length limit by using an external file
     * for the sourcefiles.
     *
     * @param b true if an external file is to be used.
     */
    public void setUseExternalFile(boolean b) {
        useExternalFile = b;
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *                           should be used, "false"|"off"|"no" when they
     *                           shouldn't be used.
     */
    public void setDefaultexcludes(boolean useDefaultExcludes) {
        this.useDefaultExcludes = useDefaultExcludes;
    }

    /**
     * Set the maximum memory to be used by the javadoc process
     *
     * @param max a string indicating the maximum memory according to the
     *        JVM conventions (e.g. 128m is 128 Megabytes)
     */
    public void setMaxmemory(String max) {
        cmd.createArgument().setValue("-J-Xmx" + max);
    }

    /**
     * Set an additional parameter on the command line
     *
     * @param add the additional command line parameter for the javadoc task.
     */
    public void setAdditionalparam(String add) {
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
    public void setSourcepath(Path src) {
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
    public void setSourcepathRef(Reference r) {
        createSourcepath().setRefid(r);
    }

    /**
     * Set the directory where the Javadoc output will be generated.
     *
     * @param dir the destination directory.
     */
    public void setDestdir(File dir) {
        destDir = dir;
        cmd.createArgument().setValue("-d");
        cmd.createArgument().setFile(destDir);
    }

    /**
     * Set the list of source files to process.
     *
     * @param src a comma separated list of source files.
     */
    public void setSourcefiles(String src) {
        StringTokenizer tok = new StringTokenizer(src, ",");
        while (tok.hasMoreTokens()) {
            String f = tok.nextToken();
            SourceFile sf = new SourceFile();
            sf.setFile(getProject().resolveFile(f.trim()));
            addSource(sf);
        }
    }

    /**
     * Add a single source file.
     *
     * @param sf the source file to be processed.
     */
    public void addSource(SourceFile sf) {
        sourceFiles.addElement(sf);
    }

    /**
     * Set the package names to be processed.
     *
     * @param packages a comma separated list of packages specs
     *        (may be wildcarded).
     *
     * @see #addPackage for wildcard information.
     */
    public void setPackagenames(String packages) {
        StringTokenizer tok = new StringTokenizer(packages, ",");
        while (tok.hasMoreTokens()) {
            String p = tok.nextToken();
            PackageName pn = new PackageName();
            pn.setName(p);
            addPackage(pn);
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
    public void addPackage(PackageName pn) {
        packageNames.addElement(pn);
    }

    /**
     * Set the list of packages to be excluded.
     *
     * @param packages a comma separated list of packages to be excluded.
     *        This may not include wildcards.
     */
    public void setExcludePackageNames(String packages) {
        StringTokenizer tok = new StringTokenizer(packages, ",");
        while (tok.hasMoreTokens()) {
            String p = tok.nextToken();
            PackageName pn = new PackageName();
            pn.setName(p);
            addExcludePackage(pn);
        }
    }

    /**
     * Add a package to be excluded from the Javadoc run.
     *
     * @param pn the name of the package (wildcards are not permitted).
     */
    public void addExcludePackage(PackageName pn) {
        excludePackageNames.addElement(pn);
    }

    /**
     * Specify the file containing the overview to be included in the generated
     * documentation.
     *
     * @param f the file containing the overview.
     */
    public void setOverview(File f) {
        cmd.createArgument().setValue("-overview");
        cmd.createArgument().setFile(f);
    }

    /**
     * Indicate whether only public classes and members are to be included in
     * the scope processed
     *
     * @param b true if scope is to be public.
     */
    public void setPublic(boolean b) {
        addArgIf(b, "-public");
    }

    /**
     * Indicate whether only protected and public classes and members are to
     * be included in the scope processed
     *
     * @param b true if scope is to be protected.
     */
    public void setProtected(boolean b) {
        addArgIf(b, "-protected");
    }

    /**
     * Indicate whether only package, protected and public classes and
     * members are to be included in the scope processed
     *
     * @param b true if scope is to be package level.
     */
    public void setPackage(boolean b) {
        addArgIf(b, "-package");
    }

    /**
     * Indicate whether all classes and
     * members are to be included in the scope processed
     *
     * @param b true if scope is to be private level.
     */
    public void setPrivate(boolean b) {
        addArgIf(b, "-private");
    }

    /**
     * Set the scope to be processed. This is an alternative to the
     * use of the setPublic, setPrivate, etc methods. It gives better build
     * file control over what scope is processed.
     *
     * @param at the scope to be processed.
     */
    public void setAccess(AccessType at) {
        cmd.createArgument().setValue("-" + at.getValue());
    }

    /**
     * Set the class that starts the doclet used in generating the
     * documentation.
     *
     * @param docletName the name of the doclet class.
     */
    public void setDoclet(String docletName) {
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
    public void setDocletPath(Path docletPath) {
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
    public void setDocletPathRef(Reference r) {
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
    public void addTaglet(ExtensionInfo tagletInfo) {
        tags.addElement(tagletInfo);
    }

    /**
     * Indicate whether Javadoc should produce old style (JDK 1.1)
     * documentation.
     *
     * This is not supported by JDK 1.1 and has been phased out in JDK 1.4
     *
     * @param b if true attempt to generate old style documentation.
     */
    public void setOld(boolean b) {
        old = b;
    }

    /**
     * Set the classpath to be used for this Javadoc run.
     *
     * @param path an Ant Path object containing the compilation
     *        classpath.
     */
    public void setClasspath(Path path) {
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
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Set the boot classpath to use.
     *
     * @param path the boot classpath.
     */
    public void setBootclasspath(Path path) {
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
    public void setBootClasspathRef(Reference r) {
        createBootclasspath().setRefid(r);
    }

    /**
     * Set the location of the extensions directories.
     *
     * @param path the string version of the path.
     * @deprecated since 1.5.x.
     *             Use the {@link #setExtdirs(Path)} version.
     */
    public void setExtdirs(String path) {
        cmd.createArgument().setValue("-extdirs");
        cmd.createArgument().setValue(path);
    }

    /**
     * Set the location of the extensions directories.
     *
     * @param path a path containing the extension directories.
     */
    public void setExtdirs(Path path) {
        cmd.createArgument().setValue("-extdirs");
        cmd.createArgument().setPath(path);
    }

    /**
     * Run javadoc in verbose mode
     *
     * @param b true if operation is to be verbose.
     */
    public void setVerbose(boolean b) {
        addArgIf(b, "-verbose");
    }

    /**
     * Set the local to use in documentation generation.
     *
     * @param locale the locale to use.
     */
    public void setLocale(String locale) {
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
    public void setEncoding(String enc) {
        cmd.createArgument().setValue("-encoding");
        cmd.createArgument().setValue(enc);
    }

    /**
     * Include the version tag in the generated documentation.
     *
     * @param b true if the version tag should be included.
     */
    public void setVersion(boolean b) {
        this.version = b;
    }

    /**
     * Generate the &quot;use&quot page for each package.
     *
     * @param b true if the use page should be generated.
     */
    public void setUse(boolean b) {
        addArgIf(b, "-use");
    }


    /**
     * Include the author tag in the generated documentation.
     *
     * @param b true if the author tag should be included.
     */
    public void setAuthor(boolean b) {
        author = b;
    }

    /**
     * Generate a split index
     *
     * @param b true if the index should be split into a file per letter.
     */
    public void setSplitindex(boolean b) {
        addArgIf(b, "-splitindex");
    }

    /**
     * Set the title to be placed in the HTML &lt;title&gt; tag of the
     * generated documentation.
     *
     * @param title the window title to use.
     */
    public void setWindowtitle(String title) {
        addArgIfNotEmpty("-windowtitle", title);
    }

    /**
     * Set the title of the generated overview page.
     *
     * @param doctitle the Document title.
     */
    public void setDoctitle(String doctitle) {
        Html h = new Html();
        h.addText(doctitle);
        addDoctitle(h);
    }

    /**
     * Add a document title to use for the overview page.
     *
     * @param text the HTML element containing the document title.
     */
    public void addDoctitle(Html text) {
        doctitle = text;
    }

    /**
     * Set the header text to be placed at the top of each output file.
     *
     * @param header the header text
     */
    public void setHeader(String header) {
        Html h = new Html();
        h.addText(header);
        addHeader(h);
    }

    /**
     * Set the header text to be placed at the top of each output file.
     *
     * @param text the header text
     */
    public void addHeader(Html text) {
        header = text;
    }

    /**
     * Set the footer text to be placed at the bottom of each output file.
     *
     * @param footer the footer text.
     */
    public void setFooter(String footer) {
        Html h = new Html();
        h.addText(footer);
        addFooter(h);
    }

    /**
     * Set the footer text to be placed at the bottom of each output file.
     *
     * @param text the footer text.
     */
    public void addFooter(Html text) {
        footer = text;
    }

    /**
     * Set the text to be placed at the bottom of each output file.
     *
     * @param bottom the bottom text.
     */
    public void setBottom(String bottom) {
        Html h = new Html();
        h.addText(bottom);
        addBottom(h);
    }

    /**
     * Set the text to be placed at the bottom of each output file.
     *
     * @param text the bottom text.
     */
    public void addBottom(Html text) {
        bottom = text;
    }

    /**
     * Link to docs at "url" using package list at "url2"
     * - separate the URLs by using a space character.
     *
     * @param src the offline link specification (url and package list)
     */
    public void setLinkoffline(String src) {
        LinkArgument le = createLink();
        le.setOffline(true);
        String linkOfflineError = "The linkoffline attribute must include"
            + " a URL and a package-list file location separated by a"
            + " space";
        if (src.trim().length() == 0) {
            throw new BuildException(linkOfflineError);
        }
        StringTokenizer tok = new StringTokenizer(src, " ", false);
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
    public void setGroup(String src) {
        group = src;
    }

    /**
     * Create links to Javadoc output at the given URL.
     * @param src the URL to link to
     */
    public void setLink(String src) {
        createLink().setHref(src);
    }

    /**
     * Control deprecation infromation
     *
     * @param b If true, do not include deprecated information.
     */
    public void setNodeprecated(boolean b) {
        addArgIf(b, "-nodeprecated");
    }

    /**
     * Control deprecated list generation
     *
     * @param b if true, do not generate deprecated list.
     */
    public void setNodeprecatedlist(boolean b) {
        addArgIf(b, "-nodeprecatedlist");
    }

    /**
     * Control class tree generation.
     *
     * @param b if true, do not generate class hierarchy.
     */
    public void setNotree(boolean b) {
        addArgIf(b, "-notree");
    }

    /**
     * Control generation of index.
     *
     * @param b if true, do not generate index.
     */
    public void setNoindex(boolean b) {
        addArgIf(b, "-noindex");
    }

    /**
     * Control generation of help link.
     *
     * @param b if true, do not generate help link
     */
    public void setNohelp(boolean b) {
        addArgIf(b, "-nohelp");
    }

    /**
     * Control generation of the navigation bar.
     *
     * @param b if true, do not generate navigation bar.
     */
    public void setNonavbar(boolean b) {
        addArgIf(b, "-nonavbar");
    }

    /**
     * Control warnings about serial tag.
     *
     * @param b if true, generate warning about the serial tag.
     */
    public void setSerialwarn(boolean b) {
        addArgIf(b, "-serialwarn");
    }

    /**
     * Specifies the CSS stylesheet file to use.
     *
     * @param f the file with the CSS to use.
     */
    public void setStylesheetfile(File f) {
        cmd.createArgument().setValue("-stylesheetfile");
        cmd.createArgument().setFile(f);
    }

    /**
     * Specifies the HTML help file to use.
     *
     * @param f the file containing help content.
     */
    public void setHelpfile(File f) {
        cmd.createArgument().setValue("-helpfile");
        cmd.createArgument().setFile(f);
    }

    /**
     * Output file encoding name.
     *
     * @param enc name of the encoding to use.
     */
    public void setDocencoding(String enc) {
        cmd.createArgument().setValue("-docencoding");
        cmd.createArgument().setValue(enc);
    }

    /**
     * The name of a file containing the packages to process.
     *
     * @param src the file containing the package list.
     */
    public void setPackageList(String src) {
        packageList = src;
    }

    /**
     * Create link to Javadoc output at the given URL.
     *
     * @return link argument to configure
     */
    public LinkArgument createLink() {
        LinkArgument la = new LinkArgument();
        links.addElement(la);
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
        private boolean resolveLink = false;

        /** Constructor for LinkArguement */
        public LinkArgument() {
            //empty
        }

        /**
         * Set the href attribute.
         * @param hr a <code>String</code> value
         */
        public void setHref(String hr) {
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
        public void setPackagelistLoc(File src) {
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
         * Set the offline attribute.
         * @param offline a <code>boolean</code> value
         */
        public void setOffline(boolean offline) {
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
        public void setResolveLink(boolean resolve) {
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
        TagArgument ta = new TagArgument();
        tags.addElement (ta);
        return ta;
    }

    /**
     * Scope element verbose names. (Defined here as fields
     * cannot be static in inner classes.) The first letter
     * from each element is used to build up the scope string.
     */
    static final String[] SCOPE_ELEMENTS = {
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
        public TagArgument () {
            //empty
        }

        /**
         * Sets the name of the tag.
         *
         * @param name The name of the tag.
         *             Must not be <code>null</code> or empty.
         */
        public void setName (String name) {
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
        public void setScope (String verboseScope) throws BuildException {
            verboseScope = verboseScope.toLowerCase(Locale.US);

            boolean[] elements = new boolean[SCOPE_ELEMENTS.length];

            boolean gotAll = false;
            boolean gotNotAll = false;

            // Go through the tokens one at a time, updating the
            // elements array and issuing warnings where appropriate.
            StringTokenizer tok = new StringTokenizer (verboseScope, ",");
            while (tok.hasMoreTokens()) {
                String next = tok.nextToken().trim();
                if (next.equals("all")) {
                    if (gotAll) {
                        getProject().log ("Repeated tag scope element: all",
                                          Project.MSG_VERBOSE);
                    }
                    gotAll = true;
                } else {
                    int i;
                    for (i = 0; i < SCOPE_ELEMENTS.length; i++) {
                        if (next.equals (SCOPE_ELEMENTS[i])) {
                            break;
                        }
                    }
                    if (i == SCOPE_ELEMENTS.length) {
                        throw new BuildException ("Unrecognised scope element: "
                                                  + next);
                    } else {
                        if (elements[i]) {
                            getProject().log ("Repeated tag scope element: "
                                              + next, Project.MSG_VERBOSE);
                        }
                        elements[i] = true;
                        gotNotAll = true;
                    }
                }
            }

            if (gotNotAll && gotAll) {
                throw new BuildException ("Mixture of \"all\" and other scope "
                                          + "elements in tag parameter.");
            }
            if (!gotNotAll && !gotAll) {
                throw new BuildException ("No scope elements specified in tag "
                                          + "parameter.");
            }
            if (gotAll) {
                this.scope = "a";
            } else {
                StringBuffer buff = new StringBuffer (elements.length);
                for (int i = 0; i < elements.length; i++) {
                    if (elements[i]) {
                        buff.append (SCOPE_ELEMENTS[i].charAt(0));
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
        public void setEnabled (boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the -tag parameter this argument represented.
         * @return the -tag parameter as a string
         * @exception BuildException if either the name or description
         *                           is <code>null</code> or empty.
         */
        public String getParameter() throws BuildException {
            if (name == null || name.equals("")) {
                throw new BuildException ("No name specified for custom tag.");
            }
            if (getDescription() != null) {
                return name + ":" + (enabled ? "" : "X")
                    + scope + ":" + getDescription();
            } else {
                return name + ":" + (enabled ? "" : "X")
                    + scope + ":" + name;
            }
        }
    }

    /**
     * Separates packages on the overview page into whatever
     * groups you specify, one group per table.
     * @return a group argument to be configured
     */
    public GroupArgument createGroup() {
        GroupArgument ga = new GroupArgument();
        groups.addElement(ga);
        return ga;
    }


    /**
     * A class corresponding to the group nested element.
     */
    public class GroupArgument {
        private Html title;
        private Vector packages = new Vector();

        /** Constructor for GroupArgument */
        public GroupArgument() {
            //empty
        }

        /**
         * Set the title attribute using a string.
         * @param src a <code>String</code> value
         */
        public void setTitle(String src) {
            Html h = new Html();
            h.addText(src);
            addTitle(h);
        }
        /**
         * Set the title attribute using a nested Html value.
         * @param text a <code>Html</code> value
         */
        public void addTitle(Html text) {
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
        public void setPackages(String src) {
            StringTokenizer tok = new StringTokenizer(src, ",");
            while (tok.hasMoreTokens()) {
                String p = tok.nextToken();
                PackageName pn = new PackageName();
                pn.setName(p);
                addPackage(pn);
            }
        }
        /**
         * Add a package nested element.
         * @param pn a nested element specifing the package.
         */
        public void addPackage(PackageName pn) {
            packages.addElement(pn);
        }

        /**
         * Get the packages as a collon separated list.
         * @return the packages as a string
         */
        public String getPackages() {
            StringBuffer p = new StringBuffer();
            for (int i = 0; i < packages.size(); i++) {
                if (i > 0) {
                    p.append(":");
                }
                p.append(packages.elementAt(i).toString());
            }
            return p.toString();
        }
    }

    /**
     * Charset for cross-platform viewing of generated documentation.
     * @param src the name of the charset
     */
    public void setCharset(String src) {
        this.addArgIfNotEmpty("-charset", src);
    }

    /**
     * Should the build process fail if Javadoc fails (as indicated by
     * a non zero return code)?
     *
     * <p>Default is false.</p>
     * @param b a <code>boolean</code> value
     */
    public void setFailonerror(boolean b) {
        failOnError = b;
    }

    /**
     * Enables the -source switch, will be ignored if Javadoc is not
     * the 1.4 version.
     * @param source a <code>String</code> value
     * @since Ant 1.5
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Sets the actual executable command to invoke, instead of the binary
     * <code>javadoc</code> found in Ant's JDK.
     * @param executable the command to invoke.
     * @since Ant 1.6.3
     */
    public void setExecutable(String executable) {
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
    public void addPackageset(DirSet packageSet) {
        packageSets.addElement(packageSet);
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
    public void addFileset(FileSet fs) {
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
    public void setLinksource(boolean b) {
        this.linksource = b;
    }

    /**
     * Enables the -linksource switch, will be ignored if Javadoc is not
     * the 1.4 version. Default is false
     * @param b a <code>String</code> value
     * @since Ant 1.6
     */
    public void setBreakiterator(boolean b) {
        this.breakiterator = b;
    }

    /**
     * Enables the -noqualifier switch, will be ignored if Javadoc is not
     * the 1.4 version.
     * @param noqualifier the parameter to the -noqualifier switch
     * @since Ant 1.6
     */
    public void setNoqualifier(String noqualifier) {
        this.noqualifier = noqualifier;
    }

    /**
     * If set to true, Ant will also accept packages that only hold
     * package.html files but no Java sources.
     * @param b a <code>boolean</code> value.
     * @since Ant 1.6.3
     */
    public void setIncludeNoSourcePackages(boolean b) {
        this.includeNoSourcePackages = b;
    }

    /**
     * Execute the task.
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        if ("javadoc2".equals(getTaskType())) {
            log("Warning: the task name <javadoc2> is deprecated. Use <javadoc> instead.",
                Project.MSG_WARN);
        }

        // Whether *this VM* is 1.4+ (but also check executable != null).
        boolean javadoc4 =
            !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)
            && !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3);
        boolean javadoc5 = javadoc4
            && !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_4);

        Vector packagesToDoc = new Vector();
        Path sourceDirs = new Path(getProject());

        if (packageList != null && sourcePath == null) {
            String msg = "sourcePath attribute must be set when "
                + "specifying packagelist.";
            throw new BuildException(msg);
        }

        if (sourcePath != null) {
            sourceDirs.addExisting(sourcePath);
        }

        parsePackages(packagesToDoc, sourceDirs);

        if (packagesToDoc.size() != 0 && sourceDirs.size() == 0) {
            String msg = "sourcePath attribute must be set when "
                + "specifying package names.";
            throw new BuildException(msg);
        }

        Vector sourceFilesToDoc = (Vector) sourceFiles.clone();
        addSourceFiles(sourceFilesToDoc);

        if (packageList == null && packagesToDoc.size() == 0
            && sourceFilesToDoc.size() == 0) {
            throw new BuildException("No source files and no packages have "
                                     + "been specified.");
        }

        log("Generating Javadoc", Project.MSG_INFO);

        Commandline toExecute = (Commandline) cmd.clone();
        if (executable != null) {
            toExecute.setExecutable(executable);
        } else {
            toExecute.setExecutable(JavaEnvUtils.getJdkExecutable("javadoc"));
        }

        // ------------------------------------------ general Javadoc arguments
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
            classpath = (new Path(getProject())).concatSystemClasspath("last");
        } else {
            classpath = classpath.concatSystemClasspath("ignore");
        }

        if (classpath.size() > 0) {
            toExecute.createArgument().setValue("-classpath");
            toExecute.createArgument().setPath(classpath);
        }
        if (sourceDirs.size() > 0) {
            toExecute.createArgument().setValue("-sourcepath");
            toExecute.createArgument().setPath(sourceDirs);
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

        // ---------------------------- javadoc2 arguments for default doclet

        if (doclet != null) {
            if (doclet.getName() == null) {
                throw new BuildException("The doclet name must be "
                                         + "specified.", getLocation());
            } else {
                toExecute.createArgument().setValue("-doclet");
                toExecute.createArgument().setValue(doclet.getName());
                if (doclet.getPath() != null) {
                    Path docletPath
                        = doclet.getPath().concatSystemClasspath("ignore");
                    if (docletPath.size() != 0) {
                        toExecute.createArgument().setValue("-docletpath");
                        toExecute.createArgument().setPath(docletPath);
                    }
                }
                for (Enumeration e = doclet.getParams();
                     e.hasMoreElements();) {
                    DocletParam param = (DocletParam) e.nextElement();
                    if (param.getName() == null) {
                        throw new BuildException("Doclet parameters must "
                                                 + "have a name");
                    }

                    toExecute.createArgument().setValue(param.getName());
                    if (param.getValue() != null) {
                        toExecute.createArgument()
                            .setValue(param.getValue());
                    }
                }
            }
        }
        Path bcp = new Path(getProject());
        if (bootclasspath != null) {
            bcp.append(bootclasspath);
        }
        bcp = bcp.concatSystemBootClasspath("ignore");
        if (bcp.size() > 0) {
            toExecute.createArgument().setValue("-bootclasspath");
            toExecute.createArgument().setPath(bcp);
        }

        // add the links arguments
        if (links.size() != 0) {
            for (Enumeration e = links.elements(); e.hasMoreElements();) {
                LinkArgument la = (LinkArgument) e.nextElement();

                if (la.getHref() == null || la.getHref().length() == 0) {
                    log("No href was given for the link - skipping",
                        Project.MSG_VERBOSE);
                    continue;
                }
                String link = null;
                if (la.shouldResolveLink()) {
                    File hrefAsFile =
                        getProject().resolveFile(la.getHref());
                    if (hrefAsFile.exists()) {
                        try {
                            link = FILE_UTILS.getFileURL(hrefAsFile)
                                .toExternalForm();
                        } catch (MalformedURLException ex) {
                            // should be impossible
                            log("Warning: link location was invalid "
                                + hrefAsFile, Project.MSG_WARN);
                        }
                    }
                }
                if (link == null) {
                    // is the href a valid URL
                    try {
                        URL base = new URL("file://.");
                        new URL(base, la.getHref());
                        link = la.getHref();
                    } catch (MalformedURLException mue) {
                        // ok - just skip
                        log("Link href \"" + la.getHref()
                            + "\" is not a valid url - skipping link",
                            Project.MSG_WARN);
                        continue;
                    }
                }

                if (la.isLinkOffline()) {
                    File packageListLocation = la.getPackagelistLoc();
                    if (packageListLocation == null) {
                        throw new BuildException("The package list"
                                                 + " location for link "
                                                 + la.getHref()
                                                 + " must be provided "
                                                 + "because the link is "
                                                 + "offline");
                    }
                    File packageListFile =
                        new File(packageListLocation, "package-list");
                    if (packageListFile.exists()) {
                        try {
                            String packageListURL =
                                FILE_UTILS.getFileURL(packageListLocation)
                                .toExternalForm();
                            toExecute.createArgument()
                                .setValue("-linkoffline");
                            toExecute.createArgument()
                                .setValue(link);
                            toExecute.createArgument()
                                .setValue(packageListURL);
                        } catch (MalformedURLException ex) {
                            log("Warning: Package list location was "
                                + "invalid " + packageListLocation,
                                Project.MSG_WARN);
                        }
                    } else {
                        log("Warning: No package list was found at "
                            + packageListLocation, Project.MSG_VERBOSE);
                    }
                } else {
                    toExecute.createArgument().setValue("-link");
                    toExecute.createArgument().setValue(link);
                }
            }
        }

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
            StringTokenizer tok = new StringTokenizer(group, ",", false);
            while (tok.hasMoreTokens()) {
                String grp = tok.nextToken().trim();
                int space = grp.indexOf(" ");
                if (space > 0) {
                    String name = grp.substring(0, space);
                    String pkgList = grp.substring(space + 1);
                    toExecute.createArgument().setValue("-group");
                    toExecute.createArgument().setValue(name);
                    toExecute.createArgument().setValue(pkgList);
                }
            }
        }

        // add the group arguments
        if (groups.size() != 0) {
            for (Enumeration e = groups.elements(); e.hasMoreElements();) {
                GroupArgument ga = (GroupArgument) e.nextElement();
                String title = ga.getTitle();
                String packages = ga.getPackages();
                if (title == null || packages == null) {
                    throw new BuildException("The title and packages must "
                                             + "be specified for group "
                                             + "elements.");
                }
                toExecute.createArgument().setValue("-group");
                toExecute.createArgument().setValue(expand(title));
                toExecute.createArgument().setValue(packages);
            }
        }

        // Javadoc 1.4 parameters
        if (javadoc4 || executable != null) {
            for (Enumeration e = tags.elements(); e.hasMoreElements();) {
                Object element = e.nextElement();
                if (element instanceof TagArgument) {
                    TagArgument ta = (TagArgument) element;
                    File tagDir = ta.getDir(getProject());
                    if (tagDir == null) {
                        // The tag element is not used as a fileset,
                        // but specifies the tag directly.
                        toExecute.createArgument().setValue ("-tag");
                        toExecute.createArgument()
                            .setValue (ta.getParameter());
                    } else {
                        // The tag element is used as a
                        // fileset. Parse all the files and create
                        // -tag arguments.
                        DirectoryScanner tagDefScanner =
                            ta.getDirectoryScanner(getProject());
                        String[] files = tagDefScanner.getIncludedFiles();
                        for (int i = 0; i < files.length; i++) {
                            File tagDefFile = new File(tagDir, files[i]);
                            try {
                                BufferedReader in
                                    = new BufferedReader(
                                          new FileReader(tagDefFile)
                                          );
                                String line = null;
                                while ((line = in.readLine()) != null) {
                                    toExecute.createArgument()
                                        .setValue("-tag");
                                    toExecute.createArgument()
                                        .setValue(line);
                                }
                                in.close();
                            } catch (IOException ioe) {
                                throw new BuildException("Couldn't read "
                                    + " tag file from "
                                    + tagDefFile.getAbsolutePath(), ioe);
                            }
                        }
                    }
                } else {
                    ExtensionInfo tagletInfo = (ExtensionInfo) element;
                    toExecute.createArgument().setValue("-taglet");
                    toExecute.createArgument().setValue(tagletInfo
                                                        .getName());
                    if (tagletInfo.getPath() != null) {
                        Path tagletPath = tagletInfo.getPath()
                            .concatSystemClasspath("ignore");
                        if (tagletPath.size() != 0) {
                            toExecute.createArgument()
                                .setValue("-tagletpath");
                            toExecute.createArgument().setPath(tagletPath);
                        }
                    }
                }
            }

            String sourceArg = source != null ? source
                : getProject().getProperty(MagicNames.BUILD_JAVAC_SOURCE);
            if (sourceArg != null) {
                toExecute.createArgument().setValue("-source");
                toExecute.createArgument().setValue(sourceArg);
            }

            if (linksource && doclet == null) {
                toExecute.createArgument().setValue("-linksource");
            }
            if (breakiterator && (doclet == null || javadoc5)) {
                toExecute.createArgument().setValue("-breakiterator");
            }
            if (noqualifier != null && doclet == null) {
                toExecute.createArgument().setValue("-noqualifier");
                toExecute.createArgument().setValue(noqualifier);
            }
        } else {
            // Not 1.4+.
            if (!tags.isEmpty()) {
                log("-tag and -taglet options not supported on Javadoc < 1.4",
                     Project.MSG_VERBOSE);
            }
            if (source != null) {
                log("-source option not supported on Javadoc < 1.4",
                     Project.MSG_VERBOSE);
            }
            if (linksource) {
                log("-linksource option not supported on Javadoc < 1.4",
                     Project.MSG_VERBOSE);
            }
            if (breakiterator) {
                log("-breakiterator option not supported on Javadoc < 1.4",
                     Project.MSG_VERBOSE);
            }
            if (noqualifier != null) {
                log("-noqualifier option not supported on Javadoc < 1.4",
                     Project.MSG_VERBOSE);
            }
        }
        // Javadoc 1.2/1.3 parameters:
        if (!javadoc4 || executable != null) {
            if (old) {
                toExecute.createArgument().setValue("-1.1");
            }
        } else {
            if (old) {
                log("Javadoc 1.4 doesn't support the -1.1 switch anymore",
                    Project.MSG_WARN);
            }
        }
        // If using an external file, write the command line options to it
        if (useExternalFile && javadoc4) {
            writeExternalArgs(toExecute);
        }

        File tmpList = null;
        PrintWriter srcListWriter = null;

        try {

            /**
             * Write sourcefiles and package names to a temporary file
             * if requested.
             */
            if (useExternalFile) {
                if (tmpList == null) {
                    tmpList = FILE_UTILS.createTempFile("javadoc", "", null);
                    tmpList.deleteOnExit();
                    toExecute.createArgument()
                        .setValue("@" + tmpList.getAbsolutePath());
                }
                srcListWriter = new PrintWriter(
                                    new FileWriter(tmpList.getAbsolutePath(),
                                                   true));
            }

            Enumeration e = packagesToDoc.elements();
            while (e.hasMoreElements()) {
                String packageName = (String) e.nextElement();
                if (useExternalFile) {
                    srcListWriter.println(packageName);
                } else {
                    toExecute.createArgument().setValue(packageName);
                }
            }

            e = sourceFilesToDoc.elements();
            while (e.hasMoreElements()) {
                SourceFile sf = (SourceFile) e.nextElement();
                String sourceFileName = sf.getFile().getAbsolutePath();
                if (useExternalFile) {
                    // XXX what is the following doing?
                    //     should it run if !javadoc4 && executable != null?
                    if (javadoc4 && sourceFileName.indexOf(" ") > -1) {
                        String name = sourceFileName;
                        if (File.separatorChar == '\\') {
                            name = sourceFileName.replace(File.separatorChar, '/');
                        }
                        srcListWriter.println("\"" + name + "\"");
                    } else {
                        srcListWriter.println(sourceFileName);
                    }
                } else {
                    toExecute.createArgument().setValue(sourceFileName);
                }
            }

        } catch (IOException e) {
            tmpList.delete();
            throw new BuildException("Error creating temporary file",
                                     e, getLocation());
        } finally {
            if (srcListWriter != null) {
                srcListWriter.close();
            }
        }

        if (packageList != null) {
            toExecute.createArgument().setValue("@" + packageList);
        }
        log(toExecute.describeCommand(), Project.MSG_VERBOSE);

        log("Javadoc execution", Project.MSG_INFO);

        JavadocOutputStream out = new JavadocOutputStream(Project.MSG_INFO);
        JavadocOutputStream err = new JavadocOutputStream(Project.MSG_WARN);
        Execute exe = new Execute(new PumpStreamHandler(out, err));
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
            int ret = exe.execute();
            if (ret != 0 && failOnError) {
                throw new BuildException("Javadoc returned " + ret,
                                         getLocation());
            }
        } catch (IOException e) {
            throw new BuildException("Javadoc failed: " + e, e, getLocation());
        } finally {
            if (tmpList != null) {
                tmpList.delete();
                tmpList = null;
            }

            out.logFlush();
            err.logFlush();
            try {
                out.close();
                err.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void writeExternalArgs(Commandline toExecute) {
        // If using an external file, write the command line options to it
        File optionsTmpFile = null;
        PrintWriter optionsListWriter = null;
        try {
            optionsTmpFile = FILE_UTILS.createTempFile(
                "javadocOptions", "", null);
            optionsTmpFile.deleteOnExit();
            String[] listOpt = toExecute.getArguments();
            toExecute.clearArgs();
            toExecute.createArgument().setValue(
                "@" + optionsTmpFile.getAbsolutePath());
            optionsListWriter = new PrintWriter(
                new FileWriter(optionsTmpFile.getAbsolutePath(), true));
            for (int i = 0; i < listOpt.length; i++) {
                String string = listOpt[i];
                if (string.startsWith("-J-")) {
                    toExecute.createArgument().setValue(string);
                } else  {
                    if (string.startsWith("-")) {
                        optionsListWriter.print(string);
                        optionsListWriter.print(" ");
                    } else {
                        optionsListWriter.println(quoteString(string));
                    }
                }
            }
            optionsListWriter.close();
        } catch (IOException ex) {
            if (optionsTmpFile != null) {
                optionsTmpFile.delete();
            }
            throw new BuildException(
                "Error creating or writing temporary file for javadoc options",
                ex, getLocation());
        } finally {
            FILE_UTILS.close(optionsListWriter);
        }
    }

    /**
     * Quote a string to place in a @ file.
     * @param str the string to quote
     * @return the quoted string, if there is no need to quote the string,
     *         return the original string.
     */
    private String quoteString(String str) {
        if (str.indexOf(' ') == -1
            && str.indexOf('\'') == -1
            && str.indexOf('"') == -1) {
            return str;
        }
        if (str.indexOf('\'') == -1) {
            return quoteString(str, '\'');
        } else {
            return quoteString(str, '"');
        }
    }

    private String quoteString(String str, char delim) {
        StringBuffer buf = new StringBuffer(str.length() * 2);
        buf.append(delim);
        if (str.indexOf('\\') != -1) {
            str = replace(str, '\\', "\\\\");
        }
        if (str.indexOf(delim) != -1) {
            str = replace(str, delim, "\\" + delim);
        }
        buf.append(str);
        buf.append(delim);
        return buf.toString();
    }

    private String replace(String str, char fromChar, String toString) {
        StringBuffer buf = new StringBuffer(str.length() * 2);
        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            if (ch == fromChar) {
                buf.append(toString);
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    /**
     * Add the files matched by the nested source files to the Vector
     * as SourceFile instances.
     *
     * @since 1.7
     */
    private void addSourceFiles(Vector sf) {
        Iterator e = nestedSourceFiles.iterator();
        while (e.hasNext()) {
            ResourceCollection rc = (ResourceCollection) e.next();
            if (!rc.isFilesystemOnly()) {
                throw new BuildException("only file system based resources are"
                                         + " supported by javadoc");
            }
            if (rc instanceof FileSet) {
                FileSet fs = (FileSet) rc;
                if (!fs.hasPatterns() && !fs.hasSelectors()) {
                    fs = (FileSet) fs.clone();
                    fs.createInclude().setName("**/*.java");
                    if (includeNoSourcePackages) {
                        fs.createInclude().setName("**/package.html");
                    }
                }
            }
            Iterator iter = rc.iterator();
            while (iter.hasNext()) {
                sf.addElement(new SourceFile(((FileResource) iter.next())
                                             .getFile()));
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
    private void parsePackages(Vector pn, Path sp) {
        Vector addedPackages = new Vector();
        Vector dirSets = (Vector) packageSets.clone();

        // for each sourcePath entry, add a directoryset with includes
        // taken from packagenames attribute and nested package
        // elements and excludes taken from excludepackages attribute
        // and nested excludepackage elements
        if (sourcePath != null) {
            PatternSet ps = new PatternSet();
            if (packageNames.size() > 0) {
                Enumeration e = packageNames.elements();
                while (e.hasMoreElements()) {
                    PackageName p = (PackageName) e.nextElement();
                    String pkg = p.getName().replace('.', '/');
                    if (pkg.endsWith("*")) {
                        pkg += "*";
                    }
                    ps.createInclude().setName(pkg);
                }
            } else {
                ps.createInclude().setName("**");
            }

            Enumeration e = excludePackageNames.elements();
            while (e.hasMoreElements()) {
                PackageName p = (PackageName) e.nextElement();
                String pkg = p.getName().replace('.', '/');
                if (pkg.endsWith("*")) {
                    pkg += "*";
                }
                ps.createExclude().setName(pkg);
            }


            String[] pathElements = sourcePath.list();
            for (int i = 0; i < pathElements.length; i++) {
                File dir = new File(pathElements[i]);
                if (dir.isDirectory()) {
                    DirSet ds = new DirSet();
                    ds.setDefaultexcludes(useDefaultExcludes);
                    ds.setDir(dir);
                    ds.createPatternSet().addConfiguredPatternset(ps);
                    dirSets.addElement(ds);
                } else {
                    log("Skipping " + pathElements[i]
                        + " since it is no directory.", Project.MSG_WARN);
                }
            }
        }

        Enumeration e = dirSets.elements();
        while (e.hasMoreElements()) {
            DirSet ds = (DirSet) e.nextElement();
            File baseDir = ds.getDir(getProject());
            log("scanning " + baseDir + " for packages.", Project.MSG_DEBUG);
            DirectoryScanner dsc = ds.getDirectoryScanner(getProject());
            String[] dirs = dsc.getIncludedDirectories();
            boolean containsPackages = false;
            for (int i = 0; i < dirs.length; i++) {
                // are there any java files in this directory?
                File pd = new File(baseDir, dirs[i]);
                String[] files = pd.list(new FilenameFilter () {
                        public boolean accept(File dir1, String name) {
                            return name.endsWith(".java")
                                || (includeNoSourcePackages
                                    && name.equals("package.html"));
                        }
                    });

                if (files.length > 0) {
                    if ("".equals(dirs[i])) {
                        log(baseDir
                            + " contains source files in the default package,"
                            + " you must specify them as source files"
                            + " not packages.",
                            Project.MSG_WARN);
                    } else {
                        containsPackages = true;
                        String packageName =
                            dirs[i].replace(File.separatorChar, '.');
                        if (!addedPackages.contains(packageName)) {
                            addedPackages.addElement(packageName);
                            pn.addElement(packageName);
                        }
                    }
                }
            }
            if (containsPackages) {
                // We don't need to care for duplicates here,
                // Path.list does it for us.
                sp.createPathElement().setLocation(baseDir);
            } else {
                log(baseDir + " doesn\'t contain any packages, dropping it.",
                    Project.MSG_VERBOSE);
            }
        }
    }

    private class JavadocOutputStream extends LogOutputStream {
        JavadocOutputStream(int level) {
            super(Javadoc.this, level);
        }

        //
        // Override the logging of output in order to filter out Generating
        // messages.  Generating messages are set to a priority of VERBOSE
        // unless they appear after what could be an informational message.
        //
        private String queuedLine = null;
        protected void processLine(String line, int messageLevel) {
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
    }

    /**
     * Convenience method to expand properties.
     * @param content the string to expand
     * @return the converted string
     */
    protected String expand(String content) {
        return getProject().replaceProperties(content);
    }

}
