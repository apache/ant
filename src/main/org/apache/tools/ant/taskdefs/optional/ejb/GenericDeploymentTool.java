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
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.net.*;

import javax.xml.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

/**
 * A deployment tool which creates generic EJB jars. Generic jars contains
 * only those classes and META-INF entries specified in the EJB 1.1 standard
 *
 * This class is also used as a framework for the creation of vendor specific
 * deployment tools. A number of template methods are provided through which the
 * vendor specific tool can hook into the EJB creation process.
 */
public class GenericDeploymentTool implements EJBDeploymentTool {
    /** Private constants that are used when constructing the standard jarfile */
    protected static final String META_DIR  = "META-INF/";
    protected static final String EJB_DD    = "ejb-jar.xml";

    /** Stores a handle to the directory of the source tree */
    private File srcDir;

    /** Stores a handle to the directory of the deployment descriptors */
    private File descriptorDir;

    /** Stores a handle to the directory to put the Jar files in */
    private File destDir;
    
    /** Instance variable that stores the jar file name when not using the naming standard */
    private String baseJarName;

    /** The classpath to use with this deployment tool. */
    private Path classpath;

    /**
     * Instance variable that determines whether to use a package structure
     * of a flat directory as the destination for the jar files.
     */
    private boolean flatDestDir = false;
    
    /** Instance variable that marks the end of the 'basename' */
    private String baseNameTerminator = "-";

    /** Instance variable that stores the suffix for the generated jarfile. */
    private String genericJarSuffix = "-generic.jar";

    /**
     * The task to which this tool belongs. This is used to access services provided
     * by the ant core, such as logging.
     */
    private Task task;
    
    /**
     * The classloader generated from the given classpath to load
     * the super classes and super interfaces.
     */
    private ClassLoader classpathLoader = null;
    
     /**
     * List of files have been loaded into the EJB jar
     */
    private List addedfiles;

    /**
     * Setter used to store the value of destination directory prior to execute()
     * being called.
     * @param inDir the destination directory.
     */
    public void setDestdir(File inDir) {
        this.destDir = inDir;
    }

    /**
     * Get the desitination directory.
     */
    protected File getDestDir() {
        return destDir;
    }
    

    /**
     * Set the task which owns this tool
     */
    public void setTask(Task task) {
        this.task = task;
    }
       
    /**
     * Get the task for this tool.
     */
    protected Task getTask() {
        return task;
    }

    /**
     * Get the basename terminator.
     */
    protected String getBaseNameTerminator() {
        return baseNameTerminator;
    }
    
    /**
     * Get the base jar name.
     */
    protected String getBaseJarName() {
        return baseJarName;
    }
    
    /**
     * Get the source dir.
     */
    protected File getSrcDir() {
        return srcDir;
    }
    
    /**
     * Get the meta-inf dir.
     * 
     */
    protected File getDescriptorDir() {
        return descriptorDir;
    }

    /**
     * Returns true, if the meta-inf dir is being explicitly set, false otherwise.
     */
    protected boolean usingBaseJarName() {
        return baseJarName != null;
    }
    
    /**
     * Setter used to store the suffix for the generated jar file.
     * @param inString the string to use as the suffix.
     */
    public void setGenericJarSuffix(String inString) {
        this.genericJarSuffix = inString;
    }

    /**
     * creates a nested classpath element.
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(task.getProject());
        }
        return classpath.createPath();
    }

    /**
     * Set the classpath to be used for this compilation.
     */
    public void setClasspath(Path classpath) {
        this.classpath = classpath;
    }

    protected Path getClasspath() {
        return classpath;
    }
    
    protected void log(String message, int level) {
        getTask().log(message, level);
    }


    /**
     * Configure this tool for use in the ejbjar task.
     */
    public void configure(File srcDir, File descriptorDir, String baseNameTerminator, 
                          String baseJarName, boolean flatDestDir, Path classpath) {
        this.srcDir = srcDir;
        this.descriptorDir = descriptorDir;
        this.baseJarName = baseJarName;
        this.baseNameTerminator = baseNameTerminator;
        this.flatDestDir = flatDestDir;
        if (this.classpath != null) {
            this.classpath.append(classpath);
        }
        else {
            this.classpath = classpath;
        }
        classpathLoader = null;
    }

