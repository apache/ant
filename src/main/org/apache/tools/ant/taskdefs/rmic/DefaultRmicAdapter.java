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

package org.apache.tools.ant.taskdefs.rmic;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Rmic;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;
import java.util.Random;
import java.util.Vector;

/**
 * This is the default implementation for the RmicAdapter interface.
 * Currently, this is a cut-and-paste of the original rmic task and
 * DefaultCopmpilerAdapter.
 *
 * @author duncan@x180.com
 * @author ludovic.claude@websitewatchers.co.uk
 * @author David Maclean <a href="mailto:david@cm.co.za">david@cm.co.za</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @author <a href="tokamoto@rd.nttdata.co.jp">Takashi Okamoto</a>
 * @since Ant 1.4
 */
public abstract class DefaultRmicAdapter implements RmicAdapter {

    private Rmic attributes;
    private FileNameMapper mapper;
    private static final Random rand = new Random();

    public DefaultRmicAdapter() {
    }

    public void setRmic(Rmic attributes) {
        this.attributes = attributes;
        mapper = new RmicFileNameMapper();
    }

    public Rmic getRmic() {
        return attributes;
    }

    protected String getStubClassSuffix() {
        return "_Stub";
    }        

    protected String getSkelClassSuffix() {
        return "_Skel";
    }        

    protected String getTieClassSuffix() {
        return "_Tie";
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
     */
    public FileNameMapper getMapper() {
        return mapper;
    }

    /**
     * The CLASSPATH this rmic process will use.
     */
    public Path getClasspath() {
        return getCompileClasspath();
    }

    /**
     * Builds the compilation classpath.
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
     * setup rmic argument for rmic.
     */
    protected Commandline setupRmicCommand() {
        return setupRmicCommand(null);
    }

    /**
     * setup rmic argument for rmic.
     *
     * @param options additional parameters needed by a specific
     *                implementation.
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
            if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
                /*
                 * XXX - This doesn't mix very well with build.systemclasspath,
                 */
                classpath.addExtdirs(attributes.getExtdirs());
            } else {
                cmd.createArgument().setValue("-extdirs");
                cmd.createArgument().setPath(attributes.getExtdirs());
            }
        }

        cmd.createArgument().setValue("-classpath");
        cmd.createArgument().setPath(classpath);

        String stubVersion = attributes.getStubVersion();
        if (null != stubVersion) {
            if ("1.1".equals(stubVersion)) {
                cmd.createArgument().setValue("-v1.1");
            } else if ("1.2".equals(stubVersion)) {
                cmd.createArgument().setValue("-v1.2");
            } else {
                cmd.createArgument().setValue("-vcompat");
            }
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
     * &qout;niceSourceList&quot;
     */
    protected void logAndAddFilesToCompile(Commandline cmd) {
        Vector compileList = attributes.getCompileList();

        attributes.log("Compilation " + cmd.describeArguments(),
                       Project.MSG_VERBOSE);

        StringBuffer niceSourceList = new StringBuffer("File");
        if (compileList.size() != 1) {
            niceSourceList.append("s");
        }
        niceSourceList.append(" to be compiled:");

        for (int i = 0; i < compileList.size(); i++) {
            String arg = (String) compileList.elementAt(i);
            cmd.createArgument().setValue(arg);
            niceSourceList.append("    " + arg);
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

        RmicFileNameMapper() {}

        /**
         * Empty implementation.
         */
        public void setFrom(String s) {}
        /**
         * Empty implementation.
         */
        public void setTo(String s) {}

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
            if (attributes.getVerify() &&
                !attributes.isValidRmiRemote(classname)) {
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
            String[] target = new String[] {name + ".tmp." + rand.nextLong()};

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
