/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.rmic.DefaultRmicAdapter;
import org.apache.tools.ant.taskdefs.rmic.WLRmic;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.depend.DependencyAnalyzer;

/**
 * Generates a dependency file for a given set of classes.
 *
 */
public class Depend extends MatchingTask {
    private static final int ONE_SECOND = 1000;

    /**
     * A class (struct) user to manage information about a class
     *
     */
    private static class ClassFileInfo {
        /** The file where the class file is stored in the file system */
        private File absoluteFile;

        /** The Java class name of this class */
        private String className;

        /** The source File containing this class */
        private File sourceFile;

        /** if user has been warned about this file not having a source file */
        private boolean isUserWarned = false;
    }

    /** The path where source files exist */
    private Path srcPath;

    /** The path where compiled class files exist. */
    private Path destPath;

    /** The directory which contains the dependency cache. */
    private File cache;

    /** The list of source paths derived from the srcPath field. */
    private String[] srcPathList;

    /**
     * A map which gives for every class a list of the class which it
     * affects.
     */
    private Hashtable affectedClassMap;

    /** A map which gives information about a class */
    private Hashtable classFileInfoMap;

    /**
     * A map which gives the list of jars and classes from the classpath
     * that a class depends upon
     */
    private Hashtable classpathDependencies;

    /** The list of classes which are out of date. */
    private Hashtable outOfDateClasses;

    /**
     * indicates that the dependency relationships should be extended beyond
     * direct dependencies to include all classes. So if A directly affects
     * B and B directly affects C, then A indirectly affects C.
     */
    private boolean closure = false;

    /**
     * flag to enable warning if we encounter RMI stubs
     */
    private boolean warnOnRmiStubs = true;

    /**
     * Flag which controls whether the reversed dependencies should be
     * dumped to the log
     */
    private boolean dump = false;

    /** The classpath to look for additional dependencies */
    private Path dependClasspath;

    /** constants used with the cache file */
    private static final String CACHE_FILE_NAME = "dependencies.txt";
    /** String Used to separate classnames in the dependency file */
    private static final String CLASSNAME_PREPEND = "||:";

    /**
     * Set the classpath to be used for this dependency check.
     *
     * @param classpath the classpath to be used when checking for
     *      dependencies on elements in the classpath
     */
    public void setClasspath(Path classpath) {
        if (dependClasspath == null) {
            dependClasspath = classpath;
        } else {
            dependClasspath.append(classpath);
        }
    }

    /**
     * Gets the classpath to be used for this dependency check.
     *
     * @return the current dependency classpath
     */
    public Path getClasspath() {
        return dependClasspath;
    }