    /**
     * Utility method that encapsulates the logic of adding a file entry to
     * a .jar file.  Used by execute() to add entries to the jar file as it is
     * constructed.
     * @param jStream A JarOutputStream into which to write the
     *        jar entry.
     * @param inputFile A File from which to read the
     *        contents the file being added.
     * @param logicalFilename A String representing the name, including
     *        all relevant path information, that should be stored for the entry
     *        being added.
     */
    protected void addFileToJar(JarOutputStream jStream,
                                File inputFile,
                                String logicalFilename)
        throws BuildException {
        FileInputStream iStream = null;
        try {
            if (!addedfiles.contains(logicalFilename)) {
                iStream = new FileInputStream(inputFile);
                // Create the zip entry and add it to the jar file
                ZipEntry zipEntry = new ZipEntry(logicalFilename);
                jStream.putNextEntry(zipEntry);
                   
                // Create the file input stream, and buffer everything over
                // to the jar output stream
                byte[] byteBuffer = new byte[2 * 1024];
                int count = 0;
                do {
                    jStream.write(byteBuffer, 0, count);
                    count = iStream.read(byteBuffer, 0, byteBuffer.length);
                } while (count != -1);
                
                //add it to list of files in jar
                addedfiles.add(logicalFilename);
           }       
        }
        catch (IOException ioe) {
            String msg = "IOException while adding entry "
                         + logicalFilename + " to jarfile from " + inputFile.getPath() + "."
                         + ioe.getMessage();
            throw new BuildException(msg, ioe);
        }
        finally {
            // Close up the file input stream for the class file
            if (iStream != null) {
                try {
                    iStream.close();
                }
                catch (IOException closeException) {}
            }
        }
    }

    protected DescriptorHandler getDescriptorHandler(File srcDir) {
        return new DescriptorHandler(srcDir);
    }
    
    public void processDescriptor(String descriptorFileName, SAXParser saxParser) {
        FileInputStream descriptorStream = null;

        try {
            DescriptorHandler handler = getDescriptorHandler(srcDir);
            
            /* Parse the ejb deployment descriptor.  While it may not
             * look like much, we use a SAXParser and an inner class to
             * get hold of all the classfile names for the descriptor.
             */
            descriptorStream = new FileInputStream(new File(getDescriptorDir(), descriptorFileName));
            saxParser.parse(new InputSource(descriptorStream), handler);
                            
            Hashtable ejbFiles = handler.getFiles();
            
            String baseName = "";
            
            // Work out what the base name is
            if (baseJarName != null) {
                baseName = baseJarName;
            } else {
                int lastSeparatorIndex = descriptorFileName.lastIndexOf(File.separator);
                int endBaseName = -1;
                if (lastSeparatorIndex != -1) {
                    endBaseName = descriptorFileName.indexOf(baseNameTerminator, 
                                                             lastSeparatorIndex);
                }
                else {
                    endBaseName = descriptorFileName.indexOf(baseNameTerminator);
                }

                if (endBaseName != -1) {
                    baseName = descriptorFileName.substring(0, endBaseName);
                }
                baseName = descriptorFileName.substring(0, endBaseName);
            }

            // First the regular deployment descriptor
            ejbFiles.put(META_DIR + EJB_DD,
                         new File(getDescriptorDir(), descriptorFileName));
            
            // now the vendor specific files, if any             
            addVendorFiles(ejbFiles, baseName);

            // add any inherited files
            checkAndAddInherited(ejbFiles);

            // Lastly create File object for the Jar files. If we are using
            // a flat destination dir, then we need to redefine baseName!
            if (flatDestDir && baseName.length() != 0) {
                int startName = baseName.lastIndexOf(File.separator);
                if (startName == -1) {
                    startName = 0;
                }
                
                int endName   = baseName.length();
                baseName = baseName.substring(startName, endName);
            }
            
            File jarFile = getVendorOutputJarFile(baseName);
            
            // By default we assume we need to build.
            boolean needBuild = true;

            if (jarFile.exists()) {
                long    lastBuild = jarFile.lastModified();
                Iterator fileIter = ejbFiles.values().iterator();
                // Set the need build to false until we find out otherwise.
                needBuild = false;

                // Loop through the files seeing if any has been touched
                // more recently than the destination jar.
                while( (needBuild == false) && (fileIter.hasNext()) ) {
                    File currentFile = (File) fileIter.next();
                    needBuild = ( lastBuild < currentFile.lastModified() );
                    if (needBuild) {
                        log("Build needed because " + currentFile.getPath() + " is out of date",
                            Project.MSG_VERBOSE);
                    }
                }
            }
            
            // Check to see if we need a build and start
            // doing the work!
            if (needBuild) {
                // Log that we are going to build...
                log( "building "
                              + jarFile.getName()
                              + " with "
                              + String.valueOf(ejbFiles.size())
                              + " files",
                              Project.MSG_INFO);
    
                // Use helper method to write the jarfile
                writeJar(baseName, jarFile, ejbFiles);

            }
            else {
                // Log that the file is up to date...
                log(jarFile.toString() + " is up to date.",
                              Project.MSG_VERBOSE);
            }

        }
        catch (SAXException se) {
            String msg = "SAXException while parsing '"
                + descriptorFileName.toString()
                + "'. This probably indicates badly-formed XML."
                + "  Details: "
                + se.getMessage();
            throw new BuildException(msg, se);
        }
        catch (IOException ioe) {
            String msg = "IOException while parsing'"
                + descriptorFileName.toString()
                + "'.  This probably indicates that the descriptor"
                + " doesn't exist. Details: "
                + ioe.getMessage();
            throw new BuildException(msg, ioe);
        }
        finally {
            if (descriptorStream != null) {
                try {
                    descriptorStream.close();
                }
                catch (IOException closeException) {}
            }
        }
    }
    
