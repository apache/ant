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
import java.net.URL;

import org.apache.tools.ant.taskdefs.optional.depend.*;

/**
 * Generate a dependency file for a given set of classes 
 *
 * @author Conor MacNeill
 */
public class Depend extends MatchingTask {
    /**
     * A class (struct) user to manage information about a class
     */
    static private class ClassFileInfo {
        /** The file where the class file is stored in the file system */
        public File absoluteFile;
        
        /** The location of the file relative to its base directory - the root
            of the package namespace */
        public String relativeName;
        
        /** The Java class name of this class */
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
     * A map which gives the list of jars and classes from the classpath that
     * a class depends upon 
     */
    private Hashtable classpathDependencies;

    /**
     * The list of classes which are out of date.
     */
    private Hashtable outOfDateClasses;
     
    /**
     * indicates that the dependency relationships should be extended
     * beyond direct dependencies to include all classes. So if A directly
     * affects B abd B directly affects C, then A indirectly affects C.
     */
    private boolean closure = false;
    
    /**
     * Flag which controls whether the reversed dependencies should be dumped
     * to the log
     */
    private boolean dump = false;

    /** The classpath to look for additional dependencies */
    private Path dependClasspath;

    /**
     * constants used with the cache file
     */
    private final static String CACHE_FILE_NAME = "dependencies.txt";
    private final static String CLASSNAME_PREPEND = "||:";

    /**
     * Set the classpath to be used for this dependency check.
     */
    public void setClasspath(Path classpath) {
        if (dependClasspath == null) {
            dependClasspath = classpath;
        } else {
            dependClasspath.append(classpath);
        }
    }

    /** Gets the classpath to be used for this dependency check. */
    public Path getClasspath() {
        return dependClasspath;
    }

    /**
     * Creates a nested classpath element.
     */
    public Path createClasspath() {
        if (dependClasspath == null) {
            dependClasspath = new Path(project);
        }
        return dependClasspath.createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }
        
    /**
     * Read the dependencies from cache file
     */
    private Hashtable readCachedDependencies() throws IOException{
        Hashtable dependencyMap = new Hashtable();
        
        if (cache != null) {
            File depFile = new File(cache, CACHE_FILE_NAME);
            BufferedReader in = null;
            if (depFile.exists()) {
                try {
                    in = new BufferedReader(new FileReader(depFile));
                    String line = null;
                    Vector dependencyList = null;
                    String className = null;
                    int prependLength = CLASSNAME_PREPEND.length();
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith(CLASSNAME_PREPEND)) {
                            dependencyList = new Vector();
                            className = line.substring(prependLength);
                            dependencyMap.put(className, dependencyList);
                        }
                        else {
                            dependencyList.addElement(line);
                        }
                    }
                }
                finally {
                    if (in != null) { 
                        in.close();
                    }
                }
            }
        }
        
