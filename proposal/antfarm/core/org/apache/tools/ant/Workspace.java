/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

import java.util.*;

/**
 *  The main class in the Ant class hierarchy. A workspace contains
 *  multiple projects, which in turn contain multiple targets, which
 *  in turn contain multiple task proxies. The workspace also handles
 *  the sorting and execution of targets during a build.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class Workspace {
    public static final char SCOPE_SEPARATOR = ':';

    private Importer importer;
    private Map projects;
    private Map tasks;
    private List listeners;

    private Task currentTask = null;

    /**
     *  Constructs new Ant workspace with no projects. The only
     *  task that will be registered is the "load" task.
     *
     *  The importer is used to handle the actual reading of build files.
     *  In theory, different importers could be used to read project info from
     *  DOM trees, serialized objects, databases, etc.
     */
    public Workspace(Importer importer) {
        this.importer = importer;
        this.projects = new HashMap();
        this.tasks = new HashMap();
        this.listeners = new ArrayList();

        registerTask("load", Load.class);
    }

    /**
     *  Assigns a task class to a name.
     */
    public void registerTask(String name, Class type) {
        tasks.put(name, type);
    }

    /**
     *  Returns the class for a task with the specified name.
     */
    public Class getTaskClass(String name) throws BuildException {
        Class type = (Class) tasks.get(name);
        if (type == null) {
            throw new BuildException("No task named \"" + name + "\" has been loaded");
        }
        return type;
    }

    /**
     *  Creates a project with the specified name. The project initially
     *  contains no targets.
     */
    public Project createProject(String name) {
        Project project = new Project(this, name);
        projects.put(name, project);
        return project;
    }

    /**
     *  Returns the project with the specified name, or throws
     *  an exception if no project exists with that name.
     */
    public Project getProject(String name) throws BuildException {
        Project project = (Project) projects.get(name);
        if (project == null) {
            throw new BuildException("Project \"" + name + "\" not found");
        }
        return project;
    }

    /**
     *  Builds all of the targets in the list. Target names must
     *  be of the form projectname:targetname.
     */
    public boolean build(List fullNames) throws BuildException {

        // This lets the tasks intercept System.exit() calls
        SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager(new AntSecurityManager());

        fireBuildStarted();

        try {
            // Parse the project files...
            importTargets(fullNames);

            // ...figure out the build order...
            List toDoList = sortTargets(fullNames);

            // ...and build the targets
            Iterator itr = toDoList.iterator();
            while (itr.hasNext()) {
                Target target = (Target) itr.next();
                buildTarget(target);
            }
            fireBuildFinished(null);
            return true;
        }
        catch(BuildException exc) {
            fireBuildFinished(exc);
            return false;
        }
        finally {
            System.setSecurityManager(sm);
        }
    }

    /**
     *  Adds a listener to the workspace.
     */
    public void addBuildListener(BuildListener listener) {
        listeners.add(listener);
    }

    /**
     *  Removes a listener to the workspace.
     */
    public void removeBuildListener(BuildListener listener) {
        listeners.remove(listener);
    }

    /**
     *  Fires a messageLogged event with DEBUG priority
     */
    public void debug(String message) {
        fireMessageLogged(message, BuildEvent.DEBUG);
    }

    /**
     *  Fires a messageLogged event with INFO priority
     */
    public void info(String message) {
        fireMessageLogged(message, BuildEvent.INFO);
    }

    /**
     *  Fires a messageLogged event with WARN priority
     */
    public void warn(String message) {
        fireMessageLogged(message, BuildEvent.WARN);
    }

    /**
     *  Fires a messageLogged event with ERROR priority
     */
    public void error(String message) {
        fireMessageLogged(message, BuildEvent.ERROR);
    }

    /**
     *  Imports into the workspace all of the projects required to
     *  build a set of targets.
     */
    private void importTargets(List fullNames) throws BuildException {
        Iterator itr = fullNames.iterator();
        while (itr.hasNext()) {
            String fullName = (String) itr.next();
            String projectName = getProjectName(fullName);
            importProject(projectName);
        }
    }

    /**
     *  Imports the project into the workspace, as well as any others
     *  that the project depends on.
     */
    public Project importProject(String projectName) throws BuildException {
        Project project = (Project) projects.get(projectName);

        // Don't parse a project file more than once
        if (project == null) {

            // Parse the project file
            project = createProject(projectName);

            fireImportStarted(project);
            try {
                importer.importProject(project);
                fireImportFinished(project, null);
            }
            catch(BuildException exc) {
                fireImportFinished(project, exc);
                throw exc;
            }

            // Parse any imported projects as well
            Iterator itr = project.getImports().iterator();
            while (itr.hasNext()) {
                Import imp = (Import) itr.next();
                importProject(imp.getName());
            }
        }

        return project;
    }



    /**
     *  Builds a specific target. This assumes that the targets it depends
     *  on have already been built.
     */
    private void buildTarget(Target target) throws BuildException {
        fireTargetStarted(target);

        try {
            List tasks = target.getTasks();
            Iterator itr = tasks.iterator();
            while (itr.hasNext()) {
                TaskProxy proxy = (TaskProxy) itr.next();
                executeTask(target, proxy);
            }

            fireTargetFinished(target, null);
        }
        catch(BuildException exc) {
            fireTargetFinished(target, null);
            throw exc;
        }
    }

    /**
     *  Instantiates the task from the proxy and executes.
     */
    private void executeTask(Target target, TaskProxy proxy) throws BuildException {
        Task task = proxy.createTask();
        task.setWorkspace(this);
        task.setProject(target.getProject());
        task.setTarget(target);

        fireTaskStarted(task);
        currentTask = task;
        try {
            task.execute();

            fireTaskFinished(task, null);
        }
        catch(BuildException exc) {
            exc.setLocation(proxy.getLocation());
            fireTaskFinished(task, exc);
            throw exc;
        }
        finally {
            currentTask = null;
        }
    }

    /**
     *  Does a topological sort on a list of target names. Returns
     *  a list of Target objects in the order to be executed.
     */
    private List sortTargets(List fullNames) throws BuildException {
        List results = new ArrayList();
        sortTargets(results, new Stack(), fullNames);
        return results;
    }

    private void sortTargets(List results, Stack visited, List fullNames) throws BuildException {
        Iterator itr = fullNames.iterator();
        while (itr.hasNext()) {
            String fullName = (String) itr.next();

            // Check for cycles
            if (visited.contains(fullName)) {
                throwCyclicDependency(visited, fullName);
            }

            // Check if we're already added this target to the list
            Target target = getTarget(fullName);
            if (results.contains(target)) {
                continue;
            }

            visited.push(fullName);
            sortTargets(results, visited, target.getDepends());
            results.add(target);
            visited.pop();
        }
    }

    /**
     *  Creates and throws an exception indicating a cyclic dependency.
     */
    private void throwCyclicDependency(Stack visited, String fullName) throws BuildException {
        StringBuffer msg = new StringBuffer("Cyclic dependency: ");
        for (int i = 0; i < visited.size(); i++) {
            msg.append((String)visited.get(i));
            msg.append(" -> ");
        }
        msg.append(fullName);
        throw new BuildException(msg.toString());
    }

    /**
     *  Parses the full target name into is project and target components,
     *  then locates the Target object.
     */
    private Target getTarget(String fullName) throws BuildException {
        String projectName = getProjectName(fullName);
        String targetName = getTargetName(fullName);

        Project project = (Project) projects.get(projectName);
        if (project == null) {
            throw new BuildException("Project \"" + projectName + "\" not found");
        }

        Target target = project.getTarget(targetName);
        if (target == null) {
            throw new BuildException("Target \"" + fullName + "\" not found");
        }

        return target;
    }

    /**
     *  Returns the project portion of a full target name.
     */
    public static String getProjectName(String fullName) throws BuildException {
        int pos = fullName.indexOf(SCOPE_SEPARATOR);
        if (pos == -1 || pos == 0) {
            throw new BuildException("\"" + fullName + "\" is not a valid target name");
        }

        return fullName.substring(0, pos);
    }

    /**
     *  Returns the target portion of a full target name.
     */
    public static String getTargetName(String fullName) throws BuildException {
        int pos = fullName.indexOf(SCOPE_SEPARATOR);
        if (pos == -1 || pos == 0) {
            throw new BuildException("\"" + fullName + "\" is not a valid target name");
        }

        return fullName.substring(pos + 1);
    }

    private void fireMessageLogged(String message, int priority) {
        BuildEvent event;
        if (currentTask == null) {
            event = new BuildEvent(this);
        }
        else {
            event = new BuildEvent(currentTask);
        }
        event.setMessage(message, priority);

        Iterator itr = listeners.iterator();
        while (itr.hasNext()) {
            BuildListener listener = (BuildListener) itr.next();
            listener.messageLogged(event);
        }
    }

    private void fireBuildStarted() {
        Iterator itr = listeners.iterator();
        while (itr.hasNext()) {
            BuildListener listener = (BuildListener) itr.next();
            BuildEvent event = new BuildEvent(this);
            listener.buildStarted(event);
        }
    }

    private void fireBuildFinished(BuildException exc) {
        BuildEvent event = new BuildEvent(this);
        event.setException(exc);

        Iterator itr = listeners.iterator();
        while (itr.hasNext()) {
            BuildListener listener = (BuildListener) itr.next();
            listener.buildFinished(event);
        }
    }

    private void fireImportStarted(Project project) {
        BuildEvent event = new BuildEvent(project);

        Iterator itr = listeners.iterator();
        while (itr.hasNext()) {
            BuildListener listener = (BuildListener) itr.next();
            listener.importStarted(event);
        }
    }

    private void fireImportFinished(Project project, BuildException exc) {
        BuildEvent event = new BuildEvent(project);
        event.setException(exc);

        Iterator itr = listeners.iterator();
        while (itr.hasNext()) {
            BuildListener listener = (BuildListener) itr.next();
            listener.importFinished(event);
        }
    }

    private void fireTargetStarted(Target target) {
        BuildEvent event = new BuildEvent(target);

        Iterator itr = listeners.iterator();
        while (itr.hasNext()) {
            BuildListener listener = (BuildListener) itr.next();
            listener.targetStarted(event);
        }
    }

    private void fireTargetFinished(Target target, BuildException exc) {
        BuildEvent event = new BuildEvent(target);
        event.setException(exc);

        Iterator itr = listeners.iterator();
        while (itr.hasNext()) {
            BuildListener listener = (BuildListener) itr.next();
            listener.targetFinished(event);
        }
    }

    private void fireTaskStarted(Task task) {
        BuildEvent event = new BuildEvent(task);

        Iterator itr = listeners.iterator();
        while (itr.hasNext()) {
            BuildListener listener = (BuildListener) itr.next();
            listener.taskStarted(event);
        }
    }

    private void fireTaskFinished(Task task, BuildException exc) {
        BuildEvent event = new BuildEvent(task);
        event.setException(exc);

        Iterator itr = listeners.iterator();
        while (itr.hasNext()) {
            BuildListener listener = (BuildListener) itr.next();
            listener.taskFinished(event);
        }
    }
}