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
    private Ant ant;
    
    /**
     *
     */
    private PrintStream out;

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

    // -----------------------------------------------------------------
    // PUBLIC ACCESSOR METHODS
    // -----------------------------------------------------------------

    /**
     *
     */
    public void addTarget(Target target) {
        // XXX check out for name, if null, reject!
        targets.put(target.getName(), target);
    }

    /**
     *
     */
    public PrintStream getOutput() {
        // XXX check if null!!!!????
        return out;
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
     *
     */
    public Enumeration getTargets() {
        return targets.elements();
    }
    
    /**
     *
     */
    public String getName() {
        return name;
    }
    
    /**
     *
     */
    public void setAnt(Ant ant) {
        this.ant = ant;
    }
    
    /**
     *
     */
    public void setOutput(PrintStream out) {
        this.out = out;
    }
    
    /**
     *
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     *
     */
    public String toString() {
        return "Project name=" + name;
    }
}