        return dependencyMap;
    }
    
    /**
     * Write the dependencies to cache file
     */
    private void writeCachedDependencies(Hashtable dependencyMap) throws IOException{
        if (cache != null) {
            PrintWriter pw = null;
            try {
                cache.mkdirs(); 
                File depFile = new File(cache, CACHE_FILE_NAME);
                
                pw = new PrintWriter(new FileWriter(depFile));
                for (Enumeration deps = dependencyMap.keys(); deps.hasMoreElements();) {
                    String className = (String)deps.nextElement();
                    
                    pw.println(CLASSNAME_PREPEND + className);
                    
                    Vector dependencyList = (Vector)dependencyMap.get(className);
                    int size = dependencyList.size();
                    for (int x = 0; x < size; x++) {
                        pw.println(dependencyList.elementAt(x));
                    }
                }
            }
            finally {
                if (pw != null) { 
                    pw.close();
                }
            }
        }
    }


    /**
     * Determine the dependencies between classes. 
     *
     * Class dependencies are determined by examining the class references in a class file
     * to other classes 
     */
    private void determineDependencies() throws IOException {
        affectedClassMap = new Hashtable();
        classFileInfoMap = new Hashtable();
        boolean cacheDirty = false;
        
        Hashtable dependencyMap = new Hashtable();
        File depCacheFile = null;
        boolean depCacheFileExists = true;
        long depCacheFileLastModified = Long.MAX_VALUE;
        
        // read the dependency cache from the disk
        if (cache != null) {
            dependencyMap = readCachedDependencies();
            depCacheFile = new File(cache, CACHE_FILE_NAME);
            depCacheFileExists = depCacheFile.exists();
            depCacheFileLastModified = depCacheFile.lastModified();
        }
        for (Enumeration e = getClassFiles(destPath).elements(); e.hasMoreElements(); ) {
            ClassFileInfo info = (ClassFileInfo)e.nextElement();
            log("Adding class info for " + info.className, Project.MSG_DEBUG);
            classFileInfoMap.put(info.className, info);
            
            Vector dependencyList = null;
            
            if (cache != null) {
                // try to read the dependency info from the map if it is not out of date
                if (depCacheFileExists && depCacheFileLastModified > info.absoluteFile.lastModified()) {
                    // depFile exists and is newer than the class file
                    // need to get dependency list from the map.
                    dependencyList = (Vector)dependencyMap.get(info.className);
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
                    if (dependencyList != null) {
                        cacheDirty = true;
                        dependencyMap.put(info.className, dependencyList);
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
        
        classpathDependencies = null;
        if (dependClasspath != null) {
            // now determine which jars each class depends upon
            classpathDependencies = new Hashtable();
            AntClassLoader loader = new AntClassLoader(getProject(), dependClasspath);
            
            Hashtable classpathFileCache = new Hashtable();
            Object nullFileMarker = new Object();
            for (Enumeration e = dependencyMap.keys(); e.hasMoreElements();) {
                String className = (String)e.nextElement();
                Vector dependencyList = (Vector)dependencyMap.get(className);
                Hashtable dependencies = new Hashtable();
                classpathDependencies.put(className, dependencies);
                for (Enumeration e2 = dependencyList.elements(); e2.hasMoreElements();) {
                    String dependency =(String)e2.nextElement();
                    Object classpathFileObject = classpathFileCache.get(dependency);
                    if (classpathFileObject == null) {
                        classpathFileObject = nullFileMarker;
                        
                        if (!dependency.startsWith("java.") && !dependency.startsWith("javax.")) {
                            URL classURL = loader.getResource(dependency.replace('.', '/') + ".class");
                            if (classURL != null) {
                                if (classURL.getProtocol().equals("jar")) {
                                    String jarFilePath = classURL.getFile();
                                    if (jarFilePath.startsWith("file:")) {
                                        int classMarker = jarFilePath.indexOf('!');
                                        jarFilePath = jarFilePath.substring(5, classMarker);
                                    }
                                    classpathFileObject = new File(jarFilePath);
                                }
                                else if (classURL.getProtocol().equals("file")) {
                                    String classFilePath = classURL.getFile();
                                    classpathFileObject = new File(classFilePath);
                                }
                                log("Class " + className + 
                                    " depends on " + classpathFileObject + 
                                    " due to " + dependency, Project.MSG_DEBUG);
                            }
                        }
                        classpathFileCache.put(dependency, classpathFileObject);
                    }
                    if (classpathFileObject != null && classpathFileObject != nullFileMarker) {
                        // we need to add this jar to the list for this class.
                        File jarFile = (File)classpathFileObject;
                        dependencies.put(jarFile, jarFile);
                    }
                }
            }
        }
        
        // write the dependency cache to the disk
        if (cache != null && cacheDirty) {
            writeCachedDependencies(dependencyMap);
        }
    }
    
    private int deleteAllAffectedFiles() {
        int count = 0;
        for (Enumeration e = outOfDateClasses.elements(); e.hasMoreElements();) {
            String className = (String)e.nextElement();
            count += deleteAffectedFiles(className);
            ClassFileInfo classInfo = (ClassFileInfo)classFileInfoMap.get(className);
            if (classInfo != null && classInfo.absoluteFile.exists()) {
                classInfo.absoluteFile.delete();
                count++;
            }
        }
        return count;            
    }
    
    private int deleteAffectedFiles(String className) {
        int count = 0;

        Hashtable affectedClasses = (Hashtable)affectedClassMap.get(className);
        if (affectedClasses != null) {
            for (Enumeration e = affectedClasses.keys(); e.hasMoreElements(); ) {
                String affectedClassName = (String)e.nextElement();
                ClassFileInfo affectedClassInfo = (ClassFileInfo)affectedClasses.get(affectedClassName);
                if (affectedClassInfo.absoluteFile.exists()) {
                    log("Deleting file " + affectedClassInfo.absoluteFile.getPath() + " since " + 
                        className + " out of date", Project.MSG_VERBOSE);
                    affectedClassInfo.absoluteFile.delete();
                    count++;
                    if (closure) {
                        count += deleteAffectedFiles(affectedClassName);
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
                                count++;
                                if (closure) {
                                    count += deleteAffectedFiles(topLevelClassName);
                                }
                            }
                        }
                    }
                }
            }
        }
        return count;
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
            
            if (dump) {            
                log("Reverse Dependency Dump for " + affectedClassMap.size() + 
                    " classes:", Project.MSG_DEBUG);
                for (Enumeration e = affectedClassMap.keys(); e.hasMoreElements(); ) {
                    String className = (String)e.nextElement();
                    log(" Class " + className + " affects:", Project.MSG_DEBUG);
                    Hashtable affectedClasses = (Hashtable)affectedClassMap.get(className);
                    for (Enumeration e2 = affectedClasses.keys(); e2.hasMoreElements(); ) {
                        String affectedClass = (String)e2.nextElement();
                        ClassFileInfo info = (ClassFileInfo)affectedClasses.get(affectedClass);
                        log("    " + affectedClass + " in " + info.absoluteFile.getPath(), Project.MSG_DEBUG);
                    }
                }
                
                if (classpathDependencies != null) {
                    log("Classpath file dependencies (Forward):", Project.MSG_DEBUG);
                    for (Enumeration e = classpathDependencies.keys(); e.hasMoreElements();) { 
                        String className = (String)e.nextElement();
                        log(" Class " + className + " depends on:", Project.MSG_DEBUG);
                        Hashtable dependencies = (Hashtable)classpathDependencies.get(className);
                        for (Enumeration e2 = dependencies.elements(); e2.hasMoreElements();) {
                            File classpathFile = (File)e2.nextElement();
                            log("    " + classpathFile.getPath(), Project.MSG_DEBUG);
                        }
                    }
                }
                            
            }
            
            // we now need to scan for out of date files. When we have the list
            // we go through and delete all class files which are affected by these files.
            outOfDateClasses = new Hashtable();
            for (int i=0; i < srcPathList.length; i++) {
                File srcDir = (File)project.resolveFile(srcPathList[i]);
                if (srcDir.exists()) {
                    DirectoryScanner ds = this.getDirectoryScanner(srcDir);
                    String[] files = ds.getIncludedFiles();
                    scanDir(srcDir, files);
                }
            }

            // now check classpath file dependencies
            if (classpathDependencies != null) {
                for (Enumeration e = classpathDependencies.keys(); e.hasMoreElements();) { 
                    String className = (String)e.nextElement();
                    if (!outOfDateClasses.containsKey(className)) {
                        ClassFileInfo info = (ClassFileInfo)classFileInfoMap.get(className);
                        
                        // if we have no info about the class - it may have been deleted already and we 
                        // are using cached info.
                        if (info != null) {
                            Hashtable dependencies = (Hashtable)classpathDependencies.get(className);
                            for (Enumeration e2 = dependencies.elements(); e2.hasMoreElements();) {
                                File classpathFile = (File)e2.nextElement();
                                if (classpathFile.lastModified() > info.absoluteFile.lastModified()) {
                                    log("Class " + className + 
                                    " is out of date with respect to " + classpathFile, Project.MSG_DEBUG);
                                    outOfDateClasses.put(className, className);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            // we now have a complete list of classes which are out of date
            // We scan through the affected classes, deleting any affected classes.
            int count = deleteAllAffectedFiles();
            
            long duration = (System.currentTimeMillis() - start) / 1000;
            log("Deleted " + count + " out of date files in " + duration + " seconds");
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
                    outOfDateClasses.put(className, className);
                }
                else {
                    if (srcFile.lastModified() > info.absoluteFile.lastModified()) {
                        outOfDateClasses.put(className, className);
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

    /**
     * Flag to indicate whether the reverse dependency list should be dumped to debug
     */
    public void setDump(boolean dump) {
        this.dump = dump;
    }
}

