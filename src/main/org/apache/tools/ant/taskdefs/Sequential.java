/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.BuildException;

import java.util.Vector;
import java.util.Enumeration;



/**
 * Sequential is a container task - it can contain other Ant tasks. The nested 
 * tasks are simply executed in sequence. Sequential's primary use is to support 
 * the sequential execution of a subset of tasks within the
 {@link Parallel Parallel Task} 

 * <p>
 * The sequential task has no attributes and does not support any nested 
 * elements apart from Ant tasks. Any valid Ant task may be embedded within the 
 * sequential task.</p>
 
 * @author Thomas Christen <a href="mailto:chr@active.ch">chr@active.ch</a>
 * @since Ant 1.4
 * @ant.task category="control"
 */
public class Sequential extends Task
                        implements TaskContainer {

    /** Optional Vector holding the nested tasks */
    private Vector nestedTasks = new Vector();

    /**
     * Override {@link org.apache.tools.ant.Task#maybeConfigure
     * maybeConfigure} in a way that leaves the nested tasks
     * unconfigured until they get executed.
     *
     * @since Ant 1.5
     */
    public void maybeConfigure() throws BuildException {
        if (isInvalid()) {
            super.maybeConfigure();
        } else {
            getRuntimeConfigurableWrapper().maybeConfigure(getProject(), false);
        }
    }

    /**
     * Add a nested task to Sequential.
     * <p>
     * @param nestedTask  Nested task to execute Sequential
     * <p>
     */
    public void addTask(Task nestedTask) {
        nestedTasks.addElement(nestedTask);
    }

    /**
     * Execute all nestedTasks.
     */
    public void execute() throws BuildException {
        for (Enumeration e = nestedTasks.elements(); e.hasMoreElements();) {
            Task nestedTask = (Task) e.nextElement();
            nestedTask.perform();
        }
    }
}
