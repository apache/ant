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
import java.util.jar.*;
import java.util.*;
import java.net.*;

import javax.xml.parsers.*;
import org.xml.sax.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.taskdefs.Java;

public class WeblogicDeploymentTool extends GenericDeploymentTool {
    public static final String PUBLICID_EJB11
        = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
    public static final String PUBLICID_EJB20
        = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";
    public static final String PUBLICID_WEBLOGIC_EJB510
        = "-//BEA Systems, Inc.//DTD WebLogic 5.1.0 EJB//EN";
    public static final String PUBLICID_WEBLOGIC_EJB600
        = "-//BEA Systems, Inc.//DTD WebLogic 6.0.0 EJB//EN";

    protected static final String DEFAULT_WL51_EJB11_DTD_LOCATION 
        = "/weblogic/ejb/deployment/xml/ejb-jar.dtd";
    protected static final String DEFAULT_WL60_EJB11_DTD_LOCATION 
        = "/weblogic/ejb20/dd/xml/ejb11-jar.dtd";
    protected static final String DEFAULT_WL60_EJB20_DTD_LOCATION 
        = "/weblogic/ejb20/dd/xml/ejb20-jar.dtd";

    protected static final String DEFAULT_WL51_DTD_LOCATION 
        = "/weblogic/ejb/deployment/xml/weblogic-ejb-jar.dtd";
    protected static final String DEFAULT_WL60_51_DTD_LOCATION 
        = "/weblogic/ejb20/dd/xml/weblogic510-ejb-jar.dtd";
    protected static final String DEFAULT_WL60_DTD_LOCATION 
        = "/weblogic/ejb20/dd/xml/weblogic600-ejb-jar.dtd";

    protected static final String DEFAULT_COMPILER = "default"; 
    
    protected static final String WL_DD = "weblogic-ejb-jar.xml";
    protected static final String WL_CMP_DD = "weblogic-cmp-rdbms-jar.xml";

    protected static final String COMPILER_EJB11 = "weblogic.ejbc";
    protected static final String COMPILER_EJB20 = "weblogic.ejbc20";
    
    /** Instance variable that stores the suffix for the weblogic jarfile. */
    private String jarSuffix = ".jar";

    /** Instance variable that stores the location of the weblogic DTD file. */
    private String weblogicDTD;

    /** Instance variable that stores the location of the ejb 1.1 DTD file. */
    private String ejb11DTD;
        
    /** Instance variable that determines whether generic ejb jars are kept. */
    private boolean keepgenerated = false;

    /** Instance variable that stores the fully qualified classname of the weblogic EJBC compiler */
    private String ejbcClass = null;

    private String additionalArgs = "";

    private boolean keepGeneric = false;

    private String compiler = null;

    private boolean alwaysRebuild = true;

    /** controls whether ejbc is run on the generated jar */
    private boolean noEJBC = false;
    
    /**
     * Indicates if the old CMP location convention is to be used.
     */
    private boolean newCMP = false;

    /** The classpath to the weblogic classes. */
    private Path wlClasspath = null;

    /**
     * Get the classpath to the weblogic classpaths
     */
    public Path createWLClasspath() {
        if (wlClasspath == null) {
            wlClasspath = new Path(getTask().getProject());
        }
        return wlClasspath.createPath();
    }
    
    public void setWLClasspath(Path wlClasspath) {
        this.wlClasspath = wlClasspath;
    }

    /**
     * The compiler (switch <code>-compiler</code>) to use
     */
    public void setCompiler(String compiler) {
        this.compiler = compiler;
    }
    
    /**
     * Set the rebuild flag to false to only update changes in the
     * jar rather than rerunning ejbc
     */
    public void setRebuild(boolean rebuild) {
        this.alwaysRebuild = rebuild;
    }
    

    /**
     * Setter used to store the suffix for the generated weblogic jar file.
     * @param inString the string to use as the suffix.
     */
    public void setSuffix(String inString) {
        this.jarSuffix = inString;
    }

