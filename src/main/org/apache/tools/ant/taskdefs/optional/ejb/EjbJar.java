/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000, 2001 The Apache Software Foundation.  All rights
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

// Standard java imports
import java.io.*;
import java.util.*;

// XML imports
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

// Apache/Ant imports
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.*;

/**
 * <p>Provides automated ejb jar file creation for ant.  Extends the MatchingTask
 * class provided in the default ant distribution to provide a directory scanning
 * EJB jarfile generator.</p>
 *
 * <p>The task works by taking the deployment descriptors one at a time and
 * parsing them to locate the names of the classes which should be placed in
 * the jar.  The classnames are translated to java.io.Files by replacing periods
 * with File.separatorChar and resolving the generated filename as a relative
 * path under the srcDir attribute.  All necessary files are then assembled into
 * a jarfile.  One jarfile is constructed for each deployment descriptor found.
 * </p>
 *
 * <p>Functionality is currently provided for standard EJB1.1 jars and Weblogic
 * 5.1 jars. The weblogic deployment descriptors, used in constructing the 
 * Weblogic jar, are located based on a simple naming convention. The name of the
 * standard deployment descriptor is taken upto the first instance of a String,
 * specified by the attribute baseNameTerminator, and then the regular Weblogic
 * descriptor name is appended. For example if baseNameTerminator is set to '-',
 * its default value, and a standard descriptor is called Foo-ejb-jar.xml then
 * the files Foo-weblogic-ejb-jar.xml and Foo-weblogic-cmp-rdbms-jar.xml will be
 * looked for, and if found, included in the jarfile.</p>
 *
 * <p>Attributes and setter methods are provided to support optional generation
 * of Weblogic5.1 jars, optional deletion of generic jar files, setting alternate
 * values for baseNameTerminator, and setting the strings to append to the names
 * of the generated jarfiles.</p>
 *
 * @author <a href="mailto:tfennell@sapient.com">Tim Fennell</a>
 */
public class EjbJar extends MatchingTask {
    
    public static class DTDLocation {
        private String publicId = null;
        private String location = null;
        
        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
        
        public String getPublicId() {
            return publicId;
        }
        
        public String getLocation() {
            return location;
        }
    }

    /**
     * A class which contains the configuration state of the ejbjar task.
     * This state is passed to the deployment tools for configuration
     */
    static class Config {
        /** Stores a handle to the directory under which to search for class files */
        public File srcDir;

        /** Stores a handle to the directory under which to search for deployment descriptors */
        public File descriptorDir;
        
        /** Instance variable that marks the end of the 'basename' */
        public String baseNameTerminator = "-";

        /** Stores a handle to the destination EJB Jar file */
        public String baseJarName;

        /**
         * Instance variable that determines whether to use a package structure
         * of a flat directory as the destination for the jar files.
         */
        public boolean flatDestDir = false;
        
        /**
         * The classpath to use when loading classes
         */
        public Path classpath;
    
        /**
         * A Fileset of support classes
         */
        public List supportFileSets = new ArrayList();
        
        /**
         * The list of configured DTD locations
         */
        public ArrayList dtdLocations = new ArrayList();
        
        /**
         * The naming scheme used to determine the generated jar name
         * from the descriptor information
         */
        public NamingScheme namingScheme;
        
        /**
         * The Manifest file
         */
        public File manifest;
    };


    public static class NamingScheme extends EnumeratedAttribute {
        static public final String EJB_NAME = "ejb-name";
        static public final String DIRECTORY = "directory";
        static public final String DESCRIPTOR = "descriptor";
        static public final String BASEJARNAME = "basejarname";
        public String[] getValues() {
            return new String[] {EJB_NAME, DIRECTORY, DESCRIPTOR, BASEJARNAME};
        }
    }

    private Config config = new Config();


    /** Stores a handle to the directory to put the Jar files in. This is only used
        by the generic deployment descriptor tool which is created if no other
        deployment descriptor tools are provided. Normally each deployment tool
        will specify the desitination dir itself. */
    private File destDir;
 
    /** Instance variable that stores the suffix for the generated jarfile. */
    private String genericJarSuffix = "-generic.jar";

    /**
     * The list of deployment tools we are going to run.
     */
    private ArrayList deploymentTools = new ArrayList();

    /**
     * Create a weblogic nested element used to configure a
     * deployment tool for Weblogic server.
     *
     * @return the deployment tool instance to be configured.
     */
    public WeblogicDeploymentTool createWeblogic() {
        WeblogicDeploymentTool tool = new WeblogicDeploymentTool();
        tool.setTask(this);
        deploymentTools.add(tool);
        return tool;
    }

    /**
     * Create a Borland nested element used to configure a
     * deployment tool for Borland server.
     *
     * @return the deployment tool instance to be configured.
     */
    public BorlandDeploymentTool createBorland() {
        log("Borland deployment tools",  Project.MSG_VERBOSE);

        BorlandDeploymentTool tool = new BorlandDeploymentTool();
        tool.setTask(this);
        deploymentTools.add(tool);
        return tool;
    }

