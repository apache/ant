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
import org.apache.ant.common.event.MessageLevel;
import org.apache.ant.common.util.AntException;

/**
 * Abstract implementation of the Task interface
 *
 * @author Conor MacNeill
 * @created 16 January 2002
 */
public abstract class AbstractTask extends AbstractComponent implements Task {
    /** The name of this task. */
    private String taskName;

    /**
     * Sets the taskName of the AbstractTask
     *
     * @param taskName the new taskName value
     */
    public final void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * Gets the taskName of the AbstractTask
     *
     * @return the taskName value
     */
    public final String getTaskName() {
        return taskName;
    }

    /**
     * Handle Output produced by the task. When a task prints to System.out
     * the container may catch this and redirect the content back to the
     * task by invoking this method. This method must NOT call System.out,
     * directly or indirectly.
     *
     * @param line The line of content produce by the task
     * @exception AntException if the output cannot be handled.
     */
    public void handleSystemOut(String line) throws AntException {
        // default behaviout is to log at INFO level
        log(line, MessageLevel.INFO);
    }

    /**
     * Handle error information produced by the task. When a task prints to
     * System.err the container may catch this and redirect the content back
     * to the task by invoking this method. This method must NOT call
     * System.err, directly or indirectly.
     *
     * @param line The line of error info produce by the task
     * @exception AntException if the output cannot be handled.
     */
    public void handleSystemErr(String line) throws AntException {
        // default behaviout is to log at WARN level
        log(line, MessageLevel.WARNING);
    }
}

