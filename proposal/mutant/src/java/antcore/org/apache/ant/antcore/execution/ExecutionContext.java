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
package org.apache.ant.antcore.execution;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.antlib.ExecutionComponent;
import org.apache.ant.common.util.Location;
import org.apache.ant.common.model.BuildElement;

/**
 * This is the core's implementation of the AntContext for all core objects.
 * Specific subclasses handle types and tasks
 *
 * @author Conor MacNeill
 * @created 20 January 2002
 */
public class ExecutionContext implements AntContext {
    /** The Frame containing this context */
    private Frame frame;

    /** the event support instance used to manage build events */
    private BuildEventSupport eventSupport;

    /** The build model associated with this context. */
    private BuildElement model;

    /** the execution component associated with the context, if any */
    private ExecutionComponent component;

    /**
     * the loader used to load this context. Note that this is not
     * necessarily the loader which is used to load the component as loading
     * may have been delegated to a parent loader.
     */
    private ClassLoader loader;

    /**
     * Initilaise this context's environment
     *
     * @param frame the frame containing this context
     * @param component the component associated with this context - may be null
     * @param model the build model associated with this component if any.
     */
    protected ExecutionContext(Frame frame, ExecutionComponent component,
                               BuildElement model) {
        this.frame = frame;
        this.eventSupport = frame.getEventSupport();
        this.model = model;
        this.component = component;
    }

    /**
     * Get an implementation of one of the core's service interfaces
     *
     * @param serviceInterfaceClass the interface class for which an
     *      implementation is required
     * @return the core's implementation of the interface.
     * @exception ExecutionException if there is a problem finding the
     *      interface
     */
    public Object getCoreService(Class serviceInterfaceClass)
         throws ExecutionException {
        return frame.getCoreService(serviceInterfaceClass);
    }


    /**
     * Gets the location associated with the ExecutionContext
     *
     * @return the location in the build model associated with this context.
     */
    public Location getLocation() {
        if (model == null) {
            return Location.UNKNOWN_LOCATION;
        }
        return model.getLocation();
    }

    /**
     * Log a message as a build event
     *
     * @param message the message to be logged
     * @param level the priority level of the message
     */
    public void log(String message, int level) {
        Object source = component;
        if (source == null) {
            source = frame.getProject();
            if (source == null) {
                source = frame;
            }
        }
        eventSupport.fireMessageLogged(source, message, level);
    }

    /**
     * Sets the classLoader of the ExecutionContext
     *
     * @param loader the new classLoader value
     */
    protected void setClassLoader(ClassLoader loader) {
        this.loader = loader;
    }

    /**
     * Gets the loader for this context
     *
     * @return the context's loader
     */
    public ClassLoader getClassLoader() {
        return loader;
    }

    /**
     * Gets the executionComponent of the ExecutionContext
     *
     * @return the executionComponent value
     */
    protected ExecutionComponent getExecutionComponent() {
        return component;
    }

    /**
     * Get the build model associated with this context.
     *
     * @return the build model or null if there is no build model.
     */
    protected BuildElement getModel() {
        return model;
    }
}

