/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import java.io.File;
import java.util.Vector;
import java.util.StringTokenizer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

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
 * @author <a href="mailto:stefan.bodewig@megabit.net">Stefan Bodewig</a> 
 */

public class Path {

    private Vector elements;
    private Project project;

    public static Path systemClasspath = 
        new Path(null, System.getProperty("java.class.path"));


    /**
     * Helper class, holds the nested <pathelement> values.
     */
    public class PathElement {
        private String[] parts;

        public void setLocation(File loc) {
            parts = new String[] {translateFile(loc.getAbsolutePath())};
        }

        public void setPath(String path) {
            parts = Path.translatePath(project, path);
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

    public Path(Project p) {
        this.project = project;
        elements = new Vector();
    }

    /**
     * Adds a element definition to the path.
     * @param location the location of the element to add (must not be
     * <code>null</code> nor empty.
     */
    public void setLocation(File location) {
        createPathElement().setLocation(location);
    }


    /**
     * Parses a path definition and creates single PathElements.
     * @param path the path definition.
     */
    public void setPath(String path) {
        createPathElement().setPath(path);
    }


    /**
     * Created the nested <pathelement> element.
     */
    public PathElement createPathElement() {
        PathElement pe = new PathElement();
        elements.addElement(pe);
        return pe;
    }

    /**
     * Adds a nested <fileset> element.
     */
    public void addFileset(FileSet fs) {
        elements.addElement(fs);
    }

    /**
     * Adds a nested <filesetref> element.
     */
    public void addFilesetRef(Reference r) {
        elements.addElement(r);
    }

    /**
     * Append the contents of the other Path instance to this.
     */
    public void append(Path other) {
        if (other == null) return;
        String[] l = other.list();
        for (int i=0; i<l.length; i++) {
            if (elements.indexOf(l[i]) == -1) {
                elements.addElement(l[i]);
            }
        }
    }

    /**
     * Returns all path elements defined by this and netsed path objects.
     * @return list of path elements.
     */
    public String[] list() {
        Vector result = new Vector(2*elements.size());
        for (int i=0; i<elements.size(); i++) {
            Object o = elements.elementAt(i);
            if (o instanceof Reference) {
                Reference r = (Reference) o;
                o = r.getReferencedObject(project);
                // we only support references to filesets right now
                if (o == null || !(o instanceof FileSet)) {
                    String msg = r.getRefId()+" doesn\'t denote a fileset";
                    throw new BuildException(msg);
                }
            }
            
            if (o instanceof String) {
                // obtained via append
                addUnlessPresent(result, (String) o);
            } else if (o instanceof PathElement) {
                String[] parts = ((PathElement) o).getParts();
                if (parts == null) {
                    throw new BuildException("You must either set location or path on <pathelement>");
                }
                for (int j=0; j<parts.length; j++) {
                    addUnlessPresent(result, parts[j]);
                }
            } else if (o instanceof FileSet) {
                FileSet fs = (FileSet) o;
                DirectoryScanner ds = fs.getDirectoryScanner(project);
                String[] s = ds.getIncludedFiles();
                File dir = fs.getDir();
                for (int j=0; j<s.length; j++) {
                    addUnlessPresent(result, 
                                     translateFile((new File(dir, s[j])).getAbsolutePath()));
                }
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
        if (list.length == 0) return "";

        // path containing one or more elements
        final StringBuffer result = new StringBuffer(list[0].toString());
        for (int i=1; i < list.length; i++) {
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
        if (source == null) return new String[0];

        PathTokenizer tok = new PathTokenizer(source);
        StringBuffer element = new StringBuffer();
        while (tok.hasMoreTokens()) {
            element.setLength(0);
            element.append(resolveFile(project, tok.nextToken()));
            for (int i=0; i<element.length(); i++) {
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
        if (source == null) return "";

        final StringBuffer result = new StringBuffer(source);
        for (int i=0; i < result.length(); i++) {
            translateFileSep(result, i);
        }

        return result.toString();
    }

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

    private static String resolveFile(Project project, String relativeName) {
        if (project != null) {
            return project.resolveFile(relativeName).getAbsolutePath();
        }
        return relativeName;
    }

    private static void addUnlessPresent(Vector v, String s) {
        if (v.indexOf(s) == -1) {
            v.addElement(s);
        }
    }

}
