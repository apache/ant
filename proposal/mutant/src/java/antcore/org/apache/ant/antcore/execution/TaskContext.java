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
import org.apache.ant.antcore.model.ModelElement;
import org.apache.ant.common.antlib.Task;
import org.apache.ant.common.util.ExecutionException;
/**
 * This is the core's implementation of the AntContext for Tasks.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 17 January 2002
 */
public class TaskContext extends ExecutionContext {

    /** The task being managed by this context */
    private Task task;

    /**
     * the loader used to load this task. Note that this is not necessarily
     * the loader which is used to load the Task class as loading may have
     * been delegated to a parent loader.
     */
    private ClassLoader loader;

    /**
     * Initilaise this context's environment
     *
     * @param frame the frame containing this context
     * @param eventSupport the event support instance used to send build
     *      events
     */
    public TaskContext(ExecutionFrame frame,
                       BuildEventSupport eventSupport) {
        super(frame, eventSupport);
    }

    /**
     * Get the task associated with this context
     *
     * @return the task instance
     */
    public Task getTask() {
        return task;
    }

    /**
     * Gets the loader for this task
     *
     * @return the task's loader
     */
    public ClassLoader getLoader() {
        return loader;
    }

    /**
     * Associate a task with this context
     *
     * @param task the task to be manager
     * @param loader the classloader
     * @param modelElement the model element associated with this context
     * @exception ExecutionException if the task cannot be initialized 
     */
    public void init(ClassLoader loader, Task task, ModelElement modelElement)
         throws ExecutionException {
        this.task = task;
        this.loader = loader;
        setModelElement(modelElement);
        task.init(this);
    }

    /**
     * execute this context's task
     *
     * @exception ExecutionException if there is a problem executing the task
     */
    public void execute() throws ExecutionException {
        task.execute();
    }

    /**
     * Destroy this context. The context can be reused for another task
     * after this one
     */
    public void destroy() {
        task.destroy();
        task = null;
        loader = null;
    }
}

