/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.types.optional.depend;

import java.util.Vector;
import java.util.Enumeration;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * A ClassfileSet is a FileSet, that enlists all classes that depend on a
 * certain set of root classes.
 *
 * A ClassfileSet extends FileSets. The
 * nested FileSet attribute provides the domain, that is used for searching
 * for dependent classes
 *
 * @author <a href="mailto:hengels@innovidata.com">Holger Engels</a>
 */
public class ClassfileSet extends FileSet {
    /** 
     * The list of root classes for this class file set. These are the 
     * classes which must be included in the fileset and which are the 
     * starting point for the dependency search.
     */
    private Vector rootClasses = new Vector();

    /**
     * The list of filesets which contain root classes
     */
    private Vector rootFileSets = new Vector();
    
    /**
     * Inner class used to contain info about root classes
     */
    public static class ClassRoot {
        /** The name of the root class */
        private String rootClass;
        
        /** 
         * Set the root class name 
         *
         * @param name the name of the root class 
         */
        public void setClassname(String name) {
            this.rootClass = name;
        }
        
        /**
         * Get the name of the root class
         *
         * @return the name of the root class.
         */
        public String getClassname() {
            return rootClass;
        }
    }

    /**
     * Default constructor
     */
    public ClassfileSet() {
    }
    
    /**
     * Add a fileset to which contains a collection of root classes used to 
     * drive the search from classes 
     *
     * @param rootFileSet a root file set to be used to search for dependent
     * classes
     */
    public void addRootFileset(FileSet rootFileSet) {
        rootFileSets.addElement(rootFileSet);
    }
    
    /**
     * Create a ClassfileSet from another ClassfileSet
     *
     * @param s the other classfileset
     */
    protected ClassfileSet(ClassfileSet s) {
        super(s);
        rootClasses = (Vector) s.rootClasses.clone();
    }

    /**
     * Set the root class attribute
     *
     * @param rootClass the name of the root class.
     */
    public void setRootClass(String rootClass) {
        rootClasses.addElement(rootClass);
    }

    /**
     * Return the DirectoryScanner associated with this FileSet.
     *
     * @param p the project used to resolve dirs, etc.
     *
     * @return a dependency scanner.
     */
    public DirectoryScanner getDirectoryScanner(Project p) {
        if (isReference()) {
            return getRef(p).getDirectoryScanner(p);
        }

        Vector allRootClasses = (Vector) rootClasses.clone();
        for (Enumeration e = rootFileSets.elements(); e.hasMoreElements();) {
            FileSet additionalRootSet = (FileSet) e.nextElement();
            DirectoryScanner additionalScanner
                = additionalRootSet.getDirectoryScanner(p);
            String[] files = additionalScanner.getIncludedFiles();
            for (int i = 0; i < files.length; ++i) {
                if (files[i].endsWith(".class")) {
                    String classFilePath 
                        = files[i].substring(0, files[i].length() - 6);
                    String className 
                        = classFilePath.replace('/', '.').replace('\\', '.');
                    allRootClasses.addElement(className);
                }
            }
        }    
                
        
        DirectoryScanner parentScanner = super.getDirectoryScanner(p);
        DependScanner scanner = new DependScanner(parentScanner);
        scanner.setBasedir(getDir(p));
        scanner.setRootClasses(allRootClasses);
        scanner.scan();
        return scanner;
    } 
    
    /** 
     * Add a nested root class definition to this class file set
     *
     * @param root the configured class root.
     */
    public void addConfiguredRoot(ClassRoot root) {
        rootClasses.addElement(root.getClassname());    
    }

    /**
     * Clone this data type.
     *
     * @return a clone of the class file set
     */
    public Object clone() {
        if (isReference()) {
            return new ClassfileSet((ClassfileSet) getRef(getProject()));
        } else {
            return new ClassfileSet(this);
        }
    }
}
