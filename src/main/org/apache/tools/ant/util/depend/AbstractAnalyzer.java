/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.util.depend;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipFile;
import org.apache.tools.ant.types.Path;

/**
 * An abstract implementation of the analyzer interface providing support
 * for the bulk of interface methods.
 *
 * @author Conor MacNeill
 */
public abstract class AbstractAnalyzer implements DependencyAnalyzer {
    /** Maximum number of loops for looking for indirect dependencies. */
    public static final int MAX_LOOPS = 1000;

    /** The source path for the source files */
    private Path sourcePath = new Path(null);

    /** The classpath containg dirs and jars of class files */
    private Path classPath = new Path(null);

    /** The list of root classes */
    private Vector rootClasses = new Vector();

    /** true if dependencies have been determined */
    private boolean determined = false;

    /** the list of File objects that the root classes depend upon */
    private Vector fileDependencies;
    /** the list of java classes the root classes depend upon */
    private Vector classDependencies;

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
    public Enumeration getFileDependencies() {
        if (!supportsFileDependencies()) {
            throw new RuntimeException("File dependencies are not supported " 
                + "by this analyzer");
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
    public Enumeration getClassDependencies() {
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
    public void config(String name, Object info) {
        // do nothing by default
    }

    /**
     * Reset the dependency list. This will reset the determined
     * dependencies and the also list of root classes.
     */
    public void reset() {
        rootClasses.removeAllElements();
        determined = false;
        fileDependencies = new Vector();
        classDependencies = new Vector();
    }

    /**
     * Get an enumeration of the root classes
     *
     * @return an enumeration of Strings, each of which is a class name 
     *         for a root class.
     */
    protected Enumeration getRootClasses() {
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
    protected abstract void determineDependencies(Vector files, Vector classes);

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
        for (int i = 0; i < paths.length; ++i) {
            File element = new File(paths[i]);
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
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(element);
                    if (zipFile.getEntry(resourceLocation) != null) {
                        return element;
                    }
                } finally {
                    if (zipFile != null) {
                        zipFile.close();
                    }
                }
            }
        }
        return null;
    }
}

