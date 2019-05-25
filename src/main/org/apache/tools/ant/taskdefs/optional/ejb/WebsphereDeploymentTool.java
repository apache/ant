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
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 * WebSphere deployment tool that augments the ejbjar task.
 * Searches for the WebSphere specific deployment descriptors and
 * adds them to the final ejb jar file. WebSphere has two specific descriptors for session
 * beans:
 * <ul>
 *    <li>ibm-ejb-jar-bnd.xmi</li>
 *    <li>ibm-ejb-jar-ext.xmi</li>
 * </ul>
 * and another two for container managed entity beans:
 * <ul>
 *    <li>Map.mapxmi</li>
 *    <li>Schema.dbxmi</li>
 * </ul>
 * In terms of WebSphere, the generation of container code and stubs is
 * called <code>deployment</code>. This step can be performed by the websphere
 * element as part of the jar generation process. If the switch
 * <code>ejbdeploy</code> is on, the ejbdeploy tool from the WebSphere toolset
 * is called for every ejb-jar. Unfortunately, this step only works, if you
 * use the ibm jdk. Otherwise, the rmic (called by ejbdeploy) throws a
 * ClassFormatError. Be sure to switch ejbdeploy off, if run ant with
 * sun jdk.
 *
 */
public class WebsphereDeploymentTool extends GenericDeploymentTool {

    /** ID for ejb 1.1 */
    public static final String PUBLICID_EJB11
         = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
    /** ID for ejb 2.0 */
    public static final String PUBLICID_EJB20
         = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";
    /** Schema directory */
    protected static final String SCHEMA_DIR = "Schema/";

    protected static final String WAS_EXT = "ibm-ejb-jar-ext.xmi";
    protected static final String WAS_BND = "ibm-ejb-jar-bnd.xmi";
    protected static final String WAS_CMP_MAP = "Map.mapxmi";
    protected static final String WAS_CMP_SCHEMA = "Schema.dbxmi";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** Instance variable that stores the suffix for the WebSphere jarfile. */
    private String jarSuffix = ".jar";

    /** Instance variable that stores the location of the ejb 1.1 DTD file. */
    private String ejb11DTD;

    /** Instance variable that determines whether generic ejb jars are kept. */

    private boolean keepGeneric = false;

    private boolean alwaysRebuild = true;

    private boolean ejbdeploy = true;

    /** Indicates if the old CMP location convention is to be used. */
    private boolean newCMP = false;

    /** The classpath to the WebSphere classes. */
    private Path wasClasspath = null;

    /** The DB Vendor name, the EJB is persisted against */
    private String dbVendor;

    /** The name of the database to create. (For top-down mapping only) */
    private String dbName;

    /** The name of the schema to create. (For top-down mappings only) */
    private String dbSchema;

    /** true - Only generate the deployment code, do not run RMIC or Javac */
    private boolean codegen;

    /** true - Only output error messages, suppress informational messages */
    private boolean quiet = true;

    /** true - Disable the validation steps */
    private boolean novalidate;

    /** true - Disable warning and informational messages */
    private boolean nowarn;

    /** true - Disable informational messages */
    private boolean noinform;

    /** true - Enable internal tracing */
    private boolean trace;

    /** Additional options for RMIC */
    private String rmicOptions;

    /** true- Use the WebSphere 3.5 compatible mapping rules */
    private boolean use35MappingRules;

    /** the scratchdir for the ejbdeploy operation */
    private String tempdir = "_ejbdeploy_temp";

    /** the home directory for WebSphere */
    private File websphereHome;

    /**
     * Get the classpath to the WebSphere classpaths.
     * @return the WebSphere classpath.
     */
    public Path createWASClasspath() {
        if (wasClasspath == null) {
            wasClasspath = new Path(getTask().getProject());
        }
        return wasClasspath.createPath();
    }

