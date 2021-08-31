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

package org.apache.tools.ant.types;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.PathTokenizer;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.FileResourceIterator;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * This object represents a path as used by CLASSPATH or PATH
 * environment variable. A path might also be described as a collection
 * of unique filesystem resources.
 * <pre>
 * &lt;sometask&gt;
 *   &lt;somepath&gt;
 *     &lt;pathelement location="/path/to/file.jar"/&gt;
 *     &lt;pathelement path="/path/to/file2.jar:/path/to/class2;/path/to/class3"/&gt;
 *     &lt;pathelement location="/path/to/file3.jar"/&gt;
 *     &lt;pathelement location="/path/to/file4.jar"/&gt;
 *   &lt;/somepath&gt;
 * &lt;/sometask&gt;
 * </pre>
 * <p>
 * The object implementation <code>sometask</code> must provide a method called
 * <code>createSomepath</code> which returns an instance of <code>Path</code>.
 * Nested path definitions are handled by the Path object and must be labeled
 * <code>pathelement</code>.
 * </p>
 * <p>
 * The path element takes a parameter <code>path</code> which will be parsed
 * and split into single elements. It will usually be used
 * to define a path from an environment variable.
 * </p>
 */

public class Path extends DataType implements Cloneable, ResourceCollection {
    // CheckStyle:VisibilityModifier OFF - bc

    // non-final as some IDE integrations (at least Eclipse) want to override it
    /** The system classpath as a Path object */
    public static Path systemClasspath = //NOSONAR
        new Path(null, System.getProperty("java.class.path"));

    /**
     * The system bootclasspath as a Path object.
     *
     * @since Ant 1.6.2
     */
    public static final Path systemBootClasspath =
        new Path(null, System.getProperty("sun.boot.class.path"));

    // CheckStyle:VisibilityModifier OFF - bc

    /**
     * Helper class, holds the nested <code>&lt;pathelement&gt;</code> values.
     */
    public class PathElement implements ResourceCollection {
        private String[] parts;

        /**
         * Set the location.
         *
         * @param loc a <code>File</code> value
         */
        public void setLocation(File loc) {
            parts = new String[] {translateFile(loc.getAbsolutePath())};
        }

        /**
         * Set the path.
         *
         * @param path a <code>String</code> value
         */
        public void setPath(String path) {
            parts = Path.translatePath(getProject(), path);
        }

        /**
         * Return the converted pathelements.
         *
         * @return a <code>String[]</code> value
         */
        public String[] getParts() {
            return parts;
        }

        /**
         * Create an iterator.
         * @return an iterator.
         */
        @Override
        public Iterator<Resource> iterator() {
            return new FileResourceIterator(getProject(), null, parts);
        }

        /**
         * Check if this resource is only for filesystems.
         * @return true.
         */
        @Override
        public boolean isFilesystemOnly() {
            return true;
        }

        /**
         * Get the number of resources.
         * @return the number of parts.
         */
        @Override
        public int size() {
            return parts == null ? 0 : parts.length;
        }

    }

    private Boolean preserveBC;

    private Union union = null;
    private boolean cache = false;

    /**
     * Invoked by IntrospectionHelper for <code>setXXX(Path p)</code>
     * attribute setters.
     * @param p the <code>Project</code> for this path.
     * @param path the <code>String</code> path definition.
     */
    public Path(Project p, String path) {
        this(p);
        createPathElement().setPath(path);
    }

    /**
     * Construct an empty <code>Path</code>.
     * @param project the <code>Project</code> for this path.
     */
    public Path(Project project) {
        setProject(project);
    }

    /**
     * Adds a element definition to the path.
     * @param location the location of the element to add (must not be
     * <code>null</code> nor empty.
     * @throws BuildException on error
     */
    public void setLocation(File location) throws BuildException {
        checkAttributesAllowed();
        createPathElement().setLocation(location);
    }

    /**
     * Parses a path definition and creates single PathElements.
     * @param path the <code>String</code> path definition.
     * @throws BuildException on error
     */
    public void setPath(String path) throws BuildException {
        checkAttributesAllowed();
        createPathElement().setPath(path);
    }

