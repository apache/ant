// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

package org.apache.ant.cli;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Entry point for Ant on the Command Line Interface. This class sets
 * up the basic environment that Ant will execute in and then hands
 * off the the CLIFrontEnd class.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class Main {

    /**
     * Command line entry point. Here we set up the environment via
     * a URLCLassLoader and then execute in the scope of that class loader
     * so that the user doesnt have to set things up themselves.
     */
    public static void main(String[] args) throws Exception {
    
        CLIFrontEnd frontEnd;
    
        // check a few things out and make sure we have the right things
        // that we need in our classpath -- set those up in a custom class
        // loader and execute from there...
    
        Vector classpathNodes = new Vector();
      
        // check to see if we have a compiler on the classpath. Right now
        // we're just checking for the old compiler, but that does tell us
        // if we have tools.jar or not
        try {
            Class clazz = Class.forName("sun.tools.javac.Main");
        } catch (ClassNotFoundException cnfe) { 
            String javaHome = System.getProperty("java.home");
            if (javaHome.endsWith("jre")) {
                javaHome = javaHome.substring(0, javaHome.length() - 4);
            }
            // XXX should check if this exists and bail out if it doesn't
            String classpath = javaHome + "/lib/tools.jar";
            URL url = new File(classpath).toURL();
            classpathNodes.addElement(url);
        }
        
        // XXX add handling for -cp [classpath] argument to set up more classpath
        // nodes
        
        URL[] urls = new URL[classpathNodes.size()];
        Enumeration enum = classpathNodes.elements();
        int i = 0;
        while (enum.hasMoreElements()) {
            urls[i++] = (URL)enum.nextElement();
        }
        
        URLClassLoader classLoader = new URLClassLoader(urls);
        try {
            frontEnd = (CLIFrontEnd)classLoader.loadClass(
                             "org.apache.ant.cli.CLIFrontEnd").newInstance();
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Crap: " + cnfe);
            return;
        } catch (InstantiationException ie) {
            System.out.println("Crap: " + ie);
            return;
        } catch (IllegalAccessException iae) {
            System.out.println("Crap: " + iae);
            return;
        }        
        frontEnd.run(args);
    }
}