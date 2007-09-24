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
package org.apache.tools.ant.types.optional.depend;

import java.util.Vector;
import java.util.Enumeration;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * A ClassfileSet is a FileSet that enlists all classes that depend on a
 * certain set of root classes.
 *
 * ClassfileSet extends FileSet, its inherited properties
 * defining the domain searched for dependent classes.
 *
 */
public class ClassfileSet extends FileSet {
    /**
     * The list of root classes for this class file set. These are the
     * classes which must be included in the fileset and which are the
     * starting point for the dependency search.
     */
    private Vector rootClasses = new Vector();

    /**
     * The list of filesets which contain root classes.
     */
    private Vector rootFileSets = new Vector();

    /**
     * Inner class used to contain info about root classes.
     */
    public static class ClassRoot {
        /** The name of the root class */
        private String rootClass;

        /**
         * Set the root class name.
         *
         * @param name the name of the root class.
         */
        public void setClassname(String name) {
            this.rootClass = name;
        }

        /**
         * Get the name of the root class.
         *
         * @return the name of the root class.
         */
        public String getClassname() {
            return rootClass;
        }
    }

    /**
     * Default constructor.
     */
    public ClassfileSet() {
    }

    /**
     * Add a fileset to which contains a collection of root classes used to
     * drive the search from classes.
     *
     * @param rootFileSet a root file set to be used to search for dependent
     * classes.
     */
    public void addRootFileset(FileSet rootFileSet) {
        rootFileSets.addElement(rootFileSet);
    }

    /**
     * Create a ClassfileSet from another ClassfileSet.
     *
     * @param s the other classfileset.
     */
    protected ClassfileSet(ClassfileSet s) {
        super(s);
        rootClasses = (Vector) s.rootClasses.clone();
    }

    /**
     * Set the root class attribute.
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
                        = files[i].substring(
                            0, files[i].length() - ".class".length());
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
     * Add a nested root class definition to this class file set.
     *
     * @param root the configured class root.
     */
    public void addConfiguredRoot(ClassRoot root) {
        rootClasses.addElement(root.getClassname());
    }

    /**
     * Clone this data type.
     *
     * @return a clone of the class file set.
     */
    public Object clone() {
        return new ClassfileSet(isReference()
            ? (ClassfileSet) (getRef(getProject())) : this);
    }

}
