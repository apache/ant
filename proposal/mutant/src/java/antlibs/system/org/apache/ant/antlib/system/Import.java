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
package org.apache.ant.antlib.system;

import org.apache.ant.common.antlib.AbstractTask;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.antlib.ValidationException;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.util.AntException;

/**
 * Task to import a component or components from a library
 *
 * @author Conor MacNeill
 * @created 27 January 2002
 */
public class Import extends AbstractTask {
    /** The Ant LIbrary Id from which the component must be imported */
    private String libraryId = null;
    /** The name of the component to be imported */
    private String name = null;
    /**
     * A ref is used to import a task which has been declared in another
     * project
     */
    private String ref = null;
    /** The alias that is to be used for the name */
    private String alias = null;

    /**
     * Sets the libraryId of the Import
     *
     * @param libraryId the new libraryId value
     */
    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
    }

    /**
     * Sets the name of the Import
     *
     * @param name the new name value
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the reference name of a task defined in a referenced frame
     *
     * @param ref the new ref value
     */
    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * Sets the alias of the Import
     *
     * @param alias the new alias value
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Validate this task is properly configured
     *
     * @exception ValidationException if the task is not configured correctly
     */
    public void validateComponent() throws ValidationException {
        if (ref != null) {
            if (libraryId != null || name != null) {
                throw new ValidationException("The \"ref\" attribute can only "
                     + "be used when \"libraryId\" and \"name\" attributes are "
                     + "not present");
            }
        } else {
            if (libraryId == null) {
                throw new ValidationException("You must specify a library "
                     + "identifier with the \"libraryid\" attribute");
            }
            if (alias != null && name == null) {
                throw new ValidationException("You may only specify an alias"
                     + " when you specify the \"name\" or \"ref\" attributes");
            }
        }
    }

    /**
     * Do the work and import the component or components
     *
     * @exception AntException if the components cannot be imported
     */
    public void execute() throws AntException {
        AntContext context = getAntContext();
        ComponentService componentService = (ComponentService)
            context.getCoreService(ComponentService.class);
        if (ref != null) {
            componentService.importFrameComponent(ref, alias);
        } else if (name == null) {
            componentService.importLibrary(libraryId);
        } else {
            componentService.importComponent(libraryId, name, alias);
        }
    }
}

