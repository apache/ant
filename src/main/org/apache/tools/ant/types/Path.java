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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PathTokenizer;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Stack;
import java.util.Vector;



/**
 * This object represents a path as used by CLASSPATH or PATH
 * environment variable.
 * <p>
 * <code>
 * &lt;sometask&gt;<br>
 * &nbsp;&nbsp;&lt;somepath&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement location="/path/to/file.jar" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement path="/path/to/file2.jar:/path/to/class2;/path/to/class3" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement location="/path/to/file3.jar" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement location="/path/to/file4.jar" /&gt;<br>
 * &nbsp;&nbsp;&lt;/somepath&gt;<br>
 * &lt;/sometask&gt;<br>
 * </code>
 * <p>
 * The object implemention <code>sometask</code> must provide a method called
 * <code>createSomepath</code> which returns an instance of <code>Path</code>.
 * Nested path definitions are handled by the Path object and must be labeled
 * <code>pathelement</code>.<p>
 *
 * The path element takes a parameter <code>path</code> which will be parsed
 * and split into single elements. It will usually be used
 * to define a path from an environment variable.
 *
 * @author Thomas.Haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class Path extends DataType implements Cloneable {

    private Vector elements;

    /** The system classspath as a Path object */
    public static Path systemClasspath = 
        new Path(null, System.getProperty("java.class.path"));


    /**
     * Helper class, holds the nested <code>&lt;pathelement&gt;</code> values.
     */
    public class PathElement {
        private String[] parts;

        public void setLocation(File loc) {
            parts = new String[] {translateFile(loc.getAbsolutePath())};
        }

        public void setPath(String path) {
            parts = Path.translatePath(getProject(), path);
        }

        public String[] getParts() {
            return parts;
        }
    }

    /**
     * Invoked by IntrospectionHelper for <code>setXXX(Path p)</code>
     * attribute setters.  
     */
    public Path(Project p, String path) {
        this(p);
        createPathElement().setPath(path);
    }

    public Path(Project project) {
        setProject(project);
        elements = new Vector();
    }

    /**
     * Adds a element definition to the path.
     * @param location the location of the element to add (must not be
     * <code>null</code> nor empty.
     */
    public void setLocation(File location) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createPathElement().setLocation(location);
    }


    /**
     * Parses a path definition and creates single PathElements.
     * @param path the path definition.
     */
    public void setPath(String path) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createPathElement().setPath(path);
    }

    /**
     * Makes this instance in effect a reference to another Path instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     */
    public void setRefid(Reference r) throws BuildException {
        if (!elements.isEmpty()) {
            throw tooManyAttributes();
        }
        elements.addElement(r);
        super.setRefid(r);
    }

    /**
     * Creates the nested <code>&lt;pathelement&gt;</code> element.
     */
    public PathElement createPathElement() throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        PathElement pe = new PathElement();
        elements.addElement(pe);
        return pe;
    }

    /**
     * Adds a nested <code>&lt;fileset&gt;</code> element.
     */
    public void addFileset(FileSet fs) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        elements.addElement(fs);
        checked = false;
    }

    /**
     * Adds a nested <code>&lt;filelist&gt;</code> element.
     */
    public void addFilelist(FileList fl) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        elements.addElement(fl);
        checked = false;
    }

    /**
     * Adds a nested <code>&lt;dirset&gt;</code> element.
     */
    public void addDirset(DirSet dset) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        elements.addElement(dset);
        checked = false;
    }

    /**
     * Creates a nested <code>&lt;path&gt;</code> element.
     */
    public Path createPath() throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        Path p = new Path(getProject());
        elements.addElement(p);
        checked = false;
        return p;
    }

    /**
     * Append the contents of the other Path instance to this.
     */
    public void append(Path other) {
        if (other == null) {
            return;
        }
        String[] l = other.list();
        for (int i = 0; i < l.length; i++) {
            if (elements.indexOf(l[i]) == -1) {
                elements.addElement(l[i]);
            }
        }
    }

     /**
     * Adds the components on the given path which exist to this
     * Path. Components that don't exist, aren't added.
     *
     * @param source - source path whose components are examined for existence
     */
    public void addExisting(Path source) {
        String[] list = source.list();
        for (int i = 0; i < list.length; i++) {
            File f = null;
            if (getProject() != null) {
                f = getProject().resolveFile(list[i]);
            } else {
                f = new File(list[i]);
            }

            if (f.exists()) {
                setLocation(f);
            } else {
                log("dropping " + f + " from path as it doesn't exist", 
                    Project.MSG_VERBOSE);
            }
        }
    }

    /**
     * Returns all path elements defined by this and nested path objects.
     * @return list of path elements.
     */
    public String[] list() {
        if (!checked) {
            // make sure we don't have a circular reference here
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, getProject());
        }

        Vector result = new Vector(2 * elements.size());
        for (int i = 0; i < elements.size(); i++) {
            Object o = elements.elementAt(i);
            if (o instanceof Reference) {
                Reference r = (Reference) o;
                o = r.getReferencedObject(getProject());
                // we only support references to paths right now
                if (!(o instanceof Path)) {
                    String msg = r.getRefId() + " doesn\'t denote a path";
                    throw new BuildException(msg);
                }
            }
            
            if (o instanceof String) {
                // obtained via append
                addUnlessPresent(result, (String) o);
            } else if (o instanceof PathElement) {
                String[] parts = ((PathElement) o).getParts();
                if (parts == null) {
                    throw new BuildException("You must either set location or" 
                        + " path on <pathelement>");
                }
                for (int j = 0; j < parts.length; j++) {
                    addUnlessPresent(result, parts[j]);
                }
            } else if (o instanceof Path) {
                Path p = (Path) o;
                if (p.getProject() == null) {
                    p.setProject(getProject());
                }
                String[] parts = p.list();
                for (int j = 0; j < parts.length; j++) {
                    addUnlessPresent(result, parts[j]);
                }
            } else if (o instanceof DirSet) {
                DirSet dset = (DirSet) o;
                DirectoryScanner ds = dset.getDirectoryScanner(getProject());
                String[] s = ds.getIncludedDirectories();
                File dir = dset.getDir(getProject());
                addUnlessPresent(result, dir, s);
            } else if (o instanceof FileSet) {
                FileSet fs = (FileSet) o;
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                String[] s = ds.getIncludedFiles();
                File dir = fs.getDir(getProject());
                addUnlessPresent(result, dir, s);
            } else if (o instanceof FileList) {
                FileList fl = (FileList) o;
                String[] s = fl.getFiles(getProject());
                File dir = fl.getDir(getProject());
                addUnlessPresent(result, dir, s);
            }
        }
        String[] res = new String[result.size()];
        result.copyInto(res);
        return res;
    }


    /**
     * Returns a textual representation of the path, which can be used as
     * CLASSPATH or PATH environment variable definition.
     * @return a textual representation of the path.
     */
    public String toString() {
        final String[] list = list();

        // empty path return empty string
        if (list.length == 0) {
            return "";
        }

        // path containing one or more elements
        final StringBuffer result = new StringBuffer(list[0].toString());
        for (int i = 1; i < list.length; i++) {
            result.append(File.pathSeparatorChar);
            result.append(list[i]);
        }

        return result.toString();
    }

    /**
     * Splits a PATH (with : or ; as separators) into its parts.
     */
    public static String[] translatePath(Project project, String source) {
        final Vector result = new Vector();
        if (source == null) {
          return new String[0];
        }

        PathTokenizer tok = new PathTokenizer(source);
        StringBuffer element = new StringBuffer();
        while (tok.hasMoreTokens()) {
            element.setLength(0);
            String pathElement = tok.nextToken();
            try {
                element.append(resolveFile(project, pathElement));
            } catch (BuildException e) {
                project.log("Dropping path element " + pathElement 
                    + " as it is not valid relative to the project", 
                    Project.MSG_VERBOSE);
            }
            for (int i = 0; i < element.length(); i++) {
                translateFileSep(element, i);
            }
            result.addElement(element.toString());
        }
        String[] res = new String[result.size()];
        result.copyInto(res);
        return res;
    }

    /**
     * Returns its argument with all file separator characters
     * replaced so that they match the local OS conventions.  
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
     * Translates all occurrences of / or \ to correct separator of the
     * current platform and returns whether it had to do any
     * replacements.  
     */
    protected static boolean translateFileSep(StringBuffer buffer, int pos) {
        if (buffer.charAt(pos) == '/' || buffer.charAt(pos) == '\\') {
            buffer.setCharAt(pos, File.separatorChar);
            return true;
        }
        return false;
    }

    /**
     * How many parts does this Path instance consist of.
     */
    public int size() {
        return list().length;
    }

    /**
     * Return a Path that holds the same elements as this instance.
     */
    public Object clone() {
        Path p = new Path(getProject());
        p.append(this);
        return p;
    }

    /**
     * Overrides the version of DataType to recurse on all DataType
     * child elements that may have been added.  
     */
    protected void dieOnCircularReference(Stack stk, Project p) 
        throws BuildException {

        if (checked) {
            return;
        }

        Enumeration enum = elements.elements();
        while (enum.hasMoreElements()) {
            Object o = enum.nextElement();
            if (o instanceof Reference) {
                o = ((Reference) o).getReferencedObject(p);
            }

            if (o instanceof DataType) {
                if (stk.contains(o)) {
                    throw circularReference();
                } else {
                    stk.push(o);
                    ((DataType) o).dieOnCircularReference(stk, p);
                    stk.pop();
                }
            }
        }
        checked = true;
    }

    /**
     * Resolve a filename with Project's help - if we know one that is.
     *
     * <p>Assume the filename is absolute if project is null.</p>
     */
    private static String resolveFile(Project project, String relativeName) {
        if (project != null) {
            File f = project.resolveFile(relativeName);
            return f.getAbsolutePath();
        }
        return relativeName;
    }

    /**
     * Adds a String to the Vector if it isn't already included.
     */
    private static void addUnlessPresent(Vector v, String s) {
        if (v.indexOf(s) == -1) {
            v.addElement(s);
        }
    }

    /**
     * Adds absolute path names of listed files in the given directory
     * to the Vector if they are not already included.
     */
    private static void addUnlessPresent(Vector v, File dir, String[] s) {
        for (int j = 0; j < s.length; j++) {
            File d = new File(dir, s[j]);
            String absolutePath = d.getAbsolutePath();
            addUnlessPresent(v, translateFile(absolutePath));
        } 
    }

    /**
     * Concatenates the system class path in the order specified by
     * the ${build.sysclasspath} property - using &quot;last&quot; as
     * default value.
     */
    public Path concatSystemClasspath() {
        return concatSystemClasspath("last");
    }

    /**
     * Concatenates the system class path in the order specified by
     * the ${build.sysclasspath} property - using the supplied value
     * if ${build.sysclasspath} has not been set.
     */
    public Path concatSystemClasspath(String defValue) {

        Path result = new Path(getProject());

        String order = defValue;
        if (getProject() != null) {
            String o = getProject().getProperty("build.sysclasspath");
            if (o != null) {
                order = o;
            }
        }
        
        if (order.equals("only")) {
            // only: the developer knows what (s)he is doing
            result.addExisting(Path.systemClasspath);
        
        } else if (order.equals("first")) {
            // first: developer could use a little help
            result.addExisting(Path.systemClasspath);
            result.addExisting(this);

        } else if (order.equals("ignore")) {
            // ignore: don't trust anyone
            result.addExisting(this);

        } else {
            // last: don't trust the developer
            if (!order.equals("last")) {
                log("invalid value for build.sysclasspath: " + order, 
                    Project.MSG_WARN);
            }

            result.addExisting(this);
            result.addExisting(Path.systemClasspath);
        }
        

        return result;

    }

    /**
     * Add the Java Runtime classes to this Path instance.
     */
    public void addJavaRuntime() {
        if (System.getProperty("java.vendor").toLowerCase(Locale.US).indexOf("microsoft") >= 0) {
            // Pull in *.zip from packages directory
            FileSet msZipFiles = new FileSet();
            msZipFiles.setDir(new File(System.getProperty("java.home") 
                + File.separator + "Packages"));
            msZipFiles.setIncludes("*.ZIP");
            addFileset(msZipFiles);
        } else if ("Kaffe".equals(System.getProperty("java.vm.name"))) {
            FileSet kaffeJarFiles = new FileSet();
            kaffeJarFiles.setDir(new File(System.getProperty("java.home") 
                                          + File.separator + "share"
                                          + File.separator + "kaffe"));
            
            kaffeJarFiles.setIncludes("*.jar");
            addFileset(kaffeJarFiles);
        } else if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            addExisting(new Path(null,
                                 System.getProperty("java.home")
                                 + File.separator + "lib"
                                 + File.separator
                                 + "classes.zip"));
        } else {
            // JDK > 1.1 seems to set java.home to the JRE directory.
            addExisting(new Path(null,
                                 System.getProperty("java.home")
                                 + File.separator + "lib"
                                 + File.separator + "rt.jar"));
            // Just keep the old version as well and let addExisting
            // sort it out.
            addExisting(new Path(null,
                                 System.getProperty("java.home")
                                 + File.separator + "jre"
                                 + File.separator + "lib"
                                 + File.separator + "rt.jar"));

            // Sun's 1.4 has JCE and JSSE in separate jars.
            String[] secJars = { "jce", "jsse" };
            for (int i = 0; i < secJars.length; i++) {
                addExisting(new Path(null,
                                     System.getProperty("java.home")
                                     + File.separator + "lib"
                                     + File.separator + secJars[i] + ".jar"));
            }

            // IBM's 1.4 has rt.jar split into 4 smaller jars and a combined
            // JCE/JSSE in security.jar.
            String[] ibmJars =
                { "core", "graphics", "security", "server", "xml" };
            for (int i = 0; i < ibmJars.length; i++) {
                addExisting(new Path(null,
                                     System.getProperty("java.home")
                                     + File.separator + "lib"
                                     + File.separator + ibmJars[i] + ".jar"));
            }

            // Added for MacOS X
            addExisting(new Path(null,
                                 System.getProperty("java.home")
                                 + File.separator + ".."
                                 + File.separator + "Classes"
                                 + File.separator + "classes.jar"));
            addExisting(new Path(null,
                                 System.getProperty("java.home")
                                 + File.separator + ".."
                                 + File.separator + "Classes"
                                 + File.separator + "ui.jar"));
        }
    }

    /**
     * Emulation of extdirs feature in java >= 1.2.
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

        String[] dirs = extdirs.list();
        for (int i = 0; i < dirs.length; i++) {
            File dir = getProject().resolveFile(dirs[i]);
            if (dir.exists() && dir.isDirectory()) {
                FileSet fs = new FileSet();
                fs.setDir(dir);
                fs.setIncludes("*");
                addFileset(fs);
            }
        }
    }
}
