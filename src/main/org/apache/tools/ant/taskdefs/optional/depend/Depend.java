/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs.optional.depend;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.taskdefs.MatchingTask;

import java.util.*;
import java.io.*;

import org.apache.tools.ant.taskdefs.optional.depend.*;

/**
 * Generate a dependency file for a given set of classes 
 *
 * @author Conor MacNeill
 */
public class Depend extends MatchingTask {
    static private class ClassFileInfo {
        public File absoluteFile;
        public String relativeName;
        public String className;
    };

    /**
     * The path where source files exist
     */    
    private Path srcPath;

    /**
     * The path where compiled class files exist.
     */
    private Path destPath;
    
    /**
     * The directory which contains the dependency cache.
     */
    private File cache;

    /**
     * A map which gives for every class a list of te class which it affects.
     */
    private Hashtable affectedClassMap;

    /**
     * A map which gives information about a class
     */
    private Hashtable classFileInfoMap;

    /**
     * The list of classes which are out of date.
     */
    private Vector outOfDateClasses;
     
    /**
     * indicates that the dependency relationships should be extended
     * beyond direct dependencies to include all classes. So if A directly
     * affects B abd B directly affects C, then A indirectly affects C.
     */
    private boolean closure = false;

    private void writeDependencyList(File depFile, Vector dependencyList) throws IOException {
        // new dependencies so need to write them out to the cache
        PrintWriter pw = null;
        try {
            String parent = depFile.getParent();
            if (parent != null) {
                new File(parent).mkdirs(); 
            }
            
            pw = new PrintWriter(new FileWriter(depFile));
            for (Enumeration deps = dependencyList.elements(); deps.hasMoreElements();) {
                pw.println(deps.nextElement());
            }
        }
        finally {
            if (pw != null) { 
                pw.close();
            }
        }
    }

