// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

package org.apache.ant;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

/**
 * Manager of tasks and all things related to tasks. Tasks can be found in a
 * wide number of locations -- and most of these locations require class loading
 * help. As well, new nodes on the task search path may be added at any time.
 * When these are added, new tasks should be scanned for.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class TaskManager {

    // -----------------------------------------------------------------
    // PRIVATE DATA MEMBERS
    // -----------------------------------------------------------------
    
    /**
     * FrontEnd that this TaskManager can communicate through.
     */
    private FrontEnd frontEnd;
    
    /**
     * Data structure where all the Class definition for all known tasks are
     * held.
     */
    private Hashtable taskClasses = new Hashtable();
    
    /**
     * Data structure that holds all the nodes where tasks are picked up from.
     */
    private Vector taskPathNodes = new Vector();

    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------
    
    /**
     * Creates a new TaskManager.
     */
    TaskManager(FrontEnd frontEnd) {
        System.out.println("CREATING TM");
        this.frontEnd = frontEnd;
    }
    
    // -----------------------------------------------------------------
    // PUBLIC METHODS
    // -----------------------------------------------------------------
 
    /**
     * Adds a node to the task path 
     */
    public void addTaskPathNode(File file) {
        taskPathNodes.addElement(file);
        processTaskPathNode(file);
    }

    // -----------------------------------------------------------------
    // PACKAGE METHODS
    // -----------------------------------------------------------------
    
    /**
     *
     */
    AbstractTask getTaskInstance(String taskName) throws AntException {
        Class clazz = (Class)taskClasses.get(taskName);
        try {
            return (AbstractTask)clazz.newInstance();
        } catch (Exception e) { 
            String msg = "Can't instantiate task: " + taskName;
            AntException ae = new AntException(msg, e);
            throw ae;
        }
    }
 
    // -----------------------------------------------------------------
    // PRIVATE METHODS
    // -----------------------------------------------------------------
   
    /**
     * Returns an enum of the task names that are defined in a given 
     * properties file.
     */
    private Enumeration getTaskNames(Properties props) {
        Vector v = new Vector();
        String s = props.getProperty("tasks");
        StringTokenizer tok = new StringTokenizer(s, ",", false);
        while (tok.hasMoreTokens()) {
            String taskName = tok.nextToken().trim();
            v.addElement(taskName);
        }
        return v.elements();
    }
   
    /**
     * Processes a directory to get class defintions from it
     */
    private void processDir(File dir) {
        frontEnd.writeMessage("Scanning " + dir + " for tasks", 
                                       FrontEnd.MSG_LEVEL_LOW);
        File file = new File(dir, "taskdef.properties");
        if (file.exists()) {
            try {
                InputStream in = new FileInputStream(file);
                Properties props = new Properties();
                props.load(in);
                in.close();
                
                Enumeration enum = getTaskNames(props);
                while (enum.hasMoreElements()) {
                    String taskName = (String)enum.nextElement();
                    String taskClass = props.getProperty("task." + taskName + ".class");
                    URLClassLoader loader = new URLClassLoader(new URL[] {dir.toURL()});
                    try {
                        Class clazz = loader.loadClass(taskClass);
                        frontEnd.writeMessage("Got Task: " + taskName +
                                                       clazz, FrontEnd.MSG_LEVEL_LOW);
                        taskClasses.put(taskName, clazz);
                    } catch (ClassNotFoundException cnfe) {
                        System.out.println("Couldn't load task: " + taskName);
                        System.out.println(cnfe);
                        // XXX error out and stop....
                    }
                }
            } catch (IOException ioe) {
                System.out.println("Could not work with dir: " + dir);
                System.out.println(ioe);
                // XXX error out and stop the build
            }
        }
    }
   
    /**
     * Processes a jar file to get class definitions from it
     */
    private void processJar(File file) {
        frontEnd.writeMessage("Scanning " + file + " for tasks", 
                                       FrontEnd.MSG_LEVEL_LOW);
        try {
            ZipFile zipFile = new ZipFile(file);
            ZipEntry zipEntry = zipFile.getEntry("taskdef.properties");
            if (zipEntry != null) {
                InputStream in = zipFile.getInputStream(zipEntry);
                Properties props = new Properties();
                props.load(in);
                in.close();
            
                Enumeration enum = getTaskNames(props);
                while (enum.hasMoreElements()) {
                    String taskName = (String)enum.nextElement();
                    String taskClass = props.getProperty("task." + taskName + ".class");
                    URLClassLoader loader = new URLClassLoader(new URL[] {file.toURL()});
                    try {
                        Class clazz = loader.loadClass(taskClass);
                       	frontEnd.writeMessage("Got Task: " + taskName +
                                                       clazz, FrontEnd.MSG_LEVEL_LOW);
                        taskClasses.put(taskName, clazz);
                    } catch (ClassNotFoundException cnfe) {
                        System.out.println("Couldn't load task: " + taskName);
                        System.out.println(cnfe);
                        // XXX error out and stop....
                    }
                }
            }
            // make sure to not leave resources hanging
            zipFile.close();
        } catch (IOException ioe) {
            System.out.println("Couldn't work with file: " + file);
            System.out.println(ioe);
            // XXX need to exception out of here properly to stop things 
        }
    }
   
    /**
     * Processes a node of the task path searching for task definitions there
     * and adding them to the list of known tasks
     */
    private void processTaskPathNode(File file) {
    
        // task path nodes can be any of the following:
        //     * jar file
        //     * directory of jar files
        //     * directory holding class files
        
        if(file.isDirectory()) {
            // first look for all jar files here
            // second look for a taskdefs.properties here to see if we should
            // treat the directory as a classpath
            
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".jar")) {
                    processJar(new File(file, files[i]));
                } else if (files[i].equals("taskdef.properties")) {
                    processDir(file);
                }
            }
        } else if (file.getName().endsWith(".jar")) {
            processJar(file);
        }
    }
    
    /**
     * Sets up the taskpath based on the currently running operating
     * system. In general, the ordering of the taskpath is: user directory,
     * system directory, and then installation. This allows users or
     * system admins to override or add tasks.
     */
    private void setUpTaskPath() {
        
        // 1st, add user's home dir.
        
        File f;
        
        String userHome = System.getProperty("user.home");
        
        // generic unix
        f = new File(userHome + ".ant", "tasks");
        if (f.exists() && f.isDirectory()) {
            addTaskPathNode(f);
        }
        
        // macos x
        f = new File(userHome + "/Library/Ant", "Tasks");
        if (f.exists() && f.isDirectory()) {
            addTaskPathNode(f);
        }
        
        // windows -- todo
        
        // 2nd, add system local dir.
        
        // generic unix
        f = new File("/usr/local/ant/tasks");
        if (f.exists() && f.isDirectory()) {
            addTaskPathNode(f);
        }
        
        // macos x
        f = new File("/Library/Ant/Tasks");
        if (f.exists() && f.isDirectory()) {
            addTaskPathNode(f);
        }
        
        // windows -- todo
        
        // 3rd, add installation local dir.
        
        //System.out.println("BASE: " + this.getClass().getResource("/"));
        
        // XXX ---- not really sure how the best way of getting this info is...
        // hafta think about it.
    }

}