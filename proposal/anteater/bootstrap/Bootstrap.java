// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Quick and dirty single class bootstrap utility for getting Ant off
 * the ground when in need. To use, compile this file in the directory
 * where the source code is in the repository, then execute it. That's
 * it.<p>
 *
 * No pretense is made that this is an elegant peice of code. This code
 * only exists to do a ground zero build of Ant. Any other building of
 * Ant should be done with itself whenever possible.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 * @author Conor MacNeill
 */
public class Bootstrap {
    
    /**
     * Command line entry point. This is the first part of the bootstrap
     * where we go and set up the environment and generally do what is
     * necessary to set up for Bootstrapping.
     */
    public static void main(String[] args) throws Exception {
      
        String[] command;
        String classpath = null;
      
        // check to see if we have a compiler on the classpath. Right now
        // we're just checking for the old compiler, but will want to check
        // for the new compiler and use it if it exists. Later.
        try {
            Class clazz = Class.forName("sun.tools.javac.Main");
        } catch (ClassNotFoundException cnfe) { 
            String javaHome = System.getProperty("java.home");
            if (javaHome.endsWith("jre")) {
                javaHome = javaHome.substring(0, javaHome.length() - 4);
            }
            // XXX should check if this exists and bail out if it doesn't
            classpath = javaHome + "/lib/tools.jar" + File.pathSeparator + ".";
        }
        
        // XXX really should check to see if compiling the bootstrap is necessary. :)
        
        System.out.println("Compiling Bootstrap2");
        if (classpath == null) {
            command = new String[] {"javac", "./Bootstrap2.java"};
        } else {
            command = new String[] {"javac", "-classpath", classpath, 
                                    "./Bootstrap2.java"};
        }
        runCommand(command);
        
        System.out.println("Running Bootstrap2");
        if (classpath == null) {
            command = new String[] {"java", "Bootstrap2"};
        } else {
            command = new String[] {"java", "-cp", classpath, "Bootstrap2"};
        }
        runCommand(command, args);
    }
    
    /** 
     * Utility method for execing processes
     */
    static void runCommand(String[] command) throws IOException {
    
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
            
        // echo output from process
            
        InputStream in = process.getInputStream();
        byte[] buf = new byte[80];
        int count = 0;
        count = in.read(buf, 0, buf.length);
        while (count != -1) {
            System.out.write(buf, 0, count);
            count = in.read(buf, 0, buf.length);
        }
            
        in = process.getErrorStream();
        count = in.read(buf, 0, buf.length);          
        if (count > 0) {
            System.out.println();
            System.out.println("Error Stream Output:");
             
            while (count != -1) {
                System.out.write(buf, 0, count);
                count = in.read(buf, 0, buf.length);
            }
        }
    }
    
    /**
     * Utility method for running processes that let some additional args
     * be specified.
     */
    static void runCommand(String[] command, String[] addtlArgs) throws IOException {
        String[] newCommand = new String[command.length + addtlArgs.length];
        for (int i = 0; i < command.length; i++) {
            newCommand[i] = command[i];
        }
        for (int i = 0; i < addtlArgs.length; i++) {
            newCommand[command.length + i] = addtlArgs[i];
        }
        runCommand(newCommand);
    }
} 
 