    /**
     * Setter used to store the value of keepGeneric
     * @param inValue a string, either 'true' or 'false'.
     */
    public void setKeepgeneric(boolean inValue) {
        this.keepGeneric = inValue;
    }

    /**
     * Sets whether -keepgenerated is passed to ejbc (that is,
     * the .java source files are kept).
     * @param inValue either 'true' or 'false'
     */
    public void setKeepgenerated(String inValue) {
        this.keepgenerated = Boolean.valueOf(inValue).booleanValue();
    }

    /**
     * sets some additional args to send to ejbc.
     */
    public void setArgs(String args) {
        this.additionalArgs = args;
    }
    
    /**
     * Set the classname of the ejbc compiler
     */
    public void setEjbcClass(String ejbcClass)
    {
        this.ejbcClass = ejbcClass;
    }
    
    /**
     * Get the ejbc compiler class
     */
    public String getEjbcClass()
    {
        return ejbcClass;
    }
     
   
    /**
     * Setter used to store the location of the ejb-jar DTD. This can be a file on the system 
     * or a resource on the classpath. 
     * @param inString the string to use as the DTD location.
     */
    public void setWeblogicdtd(String inString) {
        setEJBdtd(inString);
    }

    /**
     * Setter used to store the location of the weblogic DTD. This can be a file on the system 
     * or a resource on the classpath. 
     * @param inString the string to use as the DTD location.
     */
    public void setWLdtd(String inString) {
        this.weblogicDTD = inString;
    }

    /**
     * Setter used to store the location of the Sun's Generic EJB DTD. 
     * This can be a file on the system or a resource on the classpath. 
     * @param inString the string to use as the DTD location.
     */
    public void setEJBdtd(String inString) {
        this.ejb11DTD = inString;
    }

    /**
     * Set the value of the oldCMP scheme. This is an antonym for
     * newCMP
     */
    public void setOldCMP(boolean oldCMP) {
        this.newCMP = !oldCMP;
    }
    
    /**
     * Set the value of the newCMP scheme. The old CMP scheme locates the 
     * weblogic CMP descriptor based on the naming convention where the 
     * weblogic CMP file is expected to be named with the bean name as the prefix.
     * 
     * Under this scheme the name of the CMP descriptor does not match the name
     * actually used in the main weblogic EJB descriptor. Also, descriptors which 
     * contain multiple CMP references could not be used.
     *
     */
    public void setNewCMP(boolean newCMP) {
        this.newCMP = newCMP;
    }

    /**
     * Do not EJBC the jar after it has been put together.
     *
     */
    public void setNoEJBC(boolean noEJBC) {
        this.noEJBC = noEJBC;
    }
   
    protected void registerKnownDTDs(DescriptorHandler handler) {
        // register all the known DTDs
        handler.registerDTD(PUBLICID_EJB11, DEFAULT_WL51_EJB11_DTD_LOCATION);
        handler.registerDTD(PUBLICID_EJB11, DEFAULT_WL60_EJB11_DTD_LOCATION);
        handler.registerDTD(PUBLICID_EJB11, ejb11DTD);
        handler.registerDTD(PUBLICID_EJB20, DEFAULT_WL60_EJB20_DTD_LOCATION);
    }