    /**
     * Create a nested element used to configure a deployment tool for iPlanet
	 * Application Server.
     *
     * @return the deployment tool instance to be configured.
     */
    public IPlanetDeploymentTool createIplanet() {
        log("iPlanet Application Server deployment tools", Project.MSG_VERBOSE);

        IPlanetDeploymentTool tool = new IPlanetDeploymentTool();
        tool.setTask(this);
        deploymentTools.add(tool);
        return tool;
    }

    /**
     * Create a jboss nested element used to configure a
     * deployment tool for Jboss server.
     *
     * @return the deployment tool instance to be configured.
     */
    public JbossDeploymentTool createJboss() {
        JbossDeploymentTool tool = new JbossDeploymentTool();
        tool.setTask(this);
        deploymentTools.add(tool);
        return tool;
    }

    /**
     * Create a nested element for weblogic when using the Toplink
     * Object- Relational mapping.
     *
     * @return the deployment tool instance to be configured.
     */
    public WeblogicTOPLinkDeploymentTool createWeblogictoplink() {
        WeblogicTOPLinkDeploymentTool tool = new WeblogicTOPLinkDeploymentTool();
        tool.setTask(this);
        deploymentTools.add(tool);
        return tool;
    }

    /**
     * creates a nested classpath element.
     *
     * This classpath is used to locate the super classes and interfaces
     * of the classes that will make up the EJB jar.
     * 
     * @return the path to be configured.
     */
    public Path createClasspath() {
        if (config.classpath == null) {
            config.classpath = new Path(project);
        }
        return config.classpath.createPath();
    }

    /**
     * Create a DTD location record. This stores the location of a DTD. The DTD is identified
     * by its public Id. The location may either be a file location or a resource location.
     */
    public DTDLocation createDTD() {
        DTDLocation dtdLocation = new DTDLocation();
        config.dtdLocations.add(dtdLocation);
        
        return dtdLocation;
    }

    /**
     * Create a file set for support elements
     *
     * @return a fileset which can be populated with support files.
     */
    public FileSet createSupport() {
        FileSet supportFileSet = new FileSet();
        config.supportFileSets.add(supportFileSet);
        return supportFileSet;
    }
    

     /**
      * Set the Manifest file to use when jarring.
      *
      * As of EJB 1.1, manifest files are no longer used to configure the EJB. However, they
      * still have a vital importance if the EJB is intended to be packaged in an EAR file.
      * By adding "Class-Path" settings to a Manifest file, the EJB can look for classes inside
      * the EAR file itself, allowing for easier deployment. This is outlined in the J2EE
      * specification, and all J2EE components are meant to support it.
      */
     public void setManifest(File manifest) {
         config.manifest = manifest;
     }

    /**
     * Set the srcdir attribute. The source directory is the directory that contains
     * the classes that will be added to the EJB jar. Typically this will include the 
     * home and remote interfaces and the bean class.
     *
     * @param inDir the source directory.
     */
    public void setSrcdir(File inDir) {
        config.srcDir = inDir;
    }

    /**
     * Set the descriptor directory.
     *
     * The descriptor directory contains the EJB deployment descriptors. These are XML
     * files that declare the properties of a bean in a particular deployment scenario. Such
     * properties include, for example, the transactional nature of the bean and the security
     * access control to the bean's methods.  
     *
     * @param inDir the directory containing the deployment descriptors.
     */
    public void setDescriptordir(File inDir) {
        config.descriptorDir = inDir;
    }

    /**
     * Set the base name of the EJB jar that is to be created if it is not to be
     * determined from the name of the deployment descriptor files. 
     * 
     * @param inValue the basename that will be used when writing the jar file containing
     * the EJB
     */
    public void setBasejarname(String inValue) {
        config.baseJarName = inValue;
        if (config.namingScheme == null) {
            config.namingScheme = new NamingScheme();
            config.namingScheme.setValue(NamingScheme.BASEJARNAME);
        }
        else if (!config.namingScheme.getValue().equals(NamingScheme.BASEJARNAME)) {
            throw new BuildException("The basejarname attribute is not compatible with the " + 
                                     config.namingScheme.getValue() + " naming scheme");
        }                                     
    }

    /**
     * Set the naming scheme used to determine the name of the generated jars
     * from the deployment descriptor
     *
     * @param NamingScheme the namign scheme to be used
     */
    public void setNaming(NamingScheme namingScheme) {
        config.namingScheme = namingScheme;
        if (!config.namingScheme.getValue().equals(NamingScheme.BASEJARNAME) &&
            config.baseJarName != null) {
            throw new BuildException("The basejarname attribute is not compatible with the " + 
                                     config.namingScheme.getValue() + " naming scheme");
        }                                     
    }
    

