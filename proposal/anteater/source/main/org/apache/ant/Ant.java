// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

package org.apache.ant;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Central class of Ant. This is the core 'kernel' of ant. Interfaces into
 * ant talk to Ant through this class.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class Ant {
    
    // -----------------------------------------------------------------
    // PRIVATE DATA MEMBERS
    // -----------------------------------------------------------------
    
    /**
     *
     */
    private Hashtable abstractTaskClasses = new Hashtable();
    
    /**
     *
     */
    private Vector taskPathNodes = new Vector();
    
    /**
     *
     */
    private File buildfile;
    
    /**
     *
     */
    private Project project;
    
    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------
    
    /**
     * Constructs a new Ant instance.
     */
    public Ant() {
        setUpTaskPath();
    }
    
    // -----------------------------------------------------------------
    // PUBLIC METHODS
    // ----------------------------------------------------------------- 
    
    /**
     * Sets additional path nodes onto the task lookup path. These nodes
     * take precendence over all previously set path nodes.
     */   
    public void addTaskPathNode(File node) {
        taskPathNodes.insertElementAt(node, 0);
    }
    
    /**
     *
     */
    public void buildTarget(String targetName) throws AntException {
    
        try {
            loadTasks();
        } catch (IOException ioe) {
            throw new AntException(ioe.getMessage());
        }
        
        Target target = project.getTarget(targetName);
        
        // XXX don't forget to execute dependancies first!
        
        Enumeration enum = target.getTasks().elements();
        while (enum.hasMoreElements()) {
            Task task = (Task)enum.nextElement();
            Object o = abstractTaskClasses.get(task.getType());
            if (o != null) {
                Class c = (Class)o;
                try {
                    AbstractTask aTask = (AbstractTask)c.newInstance();
                    aTask.setAttributes(task.getAttributes());
                    aTask.setProject(project);
                    boolean b = aTask.execute();
                    if (!b) {
                        throw new AntException("STOP: Task " + task + 
                                               " did not succeed");
                    }
                } catch (Exception e) {
                    // XXX yes yes yes, this shouldn't be a catch all...
                    throw new AntException("ERR: " + e);
                }
            } else {
                throw new AntException("Don't have a class for task type: " + task);
            }
        }
    }
    
    /**
     *
     */
    public Project getProject() {
        return project;
    }
    
    /**
     * Sets the buildfile to be used. This action triggers a parse of
     * the build file and assembles a Project object from it.
     */
    public void setBuildfile(File file) throws AntException {
        buildfile = file;
        ProjectBuilder builder = new ProjectBuilder();
        project = builder.buildFromFile(file); 
        project.setAnt(this);
        System.out.println("Loaded Project: " + project.getName());
        
        // XXX remove the dump after comfort level is reached
        
        System.out.println("Dump of Project:");
        Enumeration enum = project.getTargets();
        while (enum.hasMoreElements()) {
            Target target = (Target)enum.nextElement();
            System.out.println("    Target: " + target.getName());
            Enumeration enum2 = target.getTasks().elements();
            while (enum2.hasMoreElements()) {
                Task task = (Task)enum2.nextElement();
                System.out.println("        Task: " + task.getType());
                Enumeration enum3 = task.getAttributeNames();
                while (enum3.hasMoreElements()) {
                    String atName = (String)enum3.nextElement();
                    String atValue = task.getAttribute(atName);
                    System.out.println("            Att: " + atName + " = " + 
                                       atValue);
                }
            }
        }
    }
    
    // -----------------------------------------------------------------
    // PRIVATE METHODS
    // ----------------------------------------------------------------- 
    
    /**
     * Searches through the taskpath and loads up the taskImpl hashtable
     *
     * XXX we also need to lookup a taskdef.properties file out of a few
     * strategic locations on disk to allow generic classes to be pulled
     * from the classpath
     */
    private void loadTasks() throws IOException {
        Enumeration enum = taskPathNodes.elements();
        while (enum.hasMoreElements()) {
            File dir = (File)enum.nextElement();
            String[] files = dir.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".jar")) {
                    File f = new File(dir, files[i]);
                    ZipFile zf = new ZipFile(f);
                    ZipEntry ze = zf.getEntry("/taskdef.properties");
                    if (ze != null) {
                        InputStream is = zf.getInputStream(ze);
                        Properties props = new Properties();
                        props.load(is);
                        is.close();
                        //System.out.println("Props: " + props);
                        String s = props.getProperty("tasks");
                        StringTokenizer tok = new StringTokenizer(s, ",", false);
                        while (tok.hasMoreTokens()) {
                            String taskType = tok.nextToken();
                            String taskClassName = props.getProperty(taskType + 
                                                                     ".class");
                            //System.out.println("TASK: " + taskType + " class: " +
                            //                taskClassName);
                            ClassLoader pcl = this.getClass().getClassLoader();
                            TaskClassLoader tcl = new TaskClassLoader(pcl, zf);
                            try {
                                Class clazz = tcl.findClass(taskClassName);
                                abstractTaskClasses.put(taskType, clazz);
                            } catch (ClassNotFoundException cnfe) {
                                System.out.println(cnfe);
                                System.out.println(cnfe.getMessage());
                            }
                        }
                    }
                }
            }
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
            taskPathNodes.addElement(f);
        }
        
        // macos x
        f = new File(userHome + "/Library/Ant", "Tasks");
        if (f.exists() && f.isDirectory()) {
            taskPathNodes.addElement(f);
        }
        
        // windows -- todo
        
        // 2nd, add system local dir.
        
        // generic unix
        f = new File("/usr/local/ant/tasks");
        if (f.exists() && f.isDirectory()) {
            taskPathNodes.addElement(f);
        }
        
        // macos x
        f = new File("/Library/Ant/Tasks");
        if (f.exists() && f.isDirectory()) {
            taskPathNodes.addElement(f);
        }
        
        // windows -- todo
        
        // 3rd, add installation local dir.
        
        //System.out.println("BASE: " + this.getClass().getResource("/"));
        
        // XXX ---- not really sure how the best way of getting this info is...
        // hafta think about it.
    }

}