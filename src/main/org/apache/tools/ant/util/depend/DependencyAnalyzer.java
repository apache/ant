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
 * @author Conor MacNeill
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
    Enumeration getFileDependencies();

    /**
     * Get the list of classes upon which root classes depend. This is a
     * list of Java classnames in dot notation.
     *
     * @return an enumeration of Strings, each being the name of a Java
     *      class in dot notation.
     */
    Enumeration getClassDependencies();


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

