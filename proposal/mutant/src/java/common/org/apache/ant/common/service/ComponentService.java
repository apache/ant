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
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 27 January 2002
 */
public interface ComponentService {
    /**
     * Load a single or multiple Ant libraries
     *
     * @param libLocation the location of the library or the libraries
     * @param importAll true if all components of the loaded libraries
     *      should be imported
     * @exception ExecutionException if the library or libraries cannot be
     *      imported.
     */
    void loadLib(String libLocation, boolean importAll)
         throws ExecutionException;

    /**
     * Experimental - define a new type
     *
     * @param typeName the name by which this type will be referred
     * @param factory the library factory object to create the type instances
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
     * @param factory the library factory object to create the task instances
     * @param loader the class loader to use to create the particular tasks
     * @param className the name of the class implementing the task
     * @exception ExecutionException if the task cannot be defined
     */
    void taskdef(AntLibFactory factory, ClassLoader loader,
                 String taskName, String className)
         throws ExecutionException;

}

