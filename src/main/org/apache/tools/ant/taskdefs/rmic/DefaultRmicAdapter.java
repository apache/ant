/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.rmic;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.*;

import java.io.File;
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
 * @author Takashi Okamoto <tokamoto@rd.nttdata.co.jp>
 */
public abstract class DefaultRmicAdapter implements RmicAdapter {

    private Rmic attributes;
    private FileNameMapper mapper;

    public DefaultRmicAdapter() {
    }

    public void setRmic( Rmic attributes ) {
        this.attributes = attributes;
        mapper = new RmicFileNameMapper();
    }

    public Rmic getRmic() {
        return attributes;
    }

    public String getStubClassSuffix() {
        return "_Stub";
    }        

    public String getSkelClassSuffix() {
        return "_Skel";
    }        

    /**
     * This implementation maps *.class to *getStubClassSuffix().class and - if
     * stubversion is not 1.2 - to *getSkelClassSuffix().class.
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
        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath
        Path classpath = new Path(attributes.getProject());
        classpath.setLocation(attributes.getBase());

        // Combine the build classpath with the system classpath, in an 
        // order determined by the value of build.classpath

        if (attributes.getClasspath() == null) {
            if ( attributes.getIncludeantruntime() ) {
                classpath.addExisting(Path.systemClasspath);
            }
        } else {
            if ( attributes.getIncludeantruntime() ) {
                classpath.addExisting(attributes.getClasspath().concatSystemClasspath("last"));
            } else {
                classpath.addExisting(attributes.getClasspath().concatSystemClasspath("ignore"));
            }
        }

        if (attributes.getIncludejavaruntime()) {
            if (System.getProperty("java.vendor").toLowerCase().indexOf("microsoft") >= 0) {
                // Pull in *.zip from packages directory
                FileSet msZipFiles = new FileSet();
                msZipFiles.setDir(new File(System.getProperty("java.home") + File.separator + "Packages"));
                msZipFiles.setIncludes("*.ZIP");
                classpath.addFileset(msZipFiles);
            } else if (Project.getJavaVersion() == Project.JAVA_1_1) {
                classpath.addExisting(new Path(null,
                                                System.getProperty("java.home")
                                                + File.separator + "lib"
                                                + File.separator 
                                                + "classes.zip"));
            } else if(System.getProperty("java.vm.name").equals("Kaffe")) {
                FileSet kaffeJarFiles = new FileSet();
                kaffeJarFiles.setDir(new File(System.getProperty("java.home") 
                                              + File.separator + "share"
                                              + File.separator + "kaffe"));
                
                kaffeJarFiles.setIncludes("*.jar");
                classpath.addFileset(kaffeJarFiles);
            } else {
                // JDK > 1.1 seems to set java.home to the JRE directory.
                classpath.addExisting(new Path(null,
                                                System.getProperty("java.home")
                                                + File.separator + "lib"
                                                + File.separator + "rt.jar"));
                // Just keep the old version as well and let addExistingToPath
                // sort it out.
                classpath.addExisting(new Path(null,
                                                System.getProperty("java.home")
                                                + File.separator +"jre"
                                                + File.separator + "lib"
                                                + File.separator + "rt.jar"));
            }
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
            for (int i=0; i<options.length; i++) {
                cmd.createArgument().setValue(options[i]);
            }
        }

        Path classpath = getCompileClasspath();

        cmd.createArgument().setValue("-d");
        cmd.createArgument().setFile(attributes.getBase());

        if (attributes.getExtdirs() != null) {
            if (Project.getJavaVersion().startsWith("1.1")) {
                /*
                 * XXX - This doesn't mix very well with build.systemclasspath,
                 */
                addExtdirsToClasspath(classpath);
            } else {
                cmd.createArgument().setValue("-extdirs");
                cmd.createArgument().setPath(attributes.getExtdirs());
            }
        }

        cmd.createArgument().setValue("-classpath");
        cmd.createArgument().setPath(classpath);

        String stubVersion = attributes.getStubVersion();
        if (null != stubVersion) {
            if ("1.1".equals(stubVersion))
                cmd.createArgument().setValue("-v1.1");
            else if ("1.2".equals(stubVersion))
                cmd.createArgument().setValue("-v1.2");
            else
                cmd.createArgument().setValue("-vcompat");
        }

        if (null != attributes.getSourceBase()) {
            cmd.createArgument().setValue("-keepgenerated");
        }

        if( attributes.getIiop() ) {
            attributes.log("IIOP has been turned on.", Project.MSG_INFO);
            cmd.createArgument().setValue("-iiop");
            if( attributes.getIiopopts() != null ) {
                attributes.log("IIOP Options: " + attributes.getIiopopts(),
                               Project.MSG_INFO );
                cmd.createArgument().setValue(attributes.getIiopopts());
            }
        }

        if( attributes.getIdl() )  {
            cmd.createArgument().setValue("-idl");
            attributes.log("IDL has been turned on.", Project.MSG_INFO);
            if( attributes.getIdlopts() != null ) {
                cmd.createArgument().setValue(attributes.getIdlopts());
                attributes.log("IDL Options: " + attributes.getIdlopts(),
                               Project.MSG_INFO );
            }
        }

        if( attributes.getDebug())  {
            cmd.createArgument().setValue("-g");
        }

        logAndAddFilesToCompile(cmd);
        return cmd;
     }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the 
     * &qout;niceSourceList&quot;
     */
    protected void logAndAddFilesToCompile(Commandline cmd) {
        Vector compileList = attributes.getCompileList();

        attributes.log("Compilation args: " + cmd.toString(),
                       Project.MSG_VERBOSE);

        StringBuffer niceSourceList = new StringBuffer("File");
        if (compileList.size() != 1) {
            niceSourceList.append("s");
        }
        niceSourceList.append(" to be compiled:");

        for (int i=0; i < compileList.size(); i++) {
            String arg = (String)compileList.elementAt(i);
            cmd.createArgument().setValue(arg);
            niceSourceList.append("    " + arg);
        }

        attributes.log(niceSourceList.toString(), Project.MSG_VERBOSE);
    }

    /**
     * Emulation of extdirs feature in java >= 1.2.
     * This method adds all files in the given
     * directories (but not in sub-directories!) to the classpath,
     * so that you don't have to specify them all one by one.
     * @param classpath - Path to append files to
     */
    protected void addExtdirsToClasspath(Path classpath) {
        Path extdirs = attributes.getExtdirs();
        if (extdirs == null) {
            String extProp = System.getProperty("java.ext.dirs");
            if (extProp != null) {
                extdirs = new Path(attributes.getProject(), extProp);
            } else {
                return;
            }
        }

        String[] dirs = extdirs.list();
        for (int i=0; i<dirs.length; i++) {
            if (!dirs[i].endsWith(File.separator)) {
                dirs[i] += File.separator;
            }
            File dir = attributes.getProject().resolveFile(dirs[i]);
            FileSet fs = new FileSet();
            fs.setDir(dir);
            fs.setIncludes("*");
            classpath.addFileset(fs);
        }
    }

    /**
     * Mapper that possibly returns two file names, *_Stub and *_Skel.
     */
    private class RmicFileNameMapper implements FileNameMapper {

        private GlobPatternMapper stubMapper;
        private GlobPatternMapper skelMapper;

        RmicFileNameMapper() {
            stubMapper = new GlobPatternMapper();
            stubMapper.setFrom("*.class");
            stubMapper.setTo("*"+getStubClassSuffix()+".class");

            // no _Skel file in stub version 1.2
            if (!"1.2".equals(attributes.getStubVersion())) {
                skelMapper = new GlobPatternMapper();
                skelMapper.setFrom("*.class");
                skelMapper.setTo("*"+getSkelClassSuffix()+".class");
            }
        }

        /**
         * Empty implementation.
         */
        public void setFrom(String s) {}
        /**
         * Empty implementation.
         */
        public void setTo(String s) {}

        public String[] mapFileName(String name) {
            String[] stubName = stubMapper.mapFileName(name);

            if (stubName == null
                || name.endsWith(getStubClassSuffix()+".class") 
                || name.endsWith(getSkelClassSuffix()+".class")) {
                // Not a .class file
                return null;
            }

            String classname = name.replace(File.separatorChar, '.');
            classname = classname.substring(0, classname.indexOf(".class"));
            if (attributes.getVerify() &&
                !attributes.isValidRmiRemote(classname)) {
                return null;
            }
            if (skelMapper != null) {
                return new String[] {
                    stubName[0], 
                    skelMapper.mapFileName(name)[0]
                };
            } else {
                return stubName;
            }
        }
    }

}
