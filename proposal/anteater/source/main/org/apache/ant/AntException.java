// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

package org.apache.ant;

/**
 * Signals a problem while setting up or executing a build.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class AntException extends Exception {

    // -----------------------------------------------------------------
    // PRIVATE MEMBERS
    // -----------------------------------------------------------------    
    
    /**
     * The cause of this exception.
     */
    private Throwable cause;
    
    /**
     * Project within which this exception occured, if applicable.
     */
    private Project project;
    
    /**
     * Target within which this exception occurred, if applicable.
     */
    private Target target;
    
    /**
     * Task within which this exception occurred, if applicable.
     */
    private Task task;

    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------    
    
    /**
     * Constructs a new AntException with no message.
     */
    public AntException() {
        super();
    }
    
    /**
     * Constructs a new AntException with the given message.
     */
    public AntException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs a new AntException with the given message and cause.
     */
    public AntException(String msg, Throwable cause) {
        super(msg);
        this.cause = cause;
    }
    
    /**
     * Constructs a new AntException with the given cause and a 
     * detailed message of (cause==null ? null : cause.toString())
     */
    public AntException(Throwable cause) {
        super(cause==null ? null : cause.toString());
        this.cause = cause;
    }
    
    // -----------------------------------------------------------------
    // PUBLIC METHODS
    // -----------------------------------------------------------------    
    
    /**
     * Returns the cause of this exception.
     */ 
    public Throwable getCause() {
        return cause;
    }
    
    /**
     * Returns the Project within the scope of which this exception occurred,
     * if applicable. Otherwise null.
     */
    public Project getProject() {
        return project;
    }
    
    /**
     * Returns the Target within the scope of which this exception occurred,
     * if applicable. Otherwise null.
     */
    public Target getTarget() {
        return target;
    }
     
    /**
     * Returns the Task wihtin the scope of which this exception occurred,
     * if applicable. Otherwise null.
     */
    public Task getTask() {
        return task;
    }
     
    // -----------------------------------------------------------------
    // PACKAGE METHODS
    // -----------------------------------------------------------------    
    
    /**
     * Sets the project within the scope of which this exception occurred.
     * This method is called by the internal error handling mechanism of
     * Ant before it is propogated out.
     */
    void setProject(Project project) {
        this.project = project;
    }
    
    /**
     * Sets the target within the scope of which this exception occurred.
     * This method is called by the internal error handling mechansim of
     * Ant before it is propogated out.
     */
    void setTarget(Target target) {
        this.target = target;
    }
    
    /**
     * Sets the task within the scope of which this exception occurred.
     * This method is called by the internal error handling mechanism of
     * Ant before it is propogated out.
     */
    void setTask(Task task) {
        this.task = task;
    }
}