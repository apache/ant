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
package org.apache.ant.common.antlib;

import org.apache.ant.common.util.AntException;

/**
 * Abstract implementation of the ExecutionComponent
 *
 * @author Conor MacNeill
 * @created 5 February 2002
 */
public abstract class AbstractComponent implements ExecutionComponent {
    /** The components's context */
    private AntContext context;

    /**
     * the type of the component. The type is the name of the component in
     * the build file. This may be different from the name under which this
     * componenent is known in its library due to aliasing
     */
    private String componentType;

    /**
     * Get this component's context
     *
     * @return the component context
     */
    public AntContext getAntContext() {
        return context;
    }

    /**
     * Gets the componentType of the AbstractComponent
     *
     * @return the componentType value
     */
    public String getComponentType() {
        return componentType;
    }

    /**
     * Initialise the component. The component may use the AntContext to
     * request services from the Ant core.
     *
     * @param context the component's context
     * @param componentType the type of the component
     * @exception AntException if initialisation fails
     */
    public void init(AntContext context, String componentType)
         throws AntException {
        this.context = context;
        this.componentType = componentType;
    }

    /**
     * Validate the component. This is called after the element has been
     * configured from its build model. The element may perform validation
     * of its configuration
     *
     * @exception ValidationException if validation fails
     */
    public void validateComponent() throws ValidationException {
        // no validation by default
    }

    /**
     * Short cut to get a core service instance
     *
     * @param serviceClass the required interface of which an instance is
     *      required
     * @return the core's instance of the requested service
     * @exception AntException if the core does not support the
     *      requested service
     */
    protected Object getCoreService(Class serviceClass)
         throws AntException {
        return context.getCoreService(serviceClass);
    }

    /**
     * Log a message as a build event
     *
     * @param message the message to be logged
     * @param level the priority level of the message
     */
    protected void log(String message, int level) {
        context.log(message, level);
    }
}