    /**
     * Set the destination directory.
     * 
     * The EJB jar files will be written into this directory. The jar files that exist in
     * this directory are also used when determining if the contents of the jar file 
     * have changed.
     *
     * Note that this parameter is only used if no deployment tools are specified. Typically
     * each deployment tool will specify its own destination directory.
     * 
     * @param inFile the destination directory.
     */
    public void setDestdir(File inDir) {
        this.destDir = inDir;
    }

    /**
     * Set the classpath to use when resolving classes for inclusion in the jar. 
     *
     * @param classpath the classpath to use.
     */
    public void setClasspath(Path classpath) {
        config.classpath = classpath;
    }

    /**
     * Set the flat dest dir flag.
     *
     * This flag controls whether the destination jars are written out in the 
     * destination directory with the same hierarchal structure from which 
     * the deployment descriptors have been read. If this is set to true the 
     * generated EJB jars are written into the root of the destination directory,
     * otherwise they are written out in the same relative position as the deployment
     * descriptors in the descriptor directory.
     * 
     * @param inValue the new value of the flatdestdir flag.
     */
    public void setFlatdestdir(boolean inValue) {
        config.flatDestDir = inValue;
    }
     
    /**
     * Set the suffix for the generated jar file.
     * When generic jars are generated, they have a suffix which is appended to the
     * the bean name to create the name of the jar file. Note that this suffix includes
     * the extension fo te jar file and should therefore end with an appropriate 
     * extension such as .jar or .ear
     * 
     * @param inString the string to use as the suffix.
     */
    public void setGenericjarsuffix(String inString) {
        this.genericJarSuffix = inString;
    }

    /**
     * Set the baseNameTerminator.
     *
     * The basename terminator is the string which terminates the bean name. The convention
     * used by this task is that bean descriptors are named as the BeanName with some suffix. 
     * The baseNameTerminator string separates the bean name and the suffix and is used to
     * determine the bean name.
     *
     * @param inValue a string which marks the end of the basename.
     */
    public void setBasenameterminator(String inValue) {
        config.baseNameTerminator = inValue;
    }

    private void validateConfig() {
        if (config.srcDir == null) {
            throw new BuildException("The srcDir attribute must be specified");
        }

        if (config.descriptorDir == null) {
            config.descriptorDir = config.srcDir;
        }

        if (config.namingScheme == null) {
            config.namingScheme = new NamingScheme();
            config.namingScheme.setValue(NamingScheme.DESCRIPTOR);
        }
        else if (config.namingScheme.getValue().equals(NamingScheme.BASEJARNAME) &&
                 config.baseJarName == null) {
            throw new BuildException("The basejarname attribute must be specified " + 
                                     "with the basejarname naming scheme");
        }
    }        

    /**
     * Invoked by Ant after the task is prepared, when it is ready to execute
     * this task.  
     *
     * This will configure all of the nested deployment tools to allow them to 
     * process the jar. If no deployment tools have been configured a generic 
     * tool is created to handle the jar.
     * 
     * A parser is configured and then each descriptor found is passed to all
     * the deployment tool elements for processing.
     *
     * @exception BuildException thrown whenever a problem is
     *            encountered that cannot be recovered from, to signal to ant
     *            that a major problem occurred within this task.
     */
    public void execute() throws BuildException {
        validateConfig();
        
        if (deploymentTools.size() == 0) {
            GenericDeploymentTool genericTool = new GenericDeploymentTool();
            genericTool.setTask(this);
            genericTool.setDestdir(destDir);
            genericTool.setGenericJarSuffix(genericJarSuffix);
            deploymentTools.add(genericTool);
        }
        
        for (Iterator i = deploymentTools.iterator(); i.hasNext(); ) {
            EJBDeploymentTool tool = (EJBDeploymentTool)i.next();
            tool.configure(config);
            tool.validateConfigured();
        }
        
        try {
            // Create the parser using whatever parser the system dictates
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(true);
            SAXParser saxParser = saxParserFactory.newSAXParser();
    
                        
            DirectoryScanner ds = getDirectoryScanner(config.descriptorDir);
            ds.scan();
            String[] files = ds.getIncludedFiles();
    
            log(files.length + " deployment descriptors located.",
                Project.MSG_VERBOSE);
            
            // Loop through the files. Each file represents one deployment
            // descriptor, and hence one bean in our model.
            for (int index = 0; index < files.length; ++index) {
                // process the deployment descriptor in each tool
                for (Iterator i = deploymentTools.iterator(); i.hasNext(); ) {
                    EJBDeploymentTool tool = (EJBDeploymentTool)i.next();
                    tool.processDescriptor(files[index], saxParser);
                }
            }    
        }
        catch (SAXException se) {
            String msg = "SAXException while creating parser."
                + "  Details: "
                + se.getMessage();
            throw new BuildException(msg, se);
        }
        catch (ParserConfigurationException pce) {
            String msg = "ParserConfigurationException while creating parser. "
                       + "Details: " + pce.getMessage();
            throw new BuildException(msg, pce);
        }
    } // end of execute()
}







