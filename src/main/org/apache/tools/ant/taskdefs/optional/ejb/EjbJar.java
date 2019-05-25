/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional.ejb;

// Standard java imports
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.xml.sax.SAXException;

/**
 * Provides automated EJB JAR file creation.
 * <p>
 * Extends the
 * MatchingTask class provided in the default ant distribution to provide a
 * directory scanning EJB jarfile generator.
 * </p>
 *
 * <p>
 * The task works by taking the deployment descriptors one at a time and
 * parsing them to locate the names of the classes which should be placed in
 * the jar. The classnames are translated to java.io.Files by replacing
 * periods with File.separatorChar and resolving the generated filename as a
 * relative path under the srcDir attribute. All necessary files are then
 * assembled into a jarfile. One jarfile is constructed for each deployment
 * descriptor found.
 * </p>
 *
 * */
public class EjbJar extends MatchingTask {

    /**
     * Inner class used to record information about the location of a local DTD
     */
    public static class DTDLocation
        extends org.apache.tools.ant.types.DTDLocation {
    }

    /**
     * A class which contains the configuration state of the ejbjar task.
     * This state is passed to the deployment tools for configuration
     */
    static class Config {
        // CheckStyle:VisibilityModifier OFF - bc
        /**
         * Stores a handle to the directory under which to search for class
         * files
         */
        public File srcDir;

        /**
         * Stores a handle to the directory under which to search for
         * deployment descriptors
         */
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
        public List<FileSet> supportFileSets = new ArrayList<>();

        /**
         * The list of configured DTD locations
         */
        public ArrayList<DTDLocation> dtdLocations = new ArrayList<>();

        /**
         * The naming scheme used to determine the generated jar name
         * from the descriptor information
         */
        public NamingScheme namingScheme;

        /**
         * The Manifest file
         */
        public File manifest;

        /**
         * The dependency analyzer to use to add additional classes to the jar
         */
        public String analyzer;
        // CheckStyle:VisibilityModifier ON
    }

    /**
     * An EnumeratedAttribute class for handling different EJB jar naming
     * schemes
     */
    public static class NamingScheme extends EnumeratedAttribute {
        /**
         * Naming scheme where generated jar is determined from the ejb-name in
         * the deployment descriptor
         */
        public static final String EJB_NAME = "ejb-name";

        /**
         * Naming scheme where the generated jar name is based on the
         * name of the directory containing the deployment descriptor
         */
        public static final String DIRECTORY = "directory";

        /**
         * Naming scheme where the generated jar name is based on the name of
         * the deployment descriptor file
         */
        public static final String DESCRIPTOR = "descriptor";

        /**
         * Naming scheme where the generated jar is named by the basejarname
         * attribute
         */
        public static final String BASEJARNAME = "basejarname";

        /**
         * Gets the values of the NamingScheme
         *
         * @return an array of the values of this attribute class.
         */
        @Override
        public String[] getValues() {
            return new String[] {EJB_NAME, DIRECTORY, DESCRIPTOR, BASEJARNAME};
        }
    }

    /**
     * CMP versions supported
     * valid CMP versions are 1.0 and 2.0
     * @since ant 1.6
     */
    public static class CMPVersion extends EnumeratedAttribute {
        /** 1.0 value */
        public static final String CMP1_0 = "1.0";
        /** 2.0 value */
        public static final String CMP2_0 = "2.0";
        /** {@inheritDoc}. */
        @Override
        public String[] getValues() {
            return new String[] {
                CMP1_0,
                CMP2_0,
            };
        }
    }

    /**
     * The config which is built by this task and used by the various deployment
     * tools to access the configuration of the ejbjar task
     */
    private Config config = new Config();

    /**
     * Stores a handle to the directory to put the Jar files in. This is
     * only used by the generic deployment descriptor tool which is created
     * if no other deployment descriptor tools are provided. Normally each
     * deployment tool will specify the destination dir itself.
     */
    private File destDir;

    /** Instance variable that stores the suffix for the generated jarfile. */
    private String genericJarSuffix = "-generic.jar";

    /** Instance variable that stores the CMP version for the JBoss jarfile. */
    private String cmpVersion = CMPVersion.CMP1_0;

    /** The list of deployment tools we are going to run. */
    private List<EJBDeploymentTool> deploymentTools = new ArrayList<>();

    /**
     * Add a deployment tool to the list of deployment tools that will be
     * processed
     *
     * @param deploymentTool a deployment tool instance to which descriptors
     *        will be passed for processing.
     */
    protected void addDeploymentTool(EJBDeploymentTool deploymentTool) {
        deploymentTool.setTask(this);
        deploymentTools.add(deploymentTool);
    }

    /**
     * Create a orion nested element used to configure a
     * deployment tool for Orion server.
     *
     * @return the deployment tool instance to be configured.
     * @since Ant 1.10.2
     */
    public OrionDeploymentTool createOrion() {
        OrionDeploymentTool tool = new OrionDeploymentTool();
        addDeploymentTool(tool);
        return tool;
    }