    protected DescriptorHandler getWeblogicDescriptorHandler(final File srcDir) {
        DescriptorHandler handler = 
            new DescriptorHandler(getTask(), srcDir) {        
                protected void processElement() {
                    if (currentElement.equals("type-storage")) {
                        // Get the filename of vendor specific descriptor
                        String fileNameWithMETA = currentText;
                        //trim the META_INF\ off of the file name
                        String fileName = fileNameWithMETA.substring(META_DIR.length(), 
                                                                     fileNameWithMETA.length() );
                        File descriptorFile = new File(srcDir, fileName);
                        
                        ejbFiles.put(fileNameWithMETA, descriptorFile);
                    }
                }
            };

        handler.registerDTD(PUBLICID_WEBLOGIC_EJB510, DEFAULT_WL51_DTD_LOCATION);
        handler.registerDTD(PUBLICID_WEBLOGIC_EJB510, DEFAULT_WL60_51_DTD_LOCATION);
        handler.registerDTD(PUBLICID_WEBLOGIC_EJB600, DEFAULT_WL60_DTD_LOCATION);
        handler.registerDTD(PUBLICID_WEBLOGIC_EJB510, weblogicDTD);
        handler.registerDTD(PUBLICID_WEBLOGIC_EJB600, weblogicDTD);
                            
        for (Iterator i = getConfig().dtdLocations.iterator(); i.hasNext();) {
            EjbJar.DTDLocation dtdLocation = (EjbJar.DTDLocation)i.next();
            handler.registerDTD(dtdLocation.getPublicId(), dtdLocation.getLocation());
        }
        return handler;                                    
    }

    /**
     * Add any vendor specific files which should be included in the 
     * EJB Jar.
     */
    protected void addVendorFiles(Hashtable ejbFiles, String ddPrefix) {
        File weblogicDD = new File(getConfig().descriptorDir, ddPrefix + WL_DD);

        if (weblogicDD.exists()) {
            ejbFiles.put(META_DIR + WL_DD,
                         weblogicDD);
        }
        else {
            log("Unable to locate weblogic deployment descriptor. It was expected to be in " + 
                weblogicDD.getPath(), Project.MSG_WARN);
            return;
        }

        if (!newCMP) {
            log("The old method for locating CMP files has been DEPRECATED.", Project.MSG_VERBOSE);
            log("Please adjust your weblogic descriptor and set newCMP=\"true\" " +
                "to use the new CMP descriptor inclusion mechanism. ", Project.MSG_VERBOSE);
            // The the weblogic cmp deployment descriptor
            File weblogicCMPDD = new File(getConfig().descriptorDir, ddPrefix + WL_CMP_DD);
                
            if (weblogicCMPDD.exists()) {
                ejbFiles.put(META_DIR + WL_CMP_DD,
                             weblogicCMPDD);
            }
        }
        else {
            // now that we have the weblogic descriptor, we parse the file
            // to find other descriptors needed to deploy the bean.
            // this could be the weblogic-cmp-rdbms.xml or any other O/R
            // mapping tool descriptors.
            try
            {
                File ejbDescriptor = (File)ejbFiles.get(META_DIR + EJB_DD);
                SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
                saxParserFactory.setValidating(true);
                SAXParser saxParser = saxParserFactory.newSAXParser();
                DescriptorHandler handler = getWeblogicDescriptorHandler(ejbDescriptor.getParentFile());
                saxParser.parse(new InputSource
                                (new FileInputStream
                                (weblogicDD)),
                                handler);
                                
                Hashtable ht = handler.getFiles();
                Enumeration e = ht.keys();
                while(e.hasMoreElements()){
                    String key = (String)e.nextElement();
                    ejbFiles.put(key, ht.get(key));
                }
            }
            catch(Exception e)
            { 
                String msg = "Exception while adding Vendor specific files: " + e.toString();
                throw new BuildException(msg, e);
            }
        }
    }
    
    /**
     * Get the vendor specific name of the Jar that will be output. The modification date
     * of this jar will be checked against the dependent bean classes.
     */
    File getVendorOutputJarFile(String baseName) {
        return new File(getDestDir(), baseName + jarSuffix);
    }

