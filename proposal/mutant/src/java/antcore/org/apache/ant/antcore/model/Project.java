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
package org.apache.ant.antcore.model;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.ant.common.util.Location;
import org.apache.ant.antcore.util.CircularDependencyChecker;
import org.apache.ant.antcore.util.CircularDependencyException;
import org.apache.ant.antcore.util.ConfigException;

/**
 * A project is a collection of targets and global tasks. A project may
 * reference objects in other projects using named references of the form
 * refname:object
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 9 January 2002
 */
public class Project extends ModelElement {
    /**
     * The delimiter used to separate reference names in target names, data
     * values, etc
     */
    public final static String REF_DELIMITER = ":";

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
     * interdependencies which are used to determine which targets need to
     * be executed before a given target.
     */
    private Map targets = new HashMap();

    /**
     * The global tasks for this project. These are the tasks that will get
     * executed whenever an execution context is associated with this
     * project.
     */
    private List tasks = new ArrayList();

    /**
     * The projects referenced into this project. Each referenced project is
     * given a name which is used to identify access to that project's
     * elements.
     */
    private Map referencedProjects = new HashMap();

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
        return (Target)targets.get(targetName);
    }


    /**
     * Get the names of the referenced projects.
     *
     * @return an iterator which returns the name sof the referenced
     *      projects.
     */
    public Iterator getReferencedProjectNames() {
        return referencedProjects.keySet().iterator();
    }


    /**
     * Get a referenced project by name
     *
     * @param alias the name under which the project was referenced.
     * @return the project asscociated with the given reference alias or
     *      null if there is no such project.
     */
    public Project getReferencedProject(String alias) {
        return (Project)referencedProjects.get(alias);
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
     * Get a target by its reference name - references may span multiple
     * references.
     *
     * @param fullTargetName The name of the target relative to this project
     * @return the Target object with the given name
     */
    public Target getRefTarget(String fullTargetName) {
        Project containingProject = getRefProject(fullTargetName);
        if (containingProject == this) {
            return getTarget(fullTargetName);
        }

        int index = fullTargetName.lastIndexOf(REF_DELIMITER);
        String targetName
             = fullTargetName.substring(index + REF_DELIMITER.length());

        return containingProject.getTarget(targetName);
    }

    /**
     * Get the project which directly contains the target specified by its
     * full name.
     *
     * @param fullTargetName the full name of the target for which the
     *      containing project is required.
     * @return The RefProject value
     */
    public Project getRefProject(String fullTargetName) {
        int index = fullTargetName.lastIndexOf(REF_DELIMITER);
        if (index == -1) {
            return this;
        }

        Project currentProject = this;
        String relativeName = fullTargetName.substring(0, index);
        StringTokenizer tokenizer
             = new StringTokenizer(relativeName, REF_DELIMITER);
        while (tokenizer.hasMoreTokens()) {
            String refName = tokenizer.nextToken();
            currentProject = currentProject.getReferencedProject(refName);
            if (currentProject == null) {
                return null;
            }
        }

        return currentProject;
    }

    /**
     * get the list of dependent targets which must be evaluated for the
     * given target.
     *
     * @param fullTargetName the full name (in reference space) of the
     *      target
     * @return the flattened list of targets
     * @exception ConfigException if the given target could not be found
     */
    public List getTargetDependencies(String fullTargetName)
         throws ConfigException {
        try {
            List flattenedList = new ArrayList();
            flattenDependency(flattenedList, fullTargetName);
            flattenedList.add(fullTargetName);
            return flattenedList;
        } catch (ConfigException e) {
            throw new ConfigException(fullTargetName
                 + " does not exist in project");
        }
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
     * Reference a project using the given name.
     *
     * @param referenceName the name under which the project will be
     *      referenced.
     * @param project the referenced project.
     * @throws ModelException if an existing project has already been
     *      referenced with that name.
     */
    public void referenceProject(String referenceName, Project project)
         throws ModelException {
        if (referencedProjects.containsKey(referenceName)) {
            throw new ModelException("A project has already been "
                 + "introduced with name '" + referenceName + "'");
        }
        referencedProjects.put(referenceName, project);
    }

    /**
     * Validate that this build element is configured correctly
     *
     * @param globalName The name of this project in the reference name
     *      space
     * @exception ModelException if the element is invalid
     */
    public void validate(String globalName) throws ModelException {
        if (defaultTarget == null) {
            throw new ModelException("Project must have a default "
                 + "attribute", getLocation());
        }

        Set keys = referencedProjects.keySet();
        for (Iterator i = keys.iterator(); i.hasNext(); ) {
            String refName = (String)i.next();
            Project referencedProject
                 = (Project)referencedProjects.get(refName);
            String refGlobalName = refName;
            if (globalName != null) {
                refGlobalName = globalName + REF_DELIMITER + refName;
            }
            referencedProject.validate(refGlobalName);
        }

        // we now check whether all of dependencies for our targets
        // exist in the model

        // visited contains the targets we have already visited and verified
        Set visited = new HashSet();
        // checker records the targets we are currently visiting
        CircularDependencyChecker checker
             = new CircularDependencyChecker("checking target dependencies");
        // dependency order is purely recorded for debug purposes
        List dependencyOrder = new ArrayList();

        for (Iterator i = getTargets(); i.hasNext(); ) {
            Target target = (Target)i.next();
            target.validate();
            fillinDependencyOrder(globalName, target, dependencyOrder,
                visited, checker);
        }
    }

    /**
     * Determine target dependency order within this porject and verify that
     * references to targets in other projects are valid
     *
     * @param globalName The global name of this project
     * @param target The target being examined
     * @param dependencyOrder The dependency order of targets
     * @param visited Set of targets in this project already visited.
     * @param checker A circular dependency checker
     * @exception ModelException if the dependencies of the project's
     *      targets are not valid.
     */
    public void fillinDependencyOrder(String globalName, Target target,
                                      List dependencyOrder, Set visited,
                                      CircularDependencyChecker checker)
         throws ModelException {
        if (visited.contains(target.getName())) {
            return;
        }

        try {
            String targetName = target.getName();
            String targetGlobalName = targetName;
            if (globalName != null) {
                targetGlobalName = globalName + REF_DELIMITER + targetName;
            }
            checker.visitNode(targetGlobalName);
            for (Iterator i = target.getDependencies(); i.hasNext(); ) {
                String dependency = (String)i.next();
                boolean localTarget = (dependency.indexOf(REF_DELIMITER) == -1);
                Target dependencyTarget
                     = localTarget ? getTarget(dependency)
                     : getRefTarget(dependency);

                if (dependencyTarget == null) {
                    StringBuffer sb = new StringBuffer("Target '");
                    if (globalName != null) {
                        sb.append(globalName + REF_DELIMITER);
                    }
                    sb.append(dependency);
                    sb.append("' does not exist in this project. ");
                    throw new ModelException(new String(sb),
                        target.getLocation());
                }

                if (localTarget) {
                    // need to check the targets we depend on
                    fillinDependencyOrder(globalName, dependencyTarget,
                        dependencyOrder, visited, checker);
                }
            }

            visited.add(targetName);
            checker.leaveNode(targetGlobalName);
            dependencyOrder.add(targetName);
        } catch (CircularDependencyException e) {
            throw new ModelException(e.getMessage(),
                target.getLocation());
        }
    }

    /**
     * Given a fully qualified target name, this method simply returns the
     * fully qualified name of the project
     *
     * @param fullTargetName the full qualified target name
     * @return the full name of the containing project
     */
    private String getFullProjectName(String fullTargetName) {
        int index = fullTargetName.lastIndexOf(REF_DELIMITER);
        if (index == -1) {
            return null;
        }

        return fullTargetName.substring(0, index);
    }

    /**
     * Flatten the dependencies to the given target
     *
     * @param flattenedList the List of targets that must be executed before
     *      the given target
     * @param fullTargetName the fully qualified name of the target
     * @exception ConfigException if the given target does not exist in the
     *      project hierarchy
     */
    private void flattenDependency(List flattenedList, String fullTargetName)
         throws ConfigException {
        if (flattenedList.contains(fullTargetName)) {
            return;
        }
        Project containingProject = getRefProject(fullTargetName);
        String fullProjectName = getFullProjectName(fullTargetName);
        Target target = getRefTarget(fullTargetName);
        if (target == null) {
            throw new ConfigException("Target " + fullTargetName
                 + " does not exist");
        }
        for (Iterator i = target.getDependencies(); i.hasNext(); ) {
            String localDependencyName = (String)i.next();
            String fullDependencyName
                 = fullProjectName == null ? localDependencyName
                 : fullProjectName + REF_DELIMITER + localDependencyName;
            flattenDependency(flattenedList, fullDependencyName);
            flattenedList.add(fullDependencyName);
        }
    }
}

