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
package org.apache.ant.antcore.antlib;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.antlib.AntLibFactory;
import org.apache.ant.common.util.AntException;

/**
 * A Component Library supplies components to the Ant core.
 *
 * @author Conor MacNeill
 * @created 8 February 2002
 */
public interface ComponentLibrary {
    /** constant indicating a taskdef definition */
    public static final int TASKDEF = 1;
    /** constant indicating a typedef definition */
    public static final int TYPEDEF = 2;

    /**
     * Gets the ClassLoader of the AntLibrary
     *
     * @return The ClassLoader value
     */
    ClassLoader getClassLoader();

    /**
     * Gat an instance of a factory object for creating objects in this
     * library.
     *
     * @param context the context to use for the factory creation if
     *      required
     * @return an instance of the factory, or null if this library does not
     *      support a factory
     * @exception AntException if the factory cannot be created
     */
    AntLibFactory getFactory(AntContext context) throws AntException;

    /**
     * Gets the libraryId of the AntLibrary
     *
     * @return the libraryId value
     */
    String getLibraryId();

    /**
     * Get the definition of a particular component
     *
     * @param definitionName the name of the component within the library
     * @return an AntLibDefinition instance with information about the
     *      component's definition
     */
    AntLibDefinition getDefinition(String definitionName);
}

