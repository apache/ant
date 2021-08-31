/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;
import java.util.StringTokenizer;

import org.apache.tools.ant.property.LocalProperties;
import org.apache.tools.ant.taskdefs.condition.And;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.Or;

/**
 * Class to implement a target object with required parameters.
 *
 * <p>If you are creating Targets programmatically, make sure you set
 * the Location to a useful value.  In particular all targets should
 * have different location values.</p>
 */
public class Target implements TaskContainer {

    /** Name of this target. */
    private String name;

    /** The "if" condition to test on execution. */
    private String ifString = "";

    /** The "unless" condition to test on execution. */
    private String unlessString = "";

    private Condition ifCondition;

    private Condition unlessCondition;

    /** List of targets this target is dependent on. */
    private List<String> dependencies = null;

    /** Children of this target (tasks and data types). */
    private List<Object> children = new ArrayList<>();

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
        this.ifString = other.ifString;
        this.unlessString = other.unlessString;
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
        for (String dep : parseDepends(depS, getName(), "depends")) {
            addDependency(dep);
        }
    }

    public static List<String> parseDepends(String depends,
                                                String targetName,
                                                String attributeName) {
        if (depends.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> list = new ArrayList<>();
        StringTokenizer tok = new StringTokenizer(depends, ",", true);
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken().trim();

            // Make sure the dependency is not empty string
            if (token.isEmpty() || ",".equals(token)) {
                throw new BuildException("Syntax Error: "
                                         + attributeName
                                         + " attribute of target \""
                                         + targetName
                                         + "\" contains an empty string.");
            }

            list.add(token);

            // Make sure that depends attribute does not
            // end in a ,
            if (tok.hasMoreTokens()) {
                token = tok.nextToken();
                if (!tok.hasMoreTokens() || !",".equals(token)) {
                    throw new BuildException("Syntax Error: "
                                             + attributeName
                                             + " attribute for target \""
                                             + targetName
                                             + "\" ends with a \",\" "
                                             + "character");
                }
            }
        }
        return list;
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
        List<Task> tasks = new ArrayList<>(children.size());
        for (Object o : children) {
            if (o instanceof Task) {
                tasks.add((Task) o);
            }
        }
        return tasks.toArray(new Task[0]);
    }

    /**
     * Adds a dependency to this target.
     *
     * @param dependency The name of a target this target is dependent on.
     *                   Must not be <code>null</code>.
     */
    public void addDependency(String dependency) {
        if (dependencies == null) {
            dependencies = new ArrayList<>(2);
        }
        dependencies.add(dependency);
    }

    /**
     * Returns an enumeration of the dependencies of this target.
     *
     * @return an enumeration of the dependencies of this target (enumeration of String)
     */
    public Enumeration<String> getDependencies() {
        return dependencies == null ? Collections.emptyEnumeration()
            : Collections.enumeration(dependencies);
    }

    /**
     * Does this target depend on the named target?
     * @param other the other named target.
     * @return true if the target does depend on the named target
     * @since Ant 1.6
     */
    public boolean dependsOn(String other) {
        Project p = getProject();
        Hashtable<String, Target> t = p == null ? null : p.getTargets();
        return p != null && p.topoSort(getName(), t, false).contains(t.get(other));
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
        ifString = property == null ? "" : property;
        setIf(() -> {
            PropertyHelper propertyHelper =
                PropertyHelper.getPropertyHelper(getProject());
            Object o = propertyHelper.parseProperties(ifString);
            return propertyHelper.testIfCondition(o);
        });
    }

    /**
     * Returns the "if" property condition of this target.
     *
     * @return the "if" property condition or <code>null</code> if no
     *         "if" condition had been defined.
     * @since 1.6.2
     */
    public String getIf() {
        return ifString.isEmpty() ? null : ifString;
    }

    /**
     * Same as {@link #setIf(String)} but requires a {@link Condition} instance
     *
     * @param condition Condition
     * @since 1.9
     */
    public void setIf(Condition condition) {
        if (ifCondition == null) {
            ifCondition = condition;
        } else {
            And andCondition = new And();
            andCondition.setProject(getProject());
            andCondition.setLocation(getLocation());
            andCondition.add(ifCondition);
            andCondition.add(condition);
            ifCondition = andCondition;
        }
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
        unlessString = property == null ? "" : property;
        setUnless(() -> {
            PropertyHelper propertyHelper =
                PropertyHelper.getPropertyHelper(getProject());
            Object o = propertyHelper.parseProperties(unlessString);
            return !propertyHelper.testUnlessCondition(o);
        });
    }

    /**
     * Returns the "unless" property condition of this target.
     *
     * @return the "unless" property condition or <code>null</code>
     *         if no "unless" condition had been defined.
     * @since 1.6.2
     */
    public String getUnless() {
        return unlessString.isEmpty() ? null : unlessString;
    }

    /**
     * Same as {@link #setUnless(String)} but requires a {@link Condition} instance
     *
     * @param condition Condition
     * @since 1.9
     */
    public void setUnless(Condition condition) {
        if (unlessCondition == null) {
            unlessCondition = condition;
        } else {
            Or orCondition = new Or();
            orCondition.setProject(getProject());
            orCondition.setLocation(getLocation());
            orCondition.add(unlessCondition);
            orCondition.add(condition);
            unlessCondition = orCondition;
        }
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
    @Override
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
        if (ifCondition != null && !ifCondition.eval()) {
            project.log(this, "Skipped because property '" + project.replaceProperties(ifString)
                    + "' not set.", Project.MSG_VERBOSE);
            return;
        }
        if (unlessCondition != null && unlessCondition.eval()) {
            project.log(this, "Skipped because property '"
                    + project.replaceProperties(unlessString) + "' set.", Project.MSG_VERBOSE);
            return;
        }
        LocalProperties localProperties = LocalProperties.get(getProject());
        localProperties.enterScope();
        try {
            // use index-based approach to avoid ConcurrentModificationExceptions;
            // also account for growing target children
            // do not optimize this loop by replacing children.size() by a variable
            // as children can be added dynamically as in RhinoScriptTest where a target is adding work for itself
            for (int i = 0; i < children.size(); i++) {
                Object o = children.get(i);
                if (o instanceof Task) {
                    Task task = (Task) o;
                    task.perform();
                } else {
                    ((RuntimeConfigurable) o).maybeConfigure(project);
                }
            }
        } finally {
            localProperties.exitScope();
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
}
