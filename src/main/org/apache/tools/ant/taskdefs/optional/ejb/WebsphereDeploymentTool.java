/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;

/**
 * Websphere deployment tool that augments the ejbjar task.
 * Searches for the websphere specific deployment descriptors and
 * adds them to the final ejb jar file. Websphere has two specific descriptors for session
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
 * In terms of WebSphere, the generation of container code and stubs is called <code>deployment</code>.
 * This step can be performed by the websphere element as part of the jar generation process. If the
 * switch <code>ejbdeploy</code> is on, the ejbdeploy tool from the websphere toolset is called for
 * every ejb-jar. Unfortunately, this step only works, if you use the ibm jdk. Otherwise, the rmic
 * (called by ejbdeploy) throws a ClassFormatError. Be sure to switch ejbdeploy off, if run ant with
 * sun jdk.
 *
 * @author <a href="mailto:msahu@interkeel.com">Maneesh Sahu</a>
 */
public class WebsphereDeploymentTool extends GenericDeploymentTool {
    /**
     * Enumerated attribute with the values for the database vendor types
     *
     * @author Conor MacNeill
     */
    public static class DBVendor extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{
                "SQL92", "SQL99", "DB2UDBWIN_V71", "DB2UDBOS390_V6", "DB2UDBAS400_V4R5",
                "ORACLE_V8", "INFORMIX_V92", "SYBASE_V1192", "MSSQLSERVER_V7", "MYSQL_V323"
                };
        }
    }


    public static final String PUBLICID_EJB11
         = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
    public static final String PUBLICID_EJB20
         = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";
    protected static final String SCHEMA_DIR = "Schema/";

    protected static final String WAS_EXT = "ibm-ejb-jar-ext.xmi";
    protected static final String WAS_BND = "ibm-ejb-jar-bnd.xmi";
    protected static final String WAS_CMP_MAP = "Map.mapxmi";
    protected static final String WAS_CMP_SCHEMA = "Schema.dbxmi";

    /** Instance variable that stores the suffix for the websphere jarfile. */
    private String jarSuffix = ".jar";

    /** Instance variable that stores the location of the ejb 1.1 DTD file. */
    private String ejb11DTD;

    /** Instance variable that determines whether generic ejb jars are kept. */

    private boolean keepGeneric = false;

    private boolean alwaysRebuild = true;

    private boolean ejbdeploy = true;

    /** Indicates if the old CMP location convention is to be used. */
    private boolean newCMP = false;

    /** The classpath to the websphere classes. */
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

    /** the home directory for websphere */
    private File websphereHome;

    /** Get the classpath to the websphere classpaths */
    public Path createWASClasspath() {
        if (wasClasspath == null) {
            wasClasspath = new Path(getTask().getProject());
        }
        return wasClasspath.createPath();
    }


    public void setWASClasspath(Path wasClasspath) {
        this.wasClasspath = wasClasspath;
    }


    /** Sets the DB Vendor for the Entity Bean mapping ; optional.
     * Valid options are for example:
     * <ul>
     * <li>SQL92</li> <li>SQL99</li> <li>DB2UDBWIN_V71</li>
     * <li>DB2UDBOS390_V6</li> <li>DB2UDBAS400_V4R5</li> <li>ORACLE_V8</li>
     * <li>INFORMIX_V92</li> <li>SYBASE_V1192</li> <li>MYSQL_V323</li>
     * </ul>
     * This is also used to determine the name of the Map.mapxmi and
     * Schema.dbxmi files, for example Account-DB2UDBWIN_V71-Map.mapxmi
     * and Account-DB2UDBWIN_V71-Schema.dbxmi.
     */
    public void setDbvendor(DBVendor dbvendor) {
        this.dbVendor = dbvendor.getValue();
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
     * @param noinfom
     */
    public void setNoinform(boolean noinfom) {
        this.noinform = noinform;
    }


    /**
     * Flag to enable internal tracing when set, optional, default false.
     *
     * @param trace
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    /**
     * Set the rmic options.
     *
     * @param options
     */
    public void setRmicoptions(String options) {
        this.rmicOptions = options;
    }

    /**
     * Flag to use the WebSphere 3.5 compatible mapping rules ; optional, default false.
     *
     * @param attr
     */
    public void setUse35(boolean attr) {
        use35MappingRules = attr;
    }


    /**
     * Set the rebuild flag to false to only update changes in the jar rather
     * than rerunning ejbdeploy; optional, default true.
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
     * Decide, wether ejbdeploy should be called or not;
     * optional, default true.
     *
     * @param ejbdeploy
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
     */
    public void setOldCMP(boolean oldCMP) {
        this.newCMP = !oldCMP;
    }


    /**
     * Set the value of the newCMP scheme. The old CMP scheme locates the
     * websphere CMP descriptor based on the naming convention where the
     * websphere CMP file is expected to be named with the bean name as the
     * prefix. Under this scheme the name of the CMP descriptor does not match
     * the name actually used in the main websphere EJB descriptor. Also,
     * descriptors which contain multiple CMP references could not be used.
     */
    public void setNewCMP(boolean newCMP) {
        this.newCMP = newCMP;
    }


    /**
     * The directory, where ejbdeploy will write temporary files;
     * optional, defaults to '_ejbdeploy_temp'.
     */

    public void setTempdir(String tempdir) {
        this.tempdir = tempdir;
    }


    protected DescriptorHandler getDescriptorHandler(File srcDir) {
        DescriptorHandler handler = new DescriptorHandler(getTask(), srcDir);
        // register all the DTDs, both the ones that are known and
        // any supplied by the user
        handler.registerDTD(PUBLICID_EJB11, ejb11DTD);

        for (Iterator i = getConfig().dtdLocations.iterator(); i.hasNext();) {
            EjbJar.DTDLocation dtdLocation = (EjbJar.DTDLocation) i.next();

            handler.registerDTD(dtdLocation.getPublicId(), dtdLocation.getLocation());
        }

        return handler;
    }


    protected DescriptorHandler getWebsphereDescriptorHandler(final File srcDir) {
        DescriptorHandler handler =
            new DescriptorHandler(getTask(), srcDir) {
                protected void processElement() {
                }
            };

        for (Iterator i = getConfig().dtdLocations.iterator(); i.hasNext();) {
            EjbJar.DTDLocation dtdLocation = (EjbJar.DTDLocation) i.next();

            handler.registerDTD(dtdLocation.getPublicId(), dtdLocation.getLocation());
        }
        return handler;
    }


    /**
     * Add any vendor specific files which should be included in the EJB Jar.
     */
    protected void addVendorFiles(Hashtable ejbFiles, String baseName) {

        String ddPrefix = (usingBaseJarName() ? "" : baseName);
        String dbPrefix = (dbVendor == null) ? "" : dbVendor + "-";

        // Get the Extensions document
        File websphereEXT = new File(getConfig().descriptorDir, ddPrefix + WAS_EXT);

        if (websphereEXT.exists()) {
            ejbFiles.put(META_DIR + WAS_EXT,
                websphereEXT);
        } else {
            log("Unable to locate websphere extensions. It was expected to be in " +
                websphereEXT.getPath(), Project.MSG_VERBOSE);
        }

        File websphereBND = new File(getConfig().descriptorDir, ddPrefix + WAS_BND);

        if (websphereBND.exists()) {
            ejbFiles.put(META_DIR + WAS_BND,
                websphereBND);
        } else {
            log("Unable to locate websphere bindings. It was expected to be in " +
                websphereBND.getPath(), Project.MSG_VERBOSE);
        }

        if (!newCMP) {
            log("The old method for locating CMP files has been DEPRECATED.", Project.MSG_VERBOSE);
            log("Please adjust your websphere descriptor and set newCMP=\"true\" " +
                "to use the new CMP descriptor inclusion mechanism. ", Project.MSG_VERBOSE);
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
                    log("Unable to locate the websphere Map: " +
                        websphereMAP.getPath(), Project.MSG_VERBOSE);
                }

                File websphereSchema = new File(getConfig().descriptorDir,
                    ddPrefix + dbPrefix + WAS_CMP_SCHEMA);

                if (websphereSchema.exists()) {
                    ejbFiles.put(META_DIR + SCHEMA_DIR + WAS_CMP_SCHEMA,
                        websphereSchema);
                } else {
                    log("Unable to locate the websphere Schema: " +
                        websphereSchema.getPath(), Project.MSG_VERBOSE);
                }
                // Theres nothing else to see here...keep moving sonny
            } catch (Exception e) {
                String msg = "Exception while adding Vendor specific files: " +
                    e.toString();

                throw new BuildException(msg, e);
            }
        }
    }


    /**
     * Get the vendor specific name of the Jar that will be output. The
     * modification date of this jar will be checked against the dependent
     * bean classes.
     */
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
        StringBuffer options = new StringBuffer();

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
    }// end getOptions


    /**
     * Helper method invoked by execute() for each websphere jar to be built.
     * Encapsulates the logic of constructing a java task for calling
     * websphere.ejbdeploy and executing it.
     *
     * @param sourceJar java.io.File representing the source (EJB1.1) jarfile.
     * @param destJar java.io.File representing the destination, websphere
     *      jarfile.
     */
    private void buildWebsphereJar(File sourceJar, File destJar) {
        try {
            if (ejbdeploy) {
                String args =
                    " " + sourceJar.getPath() +
                    " " + tempdir +
                    " " + destJar.getPath() +
                    " " + getOptions();

                if (getCombinedClasspath() != null && getCombinedClasspath().toString().length() > 0) {
                    args += " -cp " + getCombinedClasspath();
                }

                // Why do my ""'s get stripped away???
                log("EJB Deploy Options: " + args, Project.MSG_VERBOSE);

                Java javaTask = (Java) getTask().getProject().createTask("java");
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

                Commandline.Argument arguments = javaTask.createArg();

                arguments.setLine(args);

                Path classpath = wasClasspath;

                if (classpath == null) {
                    classpath = getCombinedClasspath();
                }

                if (classpath != null) {
                    javaTask.setClasspath(classpath);
                    javaTask.setFork(true);
                } else {
                    javaTask.setFork(true);
                }

                log("Calling websphere.ejbdeploy for " + sourceJar.toString(),
                    Project.MSG_VERBOSE);

                javaTask.execute();
            }
        } catch (Exception e) {
            // Have to catch this because of the semantics of calling main()
            String msg = "Exception while calling ejbdeploy. Details: " + e.toString();

            throw new BuildException(msg, e);
        }
    }


    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over
     * the filenames/java.io.Files in the Hashtable stored on the instance
     * variable ejbFiles.
     */
    protected void writeJar(String baseName, File jarFile, Hashtable files, String publicId)
         throws BuildException {
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
     */
    public void validateConfigured() throws BuildException {
        super.validateConfigured();
        if (ejbdeploy) {
            String home = getTask().getProject().getProperty("websphere.home");
            if (home == null) {
                throw new BuildException("The 'websphere.home' property must be set when 'ejbdeploy=true'");
            }
            websphereHome = getTask().getProject().resolveFile(home);
        }
    }


    /**
     * Helper method to check to see if a websphere EBJ1.1 jar needs to be
     * rebuilt using ejbdeploy. Called from writeJar it sees if the "Bean"
     * classes are the only thing that needs to be updated and either updates
     * the Jar with the Bean classfile or returns true, saying that the whole
     * websphere jar needs to be regened with ejbdeploy. This allows faster
     * build times for working developers. <p>
     *
     * The way websphere ejbdeploy works is it creates wrappers for the
     * publicly defined methods as they are exposed in the remote interface.
     * If the actual bean changes without changing the the method signatures
     * then only the bean classfile needs to be updated and the rest of the
     * websphere jar file can remain the same. If the Interfaces, ie. the
     * method signatures change or if the xml deployment dicriptors changed,
     * the whole jar needs to be rebuilt with ejbdeploy. This is not strictly
     * true for the xml files. If the JNDI name changes then the jar doesnt
     * have to be rebuild, but if the resources references change then it
     * does. At this point the websphere jar gets rebuilt if the xml files
     * change at all.
     *
     * @param genericJarFile java.io.File The generic jar file.
     * @param websphereJarFile java.io.File The websphere jar file to check to
     *      see if it needs to be rebuilt.
     */
    protected boolean isRebuildRequired(File genericJarFile, File websphereJarFile) {
        boolean rebuild = false;

        JarFile genericJar = null;
        JarFile wasJar = null;
        File newwasJarFile = null;
        JarOutputStream newJarStream = null;

        try {
            log("Checking if websphere Jar needs to be rebuilt for jar " + websphereJarFile.getName(),
                Project.MSG_VERBOSE);
            // Only go forward if the generic and the websphere file both exist
            if (genericJarFile.exists() && genericJarFile.isFile()
                 && websphereJarFile.exists() && websphereJarFile.isFile()) {
                //open jar files
                genericJar = new JarFile(genericJarFile);
                wasJar = new JarFile(websphereJarFile);

                Hashtable genericEntries = new Hashtable();
                Hashtable wasEntries = new Hashtable();
                Hashtable replaceEntries = new Hashtable();

                //get the list of generic jar entries
                for (Enumeration e = genericJar.entries(); e.hasMoreElements();) {
                    JarEntry je = (JarEntry) e.nextElement();

                    genericEntries.put(je.getName().replace('\\', '/'), je);
                }
                //get the list of websphere jar entries
                for (Enumeration e = wasJar.entries(); e.hasMoreElements();) {
                    JarEntry je = (JarEntry) e.nextElement();

                    wasEntries.put(je.getName(), je);
                }

                //Cycle Through generic and make sure its in websphere
                ClassLoader genericLoader = getClassLoaderFromJar(genericJarFile);

                for (Enumeration e = genericEntries.keys(); e.hasMoreElements();) {
                    String filepath = (String) e.nextElement();

                    if (wasEntries.containsKey(filepath)) {
                        // File name/path match
                        // Check files see if same
                        JarEntry genericEntry = (JarEntry) genericEntries.get(filepath);
                        JarEntry wasEntry = (JarEntry) wasEntries.get(filepath);

                        if ((genericEntry.getCrc() != wasEntry.getCrc()) ||
                            (genericEntry.getSize() != wasEntry.getSize())) {

                            if (genericEntry.getName().endsWith(".class")) {
                                //File are different see if its an object or an interface
                                String classname = genericEntry.getName().replace(File.separatorChar, '.');

                                classname = classname.substring(0, classname.lastIndexOf(".class"));

                                Class genclass = genericLoader.loadClass(classname);

                                if (genclass.isInterface()) {
                                    //Interface changed   rebuild jar.
                                    log("Interface " + genclass.getName() + " has changed", Project.MSG_VERBOSE);
                                    rebuild = true;
                                    break;
                                } else {
                                    //Object class Changed   update it.
                                    replaceEntries.put(filepath, genericEntry);
                                }
                            } else {
                                // is it the manifest. If so ignore it
                                if (!genericEntry.getName().equals("META-INF/MANIFEST.MF")) {
                                    //File other then class changed   rebuild
                                    log("Non class file " + genericEntry.getName() + " has changed", Project.MSG_VERBOSE);
                                    rebuild = true;
                                }
                                break;
                            }
                        }
                    } else {// a file doesnt exist rebuild

                        log("File " + filepath + " not present in websphere jar", Project.MSG_VERBOSE);
                        rebuild = true;
                        break;
                    }
                }

                if (!rebuild) {
                    log("No rebuild needed - updating jar", Project.MSG_VERBOSE);
                    newwasJarFile = new File(websphereJarFile.getAbsolutePath() + ".temp");
                    if (newwasJarFile.exists()) {
                        newwasJarFile.delete();
                    }

                    newJarStream = new JarOutputStream(new FileOutputStream(newwasJarFile));
                    newJarStream.setLevel(0);

                    //Copy files from old websphere jar
                    for (Enumeration e = wasEntries.elements(); e.hasMoreElements();) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        InputStream is;
                        JarEntry je = (JarEntry) e.nextElement();

                        if (je.getCompressedSize() == -1 ||
                            je.getCompressedSize() == je.getSize()) {
                            newJarStream.setLevel(0);
                        } else {
                            newJarStream.setLevel(9);
                        }

                        // Update with changed Bean class
                        if (replaceEntries.containsKey(je.getName())) {
                            log("Updating Bean class from generic Jar " + je.getName(),
                                Project.MSG_VERBOSE);
                            // Use the entry from the generic jar
                            je = (JarEntry) replaceEntries.get(je.getName());
                            is = genericJar.getInputStream(je);
                        } else {//use fle from original websphere jar

                            is = wasJar.getInputStream(je);
                        }
                        newJarStream.putNextEntry(new JarEntry(je.getName()));

                        while ((bytesRead = is.read(buffer)) != -1) {
                            newJarStream.write(buffer, 0, bytesRead);
                        }
                        is.close();
                    }
                } else {
                    log("websphere Jar rebuild needed due to changed interface or XML", Project.MSG_VERBOSE);
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
            // need to close files and perhaps rename output
            if (genericJar != null) {
                try {
                    genericJar.close();
                } catch (IOException closeException) {
                }
            }

            if (wasJar != null) {
                try {
                    wasJar.close();
                } catch (IOException closeException) {
                }
            }

            if (newJarStream != null) {
                try {
                    newJarStream.close();
                } catch (IOException closeException) {
                }

                websphereJarFile.delete();
                newwasJarFile.renameTo(websphereJarFile);
                if (!websphereJarFile.exists()) {
                    rebuild = true;
                }
            }
        }

        return rebuild;
    }


    /**
     * Helper method invoked by isRebuildRequired to get a ClassLoader for a
     * Jar File passed to it.
     *
     * @param classjar java.io.File representing jar file to get classes from.
     */
    protected ClassLoader getClassLoaderFromJar(File classjar) throws IOException {
        Path lookupPath = new Path(getTask().getProject());

        lookupPath.setLocation(classjar);

        Path classpath = getCombinedClasspath();

        if (classpath != null) {
            lookupPath.append(classpath);
        }

        return new AntClassLoader(getTask().getProject(), lookupPath);
    }
}

