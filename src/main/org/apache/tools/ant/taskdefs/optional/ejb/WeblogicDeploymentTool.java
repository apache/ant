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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Environment.Variable;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.xml.sax.InputSource;

/**
    The weblogic element is used to control the weblogic.ejbc compiler for
    generating WebLogic EJB jars. Prior to Ant 1.3, the method of locating CMP
    descriptors was to use the ejbjar naming convention. So if your ejb-jar was
    called, Customer-ejb-jar.xml, your WebLogic descriptor was called Customer-
    weblogic-ejb-jar.xml and your CMP descriptor had to be Customer-weblogic-cmp-
    rdbms-jar.xml. In addition, the &lt;type-storage&gt; element in the WebLogic
    descriptor had to be set to the standard name META-INF/weblogic-cmp-rdbms-
    jar.xml, as that is where the CMP descriptor was mapped to in the generated
    jar.
*/
public class WeblogicDeploymentTool extends GenericDeploymentTool {
    /** EJB11 id */
    public static final String PUBLICID_EJB11
         = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
    /** EJB20 id */
    public static final String PUBLICID_EJB20
         = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";
    /** WebLogic 5.1.0 id */
    public static final String PUBLICID_WEBLOGIC_EJB510
         = "-//BEA Systems, Inc.//DTD WebLogic 5.1.0 EJB//EN";
    /** WebLogic 6.0.0 id */
    public static final String PUBLICID_WEBLOGIC_EJB600
         = "-//BEA Systems, Inc.//DTD WebLogic 6.0.0 EJB//EN";
    /** WebLogic 7.0.0 id */
    public static final String PUBLICID_WEBLOGIC_EJB700
         = "-//BEA Systems, Inc.//DTD WebLogic 7.0.0 EJB//EN";

    /** WebLogic 5.1 dtd location */
    protected static final String DEFAULT_WL51_EJB11_DTD_LOCATION
         = "/weblogic/ejb/deployment/xml/ejb-jar.dtd";
    /** WebLogic 6.0 ejb 1.1 dtd location */
    protected static final String DEFAULT_WL60_EJB11_DTD_LOCATION
         = "/weblogic/ejb20/dd/xml/ejb11-jar.dtd";
    /** WebLogic 6.0 ejb 2.0 dtd location */
    protected static final String DEFAULT_WL60_EJB20_DTD_LOCATION
         = "/weblogic/ejb20/dd/xml/ejb20-jar.dtd";

    protected static final String DEFAULT_WL51_DTD_LOCATION
         = "/weblogic/ejb/deployment/xml/weblogic-ejb-jar.dtd";
    protected static final String DEFAULT_WL60_51_DTD_LOCATION
         = "/weblogic/ejb20/dd/xml/weblogic510-ejb-jar.dtd";
    protected static final String DEFAULT_WL60_DTD_LOCATION
         = "/weblogic/ejb20/dd/xml/weblogic600-ejb-jar.dtd";
    protected static final String DEFAULT_WL70_DTD_LOCATION
         = "/weblogic/ejb20/dd/xml/weblogic700-ejb-jar.dtd";

    protected static final String DEFAULT_COMPILER = "default";

    protected static final String WL_DD = "weblogic-ejb-jar.xml";
    protected static final String WL_CMP_DD = "weblogic-cmp-rdbms-jar.xml";

    protected static final String COMPILER_EJB11 = "weblogic.ejbc";
    protected static final String COMPILER_EJB20 = "weblogic.ejbc20";

    /** File utilities instance for copying jars */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** Instance variable that stores the suffix for the WebLogic jarfile. */
    private String jarSuffix = ".jar";

    /** Instance variable that stores the location of the WebLogic DTD file. */
    private String weblogicDTD;

    /** Instance variable that stores the location of the EJB 1.1 DTD file. */
    private String ejb11DTD;

    /** Instance variable that determines whether generic ejb jars are kept. */
    private boolean keepgenerated = false;

    /**
     * Instance variable that stores the fully qualified classname of the
     * WebLogic EJBC compiler
     */
    private String ejbcClass = null;