    /**
     * Adds a classpath to be used for this dependency check.
     *
     * @return A path object to be configured by Ant
     */
    public Path createClasspath() {
        if (dependClasspath == null) {
            dependClasspath = new Path(getProject());
        }
        return dependClasspath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     *
     * @param r a reference to a path object to be used as the depend
     *      classpath
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Flag to set to true if you want dependency issues with RMI
     * stubs to appear at warning level.
     * @param warnOnRmiStubs if true set dependency issues to appear at warning level.
     * @since Ant1.7
     */
    public void setWarnOnRmiStubs(boolean warnOnRmiStubs) {
        this.warnOnRmiStubs = warnOnRmiStubs;
    }

    /**
     * Read the dependencies from cache file
     *
     * @return a collection of class dependencies
     * @exception IOException if the dependency file cannot be read
     */
    private Hashtable readCachedDependencies(File depFile) throws IOException {
        Hashtable dependencyMap = new Hashtable();

        BufferedReader in = null;
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
                } else {
                    dependencyList.addElement(line);
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return dependencyMap;
    }

    /**
     * Write the dependencies to cache file
     *
     * @param dependencyMap the map of dependencies to be written out.
     * @exception IOException if the dependency file cannot be written out.
     */
    private void writeCachedDependencies(Hashtable dependencyMap)
         throws IOException {
        if (cache != null) {
            PrintWriter pw = null;
            try {
                cache.mkdirs();
                File depFile = new File(cache, CACHE_FILE_NAME);

                pw = new PrintWriter(new FileWriter(depFile));
                Enumeration e = dependencyMap.keys();
                while (e.hasMoreElements()) {
                    String className = (String) e.nextElement();

                    pw.println(CLASSNAME_PREPEND + className);

                    Vector dependencyList
                         = (Vector) dependencyMap.get(className);
                    int size = dependencyList.size();
                    for (int x = 0; x < size; x++) {
                        pw.println(dependencyList.elementAt(x));
                    }
                }
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
        }
    }

    /**
     * Get the classpath for dependency checking.
     *
     * This method removes the dest dirs if it is given from the dependency classpath
     */
    private Path getCheckClassPath() {
        if (dependClasspath == null) {
            return null;
        }

        String[] destPathElements = destPath.list();
        String[] classpathElements = dependClasspath.list();
        String checkPath = "";
        for (int i = 0; i < classpathElements.length; ++i) {
            String element = classpathElements[i];
            boolean inDestPath = false;
            for (int j = 0; j < destPathElements.length && !inDestPath; ++j) {
                inDestPath = destPathElements[j].equals(element);
            }
            if (!inDestPath) {
                if (checkPath.length() == 0) {
                    checkPath = element;
                } else {
                    checkPath += ":" + element;
                }
            }
        }

        if (checkPath.length() == 0) {
            return null;
        }

        return new Path(getProject(), checkPath);
    }

    /**
     * Determine the dependencies between classes. Class dependencies are
     * determined by examining the class references in a class file to other
     * classes.
     *
     * This method sets up the following fields
     * <ul>
     *   <li>affectedClassMap - the list of classes each class affects</li>
     *   <li>classFileInfoMap - information about each class</li>
     *   <li>classpathDependencies - the list of jars and classes from the
     *                             classpath that each class depends upon.</li>
     * </ul>
     *
     * If required, the dependencies are written to the cache.
     *
     * @exception IOException if either the dependencies cache or the class
     *      files cannot be read or written
     */
    private void determineDependencies() throws IOException {
        affectedClassMap = new Hashtable();
        classFileInfoMap = new Hashtable();
        boolean cacheDirty = false;

        Hashtable dependencyMap = new Hashtable();
        File cacheFile = null;
        boolean cacheFileExists = true;
        long cacheLastModified = Long.MAX_VALUE;

        // read the dependency cache from the disk
        if (cache != null) {
            cacheFile = new File(cache, CACHE_FILE_NAME);
            cacheFileExists = cacheFile.exists();
            cacheLastModified = cacheFile.lastModified();
            if (cacheFileExists) {
                dependencyMap = readCachedDependencies(cacheFile);
            }
        }
        Enumeration classfileEnum = getClassFiles(destPath).elements();
        while (classfileEnum.hasMoreElements()) {
            ClassFileInfo info = (ClassFileInfo) classfileEnum.nextElement();
            log("Adding class info for " + info.className, Project.MSG_DEBUG);
            classFileInfoMap.put(info.className, info);

            Vector dependencyList = null;

            if (cache != null) {
                // try to read the dependency info from the map if it is
                // not out of date
                if (cacheFileExists
                    && cacheLastModified > info.absoluteFile.lastModified()) {
                    // depFile exists and is newer than the class file
                    // need to get dependency list from the map.
                    dependencyList = (Vector) dependencyMap.get(info.className);
                }
            }

            if (dependencyList == null) {
                // not cached - so need to read directly from the class file
                DependencyAnalyzer analyzer = new AntAnalyzer();
                analyzer.addRootClass(info.className);
                analyzer.addClassPath(destPath);
                analyzer.setClosure(false);
                dependencyList = new Vector();
                Enumeration depEnum = analyzer.getClassDependencies();
                while (depEnum.hasMoreElements()) {
                    dependencyList.addElement(depEnum.nextElement());
                }
                cacheDirty = true;
                dependencyMap.put(info.className, dependencyList);
            }

            // This class depends on each class in the dependency list. For each
            // one of those, add this class into their affected classes list
            Enumeration depEnum = dependencyList.elements();
            while (depEnum.hasMoreElements()) {
                String dependentClass = (String) depEnum.nextElement();

                Hashtable affectedClasses
                    = (Hashtable) affectedClassMap.get(dependentClass);
                if (affectedClasses == null) {
                    affectedClasses = new Hashtable();
                    affectedClassMap.put(dependentClass, affectedClasses);
                }

                affectedClasses.put(info.className, info);
            }
        }

        classpathDependencies = null;
        Path checkPath = getCheckClassPath();
        if (checkPath != null) {
            // now determine which jars each class depends upon
            classpathDependencies = new Hashtable();
            AntClassLoader loader = getProject().createClassLoader(checkPath);

            Hashtable classpathFileCache = new Hashtable();
            Object nullFileMarker = new Object();
            for (Enumeration e = dependencyMap.keys(); e.hasMoreElements();) {
                String className = (String) e.nextElement();
                Vector dependencyList = (Vector) dependencyMap.get(className);
                Hashtable dependencies = new Hashtable();
                classpathDependencies.put(className, dependencies);
                Enumeration e2 = dependencyList.elements();
                while (e2.hasMoreElements()) {
                    String dependency = (String) e2.nextElement();
                    Object classpathFileObject
                        = classpathFileCache.get(dependency);
                    if (classpathFileObject == null) {
                        classpathFileObject = nullFileMarker;

                        if (!dependency.startsWith("java.")
                            && !dependency.startsWith("javax.")) {
                            URL classURL
                                = loader.getResource(dependency.replace('.', '/') + ".class");
                            if (classURL != null) {
                                if (classURL.getProtocol().equals("jar")) {
                                    String jarFilePath = classURL.getFile();
                                    int classMarker = jarFilePath.indexOf('!');
                                    jarFilePath = jarFilePath.substring(0, classMarker);
                                    if (jarFilePath.startsWith("file:")) {
                                        classpathFileObject = new File(
                                            FileUtils.getFileUtils().fromURI(jarFilePath));
                                    } else {
                                        throw new IOException(
                                            "Bizarre nested path in jar: protocol: "
                                            + jarFilePath);
                                    }
                                } else if (classURL.getProtocol().equals("file")) {
                                    classpathFileObject = new File(
                                        FileUtils.getFileUtils()
                                        .fromURI(classURL.toExternalForm()));
                                }
                                log("Class " + className
                                    + " depends on " + classpathFileObject
                                    + " due to " + dependency, Project.MSG_DEBUG);
                            }
                        }
                        classpathFileCache.put(dependency, classpathFileObject);
                    }
                    if (classpathFileObject != nullFileMarker) {
                        // we need to add this jar to the list for this class.
                        File jarFile = (File) classpathFileObject;
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

    /**
     * Delete all the class files which are out of date, by way of their
     * dependency on a class which is out of date
     *
     * @return the number of files deleted.
     */
    private int deleteAllAffectedFiles() {
        int count = 0;
        for (Enumeration e = outOfDateClasses.elements(); e.hasMoreElements();) {
            String className = (String) e.nextElement();
            count += deleteAffectedFiles(className);
            ClassFileInfo classInfo
                = (ClassFileInfo) classFileInfoMap.get(className);
            if (classInfo != null && classInfo.absoluteFile.exists()) {
                classInfo.absoluteFile.delete();
                count++;
            }
        }
        return count;
    }

    /**
     * Delete all the class files of classes which depend on the given class
     *
     * @param className the name of the class whose dependent classes will be
     *      deleted
     * @return the number of class files removed
     */
    private int deleteAffectedFiles(String className) {
        int count = 0;

        Hashtable affectedClasses = (Hashtable) affectedClassMap.get(className);
        if (affectedClasses == null) {
            return count;
        }
        for (Enumeration e = affectedClasses.keys(); e.hasMoreElements();) {
            String affectedClass = (String) e.nextElement();
            ClassFileInfo affectedClassInfo
                = (ClassFileInfo) affectedClasses.get(affectedClass);

            if (!affectedClassInfo.absoluteFile.exists()) {
                continue;
            }

            if (affectedClassInfo.sourceFile == null) {
                warnOutOfDateButNotDeleted(affectedClassInfo, affectedClass, className);
                continue;
            }

            log("Deleting file " + affectedClassInfo.absoluteFile.getPath()
                + " since " + className + " out of date", Project.MSG_VERBOSE);

            affectedClassInfo.absoluteFile.delete();
            count++;
            if (closure) {
                count += deleteAffectedFiles(affectedClass);
            } else {
                // without closure we may delete an inner class but not the
                // top level class which would not trigger a recompile.

                if (affectedClass.indexOf("$") == -1) {
                    continue;
                }
                // need to delete the main class
                String topLevelClassName
                     = affectedClass.substring(0, affectedClass.indexOf("$"));
                log("Top level class = " + topLevelClassName,
                    Project.MSG_VERBOSE);
                ClassFileInfo topLevelClassInfo
                     = (ClassFileInfo) classFileInfoMap.get(topLevelClassName);
                if (topLevelClassInfo != null
                    && topLevelClassInfo.absoluteFile.exists()) {
                    log("Deleting file "
                        + topLevelClassInfo.absoluteFile.getPath()
                        + " since one of its inner classes was removed",
                        Project.MSG_VERBOSE);
                    topLevelClassInfo.absoluteFile.delete();
                    count++;
                    if (closure) {
                        count += deleteAffectedFiles(topLevelClassName);
                    }
                }
            }
        }
        return count;
    }

    /**
     * warn when a class is out of date, but not deleted as its source is unknown.
     * MSG_WARN is the normal level, but we downgrade to MSG_VERBOSE for RMI files
     * if {@link #warnOnRmiStubs is false}
     * @param affectedClassInfo info about the affectd class
     * @param affectedClass the name of the affected .class file
     * @param className the file that is triggering the out of dateness
     */
    private void warnOutOfDateButNotDeleted(
            ClassFileInfo affectedClassInfo, String affectedClass,
            String className) {
        if (affectedClassInfo.isUserWarned) {
            return;
        }
        int level = Project.MSG_WARN;
        if (!warnOnRmiStubs) {
            //downgrade warnings on RMI stublike classes, as they are generated
            //by rmic, so there is no need to tell the user that their source is
            //missing.
            if (isRmiStub(affectedClass, className)) {
                level = Project.MSG_VERBOSE;
            }
        }
        log("The class " + affectedClass + " in file "
            + affectedClassInfo.absoluteFile.getPath()
            + " is out of date due to " + className
            + " but has not been deleted because its source file"
            + " could not be determined", level);
        affectedClassInfo.isUserWarned = true;
    }

    /**
     * test for being an RMI stub
     * @param affectedClass  class being tested
     * @param className      possible origin of the RMI stub
     * @return whether the class affectedClass is a RMI stub
     */
    private boolean isRmiStub(String affectedClass, String className) {
        return isStub(affectedClass, className, DefaultRmicAdapter.RMI_STUB_SUFFIX)
                || isStub(affectedClass, className, DefaultRmicAdapter.RMI_SKEL_SUFFIX)
                || isStub(affectedClass, className, WLRmic.RMI_STUB_SUFFIX)
                || isStub(affectedClass, className, WLRmic.RMI_SKEL_SUFFIX);
    }

    private boolean isStub(String affectedClass, String baseClass, String suffix) {
        return (baseClass + suffix).equals(affectedClass);
    }

    /**
     * Dump the dependency information loaded from the classes to the Ant log
     */
    private void dumpDependencies() {
        log("Reverse Dependency Dump for " + affectedClassMap.size()
            + " classes:", Project.MSG_DEBUG);

        Enumeration classEnum = affectedClassMap.keys();
        while (classEnum.hasMoreElements()) {
            String className = (String) classEnum.nextElement();
            log(" Class " + className + " affects:", Project.MSG_DEBUG);
            Hashtable affectedClasses
                = (Hashtable) affectedClassMap.get(className);
            Enumeration affectedClassEnum = affectedClasses.keys();
            while (affectedClassEnum.hasMoreElements()) {
                String affectedClass = (String) affectedClassEnum.nextElement();
                ClassFileInfo info
                    = (ClassFileInfo) affectedClasses.get(affectedClass);
                log("    " + affectedClass + " in "
                    + info.absoluteFile.getPath(), Project.MSG_DEBUG);
            }
        }

        if (classpathDependencies != null) {
            log("Classpath file dependencies (Forward):", Project.MSG_DEBUG);

            Enumeration classpathEnum = classpathDependencies.keys();
            while (classpathEnum.hasMoreElements()) {
                String className = (String) classpathEnum.nextElement();
                log(" Class " + className + " depends on:", Project.MSG_DEBUG);
                Hashtable dependencies
                    = (Hashtable) classpathDependencies.get(className);

                Enumeration classpathFileEnum = dependencies.elements();
                while (classpathFileEnum.hasMoreElements()) {
                    File classpathFile = (File) classpathFileEnum.nextElement();
                    log("    " + classpathFile.getPath(), Project.MSG_DEBUG);
                }
            }
        }
    }

    private void determineOutOfDateClasses() {
        outOfDateClasses = new Hashtable();
        for (int i = 0; i < srcPathList.length; i++) {
            File srcDir = getProject().resolveFile(srcPathList[i]);
            if (srcDir.exists()) {
                DirectoryScanner ds = this.getDirectoryScanner(srcDir);
                String[] files = ds.getIncludedFiles();
                scanDir(srcDir, files);
            }
        }

        // now check classpath file dependencies
        if (classpathDependencies == null) {
            return;
        }

        Enumeration classpathDepsEnum = classpathDependencies.keys();
        while (classpathDepsEnum.hasMoreElements()) {
            String className = (String) classpathDepsEnum.nextElement();
            if (outOfDateClasses.containsKey(className)) {
                continue;
            }
            ClassFileInfo info
                = (ClassFileInfo) classFileInfoMap.get(className);

            // if we have no info about the class - it may have been deleted already and we
            // are using cached info.
            if (info != null) {
                Hashtable dependencies
                    = (Hashtable) classpathDependencies.get(className);
                for (Enumeration e2 = dependencies.elements(); e2.hasMoreElements();) {
                    File classpathFile = (File) e2.nextElement();
                    if (classpathFile.lastModified()
                        > info.absoluteFile.lastModified()) {
                        log("Class " + className
                            + " is out of date with respect to "
                            + classpathFile, Project.MSG_DEBUG);
                        outOfDateClasses.put(className, className);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Does the work.
     *
     * @exception BuildException Thrown in case of an unrecoverable error.
     */
    public void execute() throws BuildException {
        try {
            long start = System.currentTimeMillis();
            if (srcPath == null) {
                throw new BuildException("srcdir attribute must be set",
                                         getLocation());
            }

            srcPathList = srcPath.list();
            if (srcPathList.length == 0) {
                throw new BuildException("srcdir attribute must be non-empty",
                                         getLocation());
            }

            if (destPath == null) {
                destPath = srcPath;
            }

            if (cache != null && cache.exists() && !cache.isDirectory()) {
                throw new BuildException("The cache, if specified, must "
                    + "point to a directory");
            }

            if (cache != null && !cache.exists()) {
                cache.mkdirs();
            }

            determineDependencies();
            if (dump) {
                dumpDependencies();
            }
            determineOutOfDateClasses();
            int count = deleteAllAffectedFiles();

            long duration = (System.currentTimeMillis() - start) / ONE_SECOND;

            final int summaryLogLevel;
            if (count > 0) {
                summaryLogLevel = Project.MSG_INFO;
            }  else {
                summaryLogLevel = Project.MSG_DEBUG;
            }

            log("Deleted " + count + " out of date files in "
                + duration + " seconds", summaryLogLevel);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Scans the directory looking for source files that are newer than
     * their class files. The results are returned in the class variable
     * compileList
     *
     * @param srcDir the source directory
     * @param files the names of the files in the source dir which are to be
     *      checked.
     */
    protected void scanDir(File srcDir, String[] files) {

        for (int i = 0; i < files.length; i++) {
            File srcFile = new File(srcDir, files[i]);
            if (files[i].endsWith(".java")) {
                String filePath = srcFile.getPath();
                String className
                    = filePath.substring(srcDir.getPath().length() + 1,
                        filePath.length() - ".java".length());
                className = ClassFileUtils.convertSlashName(className);
                ClassFileInfo info
                    = (ClassFileInfo) classFileInfoMap.get(className);
                if (info == null) {
                    // there was no class file. add this class to the list
                    outOfDateClasses.put(className, className);
                } else {
                    if (srcFile.lastModified()
                        > info.absoluteFile.lastModified()) {
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
     *      where classes can be found.
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
     * Find the source file for a given class
     *
     * @param classname the classname in slash format.
     * @param sourceFileKnownToExist if not null, a file already known to exist
     *                               (saves call to .exists())
     */
    private File findSourceFile(String classname, File sourceFileKnownToExist) {
        String sourceFilename;
        int innerIndex = classname.indexOf("$");
        if (innerIndex != -1) {
            sourceFilename = classname.substring(0, innerIndex) + ".java";
        } else {
            sourceFilename = classname + ".java";
        }

        // search the various source path entries
        for (int i = 0; i < srcPathList.length; ++i) {
            File sourceFile = new File(srcPathList[i], sourceFilename);
            if (sourceFile.equals(sourceFileKnownToExist) || sourceFile.exists()) {
                return sourceFile;
            }
        }
        return null;
    }

    /**
     * Add the list of class files from the given directory to the class
     * file vector, including any subdirectories.
     *
     * @param classFileList a list of ClassFileInfo objects for all the
     *      files in the directory tree
     * @param dir the directory tree to be searched, recursively, for class
     *      files
     * @param root the root of the source tree. This is used to determine
     *      the absolute class name from the relative position in the
     *      source tree
     */
    private void addClassFiles(Vector classFileList, File dir, File root) {
        String[] filesInDir = dir.list();

        if (filesInDir == null) {
            return;
        }
        int length = filesInDir.length;

        int rootLength = root.getPath().length();
        File sourceFileKnownToExist = null; // speed optimization
        for (int i = 0; i < length; ++i) {
            File file = new File(dir, filesInDir[i]);
            if (filesInDir[i].endsWith(".class")) {
                ClassFileInfo info = new ClassFileInfo();
                info.absoluteFile = file;
                String relativeName = file.getPath().substring(
                    rootLength + 1,
                    file.getPath().length() - ".class".length());
                info.className
                    = ClassFileUtils.convertSlashName(relativeName);
                info.sourceFile = sourceFileKnownToExist = findSourceFile(
                    relativeName, sourceFileKnownToExist);
                classFileList.addElement(info);
            } else {
                addClassFiles(classFileList, file, root);
            }
        }
    }


    /**
     * Set the directories path to find the Java source files.
     *
     * @param srcPath the source path
     */
    public void setSrcdir(Path srcPath) {
        this.srcPath = srcPath;
    }

    /**
     * Set the destination directory where the compiled Java files exist.
     *
     * @param destPath the destination areas where build files are written
     */
    public void setDestDir(Path destPath) {
        this.destPath = destPath;
    }

    /**
     * Sets the dependency cache file.
     *
     * @param cache the dependency cache file
     */
    public void setCache(File cache) {
        this.cache = cache;
    }

    /**
     * If true, transitive dependencies are followed until the
     * closure of the dependency set if reached.
     * When not set, the depend task will only follow
     * direct dependencies between classes.
     *
     * @param closure indicate if dependency closure is required.
     */
    public void setClosure(boolean closure) {
        this.closure = closure;
    }

    /**
     * If true, the dependency information will be written
     * to the debug level log.
     *
     * @param dump set to true to dump dependency information to the log
     */
    public void setDump(boolean dump) {
        this.dump = dump;
    }
}

