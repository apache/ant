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

import org.apache.tools.ant.types.Path;

/**
 * A dependency analyzer analyzes dependencies between Java classes to
 * determine the minimal set of classes which are required by a set of
 * &quot;root&quot; classes. Different implementations of this interface can
 * use different strategies and libraries to determine the required set. For
 * example, some analyzers will use class files while others might use
 * source files. Analyzer specific configuration is catered for through a
 * generic configure method
 *
 */
public interface DependencyAnalyzer {
    /**
     * Add a source path to the source path used by this analyzer. The
     * elements in the given path contain the source files for the classes
     * being analyzed. Not all analyzers will use this information.
     *
     * @param sourcePath The Path instance specifying the source path
     *      elements.
     */
    void addSourcePath(Path sourcePath);

    /**
     * Add a classpath to the classpath being used by the analyzer. The
     * classpath contains the binary classfiles for the classes being
     * analyzed The elements may either be the directories or jar files.Not
     * all analyzers will use this information.
     *
     * @param classpath the Path instance specifying the classpath elements
     */
    void addClassPath(Path classpath);

    /**
     * Add a root class. The root classes are used to drive the
     * determination of dependency information. The analyzer will start at
     * the root classes and add dependencies from there.
     *
     * @param classname the name of the class in Java dot notation.
     */
    void addRootClass(String classname);

    /**
     * Get the list of files in the file system upon which the root classes
     * depend. The files will be either the classfiles or jar files upon
     * which the root classes depend.
     *
     * @return an enumeration of File instances.
     */
    Enumeration<File> getFileDependencies();

    /**
     * Get the list of classes upon which root classes depend. This is a
     * list of Java classnames in dot notation.
     *
     * @return an enumeration of Strings, each being the name of a Java
     *      class in dot notation.
     */
    Enumeration<String> getClassDependencies();


    /**
     * Reset the dependency list. This will reset the determined
     * dependencies and the also list of root classes.
     */
    void reset();

    /**
     * Configure an aspect of the analyzer. The set of aspects that are
     * supported is specific to each analyzer instance.
     *
     * @param name the name of the aspect being configured
     * @param info the configuration information.
     */
    void config(String name, Object info);

    /**
     * Set the closure flag. If this flag is true the analyzer will traverse
     * all class relationships until it has collected the entire set of
     * direct and indirect dependencies
     *
     * @param closure true if dependencies should be traversed to determine
     *      indirect dependencies.
     */
    void setClosure(boolean closure);


    /**
     * Get the file that contains the class definition
     *
     * @param classname the name of the required class
     * @return the file instance, zip or class, containing the
     *         class or null if the class could not be found.
     * @exception IOException if the files in the classpath cannot be read.
     */
    File getClassContainer(String classname) throws IOException;

    /**
     * Get the file that contains the class source.
     *
     * @param classname the name of the required class
     * @return the file instance, zip or java, containing the
     *         source or null if the source for the class could not be found.
     * @exception IOException if the files in the sourcepath cannot be read.
     */
    File getSourceContainer(String classname) throws IOException;
}

