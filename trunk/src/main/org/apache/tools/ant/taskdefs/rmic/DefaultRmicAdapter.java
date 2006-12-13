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

package org.apache.tools.ant.taskdefs.rmic;

import java.io.File;
import java.util.Random;
import java.util.Vector;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Rmic;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * This is the default implementation for the RmicAdapter interface.
 * Currently, this is a cut-and-paste of the original rmic task and
 * DefaultCopmpilerAdapter.
 *
 * @since Ant 1.4
 */
public abstract class DefaultRmicAdapter implements RmicAdapter {

    private Rmic attributes;
    private FileNameMapper mapper;
    private static final Random RAND = new Random();
    /** suffix denoting a stub file */
    public static final String RMI_STUB_SUFFIX = "_Stub";
    /** suffix denoting a skel file */
    public static final String RMI_SKEL_SUFFIX = "_Skel";
    /** suffix denoting a tie file */
    public static final String RMI_TIE_SUFFIX = "_Tie";
    /** arg for compat */
    public static final String STUB_COMPAT = "-vcompat";
    /** arg for 1.1 */
    public static final String STUB_1_1 = "-v1.1";
    /** arg for 1.2 */
    public static final String STUB_1_2 = "-v1.2";

    /**
     * Default constructor
     */
    public DefaultRmicAdapter() {
    }

    /**
     * Sets Rmic attributes
     * @param attributes the rmic attributes
     */
    public void setRmic(final Rmic attributes) {
        this.attributes = attributes;
        mapper = new RmicFileNameMapper();
    }

    /**
     * Get the Rmic attributes
     * @return the attributes as a Rmic taskdef
     */
    public Rmic getRmic() {
        return attributes;
    }

    /**
     * Gets the stub class suffix
     * @return the stub suffix &quot;_Stub&quot;
     */
    protected String getStubClassSuffix() {
        return RMI_STUB_SUFFIX;
    }

    /**
     * Gets the skeleton class suffix
     * @return the skeleton suffix &quot;_Skel&quot;
     */
    protected String getSkelClassSuffix() {
        return RMI_SKEL_SUFFIX;
    }

    /**
     * Gets the tie class suffix
     * @return the tie suffix &quot;_Tie&quot;
     */
    protected String getTieClassSuffix() {
        return RMI_TIE_SUFFIX;
    }

    /**
     * This implementation returns a mapper that may return up to two
     * file names.
     *
     * <ul>
     *   <li>for JRMP it will return *_getStubClassSuffix (and
     *   *_getSkelClassSuffix if JDK 1.1 is used)</li>
     *
     *   <li>for IDL it will return a random name, causing &lt;rmic&gt; to
     *     always recompile.</li>
     *
     *   <li>for IIOP it will return _*_getStubClassSuffix for
     *   interfaces and _*_getStubClassSuffix for non-interfaces (and
     *   determine the interface and create _*_Stub from that).</li>
     * </ul>
     * @return a <code>FileNameMapper</code>
     */
    public FileNameMapper getMapper() {
        return mapper;
    }

    /**
     * Gets the CLASSPATH this rmic process will use.
     * @return the classpath
     */
    public Path getClasspath() {
        return getCompileClasspath();
    }

    /**
     * Builds the compilation classpath.
     * @return the classpath
     */
    protected Path getCompileClasspath() {
        Path classpath = new Path(attributes.getProject());
        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath
        classpath.setLocation(attributes.getBase());

        // Combine the build classpath with the system classpath, in an
        // order determined by the value of build.sysclasspath

        Path cp = attributes.getClasspath();
        if (cp == null) {
            cp = new Path(attributes.getProject());
        }
        if (attributes.getIncludeantruntime()) {
            classpath.addExisting(cp.concatSystemClasspath("last"));
        } else {
            classpath.addExisting(cp.concatSystemClasspath("ignore"));
        }

        if (attributes.getIncludejavaruntime()) {
            classpath.addJavaRuntime();
        }
        return classpath;
    }

