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
import org.apache.ant.tasks.Task;

public abstract class BaseTask implements Task {
    
    protected Task parent;
    protected ArrayList children = new ArrayList();
    protected String taskName;
    protected Properties properties = new Properties();
    
    public BaseTask() {
        super();
    }
    
    public void init(TaskEngine engine) throws AntException {
    }
    
    public void execute(TaskEngine engine) throws AntException {
    }
    
//    abstract public String getTaskTag();
    
    public void validate() throws AntException {}
    
    public String getFullyQualifiedName() {
        if (getParent() == null) return Task.NAME_SEPARATOR + taskName;
        return getParent().getFullyQualifiedName() + Task.NAME_SEPARATOR + taskName;
    }
    
    public int hashCode() {
        return getFullyQualifiedName().hashCode();
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public int getExecutionMode() {
        return Task.EXECUTION_MODE_PRIORITY;
    }
    
    public String getAttributeValue(String name) {
        /**@todo: Implement this org.apache.ant.Task method*/
        throw new java.lang.UnsupportedOperationException("Method getAttributeValue() not yet implemented.");
    }
    
    public Task getParent() {
        return parent;
    }
    
    /**
     * We have to trust the developers to not screw up the hierarchy by changing
     * the parent of a Task when it is a child of another.
     */
    public void setParent(Task parent) {
        this.parent = parent;
    }
    
    public Task[] getChildren() {
        return (Task[])children.toArray(new Task[children.size()]);
    }
    
    public void addChild(Task task) {
        children.add(task);
        task.setParent(this);
    }
    
    public void removeChild(Task task) {
        children.remove(task);
    }
    
    public Task getRootTask() {
        if (getParent() != null) {
            return getParent().getRootTask();
        }
        return this;
    }
    
    protected Task getTaskByName(String taskName) {
        Task[] tasks = getChildren();
        for (int i = 0, c = tasks.length; i < c; i++) {
            if (tasks[i].getTaskName().equals(taskName)) {
                return tasks[i];
            }
        }
        return null;
    }
    
    public Task getTask(String taskPath) {
        taskPath = taskPath.trim();
        int slashPos = taskPath.indexOf("/");
        
        // starts with '/'
        if (slashPos == 0) {
            return getRootTask().getTask(taskPath.substring(slashPos + 1));
        } else
            
            // starts with '../'
            if (taskPath.startsWith("../") || taskPath.equals("..")) {
                return getParent().getTask(taskPath.substring(3));
            } else
                
                // starts with './'
                if (taskPath.startsWith("./") || taskPath.equals(".")) {
                    return getTask(taskPath.substring(2));
                } else
                    
                    // starts with 'some_level/'
                    if (slashPos > 0) {
                        String levelName = taskPath.substring(0, slashPos);
                        return getTaskByName(levelName).getTask(taskPath.substring(slashPos + 1));
                    }
        
        // there are no more directories; what remains is the name of the task
        if (slashPos < 0) {
            if (taskPath.length() == 0) return this;
            return getTaskByName(taskPath);
        }
        return null;
    }
    
    public boolean isPropertyContainer() {
        return false;
    }
    
}
