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
package org.apache.tools.ant.types.optional.depend;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.StringUtils;

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
    private List<String> rootClasses = new ArrayList<>();

    /**
     * The list of filesets which contain root classes.
     */
    private List<FileSet> rootFileSets = new ArrayList<>();

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
     * Create a ClassfileSet from another ClassfileSet.
     *
     * @param s the other classfileset.
     */
    protected ClassfileSet(ClassfileSet s) {
        super(s);
        rootClasses.addAll(s.rootClasses);
    }

    /**
     * Add a fileset to which contains a collection of root classes used to
     * drive the search from classes.
     *
     * @param rootFileSet a root file set to be used to search for dependent
     * classes.
     */
    public void addRootFileset(FileSet rootFileSet) {
        rootFileSets.add(rootFileSet);
        setChecked(false);
    }

    /**
     * Set the root class attribute.
     *
     * @param rootClass the name of the root class.
     */
    public void setRootClass(String rootClass) {
        rootClasses.add(rootClass);
    }

    /**
     * Return the DirectoryScanner associated with this FileSet.
     *
     * @param p the project used to resolve dirs, etc.
     *
     * @return a dependency scanner.
     */
    @Override
    public DirectoryScanner getDirectoryScanner(Project p) {
        if (isReference()) {
            return getRef(p).getDirectoryScanner(p);
        }
        dieOnCircularReference(p);
        DirectoryScanner parentScanner = super.getDirectoryScanner(p);
        DependScanner scanner = new DependScanner(parentScanner);
        final Vector<String> allRootClasses = new Vector<>(rootClasses);
        for (FileSet additionalRootSet : rootFileSets) {
            DirectoryScanner additionalScanner
                = additionalRootSet.getDirectoryScanner(p);
            for (String file : additionalScanner.getIncludedFiles()) {
                if (file.endsWith(".class")) {
                    String classFilePath = StringUtils.removeSuffix(file, ".class");
                    String className
                        = classFilePath.replace('/', '.').replace('\\', '.');
                    allRootClasses.addElement(className);
                }
            }
            scanner.addBasedir(additionalRootSet.getDir(p));
        }
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
        rootClasses.add(root.getClassname());
    }

    /**
     * Clone this data type.
     *
     * @return a clone of the class file set.
     */
    @Override
    public Object clone() {
        return new ClassfileSet(isReference() ? getRef() : this);
    }

    @Override
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p) {
        if (isChecked()) {
            return;
        }

        // takes care of nested selectors
        super.dieOnCircularReference(stk, p);

        if (!isReference()) {
            for (FileSet additionalRootSet : rootFileSets) {
                pushAndInvokeCircularReferenceCheck(additionalRootSet, stk, p);
            }
            setChecked(true);
        }
    }

    private ClassfileSet getRef() {
        return getCheckedRef(ClassfileSet.class);
    }
}