    private String additionalArgs = "";

    /**
     * additional args to pass to the spawned jvm
     */
    private String additionalJvmArgs = "";

    private boolean keepGeneric = false;

    private String compiler = null;

    private boolean alwaysRebuild = true;

    /** controls whether ejbc is run on the generated jar */
    private boolean noEJBC = false;

    /** Indicates if the old CMP location convention is to be used.  */
    private boolean newCMP = false;

    /** The classpath to the WebLogic classes. */
    private Path wlClasspath = null;

    /** System properties for the JVM. */
    private List<Variable> sysprops = new Vector<>();

    /**
     * The weblogic.StdoutSeverityLevel to use when running the JVM that
     * executes ejbc. Set to 16 to avoid the warnings about EJB Home and
     * Remotes being in the classpath
     */
    private Integer jvmDebugLevel = null;

    private File outputDir;

    /**
     * Add a nested sysproperty element.
     * @param sysp the element to add.
     */
    public void addSysproperty(Environment.Variable sysp) {
        sysprops.add(sysp);
    }

    /**
     * Get the classpath to the WebLogic classpaths.
     * @return the classpath to configure.
     */
    public Path createWLClasspath() {
        if (wlClasspath == null) {
            wlClasspath = new Path(getTask().getProject());
        }
        return wlClasspath.createPath();
    }

    /**
     * If set ejbc will use this directory as the output
     * destination rather than a jar file. This allows for the
     * generation of &quot;exploded&quot; jars.
     * @param outputDir the directory to be used.
     */
    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Optional classpath to WL6.0.
     * WebLogic 6.0 will give a warning if the home and remote interfaces
     * of a bean are on the system classpath used to run weblogic.ejbc.
     * In that case, the standard WebLogic classes should be set with
     * this attribute (or equivalent nested element) and the
     * home and remote interfaces located with the standard classpath
     * attribute.
     * @param wlClasspath the path to be used.
     */
    public void setWLClasspath(Path wlClasspath) {
        this.wlClasspath = wlClasspath;
    }

    /**
     * The compiler (switch <code>-compiler</code>) to use; optional.
     * This allows for the selection of a different compiler
     * to be used for the compilation of the generated Java
     * files. This could be set, for example, to Jikes to
     * compile with the Jikes compiler. If this is not set
     * and the <code>build.compiler</code> property is set
     * to jikes, the Jikes compiler will be used. If this
     * is not desired, the value &quot;<code>default</code>&quot;
     * may be given to use the default compiler.
     * @param compiler the compiler to be used.
     */
    public void setCompiler(String compiler) {
        this.compiler = compiler;
    }

    /**
     * Set the rebuild flag to false to only update changes in the jar rather
     * than rerunning ejbc; optional, default true.
     * This flag controls whether weblogic.ejbc is always
     * invoked to build the jar file. In certain circumstances,
     * such as when only a bean class has been changed, the jar
     * can be generated by merely replacing the changed classes
     * and not rerunning ejbc. Setting this to false will reduce
     * the time to run ejbjar.
     * @param rebuild a <code>boolean</code> value.
     */
    public void setRebuild(boolean rebuild) {
        this.alwaysRebuild = rebuild;
    }

    /**
     * Sets the weblogic.StdoutSeverityLevel to use when running the JVM that
     * executes ejbc; optional. Set to 16 to avoid the warnings about EJB Home and
     * Remotes being in the classpath
     * @param jvmDebugLevel the value to use.
     */
    public void setJvmDebugLevel(Integer jvmDebugLevel) {
        this.jvmDebugLevel = jvmDebugLevel;
    }

    /**
     * Get the debug level.
     * @return the jvm debug level (may be null).
     */
    public Integer getJvmDebugLevel() {
        return jvmDebugLevel;
    }

    /**
     * Setter used to store the suffix for the generated WebLogic jar file.
     *
     * @param inString the string to use as the suffix.
     */
    public void setSuffix(String inString) {
        this.jarSuffix = inString;
    }

