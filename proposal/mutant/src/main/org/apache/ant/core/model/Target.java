/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.ant.core.model;

import java.util.*;
import org.apache.ant.core.support.*;

/**
 * A Target is a collection of tasks. It may have
 * dependencies on other targets
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */ 
public class Target extends BuildElement {
    private List dependencies = new ArrayList();
    
    /**
     * This target's list of tasks
     */
    private List tasks = new ArrayList();
    
    /**
     * The target's name.
     */
    private String name;
    
    /**
     * Construct the target, given its name
     *
     * @param location the location of the element
     * @param name the target's name.
     */
    public Target(Location location, String name) {
        super(location);
        this.name = name;
    }
    
    /**
     * Get this target's name.
     *
     * @return the target's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Add a task to this target
     *
     * @param task the task to be added to the target.
     */
    public void addTask(Task task) {
        tasks.add(task);
    }
    
    /**
     * Add a dependency to this target
     *
     * @param dependency the name of a target upon which this target 
     *                   depends
     */
    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }

    /**
     * Get this target's dependencies.
     *   
     * @return an iterator over the target's dependencies.
     */
    public Iterator getDependencies() {
        return dependencies.iterator();
    }
    
    /**
     * Get the tasks for this target
     *
     * @return an iterator over the set of tasks for this target.
     */
    public Iterator getTasks() {
        return tasks.iterator();
    }
    
}

