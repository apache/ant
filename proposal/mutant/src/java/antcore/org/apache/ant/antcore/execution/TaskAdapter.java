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
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.apache.ant.common.antlib.AbstractTask;
import org.apache.ant.common.event.MessageLevel;

/**
 * Use introspection to "adapt" an arbitrary Bean (not extending Task, but
 * with similar patterns).
 *
 * @author Conor MacNeill
 * @created 16 January 2002
 */
public class TaskAdapter extends AbstractTask {

    /** The real object that is performing the work */
    private Object worker;

    /** the execute method of the real object */
    private Method executeMethod = null;

    /**
     * Create an adapter for an arbitraty bean
     *
     * @param taskType the name of the task
     * @param worker an instance of the actual object that does the work
     * @exception ExecutionException if the worker object does not support
     *      an execute method
     */
    public TaskAdapter(String taskType, Object worker)
         throws ExecutionException {
        this.worker = worker;
        try {
            Class workerClass = worker.getClass();
            executeMethod = workerClass.getMethod("execute", new Class[0]);
            if (executeMethod == null) {
                throw new ExecutionException("No execute method in the class"
                     + " for the <" + taskType + "> task.");
            }
        } catch (NoSuchMethodException e) {
            throw new ExecutionException(e);
        }
    }


    /**
     * Standard Task execute method. This invokes the execute method of the
     * worker instance
     *
     * @exception ExecutionException if the proxied object throws an exception
     */
    public void execute() throws ExecutionException {
        try {
            executeMethod.invoke(worker, null);
        } catch (InvocationTargetException e) {
            log("Error in " + worker.getClass(), MessageLevel.MSG_ERR);
            Throwable t = e.getTargetException();
            if (t instanceof ExecutionException) {
                throw (ExecutionException) t;
            } else {
                throw new ExecutionException(t);
            }
        } catch (Throwable t) {
            log("Error in " + worker.getClass(), MessageLevel.MSG_ERR);
            throw new ExecutionException(t);
        }
    }
}

