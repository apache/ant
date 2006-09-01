/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.tools.ant.util.CollectionUtils;

/**
 * Class to implement a target object with required parameters.
 *
 */
public class Target implements TaskContainer {

    /** Name of this target. */
    private String name;
    /** The "if" condition to test on execution. */
    private String ifCondition = "";
    /** The "unless" condition to test on execution. */
    private String unlessCondition = "";
    /** List of targets this target is dependent on. */
    private List dependencies = null;
    /** Children of this target (tasks and data types). */
    private List children = new ArrayList();
    /** Since Ant 1.6.2 */
    private Location location = Location.UNKNOWN_LOCATION;

    /** Project this target belongs to. */
    private Project project;

    /** Description of this target, if any. */
    private String description = null;

    /** Default constructor. */
    public Target() {
        //empty
    }

    /**
     * Cloning constructor.
     * @param other the Target to clone.
     */
    public Target(Target other) {
        this.name = other.name;
        this.ifCondition = other.ifCondition;
        this.unlessCondition = other.unlessCondition;
        this.dependencies = other.dependencies;
        this.location = other.location;
        this.project = other.project;
        this.description = other.description;
        // The children are added to after this cloning
        this.children = other.children;
    }

    /**
     * Sets the project this target belongs to.
     *
     * @param project The project this target belongs to.
     *                Must not be <code>null</code>.
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Returns the project this target belongs to.
     *
     * @return The project this target belongs to, or <code>null</code> if
     *         the project has not been set yet.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Sets the location of this target's definition.
     *
     * @param location   <code>Location</code>
     * @since 1.6.2
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Get the location of this target's definition.
     *
     * @return <code>Location</code>
     * @since 1.6.2
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the list of targets this target is dependent on.
     * The targets themselves are not resolved at this time.
     *
     * @param depS A comma-separated list of targets this target
     *             depends on. Must not be <code>null</code>.
     */
    public void setDepends(String depS) {
        if (depS.length() > 0) {
            StringTokenizer tok =
                new StringTokenizer(depS, ",", true);
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken().trim();

                // Make sure the dependency is not empty string
                if ("".equals(token) || ",".equals(token)) {
                    throw new BuildException("Syntax Error: depends "
                        + "attribute of target \"" + getName()
                        + "\" has an empty string as dependency.");
                }

                addDependency(token);

                // Make sure that depends attribute does not
                // end in a ,
                if (tok.hasMoreTokens()) {
                    token = tok.nextToken();
                    if (!tok.hasMoreTokens() || !",".equals(token)) {
                        throw new BuildException("Syntax Error: Depend "
                            + "attribute for target \"" + getName()
                            + "\" ends with a , character");
                    }
                }
            }
        }
    }

    /**
     * Sets the name of this target.
     *
     * @param name The name of this target. Should not be <code>null</code>.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this target.
     *
     * @return the name of this target, or <code>null</code> if the
     *         name has not been set yet.
     */
    public String getName() {
        return name;
    }

    /**
     * Adds a task to this target.
     *
     * @param task The task to be added. Must not be <code>null</code>.
     */
    public void addTask(Task task) {
        children.add(task);
    }

    /**
     * Adds the wrapper for a data type element to this target.
     *
     * @param r The wrapper for the data type element to be added.
     *          Must not be <code>null</code>.
     */
    public void addDataType(RuntimeConfigurable r) {
        children.add(r);
    }

    /**
     * Returns the current set of tasks to be executed by this target.
     *
     * @return an array of the tasks currently within this target
     */
    public Task[] getTasks() {
        List tasks = new ArrayList(children.size());
        Iterator it = children.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof Task) {
                tasks.add(o);
            }
        }

        return (Task[]) tasks.toArray(new Task[tasks.size()]);
    }

    /**
     * Adds a dependency to this target.
     *
     * @param dependency The name of a target this target is dependent on.
     *                   Must not be <code>null</code>.
     */
    public void addDependency(String dependency) {
        if (dependencies == null) {
            dependencies = new ArrayList(2);
        }
        dependencies.add(dependency);
    }

    /**
     * Returns an enumeration of the dependencies of this target.
     *
     * @return an enumeration of the dependencies of this target
     */
    public Enumeration getDependencies() {
        return (dependencies != null ? Collections.enumeration(dependencies)
                                     : new CollectionUtils.EmptyEnumeration());
    }

    /**
     * Does this target depend on the named target?
     * @param other the other named target.
     * @return true if the target does depend on the named target
     * @since Ant 1.6
     */
    public boolean dependsOn(String other) {
        Project p = getProject();
        Hashtable t = (p == null) ? null : p.getTargets();
        return (p != null
                && p.topoSort(getName(), t, false).contains(t.get(other)));
    }

    /**
     * Sets the "if" condition to test on execution. This is the
     * name of a property to test for existence - if the property
     * is not set, the task will not execute. The property goes
     * through property substitution once before testing, so if
     * property <code>foo</code> has value <code>bar</code>, setting
     * the "if" condition to <code>${foo}_x</code> will mean that the
     * task will only execute if property <code>bar_x</code> is set.
     *
     * @param property The property condition to test on execution.
     *                 May be <code>null</code>, in which case
     *                 no "if" test is performed.
     */
    public void setIf(String property) {
        ifCondition = (property == null) ? "" : property;
    }

    /**
     * Returns the "if" property condition of this target.
     *
     * @return the "if" property condition or <code>null</code> if no
     *         "if" condition had been defined.
     * @since 1.6.2
     */
    public String getIf() {
        return ("".equals(ifCondition) ? null : ifCondition);
    }

    /**
     * Sets the "unless" condition to test on execution. This is the
     * name of a property to test for existence - if the property
     * is set, the task will not execute. The property goes
     * through property substitution once before testing, so if
     * property <code>foo</code> has value <code>bar</code>, setting
     * the "unless" condition to <code>${foo}_x</code> will mean that the
     * task will only execute if property <code>bar_x</code> isn't set.
     *
     * @param property The property condition to test on execution.
     *                 May be <code>null</code>, in which case
     *                 no "unless" test is performed.
     */
    public void setUnless(String property) {
        unlessCondition = (property == null) ? "" : property;
    }

    /**
     * Returns the "unless" property condition of this target.
     *
     * @return the "unless" property condition or <code>null</code>
     *         if no "unless" condition had been defined.
     * @since 1.6.2
     */
    public String getUnless() {
        return ("".equals(unlessCondition) ? null : unlessCondition);
    }

    /**
     * Sets the description of this target.
     *
     * @param description The description for this target.
     *                    May be <code>null</code>, indicating that no
     *                    description is available.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the description of this target.
     *
     * @return the description of this target, or <code>null</code> if no
     *         description is available.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the name of this target.
     *
     * @return the name of this target, or <code>null</code> if the
     *         name has not been set yet.
     */
    public String toString() {
        return name;
    }

    /**
     * Executes the target if the "if" and "unless" conditions are
     * satisfied. Dependency checking should be done before calling this
     * method, as it does no checking of its own. If either the "if"
     * or "unless" test prevents this target from being executed, a verbose
     * message is logged giving the reason. It is recommended that clients
     * of this class call performTasks rather than this method so that
     * appropriate build events are fired.
     *
     * @exception BuildException if any of the tasks fail or if a data type
     *                           configuration fails.
     *
     * @see #performTasks()
     * @see #setIf(String)
     * @see #setUnless(String)
     */
    public void execute() throws BuildException {
        if (testIfCondition() && testUnlessCondition()) {
            for (int taskPosition = 0;
                 taskPosition < children.size();
                 ++taskPosition) {
                Object o = children.get(taskPosition);
                if (o instanceof Task) {
                    Task task = (Task) o;
                    task.perform();
                } else {
                    RuntimeConfigurable r = (RuntimeConfigurable) o;
                    r.maybeConfigure(project);
                }
            }
        } else if (!testIfCondition()) {
            project.log(this, "Skipped because property '"
                        + project.replaceProperties(ifCondition)
                        + "' not set.", Project.MSG_VERBOSE);
        } else {
            project.log(this, "Skipped because property '"
                        + project.replaceProperties(unlessCondition)
                        + "' set.", Project.MSG_VERBOSE);
        }
    }

    /**
     * Performs the tasks within this target (if the conditions are met),
     * firing target started/target finished messages around a call to
     * execute.
     *
     * @see #execute()
     */
    public final void performTasks() {
        RuntimeException thrown = null;
        project.fireTargetStarted(this);
        try {
            execute();
        } catch (RuntimeException exc) {
            thrown = exc;
            throw exc;
        } finally {
            project.fireTargetFinished(this, thrown);
        }
    }

    /**
     * Replaces all occurrences of the given task in the list
     * of children with the replacement data type wrapper.
     *
     * @param el The task to replace.
     *           Must not be <code>null</code>.
     * @param o  The data type wrapper to replace <code>el</code> with.
     */
    void replaceChild(Task el, RuntimeConfigurable o) {
        int index;
        while ((index = children.indexOf(el)) >= 0) {
            children.set(index, o);
        }
    }

    /**
     * Replaces all occurrences of the given task in the list
     * of children with the replacement task.
     *
     * @param el The task to replace.
     *           Must not be <code>null</code>.
     * @param o  The task to replace <code>el</code> with.
     */
    void replaceChild(Task el, Task o) {
        int index;
        while ((index = children.indexOf(el)) >= 0) {
            children.set(index, o);
        }
    }

    /**
     * Tests whether or not the "if" condition is satisfied.
     *
     * @return whether or not the "if" condition is satisfied. If no
     *         condition (or an empty condition) has been set,
     *         <code>true</code> is returned.
     *
     * @see #setIf(String)
     */
    private boolean testIfCondition() {
        if ("".equals(ifCondition)) {
            return true;
        }

        String test = project.replaceProperties(ifCondition);
        return project.getProperty(test) != null;
    }

    /**
     * Tests whether or not the "unless" condition is satisfied.
     *
     * @return whether or not the "unless" condition is satisfied. If no
     *         condition (or an empty condition) has been set,
     *         <code>true</code> is returned.
     *
     * @see #setUnless(String)
     */
    private boolean testUnlessCondition() {
        if ("".equals(unlessCondition)) {
            return true;
        }
        String test = project.replaceProperties(unlessCondition);
        return project.getProperty(test) == null;
    }
}