    /**
     * Adds a deployment tool for WebLogic server.
     *
     * @return the deployment tool instance to be configured.
     */
    public WeblogicDeploymentTool createWeblogic() {
        WeblogicDeploymentTool tool = new WeblogicDeploymentTool();
        addDeploymentTool(tool);
        return tool;
    }

    /**
     * Adds a deployment tool for WebSphere 4.0 server.
     *
     * @return the deployment tool instance to be configured.
     */
    public WebsphereDeploymentTool createWebsphere() {
        WebsphereDeploymentTool tool = new WebsphereDeploymentTool();
        addDeploymentTool(tool);
        return tool;
    }

    /**
     * Adds a deployment tool for Borland server.
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
     * Adds a deployment tool for iPlanet Application Server.
     *
     * @return the deployment tool instance to be configured.
     */
    public IPlanetDeploymentTool createIplanet() {
        log("iPlanet Application Server deployment tools", Project.MSG_VERBOSE);

        IPlanetDeploymentTool tool = new IPlanetDeploymentTool();
        addDeploymentTool(tool);
        return tool;
    }

    /**
     * Adds a deployment tool for JBoss server.
     *
     * @return the deployment tool instance to be configured.
     */
    public JbossDeploymentTool createJboss() {
        JbossDeploymentTool tool = new JbossDeploymentTool();
        addDeploymentTool(tool);
        return tool;
    }

    /**
     * Adds a deployment tool for JOnAS server.
     *
     * @return the deployment tool instance to be configured.
     */
    public JonasDeploymentTool createJonas() {
        log("JOnAS deployment tools",  Project.MSG_VERBOSE);

        JonasDeploymentTool tool = new JonasDeploymentTool();
        addDeploymentTool(tool);
        return tool;
    }

    /**
     * Adds a deployment tool for WebLogic when using the TOPLink
     * Object-Relational mapping.
     *
     * @return the deployment tool instance to be configured.
     */
    public WeblogicTOPLinkDeploymentTool createWeblogictoplink() {
        log("The <weblogictoplink> element is no longer required. Please use "
            + "the <weblogic> element and set newCMP=\"true\"",
            Project.MSG_INFO);
        WeblogicTOPLinkDeploymentTool tool
            = new WeblogicTOPLinkDeploymentTool();
        addDeploymentTool(tool);
        return tool;
    }

    /**
     * Adds to the classpath used to locate the super classes and
     * interfaces of the classes that will make up the EJB JAR.
     *
     * @return the path to be configured.
     */
    public Path createClasspath() {
        if (config.classpath == null) {
            config.classpath = new Path(getProject());
        }
        return config.classpath.createPath();
    }

    /**
     * Create a DTD location record. This stores the location of a DTD. The
     * DTD is identified by its public Id. The location may either be a file
     * location or a resource location.
     *
     * @return the DTD location object to be configured by Ant
     */
    public DTDLocation createDTD() {
        DTDLocation dtdLocation = new DTDLocation();
        config.dtdLocations.add(dtdLocation);

        return dtdLocation;
    }

    /**
     * Adds a fileset for support elements.
     *
     * @return a fileset which can be populated with support files.
     */
    public FileSet createSupport() {
        FileSet supportFileSet = new FileSet();
        config.supportFileSets.add(supportFileSet);
        return supportFileSet;
    }

    /**
     * Set the Manifest file to use when jarring. As of EJB 1.1, manifest
     * files are no longer used to configure the EJB. However, they still
     * have a vital importance if the EJB is intended to be packaged in an
     * EAR file. By adding "Class-Path" settings to a Manifest file, the EJB
     * can look for classes inside the EAR file itself, allowing for easier
     * deployment. This is outlined in the J2EE specification, and all J2EE
     * components are meant to support it.
     *
     * @param manifest the manifest to be used in the EJB jar
     */
     public void setManifest(File manifest) {
         config.manifest = manifest;
     }

    /**
     * Sets the source directory, which is the directory that
     * contains the classes that will be added to the EJB jar. Typically
     * this will include the home and remote interfaces and the bean class.
     *
     * @param inDir the source directory.
     */
    public void setSrcdir(File inDir) {
        config.srcDir = inDir;
    }

    /**
     * Set the descriptor directory. The descriptor directory contains the
     * EJB deployment descriptors. These are XML files that declare the
     * properties of a bean in a particular deployment scenario. Such
     * properties include, for example, the transactional nature of the bean
     * and the security access control to the bean's methods.
     *
     * @param inDir the directory containing the deployment descriptors.
     */
    public void setDescriptordir(File inDir) {
        config.descriptorDir = inDir;
    }

    /**
     * Set the analyzer to use when adding in dependencies to the JAR.
     *
     * @param analyzer the name of the dependency analyzer or a class.
     */
    public void setDependency(String analyzer) {
        config.analyzer = analyzer;
    }