    /**
     * Setup rmic argument for rmic.
     * @return the command line
     */
    protected Commandline setupRmicCommand() {
        return setupRmicCommand(null);
    }

    /**
     * Setup rmic argument for rmic.
     * @param options additional parameters needed by a specific
     *                implementation.
     * @return the command line
     */
    protected Commandline setupRmicCommand(String[] options) {
        Commandline cmd = new Commandline();

        if (options != null) {
            for (int i = 0; i < options.length; i++) {
                cmd.createArgument().setValue(options[i]);
            }
        }

        Path classpath = getCompileClasspath();

        cmd.createArgument().setValue("-d");
        cmd.createArgument().setFile(attributes.getBase());

        if (attributes.getExtdirs() != null) {
            cmd.createArgument().setValue("-extdirs");
            cmd.createArgument().setPath(attributes.getExtdirs());
        }

        cmd.createArgument().setValue("-classpath");
        cmd.createArgument().setPath(classpath);

        //handle the many different stub options.
        String stubVersion = attributes.getStubVersion();
        //default is compatibility
        String stubOption = null;
        if (null != stubVersion) {
            if ("1.1".equals(stubVersion)) {
                stubOption = STUB_1_1;
            } else if ("1.2".equals(stubVersion)) {
                stubOption = STUB_1_2;
            } else if ("compat".equals(stubVersion)) {
                stubOption = STUB_COMPAT;
            } else {
                //anything else
                attributes.log("Unknown stub option " + stubVersion);
                //do nothing with the value? or go -v+stubVersion??
            }
        }
        //for java1.5+, we generate compatible stubs, that is, unless
        //the caller asked for IDL or IIOP support.
        if (stubOption == null
            && !attributes.getIiop()
            && !attributes.getIdl()) {
            stubOption = STUB_COMPAT;
        }
        if (stubOption != null) {
            //set the non-null stubOption
            cmd.createArgument().setValue(stubOption);
        }
        if (null != attributes.getSourceBase()) {
            cmd.createArgument().setValue("-keepgenerated");
        }

        if (attributes.getIiop()) {
            attributes.log("IIOP has been turned on.", Project.MSG_INFO);
            cmd.createArgument().setValue("-iiop");
            if (attributes.getIiopopts() != null) {
                attributes.log("IIOP Options: " + attributes.getIiopopts(),
                               Project.MSG_INFO);
                cmd.createArgument().setValue(attributes.getIiopopts());
            }
        }

        if (attributes.getIdl())  {
            cmd.createArgument().setValue("-idl");
            attributes.log("IDL has been turned on.", Project.MSG_INFO);
            if (attributes.getIdlopts() != null) {
                cmd.createArgument().setValue(attributes.getIdlopts());
                attributes.log("IDL Options: " + attributes.getIdlopts(),
                               Project.MSG_INFO);
            }
        }

        if (attributes.getDebug()) {
            cmd.createArgument().setValue("-g");
        }

        cmd.addArguments(attributes.getCurrentCompilerArgs());

        logAndAddFilesToCompile(cmd);
        return cmd;
     }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &quot;niceSourceList&quot;
     * @param cmd the commandline args
     */
    protected void logAndAddFilesToCompile(Commandline cmd) {
        Vector compileList = attributes.getCompileList();

        attributes.log("Compilation " + cmd.describeArguments(),
                       Project.MSG_VERBOSE);

        StringBuffer niceSourceList = new StringBuffer("File");
        int cListSize = compileList.size();
        if (cListSize != 1) {
            niceSourceList.append("s");
        }
        niceSourceList.append(" to be compiled:");

        for (int i = 0; i < cListSize; i++) {
            String arg = (String) compileList.elementAt(i);
            cmd.createArgument().setValue(arg);
            niceSourceList.append("    ");
            niceSourceList.append(arg);
        }

        attributes.log(niceSourceList.toString(), Project.MSG_VERBOSE);
    }