    /**
     * controls whether the generic file used as input to
     * ejbc is retained; defaults to false
     *
     * @param inValue true for keep generic
     */
    public void setKeepgeneric(boolean inValue) {
        this.keepGeneric = inValue;
    }

    /**
     * Controls whether WebLogic will keep the generated Java
     * files used to build the class files added to the
     * jar. This can be useful when debugging; default is false.
     *
     * @param inValue either 'true' or 'false'
     */
    public void setKeepgenerated(String inValue) {
        this.keepgenerated = Boolean.parseBoolean(inValue);
    }

    /**
     * Any optional extra arguments pass to the weblogic.ejbc
     * tool.
     * @param args extra arguments to pass to the ejbc tool.
     */
    public void setArgs(String args) {
        this.additionalArgs = args;
    }

    /**
     * Set any additional arguments to pass to the WebLogic JVM; optional.
     * @param args the arguments to be passed to the JVM
     */
    public void setJvmargs(String args) {
        this.additionalJvmArgs = args;
    }

    /**
     * Set the classname of the ejbc compiler;  optional
     * Normally ejbjar determines
     * the appropriate class based on the DTD used for the EJB. The EJB 2.0 compiler
     * featured in WebLogic 6 has, however, been deprecated in version 7. When
     * using with version 7 this attribute should be set to
     * &quot;weblogic.ejbc&quot; to avoid the deprecation warning.
     * @param ejbcClass the name of the class to use.
     */
    public void setEjbcClass(String ejbcClass) {
        this.ejbcClass = ejbcClass;
    }

    /**
     * Get the ejbc compiler class.
     * @return the name of the ejbc compiler class.
     */
    public String getEjbcClass() {
        return ejbcClass;
    }

    /**
     * <b>Deprecated</b>. Defines the location of the ejb-jar DTD in
     *  the WebLogic class hierarchy. Should not be needed, and the
     * nested &lt;dtd&gt; element is recommended when it is.
     *
     * @param inString the string to use as the DTD location.
     */
    public void setWeblogicdtd(String inString) {
        setEJBdtd(inString);
    }

    /**
     * <b>Deprecated</b>. Defines the location of WebLogic DTD in
     *  the WebLogic class hierarchy. Should not be needed, and the
     * nested &lt;dtd&gt; element is recommended when it is.
     *
     * @param inString the string to use as the DTD location.
     */
    public void setWLdtd(String inString) {
        this.weblogicDTD = inString;
    }

    /**
     * <b>Deprecated</b>. Defines the location of Sun's EJB DTD in
     *  the WebLogic class hierarchy. Should not be needed, and the
     * nested &lt;dtd&gt; element is recommended when it is.
     *
     * @param inString the string to use as the DTD location.
     */
    public void setEJBdtd(String inString) {
        this.ejb11DTD = inString;
    }

    /**
     * Set the value of the oldCMP scheme. This is an antonym for newCMP
     * @ant.attribute ignore="true'
     * @param oldCMP a <code>boolean</code> value.
     */
    public void setOldCMP(boolean oldCMP) {
        this.newCMP = !oldCMP;
    }

    /**
     * If this is set to true, the new method for locating
     * CMP descriptors will be used; optional, default false.
     * <P>
     * The old CMP scheme locates the
     * WebLogic CMP descriptor based on the naming convention where the
     * WebLogic CMP file is expected to be named with the bean name as the
     * prefix. Under this scheme the name of the CMP descriptor does not match
     * the name actually used in the main WebLogic EJB descriptor. Also,
     * descriptors which contain multiple CMP references could not be used.
     * @param newCMP a <code>boolean</code> value.
     */
    public void setNewCMP(boolean newCMP) {
        this.newCMP = newCMP;
    }

    /**
     * Do not EJBC the jar after it has been put together;
     * optional, default false
     * @param noEJBC a <code>boolean</code> value.
     */
    public void setNoEJBC(boolean noEJBC) {
        this.noEJBC = noEJBC;
    }

