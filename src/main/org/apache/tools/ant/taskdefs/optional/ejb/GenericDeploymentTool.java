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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import javax.xml.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

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
     * The task to which this tool belongs.
     */
    private Task task;
    
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
     * Configure this tool for use in the ejbjar task.
     */
    public void configure(File srcDir, File descriptorDir, String baseNameTerminator, 
                          String baseJarName, boolean flatDestDir) {
        this.srcDir = srcDir;
        this.descriptorDir = descriptorDir;
        this.baseJarName = baseJarName;
        this.baseNameTerminator = baseNameTerminator;
        this.flatDestDir = flatDestDir;
    }

    /**
     * Utility method that encapsulates the logic of adding a file entry to
     * a .jar file.  Used by execute() to add entries to the jar file as it is
     * constructed.
     * @param jStream A JarOutputStream into which to write the
     *        jar entry.
     * @param iStream A FileInputStream from which to read the
     *        contents the file being added.
     * @param filename A String representing the name, including
     *        all relevant path information, that should be stored for the entry
     *        being added.
     */
    protected void addFileToJar(JarOutputStream jStream,
                                FileInputStream iStream,
                                String          filename)
        throws BuildException {
        try {
            // Create the zip entry and add it to the jar file
            ZipEntry zipEntry = new ZipEntry(filename);
            jStream.putNextEntry(zipEntry);
            
            // Create the file input stream, and buffer everything over
            // to the jar output stream
            byte[] byteBuffer = new byte[2 * 1024];
            int count = 0;
            do {
                jStream.write(byteBuffer, 0, count);
                count = iStream.read(byteBuffer, 0, byteBuffer.length);
            } while (count != -1);
            
            // Close up the file input stream for the class file
            iStream.close();
        }
        catch (IOException ioe) {
            String msg = "IOException while adding entry "
                         + filename + "to jarfile."
                         + ioe.getMessage();
            throw new BuildException(msg, ioe);
        }
    }

    protected DescriptorHandler getDescriptorHandler(File srcDir) {
        return new DescriptorHandler(srcDir);
    }
    
    public void processDescriptor(String descriptorFileName, SAXParser saxParser) {
        try {
            DescriptorHandler handler = getDescriptorHandler(srcDir);
            
            /* Parse the ejb deployment descriptor.  While it may not
             * look like much, we use a SAXParser and an inner class to
             * get hold of all the classfile names for the descriptor.
             */
            saxParser.parse(new InputSource
                            (new FileInputStream
                            (new File(getDescriptorDir(), descriptorFileName))),
                            handler);
                            
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
                         
            addVendorFiles(ejbFiles, baseName);

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
                }
            }
            
            // Check to see if we need a build and start
            // doing the work!
            if (needBuild) {
                // Log that we are going to build...
                getTask().log( "building "
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
                getTask().log(jarFile.toString() + " is up to date.",
                              Project.MSG_INFO);
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
    }
    
    /**
     * Add any vendor specific files which should be included in the 
     * EJB Jar.
     */
    protected void addVendorFiles(Hashtable ejbFiles, String baseName) {
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
        Iterator entryIterator = null;
        String entryName = null;
        File entryFile = null;
	File entryDir = null;
	String innerfiles[] = null;

        try {
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
            entryIterator = files.keySet().iterator();
            while (entryIterator.hasNext()) {
                entryName = (String) entryIterator.next();
                entryFile = (File) files.get(entryName);
                
                getTask().log("adding file '" + entryName + "'",
                              Project.MSG_VERBOSE);

                addFileToJar(jarStream,
                             new FileInputStream(entryFile),
                             entryName);

		// See if there are any inner classes for this class and add them in if there are
		InnerClassFilenameFilter flt = new InnerClassFilenameFilter(entryFile.getName());
		entryDir = entryFile.getParentFile();
		innerfiles = entryDir.list(flt);
		for (int i=0, n=innerfiles.length; i < n; i++) {
	
			//get and clean up innerclass name
			entryName = entryName.substring(0, entryName.lastIndexOf(entryFile.getName())-1) + File.separatorChar + innerfiles[i];

			// link the file
			entryFile = new File(srcDir, entryName);

			getTask().log("adding innerclass file '" + entryName + "'", 
				    Project.MSG_VERBOSE);

			addFileToJar(jarStream,
                                     new FileInputStream(entryFile),
                                     entryName);

		}
            }
            // All done.  Close the jar stream.
            jarStream.close();
        }
        catch(IOException ioe) {
            String msg = "IOException while processing ejb-jar file '"
                + jarfile.toString()
                + "'. Details: "
                + ioe.getMessage();
            throw new BuildException(msg, ioe);
        }
    } // end of writeJar

 

    

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