    /**
     * Makes this instance in effect a reference to another Path instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     * @param r the reference to another Path
     * @throws BuildException on error
     */
    @Override
    public void setRefid(Reference r) throws BuildException {
        if (union != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Creates the nested <code>&lt;pathelement&gt;</code> element.
     * @return the <code>PathElement</code> to be configured
     * @throws BuildException on error
     */
    public PathElement createPathElement() throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        PathElement pe = new PathElement();
        add(pe);
        return pe;
    }

    /**
     * Adds a nested <code>&lt;fileset&gt;</code> element.
     * @param fs a <code>FileSet</code> to be added to the path
     * @throws BuildException on error
     */
    public void addFileset(FileSet fs) throws BuildException {
        if (fs.getProject() == null) {
            fs.setProject(getProject());
        }
        add(fs);
    }

    /**
     * Adds a nested <code>&lt;filelist&gt;</code> element.
     * @param fl a <code>FileList</code> to be added to the path
     * @throws BuildException on error
     */
    public void addFilelist(FileList fl) throws BuildException {
        if (fl.getProject() == null) {
            fl.setProject(getProject());
        }
        add(fl);
    }

    /**
     * Adds a nested <code>&lt;dirset&gt;</code> element.
     * @param dset a <code>DirSet</code> to be added to the path
     * @throws BuildException on error
     */
    public void addDirset(DirSet dset) throws BuildException {
        if (dset.getProject() == null) {
            dset.setProject(getProject());
        }
        add(dset);
    }

    /**
     * Adds a nested path
     * @param path a <code>Path</code> to be added to the path
     * @throws BuildException on error
     * @since Ant 1.6
     */
    public void add(Path path) throws BuildException {
        if (path == this) {
            throw circularReference();
        }
        if (path.getProject() == null) {
            path.setProject(getProject());
        }
        add((ResourceCollection) path);
    }

    /**
     * Add a nested <code>ResourceCollection</code>.
     * @param c the ResourceCollection to add.
     * @since Ant 1.7
     */
    public void add(ResourceCollection c) {
        checkChildrenAllowed();
        if (c == null) {
            return;
        }
        if (union == null) {
            union = new Union();
            union.setProject(getProject());
            union.setCache(cache);
        }
        union.add(c);
        setChecked(false);
    }

    /**
     * Creates a nested <code>&lt;path&gt;</code> element.
     * @return a <code>Path</code> to be configured
     * @throws BuildException on error
     */
    public Path createPath() throws BuildException {
        Path p = new Path(getProject());
        add(p);
        return p;
    }

    /**
     * Append the contents of the other Path instance to this.
     * @param other a <code>Path</code> to be added to the path
     */
    public void append(Path other) {
        if (other == null) {
            return;
        }
        add(other);
    }

    /**
     * Adds the components on the given path which exist to this
     * Path. Components that don't exist aren't added.
     *
     * @param source - source path whose components are examined for existence
     */
     public void addExisting(Path source) {
         addExisting(source, false);
     }

    /**
     * Same as addExisting, but support classpath behavior if tryUserDir
     * is true. Classpaths are relative to user dir, not the project base.
     * That used to break jspc test
     *
     * @param source the source path
     * @param tryUserDir  if true try the user directory if the file is not present
     */
    public void addExisting(Path source, boolean tryUserDir) {
        File userDir = (tryUserDir) ? new File(System.getProperty("user.dir"))
                : null;

        for (String name : source.list()) {
            File f = resolveFile(getProject(), name);

            // probably not the best choice, but it solves the problem of
            // relative paths in CLASSPATH
            if (tryUserDir && !f.exists()) {
                f = new File(userDir, name);
            }
            if (f.exists()) {
                setLocation(f);
            } else if (f.getParentFile() != null && f.getParentFile().exists()
                       && containsWildcards(f.getName())) {
                setLocation(f);
                log("adding " + f
                    + " which contains wildcards and may not do what you intend it to do depending on your OS or version of Java",
                    Project.MSG_VERBOSE);
            } else {
                log("dropping " + f + " from path as it doesn't exist",
                    Project.MSG_VERBOSE);
            }
        }
    }

    /**
     * Whether to cache the current path.
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setCache(boolean b) {
        checkAttributesAllowed();
        cache = b;
        if (union != null) {
            union.setCache(b);
        }
    }

    /**
     * Returns all path elements defined by this and nested path objects.
     * @return list of path elements.
     */
    public String[] list() {
        if (isReference()) {
            return getRef().list();
        }
        return assertFilesystemOnly(union) == null
            ? new String[0] : union.list();
    }

    /**
     * Returns a textual representation of the path, which can be used as
     * CLASSPATH or PATH environment variable definition.
     * @return a textual representation of the path.
     */
    @Override
    public String toString() {
        return isReference() ? getRef().toString()
            : union == null ? "" : union.toString();
    }

    /**
     * Splits a PATH (with : or ; as separators) into its parts.
     * @param project the project to use
     * @param source a <code>String</code> value
     * @return an array of strings, one for each path element
     */
    public static String[] translatePath(Project project, String source) {
        if (source == null) {
            return new String[0];
        }
        final List<String> result = new ArrayList<>();
        PathTokenizer tok = new PathTokenizer(source);
        while (tok.hasMoreTokens()) {
            StringBuffer element = new StringBuffer();
            String pathElement = tok.nextToken();
            try {
                element.append(resolveFile(project, pathElement).getPath());
            } catch (BuildException e) {
                project.log("Dropping path element " + pathElement
                    + " as it is not valid relative to the project",
                    Project.MSG_VERBOSE);
            }
            for (int i = 0; i < element.length(); i++) {
                translateFileSep(element, i);
            }
            result.add(element.toString());
        }
        return result.toArray(new String[0]);
    }

    /**
     * Returns its argument with all file separator characters
     * replaced so that they match the local OS conventions.
     * @param source the path to convert
     * @return the converted path
     */
    public static String translateFile(String source) {
        if (source == null) {
          return "";
        }
        final StringBuffer result = new StringBuffer(source);
        for (int i = 0; i < result.length(); i++) {
            translateFileSep(result, i);
        }
        return result.toString();
    }

    /**
     * Translates occurrences at a position of / or \ to correct separator of the
     * current platform and returns whether it had to do a
     * replacement.
     * @param buffer a buffer containing a string
     * @param pos the position in the string buffer to convert
     * @return true if the character was a / or \
     */
    protected static boolean translateFileSep(StringBuffer buffer, int pos) {
        if (buffer.charAt(pos) == '/' || buffer.charAt(pos) == '\\') {
            buffer.setCharAt(pos, File.separatorChar);
            return true;
        }
        return false;
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     */
    @Override
    public synchronized int size() {
        if (isReference()) {
            return getRef().size();
        }
        dieOnCircularReference();
        return union == null ? 0 : assertFilesystemOnly(union).size();
    }

    /**
     * Clone this Path.
     * @return Path with shallowly cloned Resource children.
     */
    @Override
    public Object clone() {
        try {
            Path result = (Path) super.clone();
            result.union = union == null ? union : (Union) union.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Overrides the version of DataType to recurse on all DataType
     * child elements that may have been added.
     * @param stk the stack of data types to use (recursively).
     * @param p   the project to use to dereference the references.
     * @throws BuildException on error.
     */
    @Override
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            if (union != null) {
                pushAndInvokeCircularReferenceCheck(union, stk, p);
            }
            setChecked(true);
        }
    }

    /**
     * Resolve a filename with Project's help - if we know one that is.
     */
    private static File resolveFile(Project project, String relativeName) {
        return FileUtils.getFileUtils().resolveFile(
            (project == null) ? null : project.getBaseDir(), relativeName);
    }

    /**
     * Concatenates the system class path in the order specified by
     * the ${build.sysclasspath} property - using &quot;last&quot; as
     * default value.
     * @return the concatenated path
     */
    public Path concatSystemClasspath() {
        return concatSystemClasspath("last");
    }

    /**
     * Concatenates the system class path in the order specified by
     * the ${build.sysclasspath} property - using the supplied value
     * if ${build.sysclasspath} has not been set.
     * @param defValue the order ("first", "last", "only")
     * @return the concatenated path
     */
    public Path concatSystemClasspath(String defValue) {
        return concatSpecialPath(defValue, Path.systemClasspath);
    }

    /**
     * Concatenates the system boot class path in the order specified
     * by the ${build.sysclasspath} property - using the supplied
     * value if ${build.sysclasspath} has not been set.
     * @param defValue the order ("first", "last", "only")
     * @return the concatenated path
     */
    public Path concatSystemBootClasspath(String defValue) {
        return concatSpecialPath(defValue, Path.systemBootClasspath);
    }

    /**
     * Concatenates a class path in the order specified by the
     * ${build.sysclasspath} property - using the supplied value if
     * ${build.sysclasspath} has not been set.
     */
    private Path concatSpecialPath(String defValue, Path p) {
        Path result = new Path(getProject());

        String order = defValue;
        String o = getProject() != null
            ? getProject().getProperty(MagicNames.BUILD_SYSCLASSPATH)
            : System.getProperty(MagicNames.BUILD_SYSCLASSPATH);
        if (o != null) {
            order = o;
        }
        if ("only".equals(order)) {
            // only: the developer knows what (s)he is doing
            result.addExisting(p, true);

        } else if ("first".equals(order)) {
            // first: developer could use a little help
            result.addExisting(p, true);
            result.addExisting(this);

        } else if ("ignore".equals(order)) {
            // ignore: don't trust anyone
            result.addExisting(this);

        } else {
            // last: don't trust the developer
            if (!"last".equals(order)) {
                log("invalid value for " + MagicNames.BUILD_SYSCLASSPATH
                    + ": " + order,
                    Project.MSG_WARN);
            }
            result.addExisting(this);
            result.addExisting(p, true);
        }
        return result;
    }

    /**
     * Add the Java Runtime classes to this Path instance.
     */
    public void addJavaRuntime() {
        if (JavaEnvUtils.isKaffe()) {
            // newer versions of Kaffe (1.1.1+) won't have this,
            // but this will be sorted by FileSet anyway.
            File kaffeShare = new File(JavaEnvUtils.getJavaHome()
                                       + File.separator + "share"
                                       + File.separator + "kaffe");
            if (kaffeShare.isDirectory()) {
                FileSet kaffeJarFiles = new FileSet();
                kaffeJarFiles.setDir(kaffeShare);
                kaffeJarFiles.setIncludes("*.jar");
                addFileset(kaffeJarFiles);
            }
        } else if ("GNU libgcj".equals(System.getProperty("java.vm.name"))) {
            addExisting(systemBootClasspath);
        }

        if (System.getProperty("java.vendor").toLowerCase(Locale.ENGLISH).contains("microsoft")) {
            // TODO is this code still necessary? is there any 1.2+ port?
            // Pull in *.zip from packages directory
            FileSet msZipFiles = new FileSet();
            msZipFiles.setDir(new File(JavaEnvUtils.getJavaHome()
                    + File.separator + "Packages"));
            msZipFiles.setIncludes("*.ZIP");
            addFileset(msZipFiles);
        } else {
            // JDK 1.2+ seems to set java.home to the JRE directory.
            addExisting(new Path(null, JavaEnvUtils.getJavaHome()
                    + File.separator + "lib" + File.separator + "rt.jar"));
            // Just keep the old version as well and let addExisting
            // sort it out.
            addExisting(new Path(null, JavaEnvUtils.getJavaHome()
                    + File.separator + "jre" + File.separator + "lib"
                    + File.separator + "rt.jar"));

            // Sun's and Apple's 1.4 have JCE and JSSE in separate jars.
            for (String secJar : Arrays.asList("jce", "jsse")) {
                addExisting(new Path(null, JavaEnvUtils.getJavaHome()
                        + File.separator + "lib"
                        + File.separator + secJar + ".jar"));
                addExisting(new Path(null, JavaEnvUtils.getJavaHome()
                        + File.separator + ".." + File.separator + "Classes"
                        + File.separator + secJar + ".jar"));
            }

            // IBM's 1.4 has rt.jar split into 4 smaller jars and a combined
            // JCE/JSSE in security.jar.
            for (String ibmJar : Arrays.asList("core", "graphics", "security", "server", "xml")) {
                addExisting(new Path(null, JavaEnvUtils.getJavaHome()
                        + File.separator + "lib" + File.separator + ibmJar + ".jar"));
            }

            // Added for MacOS X
            addExisting(new Path(null, JavaEnvUtils.getJavaHome()
                    + File.separator + ".." + File.separator + "Classes"
                    + File.separator + "classes.jar"));
            addExisting(new Path(null, JavaEnvUtils.getJavaHome()
                    + File.separator + ".." + File.separator + "Classes"
                    + File.separator + "ui.jar"));
        }
    }

    /**
     * Emulation of extdirs feature in Java &gt;= 1.2.
     * This method adds all files in the given
     * directories (but not in sub-directories!) to the classpath,
     * so that you don't have to specify them all one by one.
     * @param extdirs - Path to append files to
     */
    public void addExtdirs(Path extdirs) {
        if (extdirs == null) {
            String extProp = System.getProperty("java.ext.dirs");
            if (extProp != null) {
                extdirs = new Path(getProject(), extProp);
            } else {
                return;
            }
        }

        for (String d : extdirs.list()) {
            File dir = resolveFile(getProject(), d);
            if (dir.exists() && dir.isDirectory()) {
                FileSet fs = new FileSet();
                fs.setDir(dir);
                fs.setIncludes("*");
                addFileset(fs);
            }
        }
    }

    /**
     * Fulfill the ResourceCollection contract. The Iterator returned
     * will throw ConcurrentModificationExceptions if ResourceCollections
     * are added to this container while the Iterator is in use.
     * @return a "fail-fast" Iterator.
     */
    @Override
    public final synchronized Iterator<Resource> iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        dieOnCircularReference();
        if (getPreserveBC()) {
            return new FileResourceIterator(getProject(), null, list());
        }
        return union == null ? Collections.<Resource> emptySet().iterator()
            : assertFilesystemOnly(union).iterator();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this is a filesystem-only resource collection.
     */
    @Override
    public synchronized boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        }
        dieOnCircularReference();
        assertFilesystemOnly(union);
        return true;
    }

