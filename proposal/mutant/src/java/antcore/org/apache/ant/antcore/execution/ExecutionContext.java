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
import java.io.File;
import java.net.URL;
import org.apache.ant.common.context.AntContext;
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.util.FileUtils;
import org.apache.ant.antcore.event.BuildEventSupport;
import org.apache.ant.antcore.model.ModelElement;

/**
 * This is the core's implementation of the AntContext for all core objects.
 * Specific subclasses handle types and tasks
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 20 January 2002
 */
public class ExecutionContext extends AntContext {
    /** The ExecutionFrame containing this context */
    private ExecutionFrame executionFrame;
    /** the event support instance used to manage build events */
    private BuildEventSupport eventSupport;

    /** the model in the build model with which this context is associated */
    private ModelElement modelElement;

    /** General file utilities */
    private FileUtils fileUtils = new FileUtils();

    /**
     * Set the model element associated with this context
     *
     * @param modelElement the model element associated with this context
     */
    public void setModelElement(ModelElement modelElement) {
        this.modelElement = modelElement;
    }

    /**
     * Initilaise this context's environment
     *
     * @param executionFrame the frame containing this context
     * @param eventSupport the event support instance used to send build
     *      events
     */
    public void initEnvironment(ExecutionFrame executionFrame,
                                BuildEventSupport eventSupport) {
        this.executionFrame = executionFrame;
        this.eventSupport = eventSupport;
    }

    /**
     * Log a message as a build event
     *
     * @param message the message to be logged
     * @param level the priority level of the message
     */
    public void log(String message, int level) {
        eventSupport.fireMessageLogged(modelElement, message, level);
    }

    /**
     * Resolve a file according to the base directory of the project
     * associated with this context
     *
     * @param fileName the file name to be resolved.
     * @return the file resolved to the project's base dir
     * @exception AntException if the file cannot be resolved
     */
    public File resolveFile(String fileName) throws AntException {
        URL baseURL = executionFrame.getBaseURL();
        if (!baseURL.getProtocol().equals("file")) {
            throw new ExecutionException("Cannot resolve name " + fileName
                 + " to a file because the project basedir is not a file: "
                 + baseURL);
        }

        return fileUtils.resolveFile(fileUtils.normalize(baseURL.getFile()), 
            fileName);
    }
}

