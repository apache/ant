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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.ant.common.service;
import java.net.URL;
import org.apache.ant.common.antlib.AntLibFactory;
import org.apache.ant.common.util.ExecutionException;

/**
 * The Component Service is used to manage the definitions that Ant uses at
 * runtime. It supports the following operations
 * <ul>
 *   <li> Definition of library search paths
 *   <li> Importing tasks from a library
 *   <li> taskdefs
 *   <li> typedefs
 * </ul>
 *
 *
 * @author Conor MacNeill
 * @created 27 January 2002
 */
public interface ComponentService {
    /**
     * Load a single or multiple Ant libraries
     *
     * @param libLocation the location of the library or the libraries
     * @param importAll true if all components of the loaded libraries
     *      should be imported
     * @param autoImport true if libraries in the Ant namespace should be
     *                   automatically imported.
     * @exception ExecutionException if the library or libraries cannot be
     *      imported.
     */
    void loadLib(String libLocation, boolean importAll, boolean autoImport)
         throws ExecutionException;

    /**
     * Add a library path to the given library. The library path is used in
     * the construction of the library's classloader
     *
     * @param libraryId the library's unique identifier
     * @param libPath the path to be added to the list of paths used by the
     *      library.
     * @exception ExecutionException if the path cannot be used.
     */
    void addLibPath(String libraryId, URL libPath) throws ExecutionException;

    /**
     * Define a new type
     *
     * @param typeName the name by which this type will be referred
     * @param factory the library factory object to create the type
     *      instances
     * @param loader the class loader to use to create the particular types
     * @param className the name of the class implementing the type
     * @exception ExecutionException if the type cannot be defined
     */
    void typedef(AntLibFactory factory, ClassLoader loader,
                 String typeName, String className)
         throws ExecutionException;

    /**
     * Experimental - define a new task
     *
     * @param taskName the name by which this task will be referred
     * @param factory the library factory object to create the task
     *      instances
     * @param loader the class loader to use to create the particular tasks
     * @param className the name of the class implementing the task
     * @exception ExecutionException if the task cannot be defined
     */
    void taskdef(AntLibFactory factory, ClassLoader loader,
                 String taskName, String className)
         throws ExecutionException;


    /**
     * Import a single component from a library, optionally aliasing it to a
     * new name
     *
     * @param libraryId the unique id of the library from which the
     *      component is being imported
     * @param defName the name of the component within its library
     * @param alias the name under which this component will be used in the
     *      build scripts. If this is null, the components default name is
     *      used.
     * @exception ExecutionException if the component cannot be imported
     */
    void importComponent(String libraryId, String defName,
                         String alias) throws ExecutionException;

    /**
     * Import a complete library into the current execution frame
     *
     * @param libraryId The id of the library to be imported
     * @exception ExecutionException if the library cannot be imported
     */
    void importLibrary(String libraryId) throws ExecutionException;

    /**
     * Imports a component defined in another frame.
     *
     * @param relativeName the qualified name of the component relative to
     *      this execution frame
     * @param alias the name under which this component will be used in the
     *      build scripts. If this is null, the components default name is
     *      used.
     * @exception ExecutionException if the component cannot be imported
     */
    void importFrameComponent(String relativeName, String alias)
         throws ExecutionException;

    /**
     * Create a component. The component will have a context but will not be
     * configured. It should be configured using the appropriate set methods
     * and then validated before being used.
     *
     * @param componentName the name of the component
     * @return the created component. The return type of this method depends
     *      on the component type.
     * @exception ExecutionException if the component cannot be created
     */
    Object createComponent(String componentName) throws ExecutionException;

    /**
     * Create a component given its class. The component will have a context
     * but will not be configured. It should be configured using the
     * appropriate set methods and then validated before being used.
     *
     * @param componentClass the component's class
     * @param factory the factory to create the component
     * @param loader the classloader associated with the component
     * @param addTaskAdapter whenther the returned component should be a
     *      task, potentially being wrapped in an adapter
     * @param componentName the name of the component type
     * @return the created component. The return type of this method depends
     *      on the component type.
     * @exception ExecutionException if the component cannot be created
     */
    Object createComponent(AntLibFactory factory, ClassLoader loader,
                           Class componentClass, boolean addTaskAdapter,
                           String componentName) throws ExecutionException;
}