    /**
     * Helper method invoked by execute() for each WebLogic jar to be built.
     * Encapsulates the logic of constructing a java task for calling
     * weblogic.ejbc and executing it.
     * @param sourceJar java.io.File representing the source (EJB1.1) jarfile.
     * @param destJar java.io.File representing the destination, WebLogic
     *        jarfile.
     */
    private void buildWeblogicJar(File sourceJar, File destJar, String publicId) {
        org.apache.tools.ant.taskdefs.Java javaTask = null;

        if (noEJBC) {
            try {
                getTask().getProject().copyFile(sourceJar, destJar);
                if (!keepgenerated) {
                    sourceJar.delete();
                }
                return;
            }
            catch (IOException e) {
                throw new BuildException("Unable to write EJB jar", e);
            }
        }

        String ejbcClassName = ejbcClass;
        
        try {
            javaTask = (Java) getTask().getProject().createTask("java");
            javaTask.setTaskName("ejbc");
            if (ejbcClassName == null) {
                // try to determine it from publicId
                if (PUBLICID_EJB11.equals(publicId)) {
                    ejbcClassName = COMPILER_EJB11;
                }
                else if (PUBLICID_EJB20.equals(publicId)) {
                    ejbcClassName = COMPILER_EJB20;
                }
                else {
                    log("Unrecognized publicId " + publicId + " - using EJB 1.1 compiler", Project.MSG_WARN);
                    ejbcClassName = COMPILER_EJB11;
                }
            }
                 
            javaTask.setClassname(ejbcClassName);
            javaTask.createArg().setLine(additionalArgs);
            if (keepgenerated) {
                javaTask.createArg().setValue("-keepgenerated");
            }
            if (compiler == null) {
                // try to use the compiler specified by build.compiler. Right now we are just going
                // to allow Jikes
                String buildCompiler = getTask().getProject().getProperty("build.compiler");
                if (buildCompiler != null && buildCompiler.equals("jikes")) {
                    javaTask.createArg().setValue("-compiler");
                    javaTask.createArg().setValue("jikes");
                }
            }
            else {
                if (!compiler.equals(DEFAULT_COMPILER)) {
                    javaTask.createArg().setValue("-compiler");
                    javaTask.createArg().setLine(compiler);
                }
            }
            javaTask.createArg().setValue(sourceJar.getPath());
            javaTask.createArg().setValue(destJar.getPath());
            
            Path classpath = wlClasspath;
            if (classpath == null) {
                classpath = getCombinedClasspath();
            }
            
            javaTask.setFork(true);
            if (classpath != null) {
                javaTask.setClasspath(classpath);
            }

            log("Calling " + ejbcClassName + " for " + sourceJar.toString(),
                          Project.MSG_VERBOSE);

            if (javaTask.executeJava() != 0) {
                throw new BuildException("Ejbc reported an error");
            }
        }
        catch (Exception e) {
            // Have to catch this because of the semantics of calling main()
            String msg = "Exception while calling " + ejbcClassName + ". Details: " + e.toString();
            throw new BuildException(msg, e);
        }
    }

    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over the
     * filenames/java.io.Files in the Hashtable stored on the instance variable
     * ejbFiles.
     */
    protected void writeJar(String baseName, File jarFile, Hashtable files, 
                            String publicId) throws BuildException {
        // need to create a generic jar first.
        File genericJarFile = super.getVendorOutputJarFile(baseName);
        super.writeJar(baseName, genericJarFile, files, publicId);
        
        if (alwaysRebuild || isRebuildRequired(genericJarFile, jarFile))
        {
            buildWeblogicJar(genericJarFile, jarFile, publicId);
        }
        if (!keepGeneric) {
             log("deleting generic jar " + genericJarFile.toString(),
                           Project.MSG_VERBOSE);
             genericJarFile.delete();
        }
    }

