// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

package org.apache.ant;

/**
 * Abstract class that lets Ant talk to a front end such as a CLI front end,
 * GUI front end, Servlet front end, or some other front end.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public abstract class AntFrontEnd {

    // -----------------------------------------------------------------
    // CONSTANTS
    // -----------------------------------------------------------------    
    
    /**
     * Indicates that an associated message has a low importance.
     */
    public static final int MSG_LEVEL_LOW = 1;
    
    /**
     * Indicates that an associated message has a medium importance.
     */
    public static final int MSG_LEVEL_MED = 2;
    
    /**
     * Indicates that an associated message has a high importance.
     */
    public static final int MSG_LEVEL_HIGH = 3;
    
    // -----------------------------------------------------------------
    // PUBLIC METHODS
    // -----------------------------------------------------------------    

    /**
     * Send notification to the FrontEnd that execution has moved into
     * the scope of a particular project. The default implementation
     * does nothing. 
     */
    public void notifyProjectStart(Project project) {
    
    }

    /**
     * Send notification to the FrontEnd that execution has moved out
     * of the scope of a particular Project. The default implementation
     * does nothing.
     */
    public void notifyProjectEnd(Project project) {
    
    }
    
    /**
     * Send notification to the FrontEnd that execution has moved into
     * the scope of a particular target. The default implementation does
     * nothing.
     */
    public void notifyTargetStart(Target target) {
    
    }
    
    /**
     * Send notification to the FrontEnd that execution has moved out of
     * the scope of a particular target. The default implementation does
     * nothing.
     */
    public void notifyTargetEnd(Target target) {
    
    }
    
    /**
     * Send notification to the FrontEnd that execution has moved into the
     * scope of a particular task. The default implementation does nothing.
     */
    public void notifyTaskStart(Task task) {
    
    }
    
    /**
     * Send notification to the FrontEnd that execution has moved out of
     * the scope of a particular task. The default implementation does
     * nothing.
     */
    public void notifyTaskEnd(Task task) {
    
    }

    /**
     * Writes a message to the front end with a medium importance.
     */
    public void writeMessage(String message) {
        writeMessage(message, MSG_LEVEL_MED);
    }

    /**
     * Writes a message to the front end.
     */
    public abstract void writeMessage(String message, int level);

}