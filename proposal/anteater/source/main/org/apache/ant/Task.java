// ---------------------------------------------------------------------
// (c)2000 Apache Software Foundation
//
// ---------------------------------------------------------------------

package org.apache.ant;

import java.util.*;

/**
 * In memory container for an Ant target.
 *
 * XXX need a way to query which attributes are valid for this particular
 * task type... Like into Ant object to do this?
 */
public class Task {

    // -----------------------------------------------------------------
    // PRIVATE DATA MEMBERS
    // -----------------------------------------------------------------
    
    /**
     *
     */
    private Hashtable attributes = new Hashtable();
    
    /**
     * String containing the type of the task.
     */
    private String type;

    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------

    /**
     * Constructs a new Target object with the given name.
     */
    public Task(String type) {
        this.type = type;
    }

    // -----------------------------------------------------------------
    // PUBLIC ACCESSOR METHODS
    // -----------------------------------------------------------------
    
    /**
     *
     */
    public void addAttribute(String name, String value) {
        attributes.put(name, value);
    }
    
    public String getAttribute(String name) {
        return (String)attributes.get(name);
    }
    
    /**
     *
     */
    public Hashtable getAttributes() {
        return attributes;
    }
    
    /**
     *
     */
    public Enumeration getAttributeNames() {
        return attributes.keys();
    }
     
    /**
     * Returns a String containing the name of this Target.
     */
    public String getType() {
        return type;
    }
    
    /**
     *
     */
    public String toString() {
        return "TASK: " + type;
    }

}