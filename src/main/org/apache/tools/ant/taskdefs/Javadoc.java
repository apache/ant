/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Locale;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
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
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Generates Javadoc documentation for a collection
 * of source code.
 *
 * <P>Current known limitations are:
 *
 * <P><UL>
 *    <LI>patterns must be of the form "xxx.*", every other pattern doesn't
 *        work.
 *    <LI>there is no control on arguments sanity since they are left
 *        to the javadoc implementation.
 *    <LI>argument J in javadoc1 is not supported (what is that for anyway?)
 * </UL>
 *
 * <P>If no <CODE>doclet</CODE> is set, then the <CODE>version</CODE> and
 * <CODE>author</CODE> are by default <CODE>"yes"</CODE>.
 *
 * <P>Note: This task is run on another VM because the Javadoc code calls
 * <CODE>System.exit()</CODE> which would break Ant functionality.
 *
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author Stefano Mazzocchi 
 *         <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author Patrick Chanezon 
 *         <a href="mailto:chanezon@netscape.com">chanezon@netscape.com</a>
 * @author Ernst de Haan <a href="mailto:ernst@jollem.com">ernst@jollem.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
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
     * @author Conor MacNeill
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
     * @author Conor MacNeill
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
         * @see java.lang.Object#toString
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

        public SourceFile() {}
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
     * An HTML element in the javadoc.
     *
     * This class is used for those javadoc elements which contain HTML such as
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
            return text.toString();
        }
    }

    /**
     * EnumeratedAttribute implementation supporting the javadoc scoping
     * values.
     */
    public static class AccessType extends EnumeratedAttribute {
        /**
         * @see EnumeratedAttribute#getValues().
         */
        public String[] getValues() {
            // Protected first so if any GUI tool offers a default
            // based on enum #0, it will be right.
            return new String[] {"protected", "public", "package", "private"};
        }
    }

    /** The command line built to execute Javadoc. */
    private Commandline cmd = new Commandline();

    /** Flag which indicates if javadoc from JDK 1.1 is to be used. */
    private static boolean javadoc1 = 
        JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1);

    /** Flag which indicates if javadoc from JDK 1.4 is available */
    private static boolean javadoc4 =
        (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1) &&
         !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2) &&
         !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3));

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
     * Utility method to add a non JDK1.1 javadoc argument.
     *
     * @param key the argument name.
     * @param value the argument value.
     */
    private void add12ArgIfNotEmpty(String key, String value) {
        if (!javadoc1) {
            if (value != null && value.length() != 0) {
                cmd.createArgument().setValue(key);
                cmd.createArgument().setValue(value);
            } else {
                log("Warning: Leaving out empty argument '" + key + "'", 
                    Project.MSG_WARN);
            }
        }
    }

    /**
     * Utility method to add a non-JDK1.1 argument to the command line 
     * conditionally based on the given flag.
     *
     * @param b the flag which controls if the argument is added.
     * @param arg the argument value.
     */
    private void add12ArgIf(boolean b, String arg) {
        if (!javadoc1 && b) {
            cmd.createArgument().setValue(arg);
        }
    }

    /** 
     * Flag which indicates if the task should fail if there is a
     * javadoc error.
     */
    private boolean failOnError = false;
    private Path sourcePath = null;
    private File destDir = null;
    private Vector sourceFiles = new Vector();
    private Vector packageNames = new Vector(5);
    private Vector excludePackageNames = new Vector(1);
    private boolean author = true;
    private boolean version = true;
    private DocletInfo doclet = null;
    private Path classpath = null;
    private Path bootclasspath = null;
    private String group = null;
    private String packageList = null;
    private Vector links = new Vector(2);
    private Vector groups = new Vector(2);
    private Vector tags = new Vector(5);
    private boolean useDefaultExcludes = true;
    private Html doctitle = null;
    private Html header = null;
    private Html footer = null;
    private Html bottom = null;
    private boolean useExternalFile = false;
    private FileUtils fileUtils = FileUtils.newFileUtils();
    private String source = null;

    private Vector fileSets = new Vector();
    private Vector packageSets = new Vector();

    /**
     * Work around command line length limit by using an external file
     * for the sourcefiles.
     *
     * @param b true if an external file is to be used.
     */
    public void setUseExternalFile(boolean b) {
        if (!javadoc1) {
            useExternalFile = b;
        }
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
    public void setMaxmemory(String max){
        if (javadoc1) {
            cmd.createArgument().setValue("-J-mx" + max);
        } else {
            cmd.createArgument().setValue("-J-Xmx" + max);
        }
    }

    /**
     * Set an additional parameter on the command line
     *
     * @param add the additional command line parameter for the javadoc task.
     */
    public void setAdditionalparam(String add){
        cmd.createArgument().setLine(add);
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
     * Add a package to be excluded from the javadoc run.
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
        if (!javadoc1) {
            cmd.createArgument().setValue("-overview");
            cmd.createArgument().setFile(f);
        }
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
        doclet = new DocletInfo();
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
        if (b) {
            if (javadoc1) {
                log("Javadoc 1.1 doesn't support the -1.1 switch", 
                    Project.MSG_WARN);
            } else if (javadoc4) {
                log("Javadoc 1.4 doesn't support the -1.1 switch anymore", 
                    Project.MSG_WARN);
            } else {
                cmd.createArgument().setValue("-1.1");
            }
        }
    }

    /**
     * Set the classpath to be used for this javadoc run.
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
     * @deprecated Use the {@link #setExtdirs(Path)} version.
     */
    public void setExtdirs(String path) {
        if (!javadoc1) {
            cmd.createArgument().setValue("-extdirs");
            cmd.createArgument().setValue(path);
        }
    }

    /**
     * Set the location of the extensions directories.
     *
     * @param path a path containing the extension directories.
     */
    public void setExtdirs(Path path) {
        if (!javadoc1) {
            cmd.createArgument().setValue("-extdirs");
            cmd.createArgument().setPath(path);
        }
    }

    /**
     * Run javadoc in verbose mode
     *
     * @param b true if operation is to be verbose.
     */
    public void setVerbose(boolean b) {
        add12ArgIf(b, "-verbose");
    }

    /**
     * Set the local to use in documentation generation.
     *
     * @param locale the locale to use.
     */
    public void setLocale(String locale) {
        if (!javadoc1) {
            // createArgument(true) is necessary to make sure, -locale
            // is the first argument (required in 1.3+).
            cmd.createArgument(true).setValue(locale);
            cmd.createArgument(true).setValue("-locale");
        }
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
        add12ArgIf(b, "-use");
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
        add12ArgIf(b, "-splitindex");
    }

    /**
     * Set the title to be placed in the HTML &lt;title&gt; tag of the 
     * generated documentation.
     *
     * @param title the window title to use.
     */
    public void setWindowtitle(String title) {
        add12ArgIfNotEmpty("-windowtitle", title);
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
        if (!javadoc1) {
            doctitle = text;
        }
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
        if (!javadoc1) {
            header = text;
        }
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
        if (!javadoc1) {
            footer = text;
        }
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
        if (!javadoc1) {
            bottom = text;
        }
    }

    /**
     * Link to docs at "url" using package list at "url2"
     * - separate the URLs by using a space character.
     */
    public void setLinkoffline(String src) {
        if (!javadoc1) {
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
    }

    /**
     * Group specified packages together in overview page.
     */
    public void setGroup(String src) {
        group = src;
    }

    /**
     * Create links to javadoc output at the given URL.
     */
    public void setLink(String src) {
        if (!javadoc1) {
            createLink().setHref(src);
        }
    }

    /**
     * If true, do not include @deprecated information.
     */
    public void setNodeprecated(boolean b) {
        addArgIf(b, "-nodeprecated");
    }

    /**
     * If true, do not generate deprecated list.
     */
    public void setNodeprecatedlist(boolean b) {
        add12ArgIf(b, "-nodeprecatedlist");
    }

    /**
     * If true, do not generate class hierarchy.
     */
    public void setNotree(boolean b) {
        addArgIf(b, "-notree");
    }

    /**
     * If true, do not generate index.
     */
    public void setNoindex(boolean b) {
        addArgIf(b, "-noindex");
    }

    /**
     * If true, do not generate help link
     */
    public void setNohelp(boolean b) {
        add12ArgIf(b, "-nohelp");
    }

    /**
     * If true, do not generate navigation bar.
     */
    public void setNonavbar(boolean b) {
        add12ArgIf(b, "-nonavbar");
    }

    /**
     * If true, generate warning about @serial tag.
     */
    public void setSerialwarn(boolean b) {
        add12ArgIf(b, "-serialwarn");
    }

    /**
     * Specifies the CSS stylesheet file to use.
     */
    public void setStylesheetfile(File f) {
        if (!javadoc1) {
            cmd.createArgument().setValue("-stylesheetfile");
            cmd.createArgument().setFile(f);
        }
    }

    /**
     * Specifies the HTML help file to use.
     */
    public void setHelpfile(File f) {
        if (!javadoc1) {
            cmd.createArgument().setValue("-helpfile");
            cmd.createArgument().setFile(f);
        }
    }

    /**
     * Output file encoding name.
     */
    public void setDocencoding(String enc) {
        cmd.createArgument().setValue("-docencoding");
        cmd.createArgument().setValue(enc);
    }

    /**
     * The name of a file containing the packages to process.
     */
    public void setPackageList(String src) {
        if (!javadoc1) {
            packageList = src;
        }
    }

    /**
     * Create link to javadoc output at the given URL.
     */
    public LinkArgument createLink() {
        LinkArgument la = new LinkArgument();
        links.addElement(la);
        return la;
    }

    public class LinkArgument {
        private String href;
        private boolean offline = false;
        private File packagelistLoc;

        public LinkArgument() {
        }

        public void setHref(String hr) {
            href = hr;
        }

        public String getHref() {
            return href;
        }

        public void setPackagelistLoc(File src) {
            packagelistLoc = src;
        }

        public File getPackagelistLoc() {
            return packagelistLoc;
        }

        public void setOffline(boolean offline) {
            this.offline = offline;
        }

        public boolean isLinkOffline() {
            return offline;
        }
    }

    /**
     * Creates and adds a -tag argument. This is used to specify
     * custom tags. This argument is only available for JavaDoc 1.4,
     * and will generate a verbose message (and then be ignored)
     * when run on Java versions below 1.4.
     */
    public TagArgument createTag() {
        if (!javadoc4) {
            log ("-tag option not supported on JavaDoc < 1.4", 
                 Project.MSG_VERBOSE);
        }
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
    public class TagArgument {
        /** Name of the tag. */
        private String name = null;
        /** Description of the tag to place in the JavaDocs. */
        private String description = null;
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
         * Sets the description of the tag. This is what appears in
         * the JavaDoc.
         * 
         * @param description The description of the tag. 
         *                    Must not be <code>null</code> or empty.
         */
        public void setDescription (String description) {
            this.description = description;
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
                        if (next.equals (SCOPE_ELEMENTS[i]))
                            break;
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
         * 
         * @exception BuildException if either the name or description
         *                           is <code>null</code> or empty.
         */
        public String getParameter () throws BuildException {
            if (name == null || name.equals("")) {
                throw new BuildException ("No name specified for custom tag.");
            }
            if (description == null || description.equals("")){
                throw new BuildException 
                    ("No description specified for custom tag " + name);
            }
            
            return name + ":" + (enabled ? "" : "X") 
                + scope + ":" + description;
        }
    }

    /**
     * Separates packages on the overview page into whatever
     * groups you specify, one group per table.
     */
    public GroupArgument createGroup() {
        GroupArgument ga = new GroupArgument();
        groups.addElement(ga);
        return ga;
    }

    public class GroupArgument {
        private Html title;
        private Vector packages = new Vector(3);

        public GroupArgument() {
        }

        public void setTitle(String src) {
            Html h = new Html();
            h.addText(src);
            addTitle(h);
        }
        public void addTitle(Html text) {
            title = text;
        }

        public String getTitle() {
            return title != null ? title.getText() : null;
        }

        public void setPackages(String src) {
            StringTokenizer tok = new StringTokenizer(src, ",");
            while (tok.hasMoreTokens()) {
                String p = tok.nextToken();
                PackageName pn = new PackageName();
                pn.setName(p);
                addPackage(pn);
            }
        }
        public void addPackage(PackageName pn) {
            packages.addElement(pn);
        }

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
     */
    public void setCharset(String src) {
        this.add12ArgIfNotEmpty("-charset", src);
    }

    /**
     * Should the build process fail if javadoc fails (as indicated by
     * a non zero return code)?
     *
     * <p>Default is false.</p>
     */
    public void setFailonerror(boolean b) {
        failOnError = b;
    }

    /**
     * Enables the -source switch, will be ignored if javadoc is not
     * the 1.4 version.
     *
     * @since Ant 1.5
     */
    public void setSource(String source) {
        if (!javadoc4) {
            log ("-source option not supported on JavaDoc < 1.4", 
                 Project.MSG_VERBOSE);
        }
        this.source = source;
    }

    /**
     * Adds a packageset.
     *
     * <p>All included directories will be translated into package
     * names be converting the directory separator into dots.</p>
     *
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
     *
     * @since 1.5
     */
    public void addFileset(FileSet fs) {
        fileSets.addElement(fs);
    }

    public void execute() throws BuildException {
        if ("javadoc2".equals(taskType)) {
            log("!! javadoc2 is deprecated. Use javadoc instead. !!");
        }

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
        addFileSets(sourceFilesToDoc);

        if (packageList == null && packagesToDoc.size() == 0 
            && sourceFilesToDoc.size() == 0) {
            throw new BuildException("No source files and no packages have "
                                     + "been specified.");
        }

        log("Generating Javadoc", Project.MSG_INFO);

        Commandline toExecute = (Commandline) cmd.clone();
        toExecute.setExecutable(JavaEnvUtils.getJdkExecutable("javadoc"));

        // ------------------------------------------ general javadoc arguments
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

        if (!javadoc1) {
            if (classpath.size() > 0) {
                toExecute.createArgument().setValue("-classpath");
                toExecute.createArgument().setPath(classpath);
            }
            if (sourceDirs.size() > 0) {
                toExecute.createArgument().setValue("-sourcepath");
                toExecute.createArgument().setPath(sourceDirs);
            }
        } else {
            sourceDirs.append(classpath);
            if (sourceDirs.size() > 0) {
                toExecute.createArgument().setValue("-classpath");
                toExecute.createArgument().setPath(sourceDirs);
            }
        }

        if (version && doclet == null) {
            toExecute.createArgument().setValue("-version");
        }
        if (author && doclet == null) {
            toExecute.createArgument().setValue("-author");
        }

        if (javadoc1 || doclet == null) {
            if (destDir == null) {
                String msg = "destDir attribute must be set!";
                throw new BuildException(msg);
            }
        }

        // ---------------------------- javadoc2 arguments for default doclet

        if (!javadoc1) {
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
            if (bootclasspath != null && bootclasspath.size() > 0) {
                toExecute.createArgument().setValue("-bootclasspath");
                toExecute.createArgument().setPath(bootclasspath);
            }

            // add the links arguments
            if (links.size() != 0) {
                for (Enumeration e = links.elements(); e.hasMoreElements();) {
                    LinkArgument la = (LinkArgument) e.nextElement();

                    if (la.getHref() == null || la.getHref().length() == 0) {
                        log("No href was given for the link - skipping", 
                            Project.MSG_VERBOSE);
                        continue;
                    } else {
                        // is the href a valid URL
                        try {
                            URL base = new URL("file://.");
                            new URL(base, la.getHref());
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
                            throw new BuildException("The package list "
                                + " location for link " + la.getHref()
                                + " must be provided because the link is "
                                + "offline");
                        }
                        File packageListFile = 
                            new File(packageListLocation, "package-list");
                        if (packageListFile.exists()) {
                            try {
                                String packageListURL =
                                    fileUtils.getFileURL(packageListLocation)
                                    .toExternalForm();
                                toExecute.createArgument()
                                    .setValue("-linkoffline");
                                toExecute.createArgument()
                                    .setValue(la.getHref());
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
                        toExecute.createArgument().setValue(la.getHref());
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
                    if (space > 0){
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

            // JavaDoc 1.4 parameters
            if (javadoc4) {
                for (Enumeration e = tags.elements(); e.hasMoreElements();) {
                    Object element = e.nextElement();
                    if (element instanceof TagArgument) {
                        TagArgument ta = (TagArgument) element;
                        toExecute.createArgument().setValue ("-tag");
                        toExecute.createArgument().setValue (ta.getParameter());
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

                if (source != null) {
                    toExecute.createArgument().setValue("-source");
                    toExecute.createArgument().setValue(source);
                }
            }

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
                    tmpList = fileUtils.createTempFile("javadoc", "", null);
                    toExecute.createArgument()
                        .setValue("@" + tmpList.getAbsolutePath());
                }
                srcListWriter = new PrintWriter(
                                    new FileWriter(tmpList.getAbsolutePath(),
                                                   true));
            }

            Enumeration enum = packagesToDoc.elements();
            while (enum.hasMoreElements()) {
                String packageName = (String) enum.nextElement();
                if (useExternalFile) {
                    srcListWriter.println(packageName);
                } else {
                    toExecute.createArgument().setValue(packageName);
                }
            }

            enum = sourceFilesToDoc.elements();
            while (enum.hasMoreElements()) {
                SourceFile sf = (SourceFile) enum.nextElement();
                String sourceFileName = sf.getFile().getAbsolutePath();
                if (useExternalFile) {
                    srcListWriter.println(sourceFileName);
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
                throw new BuildException("Javadoc returned " + ret, getLocation());
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
            } catch (IOException e) {}
        }
    }

    /**
     * Add the files matched by the nested filesets to the Vector as
     * SourceFile instances.
     *
     * @since 1.5
     */
    private void addFileSets(Vector sf) {
        Enumeration enum = fileSets.elements();
        while (enum.hasMoreElements()) {
            FileSet fs = (FileSet) enum.nextElement();
            if (!fs.hasPatterns() && !fs.hasSelectors()) {
                fs = (FileSet) fs.clone();
                fs.createInclude().setName("**/*.java");
            }
            File baseDir = fs.getDir(getProject());
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                sf.addElement(new SourceFile(new File(baseDir, files[i])));
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
        if (sourcePath != null && packageNames.size() > 0) {
            PatternSet ps = new PatternSet();
            Enumeration enum = packageNames.elements();
            while (enum.hasMoreElements()) {
                PackageName p = (PackageName) enum.nextElement();
                String pkg = p.getName().replace('.', '/');
                if (pkg.endsWith("*")) {
                    pkg += "*";
                }
                ps.createInclude().setName(pkg);
            }

            enum = excludePackageNames.elements();
            while (enum.hasMoreElements()) {
                PackageName p = (PackageName) enum.nextElement();
                String pkg = p.getName().replace('.', '/');
                if (pkg.endsWith("*")) {
                    pkg += "*";
                }
                ps.createExclude().setName(pkg);
            }


            String[] pathElements = sourcePath.list();
            for (int i = 0; i < pathElements.length; i++) {
                DirSet ds = new DirSet();
                ds.setDefaultexcludes(useDefaultExcludes);
                ds.setDir(new File(pathElements[i]));
                ds.createPatternSet().addConfiguredPatternset(ps);
                dirSets.addElement(ds);
            }
        }

        Enumeration enum = dirSets.elements();
        while (enum.hasMoreElements()) {
            DirSet ds = (DirSet) enum.nextElement();
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
                            if (name.endsWith(".java")) {
                                return true;
                            }
                            return false;        // ignore dirs
                        }
                    });

                if (files.length > 0) {
                    containsPackages = true;
                    String packageName = 
                        dirs[i].replace(File.separatorChar, '.');
                    if (!addedPackages.contains(packageName)) {
                        addedPackages.addElement(packageName);
                        pn.addElement(packageName);
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
     */
    protected String expand(String content) {
        return getProject().replaceProperties(content);
    }

}