    /**
     * Add any vendor specific files which should be included in the 
     * EJB Jar.
     */
    protected void addVendorFiles(Hashtable ejbFiles, String baseName) {
        // nothing to add for generic tool.
    }


    /**
     * Get the vendor specific name of the Jar that will be output. The modification date
     * of this jar will be checked against the dependent bean classes.
     */
    File getVendorOutputJarFile(String baseName) {
        return new File(destDir, baseName + genericJarSuffix);
    }

    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over the
     * filenames/java.io.Files in the Hashtable stored on the instance variable
     * ejbFiles.
     */
    protected void writeJar(String baseName, File jarfile, Hashtable files) throws BuildException{

        JarOutputStream jarStream = null;
        try {
            // clean the addedfiles Vector 
            addedfiles = new ArrayList();

            /* If the jarfile already exists then whack it and recreate it.
             * Should probably think of a more elegant way to handle this
             * so that in case of errors we don't leave people worse off
             * than when we started =)
             */
            if (jarfile.exists()) {
                jarfile.delete();
            }
            jarfile.getParentFile().mkdirs();
            jarfile.createNewFile();
            
            // Create the streams necessary to write the jarfile
            
            jarStream = new JarOutputStream(new FileOutputStream(jarfile));
            jarStream.setMethod(JarOutputStream.DEFLATED);
            
            // Loop through all the class files found and add them to the jar
            for (Iterator entryIterator = files.keySet().iterator(); entryIterator.hasNext(); ) {
                String entryName = (String) entryIterator.next();
                File entryFile = (File) files.get(entryName);
                
                log("adding file '" + entryName + "'",
                              Project.MSG_VERBOSE);

                addFileToJar(jarStream, entryFile, entryName);

                // See if there are any inner classes for this class and add them in if there are
                InnerClassFilenameFilter flt = new InnerClassFilenameFilter(entryFile.getName());
                File entryDir = entryFile.getParentFile();
                String[] innerfiles = entryDir.list(flt);
                for (int i=0, n=innerfiles.length; i < n; i++) {
            
                    //get and clean up innerclass name
                    entryName = entryName.substring(0, entryName.lastIndexOf(entryFile.getName())-1) + File.separatorChar + innerfiles[i];
        
                    // link the file
                    entryFile = new File(srcDir, entryName);
        
                    log("adding innerclass file '" + entryName + "'", 
                            Project.MSG_VERBOSE);
        
                    addFileToJar(jarStream, entryFile, entryName);
        
                }
            }
        }
        catch(IOException ioe) {
            String msg = "IOException while processing ejb-jar file '"
                + jarfile.toString()
                + "'. Details: "
                + ioe.getMessage();
            throw new BuildException(msg, ioe);
        }
        finally {
            if (jarStream != null) {
                try {
                    jarStream.close();
                }
                catch (IOException closeException) {}
            }
        }
    } // end of writeJar

