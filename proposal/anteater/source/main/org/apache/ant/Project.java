// ---------------------------------------------------------------------
// (c)2000 Apache Software Foundation
//
// ---------------------------------------------------------------------

package org.apache.ant;

import java.io.*;
import java.util.*;

/**
 * In memory container for an Ant project.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class Project {

    // -----------------------------------------------------------------
    // PRIVATE DATA MEMBERS
    // -----------------------------------------------------------------

    /**
     *
     */
    //private Ant ant;
    
    /**
     * Base directory of this project. Usually this value is the directory
     * where the project file was found, but can be different.
     */
    private File baseDir;
    
    /**
     *
     */
    private String defaultTargetName;
    
    /**
     * Short description of the project.
     */
    private String description;

    /**
     * Front end that this project communicates to.
     */
    private FrontEnd frontEnd;

    /**
     * Properties of this project.
     */
    private Properties properties = new Properties();

    /**
     * Parent project to this project, if one exists.
     */
    private Project parentProject = null;

    /**
     *
     */
    private String name;

    /**
     * Hashtable containing all of the targets that are part of this
     * project. Targets are stored in this hashtable using the name
     * of the target as the key and the Target object for the target
     * as the value.
     */
    private Hashtable targets = new Hashtable();
    
    /**
     * TaskManager for this project.
     */
    private TaskManager taskManager = new TaskManager(this);

    // -----------------------------------------------------------------
    // PUBLIC  METHODS
    // -----------------------------------------------------------------

    /**
     * Adds a target to this project.
     */
    public void addTarget(Target target) {
        // XXX check out for name, if null, reject!
        targets.put(target.getName(), target);
    }

    /**
     * Returns the base directory of this project.
     */
    public File getBaseDir() {
        return baseDir;
    }

    /**
     * Returns the default target for this project, if there is one. Otherwise
     * it returns null.
     */
    public String getDefaultTargetName() {
        return defaultTargetName;
    }

    /**
     * Returns a short description of this project, if any. If not, returns 
     * null.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the front end that is running this project.
     */
    public FrontEnd getFrontEnd() {
        return frontEnd;
    }

    /**
     * Returns the parent Project object to this Project if a parent
     * project exists. If there is not a parent Project object, null
     * is returned.
     */
    public Project getParent() {
        return parentProject;
    }

    /**
     * Returns the target identified with the given name. If no target
     * is known by the given name, then null is returned.
     */
    public Target getTarget(String name) {
        return (Target)targets.get(name);
    }
    
    /**
     * Gets an exumeration of all the targets that are part of this project.
     */
    public Enumeration getTargets() {
        return targets.elements();
    }
    
    /**
     * Gets the name of this project.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the value of a property. Returns null if the property does
     * not exist.
     */
    public String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }
    
    /**
     *
     */
    //public void setAnt(Ant ant) {
    //    this.ant = ant;
    //}
    
    /**
     * Sets the base dir for this project.
     */
    public void setBaseDir(File dir) {
        // XXX should check this to make sure it's a dir!
        baseDir = dir;
    }
    
    /**
     * Sets the default target for this project.
     */
    public void setDefaultTargetName(String targetName) {
        defaultTargetName = targetName;
    }
    
    /**
     * Sets the description for this project.
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Sets the front end for this project.
     */
    public void setFrontEnd(FrontEnd frontEnd) {
        this.frontEnd = frontEnd;
    }
    
    /**
     * Sets the name of this project.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets a property on this project. If the property is already
     * set, this method will override it.
     */
    public void setProperty(String propertyName, String propertyValue) {
        properties.put(propertyName, propertyValue);
    }
    
    /**
     * Starts a build of this project using the default target if one
     * is set.
     */
    public void startBuild() throws AntException {
        // XXX need to do something if the default target isn't set..
        // maybe look for target name 'default', then bail?
        startBuild(defaultTargetName);
    }
    
    /**
     * Starts a build of this project with the entry point at the given
     * target.
     */
    public void startBuild(String targetName) throws AntException {
        
        // notify FrontEnd that we are starting a build on a project
        frontEnd.notifyProjectStart(this);
        
        Target target = getTarget(targetName);
        //TaskManager taskManager = ant.getTaskManager();
        
        frontEnd.notifyTargetStart(target);
        
        // XXX don't forget to execute dependancies first!
        
        Enumeration enum = target.getTasks().elements();
        while (enum.hasMoreElements()) {
            Task task = (Task)enum.nextElement();
            frontEnd.notifyTaskStart(task);
            try {
                AbstractTask aTask = taskManager.getTaskInstance(task.getType());
                aTask.setProject(this);
                aTask.setAttributes(task.getAttributes());
                boolean b = aTask.execute();
                if (!b) {
                    String msg = "Task " + task.getType() + " failed";
                    AntException ae = new AntException(msg);
                    throw ae;
                }
            } catch (Exception e) {
                AntException ae;
                if (!(e instanceof AntException)) {
                    ae = new AntException(e);
                } else {
                    ae = (AntException)e;
                }
                ae.setProject(this);
                ae.setTarget(target);
                ae.setTask(task);
                throw ae;
            }
            frontEnd.notifyTaskEnd(task);
        }
        
        // notify frontEnd that we are done
        frontEnd.notifyTargetEnd(target);
        frontEnd.notifyProjectEnd(this);
    }
    
    /**
     * Givens a string representation of this object. Useful for debugging.
     */
    public String toString() {
        return "Project name=" + name;
    }
}