// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

package org.apache.ant.cli;

import java.io.*;
import java.util.*;
import org.apache.ant.*;

/**
 * Front end for the Command Line Interface that gets passed to Ant so that
 * it can communicate information to the CLI.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class CLIFrontEnd extends AntFrontEnd {

    // -----------------------------------------------------------------
    // PRIVATE MEMBERS
    // -----------------------------------------------------------------

    /**
     *
     */
    private Ant ant;

    /**
     *
     */
    private int msgLevel = MSG_LEVEL_MED;
    
    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------- 

    /**
     *
     */
    public CLIFrontEnd() {
        ant = new Ant(this);
    }

    // -----------------------------------------------------------------
    // PUBLIC METHODS
    // -----------------------------------------------------------------    

    /**
     * Send notification to the FrontEnd that execution has moved into
     * the scope of a particular project. The default implementation
     * does nothing. 
     */
    public void notifyProjectStart(Project project) {
        writeMessage("Project Start: " + project.getName(), MSG_LEVEL_LOW);
    }

    /**
     * Send notification to the FrontEnd that execution has moved out
     * of the scope of a particular Project. The default implementation
     * does nothing.
     */
    public void notifyProjectEnd(Project project) {
        writeMessage("Project End: " + project.getName(), MSG_LEVEL_LOW);
    }
    
    /**
     * Send notification to the FrontEnd that execution has moved into
     * the scope of a particular target. The default implementation does
     * nothing.
     */
    public void notifyTargetStart(Target target) {
        writeMessage("Target Start: " + target.getName(), MSG_LEVEL_LOW);
    }
    
    /**
     * Send notification to the FrontEnd that execution has moved out of
     * the scope of a particular target. The default implementation does
     * nothing.
     */
    public void notifyTargetEnd(Target target) {
        writeMessage("Target End: " + target.getName(), MSG_LEVEL_LOW); 
    }
    
    /**
     * Send notification to the FrontEnd that execution has moved into the
     * scope of a particular task. The default implementation does nothing.
     */
    public void notifyTaskStart(Task task) {
        writeMessage("Task Start: " + task.getType(), MSG_LEVEL_LOW);
    }
    
    /**
     * Send notification to the FrontEnd that execution has moved out of
     * the scope of a particular task. The default implementation does
     * nothing.
     */
    public void notifyTaskEnd(Task task) {
        writeMessage("Task End: " + task.getType(), MSG_LEVEL_LOW);
    }

    /**
     * Prints help to System.out
     */  
    private void printHelp() {
        String ls = System.getProperty("line.separator");
        String msg = "Usage: ant [args] [target]" + ls +
                     "    Arguments can be any of the following:" + ls +
                     "        -help" + ls +
                     "        -taskpath [path]" + ls +
                     "        -buildfile [file]" +ls +
                     "        -verbose" + ls +
                     "        -quiet"   + ls + ls +
                     "    Note that if no buildfile argument is given, Ant will"+ls+
                     "    try to find one in the current directory. If there are"+ls+
                     "    two or more buildfiles in the current directory, it" +ls+
                     "    will bail.";
        writeMessage(msg);
    }

    /**
     *
     */
    public void run(String[] args) {
        String target = "";
        writeMessage("Ant(Eater) -- Proposed Ant 2.0");
        
        // flip through args and set things accordingly
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            // scan through -- all -aaa args come first.
            if (arg.startsWith("-")) {
                if (arg.equals("-help")) {
                    printHelp();
                    return; 
                } else if (arg.equals("-taskpath")) {
                    // XXX
                    // need to seperate on pathsep, but not today
                    ant.addTaskPathNode(new File(args[++i]));
                } else if (arg.equals("-buildfile")) {
                    // XXX
                    // need to check file to make sure it exists!
                    try {
                        ant.setBuildfile(new File(args[++i]));
                    } catch (AntException ae) {
                        writeMessage("ICK: " + ae);
                        writeMessage(ae.getMessage());
                        return;
                    }
                }
            } else {
                target = arg;
            }
        }
        
        // XXX do something if we dont' have a buildfile set!
        
        // XXX really should check to make sure that the target is set to something

        // set our listeners on the project
        
        Project project = ant.getProject();
        project.setOutput(System.out);
        
        try {
            ant.buildTarget(target);
        } catch (AntException ae) {
            writeMessage("Problem while building: " + ae);
            writeMessage(ae.getMessage());
        }        
    }

    /**
     * Writes a message to the front end.
     */
    public void writeMessage(String message, int level) {
        if (level >= msgLevel) {
            System.out.println(message);
        }
    }


}