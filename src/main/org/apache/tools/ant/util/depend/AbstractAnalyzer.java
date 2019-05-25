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
package org.apache.tools.ant.util.depend;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.VectorSet;

/**
 * An abstract implementation of the analyzer interface providing support
 * for the bulk of interface methods.
 *
 */
public abstract class AbstractAnalyzer implements DependencyAnalyzer {
    /** Maximum number of loops for looking for indirect dependencies. */
    public static final int MAX_LOOPS = 1000;

    /** The source path for the source files */
    private Path sourcePath = new Path(null);

    /** The classpath containing dirs and jars of class files */
    private Path classPath = new Path(null);

    /** The list of root classes */
    private final Vector<String> rootClasses = new VectorSet<>();

    /** true if dependencies have been determined */
    private boolean determined = false;

    /** the list of File objects that the root classes depend upon */
    private Vector<File> fileDependencies;
    /** the list of java classes the root classes depend upon */
    private Vector<String> classDependencies;

    /** true if indirect dependencies should be gathered */
    private boolean closure = true;

    /** Setup the analyzer */
    protected AbstractAnalyzer() {
        reset();
    }

    /**
     * Set the closure flag. If this flag is true the analyzer will traverse
     * all class relationships until it has collected the entire set of
     * direct and indirect dependencies
     *
     * @param closure true if dependencies should be traversed to determine
     *      indirect dependencies.
     */
    @Override
    public void setClosure(boolean closure) {
        this.closure = closure;
    }

    /**
     * Get the list of files in the file system upon which the root classes
     * depend. The files will be either the classfiles or jar files upon
     * which the root classes depend.
     *
     * @return an enumeration of File instances.
     */
    @Override
    public Enumeration<File> getFileDependencies() {
        if (!supportsFileDependencies()) {
            throw new BuildException(
                "File dependencies are not supported by this analyzer");
        }
        if (!determined) {
            determineDependencies(fileDependencies, classDependencies);
        }
        return fileDependencies.elements();
    }

    /**
     * Get the list of classes upon which root classes depend. This is a
     * list of Java classnames in dot notation.
     *
     * @return an enumeration of Strings, each being the name of a Java
     *      class in dot notation.
     */
    @Override
    public Enumeration<String> getClassDependencies() {
        if (!determined) {
            determineDependencies(fileDependencies, classDependencies);
        }
        return classDependencies.elements();
    }

    /**
     * Get the file that contains the class definition
     *
     * @param classname the name of the required class
     * @return the file instance, zip or class, containing the
     *         class or null if the class could not be found.
     * @exception IOException if the files in the classpath cannot be read.
     */
    @Override
    public File getClassContainer(String classname) throws IOException {
        String classLocation = classname.replace('.', '/') + ".class";
        // we look through the classpath elements. If the element is a dir
        // we look for the file. IF it is a zip, we look for the zip entry
        return getResourceContainer(classLocation, classPath.list());
    }

    /**
     * Get the file that contains the class source.
     *
     * @param classname the name of the required class
     * @return the file instance, zip or java, containing the
     *         source or null if the source for the class could not be found.
     * @exception IOException if the files in the sourcepath cannot be read.
     */
    @Override
    public File getSourceContainer(String classname) throws IOException {
        String sourceLocation = classname.replace('.', '/') + ".java";

        // we look through the source path elements. If the element is a dir
        // we look for the file. If it is a zip, we look for the zip entry.
        // This isn't normal for source paths but we get it for free
        return getResourceContainer(sourceLocation, sourcePath.list());
    }

    /**
     * Add a source path to the source path used by this analyzer. The
     * elements in the given path contain the source files for the classes
     * being analyzed. Not all analyzers will use this information.
     *
     * @param sourcePath The Path instance specifying the source path
     *      elements.
     */
    @Override
    public void addSourcePath(Path sourcePath) {
        if (sourcePath == null) {
            return;
        }
        this.sourcePath.append(sourcePath);
        this.sourcePath.setProject(sourcePath.getProject());
    }

    /**
     * Add a classpath to the classpath being used by the analyzer. The
     * classpath contains the binary classfiles for the classes being
     * analyzed The elements may either be the directories or jar files.Not
     * all analyzers will use this information.
     *
     * @param classPath the Path instance specifying the classpath elements
     */
    @Override
    public void addClassPath(Path classPath) {
        if (classPath == null) {
            return;
        }

        this.classPath.append(classPath);
        this.classPath.setProject(classPath.getProject());
    }

    /**
     * Add a root class. The root classes are used to drive the
     * determination of dependency information. The analyzer will start at
     * the root classes and add dependencies from there.
     *
     * @param className the name of the class in Java dot notation.
     */
    @Override
    public void addRootClass(String className) {
        if (className == null) {
            return;
        }
        if (!rootClasses.contains(className)) {
            rootClasses.addElement(className);
        }
    }

    /**
     * Configure an aspect of the analyzer. The set of aspects that are
     * supported is specific to each analyzer instance.
     *
     * @param name the name of the aspect being configured
     * @param info the configuration info.
     */
    @Override
    public void config(String name, Object info) {
        // do nothing by default
    }

    /**
     * Reset the dependency list. This will reset the determined
     * dependencies and the also list of root classes.
     */
    @Override
    public void reset() {
        rootClasses.removeAllElements();
        determined = false;
        fileDependencies = new Vector<>();
        classDependencies = new Vector<>();
    }

    /**
     * Get an enumeration of the root classes
     *
     * @return an enumeration of Strings, each of which is a class name
     *         for a root class.
     */
    protected Enumeration<String> getRootClasses() {
        return rootClasses.elements();
    }

    /**
     * Indicate if the analyzer is required to follow
     * indirect class relationships.
     *
     * @return true if indirect relationships should be followed.
     */
    protected boolean isClosureRequired() {
        return closure;
    }

    /**
     * Determine the dependencies of the current set of root classes
     *
     * @param files a vector into which Files upon which the root classes
     *      depend should be placed.
     * @param classes a vector of Strings into which the names of classes
     *      upon which the root classes depend should be placed.
     */
    protected abstract void determineDependencies(Vector<File> files, Vector<String> classes);

    /**
     * Indicate if the particular subclass supports file dependency
     * information.
     *
     * @return true if file dependencies are supported.
     */
    protected abstract boolean supportsFileDependencies();

    /**
     * Get the file that contains the resource
     *
     * @param resourceLocation the name of the required resource.
     * @param paths the paths which will be searched for the resource.
     * @return the file instance, zip or class, containing the
     *         class or null if the class could not be found.
     * @exception IOException if the files in the given paths cannot be read.
     */
    private File getResourceContainer(String resourceLocation, String[] paths)
         throws IOException {
        for (String path : paths) {
            File element = new File(path);
            if (!element.exists()) {
                continue;
            }
            if (element.isDirectory()) {
                File resource = new File(element, resourceLocation);
                if (resource.exists()) {
                    return resource;
                }
            } else {
                // must be a zip of some sort
                try (ZipFile zipFile = new ZipFile(element)) {
                    if (zipFile.getEntry(resourceLocation) != null) {
                        return element;
                    }
                }
            }
        }
        return null;
    }
}