    /**
     * Mapper that may return up to two file names.
     *
     * <ul>
     *   <li>for JRMP it will return *_getStubClassSuffix (and
     *   *_getSkelClassSuffix if JDK 1.1 is used)</li>
     *
     *   <li>for IDL it will return a random name, causing <rmic> to
     *     always recompile.</li>
     *
     *   <li>for IIOP it will return _*_getStubClassSuffix for
     *   interfaces and _*_getStubClassSuffix for non-interfaces (and
     *   determine the interface and create _*_Stub from that).</li>
     * </ul>
     */
    private class RmicFileNameMapper implements FileNameMapper {

        RmicFileNameMapper() {
        }

        /**
         * Empty implementation.
         */
        public void setFrom(String s) {
        }
        /**
         * Empty implementation.
         */
        public void setTo(String s) {
        }

        public String[] mapFileName(String name) {
            if (name == null
                || !name.endsWith(".class")
                || name.endsWith(getStubClassSuffix() + ".class")
                || name.endsWith(getSkelClassSuffix() + ".class")
                || name.endsWith(getTieClassSuffix() + ".class")) {
                // Not a .class file or the one we'd generate
                return null;
            }

            // we know that name.endsWith(".class")
            String base = name.substring(0, name.length() - 6);

            String classname = base.replace(File.separatorChar, '.');
            if (attributes.getVerify()
                && !attributes.isValidRmiRemote(classname)) {
                return null;
            }

            /*
             * fallback in case we have trouble loading the class or
             * don't know how to handle it (there is no easy way to
             * know what IDL mode would generate.
             *
             * This is supposed to make Ant always recompile the
             * class, as a file of that name should not exist.
             */
            String[] target = new String[] {name + ".tmp." + RAND.nextLong()};

            if (!attributes.getIiop() && !attributes.getIdl()) {
                // JRMP with simple naming convention
                if ("1.2".equals(attributes.getStubVersion())) {
                    target = new String[] {
                        base + getStubClassSuffix() + ".class"
                    };
                } else {
                    target = new String[] {
                        base + getStubClassSuffix() + ".class",
                        base + getSkelClassSuffix() + ".class",
                    };
                }
            } else if (!attributes.getIdl()) {
                int lastSlash = base.lastIndexOf(File.separatorChar);

                String dirname = "";
                /*
                 * I know, this is not necessary, but I prefer it explicit (SB)
                 */
                int index = -1;
                if (lastSlash == -1) {
                    // no package
                    index = 0;
                } else {
                    index = lastSlash + 1;
                    dirname = base.substring(0, index);
                }

                String filename = base.substring(index);

                try {
                    Class c = attributes.getLoader().loadClass(classname);

                    if (c.isInterface()) {
                        // only stub, no tie
                        target = new String[] {
                            dirname + "_" + filename + getStubClassSuffix()
                            + ".class"
                        };
                    } else {
                        /*
                         * stub is derived from implementation,
                         * tie from interface name.
                         */
                        Class interf = attributes.getRemoteInterface(c);
                        String iName = interf.getName();
                        String iDir = "";
                        int iIndex = -1;
                        int lastDot = iName.lastIndexOf(".");
                        if (lastDot == -1) {
                            // no package
                            iIndex = 0;
                        } else {
                            iIndex = lastDot + 1;
                            iDir = iName.substring(0, iIndex);
                            iDir = iDir.replace('.', File.separatorChar);
                        }

                        target = new String[] {
                            dirname + "_" + filename + getTieClassSuffix()
                            + ".class",
                            iDir + "_" + iName.substring(iIndex)
                            + getStubClassSuffix() + ".class"
                        };
                    }
                } catch (ClassNotFoundException e) {
                    attributes.log("Unable to verify class " + classname
                                   + ". It could not be found.",
                                   Project.MSG_WARN);
                } catch (NoClassDefFoundError e) {
                    attributes.log("Unable to verify class " + classname
                                   + ". It is not defined.", Project.MSG_WARN);
                } catch (Throwable t) {
                    attributes.log("Unable to verify class " + classname
                                   + ". Loading caused Exception: "
                                   + t.getMessage(), Project.MSG_WARN);
                }
            }
            return target;
        }
    }
}
