/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.ant.builder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * A helper class which allows the build files which have been converted to
 * code to be built.
 *
 * @author Conor MacNeill
 * @created 16 February 2002
 */
public class BuildHelper {

    /**
     * Simple data class for storing info about a fileset.
     *
     * @author Conor MacNeill
     * @created 18 February 2002
     */
    private static class FileSetInfo {
        /** The root directory of this fileset */
        private File root;
        /** the list of files in the file set */
        private File[] files;

    }


    /** The properties which have been defined in the build */
    private Map properties = new HashMap();

    /** Path objects created in the build */
    private Map paths = new HashMap();

    /** Filesets created in the build */
    private Map filesets = new HashMap();

    /** The targets which have been run */
    private Set runTargets = new HashSet();

    /**
     * Set a property for the build
     *
     * @param propertyName the name of the property
     * @param propertyValue the value of the property
     */
    protected void setProperty(String propertyName, String propertyValue) {
        if (!properties.containsKey(propertyName)) {
            String value = resolve(propertyValue);

            properties.put(propertyName, value);
        }
    }

    /**
     * Set the parent helper when creating a new build context
     *
     * @param parentHelper the parent helper
     */
    protected void setParent(BuildHelper parentHelper) {
        // grab the parent's properties
        Map parentProperties = parentHelper.properties;
        for (Iterator i = parentProperties.keySet().iterator(); i.hasNext();) {
            String propertyName = (String) i.next();
            String propertyValue = (String) parentProperties.get(propertyName);
            setProperty(propertyName, propertyValue);
        }
    }


    /**
     * Create a Jar
     *
     * @param basedir the base directpory from which files are added to the
     *      jar
     * @param metaInfDir the directory containing the META-INF for the jar
     * @param metaInfIncludes the files to be included in the META-INF area of
     *      the jar
     * @param jarFile the file in which the Jar is created
     * @param classpath Class-Path attribute in manifest
     * @param mainClass Main-Class attribute in manifest
     */
    protected void jar(String basedir, String jarFile, String metaInfDir,
                       String metaInfIncludes,
                       String classpath, String mainClass) {
        try {
            File base = new File(resolve(basedir));
            File jar = new File(resolve(jarFile));
            System.out.println("        [jar] Creating jar " + jar);

            Manifest manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.putValue("Manifest-Version", "1.0");
            attributes.putValue("Created-By", "Mutant Bootstrap");

            if (classpath != null) {
                attributes.putValue("Class-Path", classpath);
            }
            if (mainClass != null) {
                attributes.putValue("Main-Class", mainClass);
            }

            JarOutputStream jos
                 = new JarOutputStream(new FileOutputStream(jar), manifest);

            addToJar(jos, base, null);
            if (metaInfDir != null) {
                File[] metaFileSet = buildFileSet(metaInfDir, metaInfIncludes);

                addFilesToJar(jos, new File(resolve(metaInfDir)),
                    metaFileSet, "META-INF");
            }
            jos.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to Jar file");
        }
    }


    /**
     * Compile a set of files
     *
     * @param srcDir the source directory
     * @param destDir where the compiled classes will go
     * @param classpathRef the id of a path object with the classpath for the
     *      build
     */
    protected void javac(String srcDir, String destDir, String classpathRef) {
        List javaFiles = new ArrayList();
        String src = resolve(srcDir);
        StringTokenizer tokenizer = new StringTokenizer(src, ":");

        while (tokenizer.hasMoreTokens()) {
            File srcLocation = new File(tokenizer.nextToken());

            getJavaFiles(srcLocation, javaFiles);
        }

        File dest = new File(resolve(destDir));
        int numArgs = javaFiles.size() + 2;

        if (classpathRef != null) {
            numArgs += 2;
        }
        String[] args = new String[numArgs];
        int index = 0;

        args[index++] = "-d";
        args[index++] = dest.getPath();
        if (classpathRef != null) {
            String path = (String) paths.get(resolve(classpathRef));

            args[index++] = "-classpath";
            args[index++] = path;
        }
        for (Iterator i = javaFiles.iterator(); i.hasNext();) {
            args[index++] = ((File) i.next()).getPath();
        }

        // System.out.println("Javac Arguments");
        // for (int i = 0; i < args.length; ++i) {
        //     System.out.println("   " + args[i]);
        // }

        try {
            Class c = Class.forName("com.sun.tools.javac.Main");
            Object compiler = c.newInstance();
            Method compile = c.getMethod("compile",
                new Class[]{(new String[]{}).getClass()});

            compile.invoke(compiler, new Object[]{args});
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Compile failed");
        }
    }


    /**
     * Copy a directory
     *
     * @param fromDir the source directory name
     * @param toDir the destination directory name
     */
    protected void copyFileset(String fromDir, String toDir) {
        File from = new File(resolve(fromDir));
        File to = new File(resolve(toDir));

        copyDir(from, to);
    }


