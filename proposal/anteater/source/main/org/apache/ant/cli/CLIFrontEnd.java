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
    private String[] args;

    /**
     * ProjectBuilder that is associated with this frontEnd. 
     */
    private ProjectBuilder projectBuilder;

    /**
     *
     */
    private int msgLevelFilter = MSG_LEVEL_MED;
    
    /**
     * TaskManager instance that we can set taskpaths and such on.
     */
    private TaskManager taskManager;
    
    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------- 

    /**
     * Creates a new CLIFrontEnd that can drive an Ant build from the Command
     * Line.
     */
    public CLIFrontEnd() {
        projectBuilder = new ProjectBuilder(this);
        taskManager = projectBuilder.getTaskManager();
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
     *
     */
    public void run(String[] args) {
        this.args = args;
        String target = "";
        File buildFile = null;
        writeMessage("Ant(Eater) -- Proposed Ant 2.0");
        
        // process through the args set
        
        if (isArg("help")) {
            printHelp();
            return;
        }
        
        if (isArg("quiet")) {
            msgLevelFilter = MSG_LEVEL_HIGH;
        }
        
        if (isArg("verbose")) {
            msgLevelFilter = MSG_LEVEL_LOW;
        }
        
        String argTaskpath = getArgValue("taskpath");
        if (argTaskpath != null) {
            if (argTaskpath.equals("")) {
                writeMessage("Must give a value for -taskpath");
                return;
            } else {
                // XXX need to separate on path seps so that real paths can be taken
                try {
                    taskManager.addTaskPathNode(new File(argTaskpath));
                } catch (AntException ae) {
                    System.out.println(ae);
                    System.out.println(ae.getMessage());
                    ae.printStackTrace(System.out);
                    return;
                }
            }
        }
        
        String argBuildfile = getArgValue("buildfile");
        if (argBuildfile != null) {
            if (argBuildfile.equals("")) {
                writeMessage("Must give a value for -buildfile");
                return;
            } else {
                //try {
                buildFile = new File(argBuildfile);
                    //ant.setBuildfile(new File(argBuildfile));
                //} catch (AntException ae) {
                //    writeMessage("Can't set buildfile");
                //    writeMessage(ae.toString());
                //    return;
                //}
            }
        }
        
        target = getTargetArg();
                
        // XXX do something if we dont' have a buildfile set!
        
        // XXX really should check to make sure that the target is set to something
        // like get the default...
        
        try {
            Project project = projectBuilder.buildFromFile(buildFile);
            
            project.setFrontEnd(this);
            project.startBuild(target);
        } catch (AntException ae) {  
        
            //XXX this whole write a string at a time message handling
            // sucks and needs to be improved...
                    
            writeMessage("Build Stopped");
            writeMessage("    Project: " + ae.getProject().getName());
            writeMessage("     Target: " + ae.getTarget().getName());
            writeMessage("  Task Type: " + ae.getTask().getType());
            writeMessage("Details Follow");
            writeMessage("");
            writeMessage(ae.getMessage());
            ae.printStackTrace(System.out);
            Throwable t = ae.getCause();
            if (t != null) {
                writeMessage("");
                writeMessage("Cause Exception: " + t.toString());
                writeMessage(t.getMessage());
                t.printStackTrace(System.out);
            }
        }        
    }

    /**
     * Writes a message to the front end.
     */
    public void writeMessage(String message, int level) {
        if (level >= msgLevelFilter) {
            System.out.println(message);
        }
    }
    
    // -----------------------------------------------------------------
    // PRIVATE METHODS
    // ----------------------------------------------------------------- 
    
    /**
     * Returns the value for a given argument name, null if the argument
     * name isn't in the argument set, or "" if the argument doesn't have
     * a value.
     */
    private String getArgValue(String argName) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-" + argName)) {
                if (i != args.length - 1) {
                    return args[i + 1];
                } else {
                    return "";
                }
            }
        }
        return null;
    }
    
    /**
     * Returns the target that was requested to be built, if any. If no
     * target is determined, returns null.
     */
    public String getTargetArg() {
        String possibleTarget = getArgValue("target");
        if (possibleTarget != null) {
            if (possibleTarget.equals("")) {
                writeMessage("Must give a value for -target");
            } else {
                return possibleTarget;
            }
        } 
        
        possibleTarget = args[args.length - 1];
        if (possibleTarget.startsWith("-")) {
            return null;
        }
        if (args[args.length - 2].startsWith("-")) {
            // our possible target might be an arg value instead of a target
            // XXX ugh -- there has to be a better way here. We need to hold
            // a list of all args that don't have values somewhere.
            if (args[args.length - 2].equals("-help") ||
                args[args.length - 2].equals("-verbose") ||
                args[args.length - 2].equals("-quiet")) {
                // we're ok, the arg before the possible target doesn't have a value
                return possibleTarget;
            } else {
                return null;
            }
        } else {
            return possibleTarget;
        }
    }
    
    /**
     * Indicates whether or not a given argument name exists in the argument
     * set.
     */
    private boolean isArg(String argName) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-" + argName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Prints help to System.out
     */  
    private void printHelp() {
    
        // XXX resource bundle this.
    
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
}