    /**
     * Verify the specified ResourceCollection is filesystem-only.
     * @param rc the ResourceCollection to check.
     * @throws BuildException if <code>rc</code> is not filesystem-only.
     * @return the passed in ResourceCollection.
     */
    protected ResourceCollection assertFilesystemOnly(ResourceCollection rc) {
        if (rc != null && !(rc.isFilesystemOnly())) {
            throw new BuildException("%s allows only filesystem resources.",
                getDataTypeName());
        }
        return rc;
    }

    /**
     * Helps determine whether to preserve BC by calling <code>list()</code> on subclasses.
     * The default behavior of this method is to return <code>true</code> for any subclass
     * that implements <code>list()</code>; this can, of course, be avoided by overriding
     * this method to return <code>false</code>. It is not expected that the result of this
     * method should change over time, thus it is called only once.
     * @return <code>true</code> if <code>iterator()</code> should delegate to <code>list()</code>.
     */
    protected boolean delegateIteratorToList() {
        if (getClass().equals(Path.class)) {
            return false;
        }
        try {
            Method listMethod = getClass().getMethod("list");
            return !listMethod.getDeclaringClass().equals(Path.class);
        } catch (Exception e) {
            //shouldn't happen, but
            return false;
        }
    }

    private synchronized boolean getPreserveBC() {
        if (preserveBC == null) {
            preserveBC = delegateIteratorToList() ? Boolean.TRUE : Boolean.FALSE;
        }
        return preserveBC;
    }

    /**
     * Does the given file name contain wildcards?
     * @since Ant 1.8.2
     */
    private static boolean containsWildcards(String path) {
        return path != null && (path.contains("*") || path.contains("?"));
    }

    private Path getRef() {
        return getCheckedRef(Path.class);
    }

}