    /**
     * Register the DTDs.
     * @param handler the handler to use.
     */
    @Override
    protected void registerKnownDTDs(DescriptorHandler handler) {
        // register all the known DTDs
        handler.registerDTD(PUBLICID_EJB11, DEFAULT_WL51_EJB11_DTD_LOCATION);
        handler.registerDTD(PUBLICID_EJB11, DEFAULT_WL60_EJB11_DTD_LOCATION);
        handler.registerDTD(PUBLICID_EJB11, ejb11DTD);
        handler.registerDTD(PUBLICID_EJB20, DEFAULT_WL60_EJB20_DTD_LOCATION);
    }

    /**
     * Get the WebLogic descriptor handler.
     * @param srcDir the source directory.
     * @return the descriptor.
     */
    protected DescriptorHandler getWeblogicDescriptorHandler(final File srcDir) {
        DescriptorHandler handler = new DescriptorHandler(getTask(), srcDir) {
            @Override
            protected void processElement() {
                if ("type-storage".equals(currentElement)) {
                    // Get the filename of vendor specific descriptor
                    // trim the META_INF\ off of the file name
                    ejbFiles.put(currentText, new File(srcDir,
                            currentText.substring(META_DIR.length())));
                }
            }
        };

        handler.registerDTD(PUBLICID_WEBLOGIC_EJB510, DEFAULT_WL51_DTD_LOCATION);
        handler.registerDTD(PUBLICID_WEBLOGIC_EJB510, DEFAULT_WL60_51_DTD_LOCATION);
        handler.registerDTD(PUBLICID_WEBLOGIC_EJB600, DEFAULT_WL60_DTD_LOCATION);
        handler.registerDTD(PUBLICID_WEBLOGIC_EJB700, DEFAULT_WL70_DTD_LOCATION);
        handler.registerDTD(PUBLICID_WEBLOGIC_EJB510, weblogicDTD);
        handler.registerDTD(PUBLICID_WEBLOGIC_EJB600, weblogicDTD);

        getConfig().dtdLocations.forEach(l -> handler.registerDTD(l.getPublicId(), l.getLocation()));
        return handler;
    }

    /**
     * Add any vendor specific files which should be included in the EJB Jar.
     * @param ejbFiles the hash table to be populated.
     * @param ddPrefix the prefix to use.
     */
    @Override
    protected void addVendorFiles(Hashtable<String, File> ejbFiles, String ddPrefix) {
        File weblogicDD = new File(getConfig().descriptorDir, ddPrefix + WL_DD);

        if (weblogicDD.exists()) {
            ejbFiles.put(META_DIR + WL_DD,
                weblogicDD);
        } else {
            log("Unable to locate weblogic deployment descriptor. It was expected to be in "
                + weblogicDD.getPath(), Project.MSG_WARN);
            return;
        }

        if (!newCMP) {
            log("The old method for locating CMP files has been DEPRECATED.", Project.MSG_VERBOSE);
            log("Please adjust your weblogic descriptor and set newCMP=\"true\" to use the new CMP descriptor inclusion mechanism. ",
                Project.MSG_VERBOSE);
            // The the WebLogic CMP deployment descriptor
            File weblogicCMPDD = new File(getConfig().descriptorDir, ddPrefix + WL_CMP_DD);

            if (weblogicCMPDD.exists()) {
                ejbFiles.put(META_DIR + WL_CMP_DD, weblogicCMPDD);
            }
        } else {
            // now that we have the WebLogic descriptor, we parse the file
            // to find other descriptors needed to deploy the bean.
            // this could be the weblogic-cmp-rdbms.xml or any other O/R
            // mapping tool descriptors.
            try {
                File ejbDescriptor = ejbFiles.get(META_DIR + EJB_DD);
                SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

                saxParserFactory.setValidating(true);

                SAXParser saxParser = saxParserFactory.newSAXParser();
                DescriptorHandler handler
                    = getWeblogicDescriptorHandler(ejbDescriptor.getParentFile());

                try (InputStream in = Files.newInputStream(weblogicDD.toPath())) {
                    saxParser.parse(new InputSource(in), handler);
                }
                ejbFiles.putAll(handler.getFiles());
            } catch (Exception e) {
                throw new BuildException(
                    "Exception while adding Vendor specific files: "
                        + e.toString(),
                    e);
            }
        }
    }