    /**
     * Set the WebSphere classpath.
     * @param wasClasspath the WebSphere classpath.
     */
    public void setWASClasspath(Path wasClasspath) {
        this.wasClasspath = wasClasspath;
    }

    /** Sets the DB Vendor for the Entity Bean mapping; optional.
     * <p>
     * Valid options can be obtained by running the following command:
     * <code>
     * &lt;WAS_HOME&gt;/bin/EJBDeploy.[sh/bat] -help
     * </code>
     * </p>
     * <p>
     * This is also used to determine the name of the Map.mapxmi and
     * Schema.dbxmi files, for example Account-DB2UDB_V81-Map.mapxmi
     * and Account-DB2UDB_V81-Schema.dbxmi.
     * </p>
     *
     * @param dbvendor database vendor type
     */
    public void setDbvendor(String dbvendor) {
        this.dbVendor = dbvendor;
    }

    /**
     * Sets the name of the Database to create; optional.
     *
     * @param dbName name of the database
     */
    public void setDbname(String dbName) {
        this.dbName = dbName;
    }

    /**
     * Sets the name of the schema to create; optional.
     *
     * @param dbSchema name of the schema
     */
    public void setDbschema(String dbSchema) {
        this.dbSchema = dbSchema;
    }

    /**
     * Flag, default false, to only generate the deployment
     * code, do not run RMIC or Javac
     *
     * @param codegen option
     */
    public void setCodegen(boolean codegen) {
        this.codegen = codegen;
    }

    /**
     * Flag, default true, to only output error messages.
     *
     * @param quiet option
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    /**
     * Flag to disable the validation steps; optional, default false.
     *
     * @param novalidate option
     */
    public void setNovalidate(boolean novalidate) {
        this.novalidate = novalidate;
    }

    /**
     * Flag to disable warning and informational messages; optional, default false.
     *
     * @param nowarn option
     */
    public void setNowarn(boolean nowarn) {
        this.nowarn = nowarn;
    }

    /**
     * Flag to disable informational messages; optional, default false.
     *
     * @param noinform if true disables informational messages
     */
    public void setNoinform(boolean noinform) {
        this.noinform = noinform;
    }

    /**
     * Flag to enable internal tracing when set, optional, default false.
     *
     * @param trace a <code>boolean</code> value.
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    /**
     * Set the rmic options.
     *
     * @param options the options to use.
     */
    public void setRmicoptions(String options) {
        this.rmicOptions = options;
    }

    /**
     * Flag to use the WebSphere 3.5 compatible mapping rules; optional, default false.
     *
     * @param attr a <code>boolean</code> value.
     */
    public void setUse35(boolean attr) {
        use35MappingRules = attr;
    }

    /**
     * Set the rebuild flag to false to only update changes in the jar rather
     * than rerunning ejbdeploy; optional, default true.
     * @param rebuild a <code>boolean</code> value.
     */
    public void setRebuild(boolean rebuild) {
        this.alwaysRebuild = rebuild;
    }

    /**
     * String value appended to the basename of the deployment
     * descriptor to create the filename of the WebLogic EJB
     * jar file. Optional, default '.jar'.
     * @param inString the string to use as the suffix.
     */
    public void setSuffix(String inString) {
        this.jarSuffix = inString;
    }

    /**
     * This controls whether the generic file used as input to
     * ejbdeploy is retained; optional, default false.
     * @param inValue either 'true' or 'false'.
     */
    public void setKeepgeneric(boolean inValue) {
        this.keepGeneric = inValue;
    }

    /**
     * Decide, whether ejbdeploy should be called or not;
     * optional, default true.
     *
     * @param ejbdeploy a <code>boolean</code> value.
     */
    public void setEjbdeploy(boolean ejbdeploy) {
        this.ejbdeploy = ejbdeploy;
    }