    /**
     * Add a fileset to this build helper
     *
     * @param name the name of the fileset (its id)
     * @param root the root directory of the fileset
     * @param files the files in the fileset
     */
    protected void addFileSet(String name, File root, File[] files) {
        FileSetInfo info = new FileSetInfo();

        info.root = root;
        info.files = files;
        filesets.put(name, info);
    }


    /**
     * Copy a fileset given a reference to the source fileset
     *
     * @param toDir the name of the destination directory
     * @param fileSetRef the fileset to be copied
     */
    protected void copyFilesetRef(String fileSetRef, String toDir) {
        FileSetInfo fileset = (FileSetInfo) filesets.get(resolve(fileSetRef));

        if (fileset != null) {
            File to = new File(resolve(toDir));

            copyFileList(fileset.root, fileset.files, to);
        }
    }


    /**
     * Make a directory
     *
     * @param dirName the name of the directory path to be created.
     */
    protected void mkdir(String dirName) {
        File dir = new File(resolve(dirName));

        dir.mkdirs();
    }


    /**
     * Create a path object
     *
     * @param pathName the name of the path object in the build
     */
    protected void createPath(String pathName) {
        String path = "";

        paths.put(pathName, path);
    }


    /**
     * Add a fileset to a path
     *
     * @param pathName the name of the path
     * @param filesetDir the base directory of the fileset
     * @param filesetIncludes the files to be included in the fileset
     */
    protected void addFileSetToPath(String pathName, String filesetDir,
                                    String filesetIncludes) {
        File[] files = buildFileSet(filesetDir, filesetIncludes);
        String currentPath = (String) paths.get(pathName);

        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                if (currentPath == null || currentPath.length() == 0) {
                    currentPath = files[i].getPath();
                } else {
                    currentPath = currentPath + File.pathSeparator
                         + files[i].getPath();
                }
            }
        }
        paths.put(pathName, currentPath);
    }


    /**
     * Add a new element to a path
     *
     * @param pathName the name of the path object to be updated
     * @param location the location to be added to the path
     */
    protected void addPathElementToPath(String pathName, String location) {
        String pathElement = resolve(location).replace('/', File.separatorChar);
        String currentPath = (String) paths.get(pathName);

        if (currentPath == null || currentPath.length() == 0) {
            currentPath = pathElement;
        } else {
            currentPath = currentPath + File.pathSeparator + pathElement;
        }
        paths.put(pathName, currentPath);
    }


    /**
     * Add an existing path to another path
     *
     * @param pathName the name of the path to which the path is to be added
     * @param pathNameToAdd the name of the path to be added.
     */
    protected void addPathToPath(String pathName, String pathNameToAdd) {
        String pathToAdd = (String) paths.get(pathNameToAdd);

        if (pathToAdd == null || pathToAdd.length() == 0) {
            return;
        }

        String currentPath = (String) paths.get(pathName);

        if (currentPath == null || currentPath.length() == 0) {
            currentPath = pathToAdd;
        } else {
            currentPath = currentPath + File.pathSeparator + pathToAdd;
        }
        paths.put(pathName, currentPath);
    }


    /**
     * Get the set of Java files to be compiled
     *
     * @param srcDir the directory to search (recursively searched)
     * @param javaFiles the list of files to which Java files are added
     */
    private void getJavaFiles(File srcDir, List javaFiles) {
        File[] files = srcDir.listFiles();

        for (int i = 0; i < files.length; ++i) {
            if (files[i].isDirectory()) {
                getJavaFiles(files[i], javaFiles);
            } else if (files[i].getPath().endsWith(".java")) {
                javaFiles.add(files[i]);
            }
        }
    }


    /**
     * Copy a file
     *
     * @param from the source location
     * @param dest the destination location
     */
    private void copyFile(File from, File dest) {
        if (from.exists()) {
            dest.getParentFile().mkdirs();
            try {
                FileInputStream in = new FileInputStream(from);
                FileOutputStream out = new FileOutputStream(dest);
                byte[] buf = new byte[1024 * 16];
                int count = 0;

                count = in.read(buf, 0, buf.length);
                while (count != -1) {
                    out.write(buf, 0, count);
                    count = in.read(buf, 0, buf.length);
                }

                in.close();
                out.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw new RuntimeException("Unable to copy files");
            }
        }
    }


    /**
     * Copy a list of files from one directory to another, preserving the
     * relative paths
     *
     * @param root the root of the source directory
     * @param files the files to be copied
     * @param to the destination directory
     */
    private void copyFileList(File root, File[] files, File to) {
        for (int i = 0; i < files.length; ++i) {
            if (files[i].getName().equals("CVS")) {
                continue;
            }
            String name
                 = files[i].getPath().substring(root.getPath().length() + 1);
            File dest = new File(to, name);

            if (files[i].isDirectory()) {
                copyDir(files[i], dest);
            } else {
                copyFile(files[i], dest);
            }
        }
    }


    /**
     * Copy a directory
     *
     * @param from the source directory
     * @param to the destination directory
     */
    private void copyDir(File from, File to) {
        to.mkdirs();

        File[] files = from.listFiles();

        copyFileList(from, files, to);
    }


    /**
     * Add a directory to a Jar
     *
     * @param jos the JarOutputStream representing the Jar being created
     * @param dir the directory to be added to the jar
     * @param prefix the prefix in the jar at which the directory is to be
     *      added
     * @exception IOException if the files cannot be added to the jar
     */
    private void addToJar(JarOutputStream jos, File dir, String prefix)
         throws IOException {
        File[] files = dir.listFiles();

        addFilesToJar(jos, dir, files, prefix);
    }


    /**
     * Add a set of files to a jar
     *
     * @param jos the JarOutputStream representing the Jar being created
     * @param dir the directory fro which the files are taken
     * @param prefix the prefix in the jar at which the directory is to be
     *      added
     * @param files the list of files to be added to the jar
     * @exception IOException if the files cannot be added to the jar
     */
    private void addFilesToJar(JarOutputStream jos, File dir,
                               File[] files, String prefix) throws IOException {
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getPath().replace('\\', '/');

            name = name.substring(dir.getPath().length() + 1);
            if (prefix != null) {
                name = prefix + "/" + name;
            }
            ZipEntry ze = new ZipEntry(name);

            jos.putNextEntry(ze);
            if (files[i].isDirectory()) {
                addToJar(jos, files[i], name);
            } else {
                FileInputStream fis = new FileInputStream(files[i]);
                int count = 0;
                byte[] buf = new byte[8 * 1024];

                count = fis.read(buf, 0, buf.length);
                while (count != -1) {
                    jos.write(buf, 0, count);
                    count = fis.read(buf, 0, buf.length);
                }
                fis.close();
            }
        }
    }


    /**
     * Build a simple fileset. Only simple inclusion filtering is supported -
     * no complicated patterns.
     *
     * @param filesetDir the base directory of the fileset
     * @param filesetIncludes the simple includes spec for the fileset
     * @return the fileset expressed as an array of File instances.
     */
    private File[] buildFileSet(String filesetDir, String filesetIncludes) {
        if (filesetDir == null) {
            return new File[0];
        }
        final String includes = resolve(filesetIncludes);

        if (includes.indexOf("**") != -1) {
            throw new RuntimeException("Simple fileset cannot handle ** "
                 + "style includes");
        }
        int index = 0;

        if (includes.charAt(0) == '*') {
            index = 1;
        }
        if (includes.indexOf("*", index) != -1) {
            throw new RuntimeException("Simple fileset cannot handle * "
                 + "style includes except at start");
        }

        File base = new File(resolve(filesetDir));

        return base.listFiles(
            new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (includes.startsWith("*")) {
                        return name.endsWith(includes.substring(1));
                    } else {
                        return name.equals(includes);
                    }
                }
            });
    }


    /**
     * Run a target in the build
     *
     * @param builder The builder object created from the original XML build
     *                file.
     * @param target The target to run.
     */
    private void runTarget(Object builder, String target) {
        try {
            // use reflection to get a method with the given name
            Method targetMethod
                 = builder.getClass().getDeclaredMethod(target,
                new Class[]{BuildHelper.class});
            targetMethod.invoke(builder, new Object[]{this});
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to run target \""
                 + target + "\"");
        }
    }

    /**
     * Run the dependencies of the given target.
     *
     * @param builder The builder object created from the original XML build
     *                file.
     * @param targetName the target whose dependencies should be run
     * @param depends the comma separated list of dependencies.
     */
    public void runDepends(Object builder, String targetName, String depends) {
        StringTokenizer tokenizer = new StringTokenizer(depends, ", ");
        while (tokenizer.hasMoreTokens()) {
            String target = tokenizer.nextToken();
            // has this target been run
            if (!runTargets.contains(target)) {
                runTarget(builder, target);
            }
        }
        runTargets.add(targetName);
    }

    /**
     * Resolve the property references in a string
     *
     * @param propertyValue the string to be resolved
     * @return the string with property references replaced by their current
     *      value.
     */
    protected String resolve(String propertyValue) {
        String newValue = propertyValue;

        while (newValue.indexOf("${") != -1) {
            int index = newValue.indexOf("${");
            int endIndex = newValue.indexOf("}", index);
            String propertyName = newValue.substring(index + 2, endIndex);
            String repValue = (String) properties.get(propertyName);

            newValue = newValue.substring(0, index) +
                repValue + newValue.substring(endIndex + 1);
        }
        return newValue;
    }
}

