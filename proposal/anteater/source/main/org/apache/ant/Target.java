// ---------------------------------------------------------------------
// (c)2000 Apache Software Foundation
//
// ---------------------------------------------------------------------

package org.apache.ant;

import java.util.*;

/**
 * In memory container for an Ant target.
 */
public class Target {

    // -----------------------------------------------------------------
    // PRIVATE DATA MEMBERS
    // -----------------------------------------------------------------

    /**
     * String containing the name of the target. This name must be
     * unique withing a project.
     */
    private String name;

    /**
     * Vector containing the names of the targets that this target
     * depends on.
     */
    private Vector dependsList = new Vector();

    /**
     * Vector containing the tasks that are part of this target.
     */
    private Vector tasks = new Vector();

    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------

    /**
     * Constructs a new Target object with the given name.
     */
    public Target(String name) {
        this.name = name;
    }

    // -----------------------------------------------------------------
    // PUBLIC ACCESSOR METHODS
    // -----------------------------------------------------------------
    
    /**
     * Adds a dependancy to this task.
     */
    public void addDependancy(String targetName) {
        dependsList.addElement(targetName);
    }
       
    /**
     *
     */
    public void addTask(Task task) {
        tasks.addElement(task);
    }
    
    /**
     * Returns a String containing the name of this Target.
     */
    public String getName() {
        return name;
    }
    
    /**
     *
     */
    public String toString() {
        return "TARGET: " + name;
    }

    /**
     * Returns a Vector of Tasks contained in this Target. 
     * <p>
     * Please use caution when using this method. I am not happy
     * about exposing this data as something other than a 
     * Collection, but don't want to use 1.1 collections. So, 
     * this method may change in the future. You have been warned.
     */
    public Vector getTasks() {
        return tasks;
    }
}