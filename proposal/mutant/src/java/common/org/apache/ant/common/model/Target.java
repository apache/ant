/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.ant.common.model;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ant.common.util.Location;

/**
 *  A Target is a collection of tasks. It may have dependencies on other
 *  targets
 *
 * @author  Conor MacNeill
 * @created  12 January 2002
 */
public class Target extends ModelElement {
    /**  This target's dependencies on other targets, if any */
    private List dependencies = new ArrayList();

    /**  This target's list of tasks */
    private List tasks = new ArrayList();

    /**  The target's name. */
    private String name;

    /**  The Target's description */
    private String description;

    /**  Description of the Field */
    private String ifCondition;

    /**  Description of the Field */
    private String unlessCondition;

    /**
     *  Construct the target, given its name
     *
     * @param  location the location of the element
     * @param  name the target's name.
     */
    public Target(Location location, String name) {
        super(location);
        this.name = name;
    }

    /**
     *  Sets the IfCondition of the Target
     *
     * @param  ifCondition The new IfCondition value
     */
    public void setIfCondition(String ifCondition) {
        this.ifCondition = ifCondition;
    }

    /**
     *  Sets the UnlessCondition of the Target
     *
     * @param  unlessCondition The new UnlessCondition value
     */
    public void setUnlessCondition(String unlessCondition) {
        this.unlessCondition = unlessCondition;
    }

    /**
     *  Sets the Target's description
     *
     * @param  description The new description value
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *  Gets the IfCondition of the Target
     *
     * @return  The IfCondition value
     */
    public String getIfCondition() {
        return ifCondition;
    }

    /**
     *  Gets the UnlessCondition of the Target
     *
     * @return  The UnlessCondition value
     */
    public String getUnlessCondition() {
        return unlessCondition;
    }

    /**
     *  Get this target's name.
     *
     * @return  the target's name.
     */
    public String getName() {
        return name;
    }

    /**
     *  Gets the Target's description
     *
     * @return  The description value
     */
    public String getDescription() {
        return description;
    }


    /**
     *  Get this target's dependencies.
     *
     * @return  an iterator over the target's dependencies.
     */
    public Iterator getDependencies() {
        return dependencies.iterator();
    }

    /**
     *  Get the tasks for this target
     *
     * @return  an iterator over the set of tasks for this target.
     */
    public Iterator getTasks() {
        return tasks.iterator();
    }

    /**
     *  Add a task to this target
     *
     * @param  task the task to be added to the target.
     */
    public void addTask(BuildElement task) {
        tasks.add(task);
    }

    /**
     *  Add a dependency to this target
     *
     * @param  dependency the name of a target upon which this target
     *      depends
     */
    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }

    /**
     *  Validate that this build element is configured correctly
     *
     * @exception  ModelException if the element is invalid
     */
    public void validate() throws ModelException {
        if (name == null) {
            throw new ModelException("Target must have a name",
                getLocation());
        }
    }
}