    /**
     * Setter used to store the location of the Sun's Generic EJB DTD. This
     * can be a file on the system or a resource on the classpath.
     *
     * @param inString the string to use as the DTD location.
     */
    public void setEJBdtd(String inString) {
        this.ejb11DTD = inString;
    }

    /**
     * Set the value of the oldCMP scheme. This is an antonym for newCMP
     * @ant.attribute ignore="true"
     * @param oldCMP a <code>boolean</code> value.
     */
    public void setOldCMP(boolean oldCMP) {
        this.newCMP = !oldCMP;
    }

    /**
     * Set the value of the newCMP scheme. The old CMP scheme locates the
     * WebSphere CMP descriptor based on the naming convention where the
     * WebSphere CMP file is expected to be named with the bean name as the
     * prefix. Under this scheme the name of the CMP descriptor does not match
     * the name actually used in the main WebSphere EJB descriptor. Also,
     * descriptors which contain multiple CMP references could not be used.
     * @param newCMP a <code>boolean</code> value.
     */
    public void setNewCMP(boolean newCMP) {
        this.newCMP = newCMP;
    }

    /**
     * The directory, where ejbdeploy will write temporary files;
     * optional, defaults to '_ejbdeploy_temp'.
     * @param tempdir the directory name to use.
     */
    public void setTempdir(String tempdir) {
        this.tempdir = tempdir;
    }

    /** {@inheritDoc}. */
    @Override
    protected DescriptorHandler getDescriptorHandler(File srcDir) {
        DescriptorHandler handler = new DescriptorHandler(getTask(), srcDir);
        // register all the DTDs, both the ones that are known and
        // any supplied by the user
        handler.registerDTD(PUBLICID_EJB11, ejb11DTD);

        getConfig().dtdLocations.forEach(l -> handler.registerDTD(l.getPublicId(), l.getLocation()));
        return handler;
    }

    /**
     * Get a description handler.
     * @param srcDir the source directory.
     * @return the handler.
     */
    protected DescriptorHandler getWebsphereDescriptorHandler(final File srcDir) {
        DescriptorHandler handler =
            new DescriptorHandler(getTask(), srcDir) {
                @Override
                protected void processElement() {
                }
            };

        getConfig().dtdLocations.forEach(l -> handler.registerDTD(l.getPublicId(), l.getLocation()));
        return handler;
    }


