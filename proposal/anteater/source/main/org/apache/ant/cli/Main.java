// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

package org.apache.ant.cli;

import java.io.*;
import java.util.*;
import org.apache.ant.*;

/**
 * Entry point for Ant on the Command Line Interface.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class Main {

    /**
     * Command line entry point.
     */
    public static void main(String[] args) {
        Ant ant = new Ant();
        String target = "";
        
        System.out.println("Ant(Eater) -- Proposed Ant 2.0");

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
                        System.out.println("ICK: " + ae);
                        System.out.println(ae.getMessage());
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

        System.out.println();
        System.out.println("Executing Target: " + target);
        
        try {
            ant.buildTarget(target);
        } catch (AntException ae) {
            System.out.println("Problem while building: " + ae);
            System.out.println(ae.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // PRIVATE METHODS
    // -----------------------------------------------------------------  
    
    /**
     * Prints help to System.out
     */  
    private static void printHelp() {
        System.out.println("Usage: ant [args] [target]");
        System.out.println("   Arguments:");
        System.out.println("       -help");
        System.out.println("       -taskpath [path]");
        System.out.println("       -buildfile [file]");
    }
}