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
package org.apache.ant.engine;


import java.util.*;
import org.apache.ant.AntException;
import org.apache.ant.tasks.Task;

/**
 * The TaskEngine interface defines the methods that a TaskEngine are required
 * to implement. This interface is also passed to each Task in order for them to
 * get access to some utility functions like inserting a new Task during a run,
 * or forcing the execution path of Tasks to be modified.
 */
public interface TaskEngine {
    
    void addAntEngineListener(AntEngineListener listener);
    
    void removeAntEngineListener(AntEngineListener listener);
    
    void execute(Task task) throws AntException;
    
    void execute(Task root, Task task) throws AntException;
    
    void message(Task task, String message);
    
    Task getNextExecuteTask();
    
    /**
     * Returns a list of all property names that the current task stack is aware
     * of. This is a recursive list of all property names.
     */
    List getPropertyNames();
    
    /**
     * May be called to obtain property values that have been defined. Property
     * values are maintained in a hierarchical manner as each task is executed.
     * When a property is requested, if the current execution level does not
     * contain the property, the execution parent is then queried. This
     * continues until there is no where else to go!
     * <p></p>
     * Maybe this should be a Hashtable implementation and be able to return
     * Object? Is that a little overkill considering these values will usually
     * be Strings? Perhaps someone will have a farfetched idea of storing a
     * Task in a property?
     */
    Object getPropertyValue(String name);
    
    /**
     * Adds the name-value pair to this execution stack property list. If the
     * property is declared in parent tasks, I don't really see a reason for not
     * adding it again to this execution list. This would achieve a nice scoped
     * parameter list that is dictated by nesting levels.
     * <p></p>
     * This is against the current Ant (1.2) specification, but I'm not sure why
     * that restriction was there. It would be simple to implement here if it
     * again required.
     */
    void setPropertyValue(String name, Object value);
    
    /**
     * Removes the given property from the property list. I haven't thought too
     * much about the rules behind this method. My current thinking is that the
     * property is removed no matter what level of the execution stack the
     * property was defined in. I think this should be good in most cases. If it
     * ever surfaces that the property should just be unavailable for this stack
     * level (and other's below it), then the implementation can be modified to
     * keep a list of these "unavailable" properties.
     */
    void removePropertyValue(String name);
    
}