    private Vector readDependencyList(File depFile) throws IOException {
        Vector dependencyList = null;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(depFile));
            String line = null;
            dependencyList = new Vector();
            while ((line = in.readLine()) != null) {
                dependencyList.addElement(line);
            }
        }
        finally {
            if (in != null) { 
                in.close();
            }
        }
        
        return dependencyList;
    }


    private void determineDependencies() throws IOException {
        affectedClassMap = new Hashtable();
        classFileInfoMap = new Hashtable();
        for (Enumeration e = getClassFiles(destPath).elements(); e.hasMoreElements(); ) {
            ClassFileInfo info = (ClassFileInfo)e.nextElement();
            log("Adding class info for " + info.className, Project.MSG_DEBUG);
            classFileInfoMap.put(info.className, info);
            
            Vector dependencyList = null;
            
            if (cache != null) {
                // try to read the dependency info from the cache if it is not out of date
                File depFile = new File(cache, info.relativeName + ".dep");
                if (depFile.exists() && depFile.lastModified() > info.absoluteFile.lastModified()) {
                    // depFile exists and is newer than the class file
                    // need to read dependency list from the file.
                    dependencyList = readDependencyList(depFile);
                }
            }
            
            if (dependencyList == null) {
                // not cached - so need to read directly from the class file
                FileInputStream inFileStream = null;
                try {
                    inFileStream = new FileInputStream(info.absoluteFile);
                    ClassFile classFile = new ClassFile();
                    classFile.read(inFileStream);
                    
                    dependencyList = classFile.getClassRefs();
                    
                    if (cache != null) {
                        // new dependencies so need to write them out to the cache
                        File depFile = new File(cache, info.relativeName + ".dep");
                        writeDependencyList(depFile, dependencyList);
                    }
                }
                finally {
                    if (inFileStream != null) {
                        inFileStream.close();
                    }
                }
            }
            
            // This class depends on each class in the dependency list. For each
            // one of those, add this class into their affected classes list 
            
            for (Enumeration depEnum = dependencyList.elements(); depEnum.hasMoreElements(); ) {
                String dependentClass = (String)depEnum.nextElement();
                
                Hashtable affectedClasses = (Hashtable)affectedClassMap.get(dependentClass);
                if (affectedClasses == null) {
                    affectedClasses = new Hashtable();
                    affectedClassMap.put(dependentClass, affectedClasses);
                }
                
                affectedClasses.put(info.className, info);
            }
        }
    }
    
    
    private void deleteAllAffectedFiles() {
        for (Enumeration e = outOfDateClasses.elements(); e.hasMoreElements();) {
            String className = (String)e.nextElement();
            deleteAffectedFiles(className);
        }            
    }
    
    private void deleteAffectedFiles(String className) {
        Hashtable affectedClasses = (Hashtable)affectedClassMap.get(className);
        if (affectedClasses != null) {
            for (Enumeration e = affectedClasses.keys(); e.hasMoreElements(); ) {
                String affectedClassName = (String)e.nextElement();
                ClassFileInfo affectedClassInfo = (ClassFileInfo)affectedClasses.get(affectedClassName);
                if (affectedClassInfo.absoluteFile.exists()) {
                    log("Deleting file " + affectedClassInfo.absoluteFile.getPath() + " since " + 
                        className + " out of date", Project.MSG_VERBOSE);
                    affectedClassInfo.absoluteFile.delete();
                    if (closure) {
                        deleteAffectedFiles(affectedClassName);
                    }
                    else {
                        // without closure we may delete an inner class but not the
                        // top level class which would not trigger a recompile.
                           
                        if (affectedClassName.indexOf("$") != -1) {
                            // need to delete the main class
                            String topLevelClassName 
                                = affectedClassName.substring(0, affectedClassName.indexOf("$"));
                            log("Top level class = " + topLevelClassName, Project.MSG_VERBOSE);
                            ClassFileInfo topLevelClassInfo 
                                = (ClassFileInfo)classFileInfoMap.get(topLevelClassName);
                            if (topLevelClassInfo != null &&
                                topLevelClassInfo.absoluteFile.exists()) {
                                log("Deleting file " + topLevelClassInfo.absoluteFile.getPath() + " since " + 
                                    "one of its inner classes was removed", Project.MSG_VERBOSE);
                                topLevelClassInfo.absoluteFile.delete();
                                if (closure) {
                                    deleteAffectedFiles(topLevelClassName);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Does the work.
     *
     * @exception BuildException Thrown in unrecovrable error.
     */
    public void execute() throws BuildException {
        try {
            long start = System.currentTimeMillis();
            String [] srcPathList = srcPath.list();
            if (srcPathList.length == 0) {
                throw new BuildException("srcdir attribute must be set!", location);
            }
        
            if (destPath == null) {
                destPath = srcPath;
            }
            
            if (cache != null && cache.exists() && !cache.isDirectory()) {
                throw new BuildException("The cache, if specified, must point to a directory");
            }
        
            if (cache != null && !cache.exists()) {
                cache.mkdirs();
            }
        
            determineDependencies();
 
/*            
            for (Enumeration e = affectedClassMap.keys(); e.hasMoreElements(); ) {
                String className = (String)e.nextElement();
                log("Class " + className + " affects:", Project.MSG_DEBUG);
                Hashtable affectedClasses = (Hashtable)affectedClassMap.get(className);
                for (Enumeration e2 = affectedClasses.keys(); e2.hasMoreElements(); ) {
                    String affectedClass = (String)e2.nextElement();
                    ClassFileInfo info = (ClassFileInfo)affectedClasses.get(affectedClass);
                    log("   " + affectedClass + " in " + info.absoluteFile.getPath(), Project.MSG_DEBUG);
                }
            }
*/            
            // we now need to scan for out of date files. When we have the list
            // we go through and delete all class files which are affected by these files.
            outOfDateClasses = new Vector();
            for (int i=0; i<srcPathList.length; i++) {
                File srcDir = (File)project.resolveFile(srcPathList[i]);
                if (srcDir.exists()) {
        
                    DirectoryScanner ds = this.getDirectoryScanner(srcDir);
        
                    String[] files = ds.getIncludedFiles();
        
                    scanDir(srcDir, files);
                }
            }
            
            // we now have a complete list of classes which are out of date
            // We scan through the affected classes, deleting any affected classes.
            deleteAllAffectedFiles();
            
            log("Duration = " + (System.currentTimeMillis() - start));
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Scans the directory looking for source files that are newer than their class files.
     * The results are returned in the class variable compileList
     */
    protected void scanDir(File srcDir, String files[]) {

        long now = System.currentTimeMillis();

        for (int i = 0; i < files.length; i++) {
            File srcFile = new File(srcDir, files[i]);
            if (files[i].endsWith(".java")) {
                String filePath = srcFile.getPath();
                String className = filePath.substring(srcDir.getPath().length() + 1,
                                                      filePath.length() - ".java".length());
                className = ClassFileUtils.convertSlashName(className);                                                      
                ClassFileInfo info = (ClassFileInfo)classFileInfoMap.get(className);
                if (info == null) {
                    // there was no class file. add this class to the list
                    outOfDateClasses.addElement(className);
                }
                else {
                    if (srcFile.lastModified() > info.absoluteFile.lastModified()) {
                        outOfDateClasses.addElement(className);
                    }
                }
            }
        }
    }


    /** 
     * Get the list of class files we are going to analyse.
     *
     * @param classLocations a path structure containing all the directories
     *                       where classes can be found.
     * @return a vector containing the classes to analyse.
     */
    private Vector getClassFiles(Path classLocations) {
        // break the classLocations into its components.        
        String[] classLocationsList = classLocations.list();            
        
        Vector classFileList = new Vector();
        
        for (int i = 0; i < classLocationsList.length; ++i) {
            File dir = new File(classLocationsList[i]);
            if (dir.isDirectory()) {
                addClassFiles(classFileList, dir, dir);
            }
        }
        
        return classFileList;
    }

    /** 
     * Add the list of class files from the given directory to the 
     * class file vector, including any subdirectories.
     *
     * @param classLocations a path structure containing all the directories
     *                       where classes can be found.
     * @return a vector containing the classes to analyse.
     */
    private void addClassFiles(Vector classFileList, File dir, File root) {
        String[] filesInDir = dir.list();
        
        if (filesInDir != null) {
            int length = filesInDir.length;

            for (int i = 0; i < length; ++i) {
                File file = new File(dir, filesInDir[i]);
                if (file.isDirectory()) {
                    addClassFiles(classFileList, file, root);
                }
                else if (file.getName().endsWith(".class")) {
                    ClassFileInfo info = new ClassFileInfo();
                    info.absoluteFile = file;
                    info.relativeName = file.getPath().substring(root.getPath().length() + 1,
                                                                 file.getPath().length() - 6);
                    info.className = ClassFileUtils.convertSlashName(info.relativeName);                                                                 
                    classFileList.addElement(info);
                }
            } 
        } 
    }
    
    
    /**
     * Set the source dirs to find the source Java files.
     */
    public void setSrcdir(Path srcPath) {
        this.srcPath = srcPath;
    }

    /**
     * Set the destination directory where the compiled java files exist.
     */
    public void setDestDir(Path destPath) {
        this.destPath = destPath;
    }
    
    public void setCache(File cache) {
        this.cache = cache;
    }
    
    public void setClosure(boolean closure) {
        this.closure = closure;
    }
}