    /**
     * Check if a EJB Class Inherits from a Superclass, and if a Remote Interface
     * extends an interface other then javax.ejb.EJBObject directly.  Then add those 
     * classes to the generic-jar so they dont have to added elsewhere.
     *
     */
    protected void checkAndAddInherited(Hashtable checkEntries) throws BuildException
    {
        //Copy hashtable so were not changing the one we iterate through
        Hashtable copiedHash = (Hashtable)checkEntries.clone();

        // Walk base level EJBs and see if they have superclasses or extend extra interfaces which extend EJBObject
        for (Iterator entryIterator = copiedHash.keySet().iterator(); entryIterator.hasNext(); ) 
        {
            String entryName = (String)entryIterator.next();
            File entryFile = (File)copiedHash.get(entryName);

            // only want class files, xml doesnt reflect very well =)
            if (entryName.endsWith(".class"))
            {
                String classname = entryName.substring(0,entryName.lastIndexOf(".class")).replace(File.separatorChar,'.');
                ClassLoader loader = getClassLoaderForBuild();
                try {
                    Class c = loader.loadClass(classname);

                    // No primatives!!  sanity check, probably not nessesary
                    if (!c.isPrimitive())
                    {
                        if (c.isInterface()) //get as an interface
                        {
                            log("looking at interface " + c.getName(),  Project.MSG_VERBOSE);
                            Class[] interfaces = c.getInterfaces();
                            for (int i = 0; i < interfaces.length; i++){
                                log("     implements " + interfaces[i].getName(),  Project.MSG_VERBOSE);
                                addInterface(interfaces[i], checkEntries);
                            }
                        }
                        else  // get as a class
                        {
                            log("looking at class " + c.getName(),  Project.MSG_VERBOSE);
                            Class s = c.getSuperclass();
                            addSuperClass(c.getSuperclass(), checkEntries);
                        }
                    } //if primative
                }
                catch (ClassNotFoundException cnfe) {
                    log("Could not load class " + classname + " for super class check", 
                                  Project.MSG_WARN);
                }                            
            } //if 
        } // while 
    }

    private void addInterface(Class theInterface, Hashtable checkEntries) {
        if (!theInterface.getName().startsWith("java")) // do not add system interfaces
        { 
            File interfaceFile = new File(srcDir.getAbsolutePath() 
                                        + File.separatorChar 
                                        + theInterface.getName().replace('.',File.separatorChar)
                                        + ".class"
                                        );
            if (interfaceFile.exists() && interfaceFile.isFile())
            {
                checkEntries.put(theInterface.getName().replace('.',File.separatorChar)+".class",
                                 interfaceFile);
                Class[] superInterfaces = theInterface.getInterfaces();
                for (int i = 0; i < superInterfaces.length; i++) {
                    addInterface(superInterfaces[i], checkEntries);
                }
            }
        }
    }
     
    private void addSuperClass(Class superClass, Hashtable checkEntries) {
    
        if (!superClass.getName().startsWith("java"))
        {
            File superClassFile = new File(srcDir.getAbsolutePath() 
                                            + File.separatorChar 
                                            + superClass.getName().replace('.',File.separatorChar)
                                            + ".class");
            if (superClassFile.exists() && superClassFile.isFile())
            {
                checkEntries.put(superClass.getName().replace('.',File.separatorChar) + ".class", 
                                 superClassFile);
                
                // now need to get super classes and interfaces for this class
                Class[] superInterfaces = superClass.getInterfaces();
                for (int i = 0; i < superInterfaces.length; i++) {
                    addInterface(superInterfaces[i], checkEntries);
                }
                
                addSuperClass(superClass.getSuperclass(), checkEntries);
            }               
        }
    }
    
    /**
     * Returns a Classloader object which parses the passed in generic EjbJar classpath.
     * The loader is used to dynamically load classes from javax.ejb.* and the classes 
     * being added to the jar.
     *
     */ 
    protected ClassLoader getClassLoaderForBuild()
    {
        if (classpathLoader != null) {
            return classpathLoader;
        }
        
        // only generate a URLClassLoader if we have a classpath
        if (classpath == null) {
            classpathLoader = getClass().getClassLoader();
        }
        else {
            classpathLoader = new AntClassLoader(getTask().getProject(), classpath);
        }
        
        return classpathLoader;
    }
 
    /**
     * Called to validate that the tool parameters have been configured.
     *
     */
    public void validateConfigured() throws BuildException {
        if (destDir == null) {
            throw new BuildException("The destdir attribute must be specified");
        }
    }
}
