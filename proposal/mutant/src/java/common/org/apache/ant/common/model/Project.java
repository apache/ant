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
package org.apache.ant.common.model;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ant.common.util.CircularDependencyChecker;
import org.apache.ant.common.util.CircularDependencyException;
import org.apache.ant.common.util.Location;

/**
 * A project is a collection of targets and global tasks. A project may
 * reference objects in other projects using named references of the form
 * refname:object
 *
 * @author Conor MacNeill
 * @created 9 January 2002
 */
public class Project extends ModelElement {
    /**
     * The delimiter used to separate reference names in target names, data
     * values, etc
     */
    public static final String REF_DELIMITER = ":";

    /** The default target in this project. */
    private String defaultTarget = null;

    /**
     * The base URL of this project. Relative locations are relative to this
     * base.
     */
    private String base;

    /**
     * The name of this project when referenced by a script within this
     * project.
     */
    private String name;

    /**
     * These are the targets which belong to the project. They will have
     * interdependencies which are used to determine which targets need to be
     * executed before a given target.
     */
    private Map targets = new HashMap();

    /**
     * The global tasks for this project. These are the tasks that will get
     * executed whenever an execution context is associated with this project.
     */
    private List tasks = new ArrayList();

    /** The URL where the project is defined. */
    private URL sourceURL;


    /**
     * Create a Project
     *
     * @param sourceURL the URL where the project is defined.
     * @param location the location of this element within the source.
     */
    public Project(URL sourceURL, Location location) {
        super(location);
        this.sourceURL = sourceURL;
    }


    /**
     * Set the defautl target of this project.
     *
     * @param defaultTarget the name of the defaultTarget of this project.
     */
    public void setDefaultTarget(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }


    /**
     * Set the base URL for this project.
     *
     * @param base the baseURL for this project.
     */
    public void setBase(String base) {
        this.base = base;
    }


    /**
     * Set the name of this project.
     *
     * @param name the name for this project.
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Get the URL where this project is defined
     *
     * @return the project source URL
     */
    public URL getSourceURL() {
        return sourceURL;
    }


    /**
     * Get the Project's default Target, if any
     *
     * @return the project's defautl target or null if there is no default.
     */
    public String getDefaultTarget() {
        return defaultTarget;
    }


    /**
     * Get the base URL for this project.
     *
     * @return the baseURL for this project as a string.
     */
    public String getBase() {
        return base;
    }


    /**
     * Get the name of the project element
     *
     * @return the project's name
     */
    public String getName() {
        return name;
    }


    /**
     * Get the targets in this project.
     *
     * @return an iterator returning Target objects.
     */
    public Iterator getTargets() {
        return targets.values().iterator();
    }


    /**
     * Get the target with the given name
     *
     * @param targetName the name of the desired target.
     * @return the target with the given name or null if there is no such
     *      target.
     */
    public Target getTarget(String targetName) {
        return (Target) targets.get(targetName);
    }


    /**
     * Get the initialisation tasks for this project
     *
     * @return an iterator over the set of tasks for this project.
     */
    public Iterator getTasks() {
        return tasks.iterator();
    }

    /**
     * Add a target to the project.
     *
     * @param target the Target to be added
     * @throws ModelException if a target with the same name already exists.
     */
    public void addTarget(Target target) throws ModelException {
        if (targets.containsKey(target.getName())) {
            throw new ModelException("A target with name '"
                 + target.getName() +
                "' has already been defined in this project",
                target.getLocation());
        }
        targets.put(target.getName(), target);
    }


    /**
     * Add a task to the list of global tasks for this project.
     *
     * @param task a task to be executed when an execution context is
     *      associated with the Project (a non-target task)
     */
    public void addTask(BuildElement task) {
        tasks.add(task);
    }

    /**
     * Validate this project
     *
     * @exception ModelException if the project is not valid
     */
    public void validate() throws ModelException {
        // check whether all of dependencies for our targets
        // exist in the model

        // visited contains the targets we have already visited and verified
        Set visited = new HashSet();
        // checker records the targets we are currently visiting
        CircularDependencyChecker checker
             = new CircularDependencyChecker("checking target dependencies");
        // dependency order is purely recorded for debug purposes
        List dependencyOrder = new ArrayList();

        for (Iterator i = getTargets(); i.hasNext();) {
            Target target = (Target) i.next();

            target.validate();
            fillinDependencyOrder(target, dependencyOrder,
                visited, checker);
        }
    }


    /**
     * Determine target dependency order within this project.
     *
     * @param target The target being examined
     * @param dependencyOrder The dependency order of targets
     * @param visited Set of targets in this project already visited.
     * @param checker A circular dependency checker
     * @exception ModelException if the dependencies of the project's targets
     *      are not valid.
     */
    public void fillinDependencyOrder(Target target,
                                      List dependencyOrder, Set visited,
                                      CircularDependencyChecker checker)
         throws ModelException {
        if (visited.contains(target.getName())) {
            return;
        }

        try {
            String targetName = target.getName();
            checker.visitNode(targetName);

            for (Iterator i = target.getDependencies(); i.hasNext();) {
                String dependency = (String) i.next();
                boolean localTarget = (dependency.indexOf(REF_DELIMITER) == -1);
                if (localTarget) {
                    Target dependencyTarget = getTarget(dependency);

                    if (dependencyTarget == null) {
                        StringBuffer sb = new StringBuffer("Target '");

                        sb.append(dependency);
                        sb.append("' does not exist in this project. ");
                        throw new ModelException(new String(sb),
                            target.getLocation());
                    }

                    // need to check the targets we depend on
                    fillinDependencyOrder(dependencyTarget,
                        dependencyOrder, visited, checker);
                }
            }

            visited.add(targetName);
            checker.leaveNode(targetName);
            dependencyOrder.add(targetName);
        } catch (CircularDependencyException e) {
            throw new ModelException(e.getMessage(),
                target.getLocation());
        }
    }

}