    /**
     * Set the base name of the EJB JAR that is to be created if it is not
     * to be determined from the name of the deployment descriptor files.
     *
     * @param inValue the basename that will be used when writing the jar
     *      file containing the EJB
     */
    public void setBasejarname(String inValue) {
        config.baseJarName = inValue;
        if (config.namingScheme == null) {
            config.namingScheme = new NamingScheme();
            config.namingScheme.setValue(NamingScheme.BASEJARNAME);
        } else if (!NamingScheme.BASEJARNAME.equals(config.namingScheme.getValue())) {
            throw new BuildException(
                "The basejarname attribute is not compatible with the %s naming scheme",
                config.namingScheme.getValue());
        }
    }

    /**
     * Set the naming scheme used to determine the name of the generated jars
     * from the deployment descriptor
     *
     * @param namingScheme the naming scheme to be used
     */
    public void setNaming(NamingScheme namingScheme) {
        config.namingScheme = namingScheme;
        if (!NamingScheme.BASEJARNAME.equals(config.namingScheme.getValue())
            && config.baseJarName != null) {
            throw new BuildException(
                "The basejarname attribute is not compatible with the %s naming scheme",
                config.namingScheme.getValue());
        }
    }

    /**
     * Gets the destination directory.
     *
     * @return destination directory
     * @since ant 1.6
     */
    public File getDestdir() {
        return this.destDir;
    }

    /**
     * Set the destination directory. The EJB jar files will be written into
     * this directory. The jar files that exist in this directory are also
     * used when determining if the contents of the jar file have changed.
     * Note that this parameter is only used if no deployment tools are
     * specified. Typically each deployment tool will specify its own
     * destination directory.
     *
     * @param inDir the destination directory in which to generate jars
     */
    public void setDestdir(File inDir) {
        this.destDir = inDir;
    }

    /**
     * Gets the CMP version.
     *
     * @return CMP version
     * @since ant 1.6
     */
    public String getCmpversion() {
        return this.cmpVersion;
    }

    /**
     * Sets the CMP version.
     * Must be either <code>1.0</code> or <code>2.0</code>.
     * Default is <code>1.0</code>.
     * Initially, only the JBoss implementation does something specific for CMP 2.0.
     *
     * @param version CMP version.
     * @since ant 1.6
     */
    public void setCmpversion(CMPVersion version) {
        this.cmpVersion = version.getValue();
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
     * Controls whether the
     * destination JARs are written out in the destination directory with
     * the same hierarchical structure from which the deployment descriptors
     * have been read. If this is set to true the generated EJB jars are
     * written into the root of the destination directory, otherwise they
     * are written out in the same relative position as the deployment
     * descriptors in the descriptor directory.
     *
     * @param inValue the new value of the flatdestdir flag.
     */
    public void setFlatdestdir(boolean inValue) {
        config.flatDestDir = inValue;
    }

    /**
     * Set the suffix for the generated jar file. When generic jars are
     * generated, they have a suffix which is appended to the the bean name
     * to create the name of the jar file. Note that this suffix includes
     * the extension fo te jar file and should therefore end with an
     * appropriate extension such as .jar or .ear
     *
     * @param inString the string to use as the suffix.
     */
    public void setGenericjarsuffix(String inString) {
        this.genericJarSuffix = inString;
    }

    /**
     * The string which terminates the bean name.
     * The convention used by this task is
     * that bean descriptors are named as the BeanName with some suffix. The
     * baseNameTerminator string separates the bean name and the suffix and
     * is used to determine the bean name.
     *
     * @param inValue a string which marks the end of the basename.
     */
    public void setBasenameterminator(String inValue) {
        config.baseNameTerminator = inValue;
    }

    /**
     * Validate the config that has been configured from the build file
     *
     * @throws BuildException if the config is not valid
     */
    private void validateConfig() throws BuildException {
        if (config.srcDir == null) {
            throw new BuildException("The srcDir attribute must be specified");
        }

        if (config.descriptorDir == null) {
            config.descriptorDir = config.srcDir;
        }

        if (config.namingScheme == null) {
            config.namingScheme = new NamingScheme();
            config.namingScheme.setValue(NamingScheme.DESCRIPTOR);
        } else if (NamingScheme.BASEJARNAME.equals(config.namingScheme.getValue())
                    && config.baseJarName == null) {
            throw new BuildException(
                "The basejarname attribute must be specified with the basejarname naming scheme");
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
    @Override
    public void execute() throws BuildException {
        validateConfig();

        if (deploymentTools.isEmpty()) {
            GenericDeploymentTool genericTool = new GenericDeploymentTool();
            genericTool.setTask(this);
            genericTool.setDestdir(destDir);
            genericTool.setGenericJarSuffix(genericJarSuffix);
            deploymentTools.add(genericTool);
        }

        for (EJBDeploymentTool tool : deploymentTools) {
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
            for (String file : files) {
                // process the deployment descriptor in each tool
                for (EJBDeploymentTool tool : deploymentTools) {
                    tool.processDescriptor(file, saxParser);
                }
            }
        } catch (SAXException se) {
            throw new BuildException("SAXException while creating parser.", se);
        } catch (ParserConfigurationException pce) {
            throw new BuildException(
                "ParserConfigurationException while creating parser. ", pce);
        }
    } // end of execute()

}
