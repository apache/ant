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
package org.apache.tools.ant;

import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.util.AntException;

/**
 * ProjectComponent facade
 *
 * @author Conor MacNeill
 * @created 31 January 2002
 */
public abstract class ProjectComponent {
    /** The project in which the project component operates */
    protected Project project;
    /** The core context for this component */
    private AntContext context;
    /** The type of the component bneing created */
    private String componentType;

    /**
     * Sets the project of the ProjectComponent
     *
     * @param project the project object with which this component is
     *      associated
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Gets the project of the ProjectComponent
     *
     * @return the project
     */
    public Project getProject() {
        return project;
    }

    /**
     * Gets the componentType of the ProjectComponent
     *
     * @return the componentType value
     */
    public String getComponentType() {
        return componentType;
    }


    /**
     * Get the context associated with this component
     *
     * @return the AntContext
     */
    public AntContext getAntContext() {
        return context;
    }

    /**
     * Initialise this component
     *
     * @param context the core context for this component
     * @param componentType the component type of this component
     * @exception AntException if the component cannot be initialized
     */
    public void init(AntContext context, String componentType)
            throws AntException {
        this.context = context;
        this.componentType = componentType;

    }

    /**
     * Log a message as a build event
     *
     * @param message the message to be logged
     * @param level the priority level of the message
     */
    public void log(String message, int level) {
        if (context != null) {
            context.log(message, level);
        }
    }

    /**
     * Log a message as a build event
     *
     * @param message the message to be logged
     */
    public void log(String message) {
        log(message, Project.MSG_INFO);
    }
}

