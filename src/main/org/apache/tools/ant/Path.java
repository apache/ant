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

package org.apache.tools.ant;

import java.io.File;
import java.util.Vector;
import java.util.StringTokenizer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * This object represents a path as used by CLASSPATH or PATH
 * environment variable.
 *
 * <code>
 * &lt;sometask&gt;<br>
 * &nbsp;&nbsp;&lt;somepath&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement location="/path/to/file.jar" /&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement path="/path/to/file2.jar:/path/to/class2;/path/to/class3" /&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement location="/path/to/file3.jar" /&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement location="/path/to/file4.jar" /&gt;
 * &nbsp;&nbsp;&lt;/somepath&gt;
 * &lt;/sometask&gt;<br>
 * </code>
 *
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
 */

public class Path {

    private Vector definition;

    public static Path systemClasspath = 
        new Path(System.getProperty("java.class.path"));

    public Path(String path) {
        this();
        setPath(path);
    }

    public Path() {
        definition = new Vector();
    }

    /**
     * Adds a element definition to the path.
     * @param location the location of the element to add (must not be
     * <code>null</code> nor empty.
     */
    public void setLocation(String location) {
        if (location != null && location.length() > 0) {
            String element = translateFile(location);
            if (definition.indexOf(element) == -1) {
                definition.addElement(element);
            }
        }
    }


    /**
     * Append the contents of the other Path instance to this.
     */
    public void append(Path other) {
        String[] l = other.list();
        for (int i=0; i<l.length; i++) {
            if (definition.indexOf(l[i]) == -1) {
                definition.addElement(l[i]);
            }
        }
    }

    /**
     * Parses a path definition and creates single PathElements.
     * @param path the path definition.
     */
    public void setPath(String path) {
        final Vector elements = translatePath(path);
        for (int i=0; i < elements.size(); i++) {
            String element = (String) elements.elementAt(i);
            if (definition.indexOf(element) == -1) {
                definition.addElement(element);
            }
        }
    }


    public Path createPathElement() {
        return this;
    }


    /**
     * Returns all path elements defined by this and netsed path objects.
     * @return list of path elements.
     */
    public String[] list() {
        final String[] result = new String[definition.size()];
        definition.copyInto(result);
        return result;
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



    public static Vector translatePath(String source) {
        final Vector result = new Vector();
        if (source == null) return result;

        PathTokenizer tok = new PathTokenizer(source);
        StringBuffer element = new StringBuffer();
        while (tok.hasMoreTokens()) {
            element.setLength(0);
            element.append(tok.nextToken());
            for (int i=0; i<element.length(); i++) {
                translateFileSep(element, i);
            }
            result.addElement(element.toString());
        }
        return result;
    }


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
}