    /**
     * Called to validate that the tool parameters have been configured.
     *
     */
    public void validateConfigured() throws BuildException {
        super.validateConfigured();
    }

    
    /**
     * Helper method to check to see if a weblogic EBJ1.1 jar needs to be rebuilt using 
     * ejbc.  Called from writeJar it sees if the "Bean" classes  are the only thing that needs
     * to be updated and either updates the Jar with the Bean classfile or returns true,
     * saying that the whole weblogic jar needs to be regened with ejbc.  This allows faster 
     * build times for working developers.
     * <p>
     * The way weblogic ejbc works is it creates wrappers for the publicly defined methods as 
     * they are exposed in the remote interface.  If the actual bean changes without changing the 
     * the method signatures then only the bean classfile needs to be updated and the rest of the 
     * weblogic jar file can remain the same.  If the Interfaces, ie. the method signatures change 
     * or if the xml deployment dicriptors changed, the whole jar needs to be rebuilt with ejbc.  
     * This is not strictly true for the xml files.  If the JNDI name changes then the jar doesnt
     * have to be rebuild, but if the resources references change then it does.  At this point the
     * weblogic jar gets rebuilt if the xml files change at all.
     *
     * @param genericJarFile java.io.File The generic jar file.
     * @param weblogicJarFile java.io.File The weblogic jar file to check to see if it needs to be rebuilt.
     */
    protected boolean isRebuildRequired(File genericJarFile, File weblogicJarFile)
    {
        boolean rebuild = false;

        JarFile genericJar = null;
        JarFile wlJar = null;
        File newWLJarFile = null;
        JarOutputStream newJarStream = null;
        
        try 
        {
            log("Checking if weblogic Jar needs to be rebuilt for jar " + weblogicJarFile.getName(),
                Project.MSG_VERBOSE);
            // Only go forward if the generic and the weblogic file both exist
            if (genericJarFile.exists() && genericJarFile.isFile() 
                && weblogicJarFile.exists() && weblogicJarFile.isFile())
            {
                //open jar files
                genericJar = new JarFile(genericJarFile);
                wlJar = new JarFile(weblogicJarFile);

                Hashtable genericEntries = new Hashtable();
                Hashtable wlEntries = new Hashtable();
                Hashtable replaceEntries = new Hashtable();
                
                //get the list of generic jar entries
                for (Enumeration e = genericJar.entries(); e.hasMoreElements();)
                {
                    JarEntry je = (JarEntry)e.nextElement();
                    genericEntries.put(je.getName().replace('\\', '/'), je);
                }
                //get the list of weblogic jar entries
                for (Enumeration e = wlJar.entries() ; e.hasMoreElements();)
                {
                    JarEntry je = (JarEntry)e.nextElement();
                    wlEntries.put(je.getName(), je);
                }

                //Cycle Through generic and make sure its in weblogic
                ClassLoader genericLoader = getClassLoaderFromJar(genericJarFile);
                for (Enumeration e = genericEntries.keys(); e.hasMoreElements();)
                {
                    String filepath = (String)e.nextElement();
                    if (wlEntries.containsKey(filepath))    // File name/path match
                    {
                        // Check files see if same
                        JarEntry genericEntry = (JarEntry)genericEntries.get(filepath);
                        JarEntry wlEntry = (JarEntry)wlEntries.get(filepath);
                        if ((genericEntry.getCrc() !=  wlEntry.getCrc())  || // Crc's Match
                            (genericEntry.getSize() != wlEntry.getSize()) ) // Size Match
                        {
                            if (genericEntry.getName().endsWith(".class"))
                            {
                                //File are different see if its an object or an interface
                                String classname = genericEntry.getName().replace(File.separatorChar,'.');
                                classname = classname.substring(0,classname.lastIndexOf(".class"));
                                Class genclass = genericLoader.loadClass(classname);
                                if (genclass.isInterface())
                                {
                                    //Interface changed   rebuild jar.
                                    log("Interface " + genclass.getName() + " has changed",Project.MSG_VERBOSE);
                                    rebuild = true;
                                    break;
                                }
                                else
                                {
                                    //Object class Changed   update it.
                                    replaceEntries.put(filepath, genericEntry);
                                }
                            }
                            else
                            {
                                // is it the manifest. If so ignore it
                                if (!genericEntry.getName().equals("META-INF/MANIFEST.MF")) {
                                    //File other then class changed   rebuild
                                    log("Non class file " + genericEntry.getName() + " has changed",Project.MSG_VERBOSE);
                                    rebuild = true;
                                    break;
                                }
                            }
                        }
                    }
                    else // a file doesnt exist rebuild
                    {
                        log("File " + filepath + " not present in weblogic jar",Project.MSG_VERBOSE);
                        rebuild =  true;
                        break;
                    }
                }
                
                if (!rebuild)
                {
                    log("No rebuild needed - updating jar",Project.MSG_VERBOSE);
                    newWLJarFile = new File(weblogicJarFile.getAbsolutePath() + ".temp");
                    if (newWLJarFile.exists()) {
                        newWLJarFile.delete();
                    }
                    
                    newJarStream = new JarOutputStream(new FileOutputStream(newWLJarFile));
                    newJarStream.setLevel(0);
                    
                    //Copy files from old weblogic jar
                    for (Enumeration e = wlEntries.elements() ; e.hasMoreElements();)
                    {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        InputStream is;
                        JarEntry je = (JarEntry)e.nextElement();
                        if (je.getCompressedSize() == -1 ||
                                je.getCompressedSize() == je.getSize()) {
                            newJarStream.setLevel(0);
                        }
                        else {
                            newJarStream.setLevel(9);
                        }
                            
                        // Update with changed Bean class
                        if (replaceEntries.containsKey(je.getName()))
                        {
                            log("Updating Bean class from generic Jar " + je.getName(),Project.MSG_VERBOSE);
                            // Use the entry from the generic jar
                            je = (JarEntry)replaceEntries.get(je.getName());
                            is = genericJar.getInputStream(je);
                        }   
                        else  //use fle from original weblogic jar
                        {
                            is = wlJar.getInputStream(je);
                        }
                        newJarStream.putNextEntry(new JarEntry(je.getName()));

                        while ((bytesRead = is.read(buffer)) != -1)
                        {
                            newJarStream.write(buffer,0,bytesRead);
                        }
                        is.close();
                    }
                }
                else
                {
                    log("Weblogic Jar rebuild needed due to changed interface or XML",Project.MSG_VERBOSE);
                }       
            }
            else
            {
                rebuild = true;
            }
        }
        catch(ClassNotFoundException cnfe)
        {
            String cnfmsg = "ClassNotFoundException while processing ejb-jar file"
                + ". Details: "
                + cnfe.getMessage();
            throw new BuildException(cnfmsg, cnfe);
        }
        catch(IOException ioe) {
            String msg = "IOException while processing ejb-jar file "
                + ". Details: "
                + ioe.getMessage();
            throw new BuildException(msg, ioe);
        }
        finally {
            // need to close files and perhaps rename output
            if (genericJar != null) {
                try {
                    genericJar.close();
                }
                catch (IOException closeException) {}
            }
            
            if (wlJar != null) {
                try {
                    wlJar.close();
                }
                catch (IOException closeException) {}
            }
            
            if (newJarStream != null) {
                try {
                    newJarStream.close();
                }
                catch (IOException closeException) {}

                weblogicJarFile.delete();
                newWLJarFile.renameTo(weblogicJarFile);
                if (!weblogicJarFile.exists()) {
                    rebuild = true;
                }
            }
        }

        return rebuild;
    }
    
    /**
    * Helper method invoked by isRebuildRequired to get a ClassLoader for 
    * a Jar File passed to it.
    *
    * @param classjar java.io.File representing jar file to get classes from.
    */
    protected ClassLoader getClassLoaderFromJar(File classjar) throws IOException
    {
        Path lookupPath = new Path(getTask().getProject());
        lookupPath.setLocation(classjar);
        
        Path classpath = getCombinedClasspath();
        if (classpath != null) {
            lookupPath.append(classpath);
        }
        
        return new AntClassLoader(getTask().getProject(), lookupPath);
    }
}