    /**
     * Get the vendor specific name of the Jar that will be output. The
     * modification date of this jar will be checked against the dependent
     * bean classes.
     */
    @Override
    File getVendorOutputJarFile(String baseName) {
        return new File(getDestDir(), baseName + jarSuffix);
    }

    /**
     * Helper method invoked by execute() for each WebLogic jar to be built.
     * Encapsulates the logic of constructing a java task for calling
     * weblogic.ejbc and executing it.
     *
     * @param sourceJar java.io.File representing the source (EJB1.1) jarfile.
     * @param destJar java.io.File representing the destination, WebLogic
     *      jarfile.
     */
    private void buildWeblogicJar(File sourceJar, File destJar, String publicId) {

        if (noEJBC) {
            try {
                FILE_UTILS.copyFile(sourceJar, destJar);
                if (!keepgenerated) {
                    sourceJar.delete();
                }
                return;
            } catch (IOException e) {
                throw new BuildException("Unable to write EJB jar", e);
            }
        }

        String ejbcClassName = ejbcClass;

        try {
            Java javaTask = new Java(getTask());
            javaTask.setTaskName("ejbc");

            javaTask.createJvmarg().setLine(additionalJvmArgs);
            sysprops.forEach(javaTask::addSysproperty);

            if (getJvmDebugLevel() != null) {
                javaTask.createJvmarg().setLine(" -Dweblogic.StdoutSeverityLevel=" + jvmDebugLevel);
            }

            if (ejbcClassName == null) {
                // try to determine it from publicId
                if (PUBLICID_EJB11.equals(publicId)) {
                    ejbcClassName = COMPILER_EJB11;
                } else if (PUBLICID_EJB20.equals(publicId)) {
                    ejbcClassName = COMPILER_EJB20;
                } else {
                    log("Unrecognized publicId " + publicId
                        + " - using EJB 1.1 compiler", Project.MSG_WARN);
                    ejbcClassName = COMPILER_EJB11;
                }
            }

            javaTask.setClassname(ejbcClassName);
            javaTask.createArg().setLine(additionalArgs);
            if (keepgenerated) {
                javaTask.createArg().setValue("-keepgenerated");
            }
            if (compiler == null) {
                // try to use the compiler specified by build.compiler.
                // Right now we are just going to allow Jikes
                String buildCompiler
                    = getTask().getProject().getProperty("build.compiler");

                if ("jikes".equals(buildCompiler)) {
                    javaTask.createArg().setValue("-compiler");
                    javaTask.createArg().setValue("jikes");
                }
            } else if (!DEFAULT_COMPILER.equals(compiler)) {
                javaTask.createArg().setValue("-compiler");
                javaTask.createArg().setLine(compiler);
            }

            Path combinedClasspath = getCombinedClasspath();
            if (wlClasspath != null && combinedClasspath != null
                    && !combinedClasspath.toString().trim().isEmpty()) {
                javaTask.createArg().setValue("-classpath");
                javaTask.createArg().setPath(combinedClasspath);
            }

            javaTask.createArg().setValue(sourceJar.getPath());
            if (outputDir == null) {
                javaTask.createArg().setValue(destJar.getPath());
            } else {
                javaTask.createArg().setValue(outputDir.getPath());
            }

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
        } catch (Exception e) {
            // Have to catch this because of the semantics of calling main()
            throw new BuildException("Exception while calling " + ejbcClassName
                + ". Details: " + e.toString(), e);
        }
    }

    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over
     * the filenames/java.io.Files in the Hashtable stored on the instance
     * variable ejbFiles.
     * @param baseName the base name.
     * @param jarFile the jar file to populate.
     * @param files   the hash table of files to write.
     * @param publicId the id to use.
     * @throws BuildException if there is a problem.
     */
    @Override
    protected void writeJar(String baseName, File jarFile, Hashtable<String, File> files,
                            String publicId) throws BuildException {
        // need to create a generic jar first.
        File genericJarFile = super.getVendorOutputJarFile(baseName);

        super.writeJar(baseName, genericJarFile, files, publicId);

        if (alwaysRebuild || isRebuildRequired(genericJarFile, jarFile)) {
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
     * @throws BuildException if there is an error.
     */
    @Override
    public void validateConfigured() throws BuildException {
        super.validateConfigured();
    }

    /**
     * Helper method to check to see if a WebLogic EJB 1.1 jar needs to be
     * rebuilt using ejbc. Called from writeJar it sees if the "Bean" classes
     * are the only thing that needs to be updated and either updates the Jar
     * with the Bean classfile or returns true, saying that the whole WebLogic
     * jar needs to be regenerated with ejbc. This allows faster build times for
     * working developers. <p>
     *
     * The way WebLogic ejbc works is it creates wrappers for the publicly
     * defined methods as they are exposed in the remote interface. If the
     * actual bean changes without changing the the method signatures then
     * only the bean classfile needs to be updated and the rest of the
     * WebLogic jar file can remain the same. If the Interfaces, ie. the
     * method signatures change or if the xml deployment descriptors changed,
     * the whole jar needs to be rebuilt with ejbc. This is not strictly true
     * for the xml files. If the JNDI name changes then the jar doesn't have to
     * be rebuild, but if the resources references change then it does. At
     * this point the WebLogic jar gets rebuilt if the xml files change at
     * all.
     *
     * @param genericJarFile java.io.File The generic jar file.
     * @param weblogicJarFile java.io.File The WebLogic jar file to check to
     *      see if it needs to be rebuilt.
     * @return true if the jar needs to be rebuilt.
     */
    // CheckStyle:MethodLength OFF - this will no be fixed
    protected boolean isRebuildRequired(File genericJarFile, File weblogicJarFile) {
        boolean rebuild = false;

        JarFile genericJar = null;
        JarFile wlJar = null;
        File newWLJarFile = null;
        JarOutputStream newJarStream = null;
        ClassLoader genericLoader = null;

        try {
            log("Checking if weblogic Jar needs to be rebuilt for jar " + weblogicJarFile.getName(),
                Project.MSG_VERBOSE);
            // Only go forward if the generic and the WebLogic file both exist
            if (genericJarFile.exists() && genericJarFile.isFile()
                 && weblogicJarFile.exists() && weblogicJarFile.isFile()) {
                //open jar files
                genericJar = new JarFile(genericJarFile);
                wlJar = new JarFile(weblogicJarFile);

                Map<String, JarEntry> replaceEntries = new HashMap<>();

                //get the list of generic jar entries
                Map<String, JarEntry> genericEntries = genericJar.stream()
                        .collect(Collectors.toMap(je -> je.getName().replace('\\', '/'),
                                je -> je, (a, b) -> b));
                // get the list of WebLogic jar entries
                Map<String, JarEntry> wlEntries = wlJar.stream().collect(Collectors.toMap(ZipEntry::getName,
                        je -> je, (a, b) -> b));

                // Cycle through generic and make sure its in WebLogic
                genericLoader = getClassLoaderFromJar(genericJarFile);

                for (String filepath : genericEntries.keySet()) {
                    if (!wlEntries.containsKey(filepath)) {
                        // a file doesn't exist rebuild
                        log("File " + filepath + " not present in weblogic jar",
                            Project.MSG_VERBOSE);
                        rebuild = true;
                        break;
                    }
                    // File name/path match
                    // Check files see if same
                    JarEntry genericEntry = genericEntries.get(filepath);
                    JarEntry wlEntry = wlEntries.get(filepath);

                    if (genericEntry.getCrc() != wlEntry.getCrc()
                        || genericEntry.getSize() != wlEntry.getSize()) {

                        if (genericEntry.getName().endsWith(".class")) {
                            //File are different see if its an object or an interface
                            String classname = genericEntry.getName()
                                    .replace(File.separatorChar, '.')
                                    .replace('/', '.');

                            classname = classname.substring(0, classname.lastIndexOf(".class"));

                            Class<?> genclass = genericLoader.loadClass(classname);

                            if (genclass.isInterface()) {
                                //Interface changed   rebuild jar.
                                log("Interface " + genclass.getName()
                                    + " has changed", Project.MSG_VERBOSE);
                                rebuild = true;
                                break;
                            }
                            //Object class Changed   update it.
                            replaceEntries.put(filepath, genericEntry);
                        } else if (!genericEntry.getName().equals("META-INF/MANIFEST.MF")) {
                            // it is not the manifest, otherwise we'd ignore it
                            // File other then class changed   rebuild
                            log("Non class file " + genericEntry.getName()
                                + " has changed", Project.MSG_VERBOSE);
                            rebuild = true;
                            break;
                        }
                    }
                }

                if (!rebuild) {
                    log("No rebuild needed - updating jar", Project.MSG_VERBOSE);
                    newWLJarFile = new File(weblogicJarFile.getAbsolutePath() + ".temp");
                    if (newWLJarFile.exists()) {
                        newWLJarFile.delete();
                    }

                    newJarStream = new JarOutputStream(Files.newOutputStream(newWLJarFile.toPath()));
                    newJarStream.setLevel(0);

                    // Copy files from old WebLogic jar
                    for (JarEntry je : wlEntries.values()) {
                        if (je.getCompressedSize() == -1
                            || je.getCompressedSize() == je.getSize()) {
                            newJarStream.setLevel(0);
                        } else {
                            newJarStream.setLevel(JAR_COMPRESS_LEVEL);
                        }

                        InputStream is;
                        // Update with changed Bean class
                        if (replaceEntries.containsKey(je.getName())) {
                            log("Updating Bean class from generic Jar "
                                + je.getName(), Project.MSG_VERBOSE);
                            // Use the entry from the generic jar
                            je = replaceEntries.get(je.getName());
                            is = genericJar.getInputStream(je);
                        } else {
                            //use file from original WebLogic jar

                            is = wlJar.getInputStream(je);
                        }
                        newJarStream.putNextEntry(new JarEntry(je.getName()));

                        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            newJarStream.write(buffer, 0, bytesRead);
                        }
                        is.close();
                    }
                } else {
                    log("Weblogic Jar rebuild needed due to changed "
                         + "interface or XML", Project.MSG_VERBOSE);
                }
            } else {
                rebuild = true;
            }
        } catch (ClassNotFoundException cnfe) {
            String cnfmsg = "ClassNotFoundException while processing ejb-jar file"
                 + ". Details: "
                 + cnfe.getMessage();

            throw new BuildException(cnfmsg, cnfe);
        } catch (IOException ioe) {
            String msg = "IOException while processing ejb-jar file "
                 + ". Details: "
                 + ioe.getMessage();

            throw new BuildException(msg, ioe);
        } finally {
            FileUtils.close(genericJar);
            FileUtils.close(wlJar);
            FileUtils.close(newJarStream);

            if (newJarStream != null) {
                try {
                    FILE_UTILS.rename(newWLJarFile, weblogicJarFile);
                } catch (IOException renameException) {
                    log(renameException.getMessage(), Project.MSG_WARN);
                    rebuild = true;
                }
            }
            if (genericLoader instanceof AntClassLoader) {
                @SuppressWarnings("resource")
                AntClassLoader loader = (AntClassLoader) genericLoader;
                loader.cleanup();
            }
        }
        return rebuild;
    }


    /**
     * Helper method invoked by isRebuildRequired to get a ClassLoader for a
     * Jar File passed to it.
     *
     * @param classjar java.io.File representing jar file to get classes from.
     * @return the classloader for the jarfile.
     * @throws IOException if there is a problem.
     */
    protected ClassLoader getClassLoaderFromJar(File classjar) throws IOException {
        Path lookupPath = new Path(getTask().getProject());

        lookupPath.setLocation(classjar);

        Path classpath = getCombinedClasspath();

        if (classpath != null) {
            lookupPath.append(classpath);
        }
        return getTask().getProject().createClassLoader(lookupPath);
    }
}
