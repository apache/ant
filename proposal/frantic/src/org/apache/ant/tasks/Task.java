/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package org.apache.ant.tasks;


import java.util.*;
import org.apache.ant.AntException;
import org.apache.ant.engine.TaskEngine;

/**
 * Task is the core interface for all structures that will be processed by the
 * TaskEngine.
 */
public interface Task {
    
    public static final int EXECUTION_MODE_IMPLICIT = 0;
    public static final int EXECUTION_MODE_EXPLICIT = 1;
    public static final int EXECUTION_MODE_PRIORITY = 2;
    
    public static final char NAME_SEPARATOR = '/';
    
    /**
     * Causes the existing Task implementation to execute.
     */
    void execute(TaskEngine engine) throws AntException;
    
    /**
     * Called when the Task first gets "noticed" by the TaskEngine.
     */
    void init(TaskEngine engine) throws AntException;
    
    /**
     * Each Task should have the ability to validate its state. This would be
     * invoked by the TaskEngine prior to commencing an execution cycle.
     */
    void validate() throws AntException;
    
    /**
     * Just a simple name used to identify a Task. This name is only sufficient
     * for simple debugging and GUI output. It does not uniquely identify a
     * Task.
     *
     * @see #getFullyQualifiedName
     */
    String getTaskName();
    
    /**
     * Although this method seems to tie the concept of XML "tags", it is not
     * necessarily so. The tag name will serve as a general description of the
     * type of tag represented by this class instance. It is primarily used by
     * Tasks with sub-Tasks that are not assigned to a specific class
     * implementation.
     *
     * @see org.apache.ant.tasks.UnknownTask
     */
//    String getTaskTag();
    
    /**
     * A mechanism for locating a task relative to the current task. This
     * navigation sceme will mimic a typical OS system. '..' will move back
     * a level in the Task tree. If taskPath begins with '/' then the root node
     * will be used as a starting point.
     * <p></p>
     * Returns null of no task is found at this location.
     */
    Task getTask(String taskPath);
    
    /**
     * Proceed backwards through the nodes until we come across the first Task
     * in the tree. This is the root Task.
     */
    Task getRootTask();
    
    /**
     * The "fully-qualified" name of a Task is the Task's name, prepended by its
     * parent's name, prepended by its parent's name, etc. This method may be
     * used by the Task's hashCode() method to calculate a hash that will
     * uniquely identify a Task.
     */
    String getFullyQualifiedName();
    
    /**
     * Determines whether this Task is executed whenever its parent is executed,
     * or if its execution must be specifically requested.
     * <p></p>
     * <dl><dt>EXECUTION_MODE_EXPLICIT</dt>
     * <dd>Requires interaction by the TaskEngine in order to execute.</dd>
     * <dt>EXECUTION_MODE_IMPLICIT</dt>
     * <dd>This Task is automatically executed when its parent is
     * executed.</dd>
     * <dt>EXECUTION_MODE_PRIORITY</dt>
     * <dd>These Tasks are executed prior to its parent's execution</dd></dl>
     * <p></p>
     * The default mode should probably be EXECUTION_MODE_IMPLICIT. In the
     * build domain of Ant, every Task below a Task will normally be executed.
     * The major exception to this is the Target. When a Project Task is
     * executed, all Target Tasks do <i>not</i> automatically fire, however all
     * Property Tasks <i>do</i> execute.
     */
    int getExecutionMode();
    
    /**
     * Determines whether a Task is suitable for holding property values.
     */
    boolean isPropertyContainer();
    
    /**
     * Each Task will hold its attributes in some manner. This method will allow
     * the Task implementation to return the value of its attribute.
     */
    String getAttributeValue(String name);
    
    /**
     * Returns this Task's parent Task. If this Task is the root Task, then this
     * method will return null.
     */
    Task getParent();
    
    /**
     * Sets the Task's parent.
     */
    void setParent(Task parent);
    
    /**
     * Returns the an array of Task objects that are subordinate to this Task.
     */
    Task[] getChildren();
}