    /**
     * Add any vendor specific files which should be included in the EJB Jar.
     * @param ejbFiles a hashtable entryname -&gt; file.
     * @param baseName a prefix to use.
     */
    @Override
    protected void addVendorFiles(Hashtable<String, File> ejbFiles, String baseName) {

        String ddPrefix = usingBaseJarName() ? "" : baseName;
        String dbPrefix = (dbVendor == null) ? "" : dbVendor + "-";

        // Get the Extensions document
        File websphereEXT = new File(getConfig().descriptorDir, ddPrefix + WAS_EXT);

        if (websphereEXT.exists()) {
            ejbFiles.put(META_DIR + WAS_EXT,
                websphereEXT);
        } else {
            log("Unable to locate websphere extensions. It was expected to be in "
                + websphereEXT.getPath(), Project.MSG_VERBOSE);
        }

        File websphereBND = new File(getConfig().descriptorDir, ddPrefix + WAS_BND);

        if (websphereBND.exists()) {
            ejbFiles.put(META_DIR + WAS_BND,
                websphereBND);
        } else {
            log("Unable to locate websphere bindings. It was expected to be in "
                + websphereBND.getPath(), Project.MSG_VERBOSE);
        }

        if (!newCMP) {
            log("The old method for locating CMP files has been DEPRECATED.",
                Project.MSG_VERBOSE);
            log("Please adjust your websphere descriptor and set newCMP=\"true\" to use the new CMP descriptor inclusion mechanism. ",
                Project.MSG_VERBOSE);
        } else {
            // We attempt to put in the MAP and Schema files of CMP beans
            try {
                // Add the Map file
                File websphereMAP = new File(getConfig().descriptorDir,
                    ddPrefix + dbPrefix + WAS_CMP_MAP);

                if (websphereMAP.exists()) {
                    ejbFiles.put(META_DIR + WAS_CMP_MAP,
                        websphereMAP);
                } else {
                    log("Unable to locate the websphere Map: "
                        + websphereMAP.getPath(), Project.MSG_VERBOSE);
                }

                File websphereSchema = new File(getConfig().descriptorDir,
                    ddPrefix + dbPrefix + WAS_CMP_SCHEMA);

                if (websphereSchema.exists()) {
                    ejbFiles.put(META_DIR + SCHEMA_DIR + WAS_CMP_SCHEMA,
                        websphereSchema);
                } else {
                    log("Unable to locate the websphere Schema: "
                        + websphereSchema.getPath(), Project.MSG_VERBOSE);
                }
                // There is nothing else to see here...keep moving sonny
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
     * Gets the options for the EJB Deploy operation
     *
     * @return String
     */
    protected String getOptions() {
        // Set the options
        StringBuilder options = new StringBuilder();

        if (dbVendor != null) {
            options.append(" -dbvendor ").append(dbVendor);
        }
        if (dbName != null) {
            options.append(" -dbname \"").append(dbName).append("\"");
        }

        if (dbSchema != null) {
            options.append(" -dbschema \"").append(dbSchema).append("\"");
        }

        if (codegen) {
            options.append(" -codegen");
        }

        if (quiet) {
            options.append(" -quiet");
        }

        if (novalidate) {
            options.append(" -novalidate");
        }

        if (nowarn) {
            options.append(" -nowarn");
        }

        if (noinform) {
            options.append(" -noinform");
        }

        if (trace) {
            options.append(" -trace");
        }

        if (use35MappingRules) {
            options.append(" -35");
        }

        if (rmicOptions != null) {
            options.append(" -rmic \"").append(rmicOptions).append("\"");
        }

        return options.toString();
    }

    /**
     * Helper method invoked by execute() for each WebSphere jar to be built.
     * Encapsulates the logic of constructing a java task for calling
     * websphere.ejbdeploy and executing it.
     *
     * @param sourceJar java.io.File representing the source (EJB1.1) jarfile.
     * @param destJar java.io.File representing the destination, WebSphere
     *      jarfile.
     */
    private void buildWebsphereJar(File sourceJar, File destJar) {
        try {
            if (ejbdeploy) {
                Java javaTask = new Java(getTask());
                // Set the JvmArgs
                javaTask.createJvmarg().setValue("-Xms64m");
                javaTask.createJvmarg().setValue("-Xmx128m");

                // Set the Environment variable
                Environment.Variable var = new Environment.Variable();

                var.setKey("websphere.lib.dir");
                File libdir = new File(websphereHome, "lib");
                var.setValue(libdir.getAbsolutePath());
                javaTask.addSysproperty(var);

                // Set the working directory
                javaTask.setDir(websphereHome);

                // Set the Java class name
                javaTask.setTaskName("ejbdeploy");
                javaTask.setClassname("com.ibm.etools.ejbdeploy.EJBDeploy");

                javaTask.createArg().setValue(sourceJar.getPath());
                javaTask.createArg().setValue(tempdir);
                javaTask.createArg().setValue(destJar.getPath());
                javaTask.createArg().setLine(getOptions());
                if (getCombinedClasspath() != null
                    && !getCombinedClasspath().toString().isEmpty()) {
                    javaTask.createArg().setValue("-cp");
                    javaTask.createArg().setValue(getCombinedClasspath().toString());
                }

                Path classpath = wasClasspath;

                if (classpath == null) {
                    classpath = getCombinedClasspath();
                }

                javaTask.setFork(true);
                if (classpath != null) {
                    javaTask.setClasspath(classpath);
                }

                log("Calling websphere.ejbdeploy for " + sourceJar.toString(),
                    Project.MSG_VERBOSE);

                javaTask.execute();
            }
        } catch (Exception e) {
            // Have to catch this because of the semantics of calling main()
            throw new BuildException(
                "Exception while calling ejbdeploy. Details: " + e.toString(),
                e);
        }
    }

    /** {@inheritDoc}. */
    @Override
    protected void writeJar(String baseName, File jarFile,
        Hashtable<String, File> files, String publicId) throws BuildException {
        if (ejbdeploy) {
            // create the -generic.jar, if required
            File genericJarFile = super.getVendorOutputJarFile(baseName);

            super.writeJar(baseName, genericJarFile, files, publicId);

            // create the output .jar, if required
            if (alwaysRebuild || isRebuildRequired(genericJarFile, jarFile)) {
                buildWebsphereJar(genericJarFile, jarFile);
            }
            if (!keepGeneric) {
                log("deleting generic jar " + genericJarFile.toString(),
                    Project.MSG_VERBOSE);
                genericJarFile.delete();
            }
        } else {
            // create the "undeployed" output .jar, if required
            super.writeJar(baseName, jarFile, files, publicId);
        }
    }

    /**
     * Called to validate that the tool parameters have been configured.
     * @throws BuildException if there is an error.
     */
    @Override
    public void validateConfigured() throws BuildException {
        super.validateConfigured();
        if (ejbdeploy) {
            String home = getTask().getProject().getProperty("websphere.home");
            if (home == null) {
                throw new BuildException(
                    "The 'websphere.home' property must be set when 'ejbdeploy=true'");
            }
            websphereHome = getTask().getProject().resolveFile(home);
        }
    }

    /**
     * Helper method to check to see if a WebSphere EJB 1.1 jar needs to be
     * rebuilt using ejbdeploy. Called from writeJar it sees if the "Bean"
     * classes are the only thing that needs to be updated and either updates
     * the Jar with the Bean classfile or returns true, saying that the whole
     * WebSphere jar needs to be regenerated with ejbdeploy. This allows faster
     * build times for working developers. <p>
     *
     * The way WebSphere ejbdeploy works is it creates wrappers for the
     * publicly defined methods as they are exposed in the remote interface.
     * If the actual bean changes without changing the the method signatures
     * then only the bean classfile needs to be updated and the rest of the
     * WebSphere jar file can remain the same. If the Interfaces, ie. the
     * method signatures change or if the xml deployment descriptors changed,
     * the whole jar needs to be rebuilt with ejbdeploy. This is not strictly
     * true for the xml files. If the JNDI name changes then the jar doesn't
     * have to be rebuild, but if the resources references change then it
     * does. At this point the WebSphere jar gets rebuilt if the xml files
     * change at all.
     *
     * @param genericJarFile java.io.File The generic jar file.
     * @param websphereJarFile java.io.File The WebSphere jar file to check to
     *      see if it needs to be rebuilt.
     * @return true if a rebuild is required.
     */
    // CheckStyle:MethodLength OFF - this will no be fixed
    protected boolean isRebuildRequired(File genericJarFile, File websphereJarFile) {
        boolean rebuild = false;

        JarFile genericJar = null;
        JarFile wasJar = null;
        File newwasJarFile = null;
        JarOutputStream newJarStream = null;
        ClassLoader genericLoader = null;

        try {
            log("Checking if websphere Jar needs to be rebuilt for jar "
                + websphereJarFile.getName(), Project.MSG_VERBOSE);
            // Only go forward if the generic and the WebSphere file both exist
            if (genericJarFile.exists() && genericJarFile.isFile()
                 && websphereJarFile.exists() && websphereJarFile.isFile()) {
                //open jar files
                genericJar = new JarFile(genericJarFile);
                wasJar = new JarFile(websphereJarFile);

                //get the list of generic jar entries
                Map<String, JarEntry> genericEntries = genericJar.stream()
                        .collect(Collectors.toMap(je -> je.getName().replace('\\', '/'),
                                je -> je, (a, b) -> b));

                // get the list of WebSphere jar entries
                Map<String, JarEntry> wasEntries = wasJar.stream()
                        .collect(Collectors.toMap(ZipEntry::getName, je -> je, (a, b) -> b));

                // Cycle through generic and make sure its in WebSphere
                genericLoader = getClassLoaderFromJar(genericJarFile);

                Map<String, JarEntry> replaceEntries = new HashMap<>();
                for (String filepath : genericEntries.keySet()) {
                    if (!wasEntries.containsKey(filepath)) {
                        // a file doesn't exist rebuild
                        log("File " + filepath + " not present in websphere jar",
                            Project.MSG_VERBOSE);
                        rebuild = true;
                        break;
                    }
                    // File name/path match
                    // Check files see if same
                    JarEntry genericEntry = genericEntries.get(filepath);
                    JarEntry wasEntry = wasEntries.get(filepath);

                    if (genericEntry.getCrc() != wasEntry.getCrc()
                        || genericEntry.getSize() != wasEntry.getSize()) {

                        if (genericEntry.getName().endsWith(".class")) {
                            //File are different see if its an object or an interface
                            String classname
                                = genericEntry.getName().replace(File.separatorChar, '.');

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
                        } else {
                            // is it the manifest. If so ignore it
                            if (!genericEntry.getName().equals("META-INF/MANIFEST.MF")) {
                                //File other then class changed  rebuild
                                log("Non class file " + genericEntry.getName()
                                    + " has changed", Project.MSG_VERBOSE);
                                rebuild = true;
                            }
                            break;
                        }
                    }
                }

                if (!rebuild) {
                    log("No rebuild needed - updating jar", Project.MSG_VERBOSE);
                    newwasJarFile = new File(websphereJarFile.getAbsolutePath() + ".temp");
                    if (newwasJarFile.exists()) {
                        newwasJarFile.delete();
                    }

                    newJarStream = new JarOutputStream(Files.newOutputStream(newwasJarFile.toPath()));
                    newJarStream.setLevel(0);

                    // Copy files from old WebSphere jar
                    for (JarEntry je : wasEntries.values()) {
                        if (je.getCompressedSize() == -1
                            || je.getCompressedSize() == je.getSize()) {
                            newJarStream.setLevel(0);
                        } else {
                            newJarStream.setLevel(JAR_COMPRESS_LEVEL);
                        }

                        InputStream is;
                        // Update with changed Bean class
                        if (replaceEntries.containsKey(je.getName())) {
                            log("Updating Bean class from generic Jar " + je.getName(),
                                Project.MSG_VERBOSE);
                            // Use the entry from the generic jar
                            je = replaceEntries.get(je.getName());
                            is = genericJar.getInputStream(je);
                        } else {
                            // use fle from original WebSphere jar

                            is = wasJar.getInputStream(je);
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
                    log("websphere Jar rebuild needed due to changed "
                        + "interface or XML", Project.MSG_VERBOSE);
                }
            } else {
                rebuild = true;
            }
        } catch (ClassNotFoundException cnfe) {
            throw new BuildException(
                "ClassNotFoundException while processing ejb-jar file. Details: "
                    + cnfe.getMessage(),
                cnfe);
        } catch (IOException ioe) {
            throw new BuildException(
                "IOException while processing ejb-jar file . Details: "
                    + ioe.getMessage(),
                ioe);
        } finally {
            // need to close files and perhaps rename output
            FileUtils.close(genericJar);
            FileUtils.close(wasJar);
            FileUtils.close(newJarStream);

            if (newJarStream != null) {
                try {
                    FILE_UTILS.rename(newwasJarFile, websphereJarFile);
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
     * @return a classloader for the jar file.
     * @throws IOException if there is an error.
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
