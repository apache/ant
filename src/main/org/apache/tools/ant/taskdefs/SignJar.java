/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Signs jar or zip files with the javasign command line tool. The
 * tool detailed dependency checking: files are only signed if they
 * are not signed. The <tt>signjar</tt> attribute can point to the file to
 * generate; if this file exists then
 * its modification date is used as a cue as to whether to resign any JAR file.
 * <br>
 * <strong>Note:</strong> Requires Java 1.2 or later. </p>

 *
 * @author Peter Donald
 *         <a href="mailto:donaldp@apache.org">donaldp@apache.org</a>
 * @author Nick Fortescue
 *         <a href="mailto:nick@ox.compsoc.net">nick@ox.compsoc.net</a>
 * @since Ant 1.1
 * @ant.task category="java"
 */
public class SignJar extends Task {

    /**
     * The name of the jar file.
     */
    protected File jar;

    /**
     * The alias of signer.
     */
    protected String alias;

    /**
     * The name of keystore file.
     */
    private String keystore;

    protected String storepass;
    protected String storetype;
    protected String keypass;
    protected String sigfile;
    protected File signedjar;
    protected boolean verbose;
    protected boolean internalsf;
    protected boolean sectionsonly;

    /** The maximum amount of memory to use for Jar signer */
    private String maxMemory;

    /**
     * the filesets of the jars to sign
     */
    protected Vector filesets = new Vector();

    /**
     * Whether to assume a jar which has an appropriate .SF file in is already
     * signed.
     */
    protected boolean lazy;


    /**
     * Set the maximum memory to be used by the jarsigner process
     *
     * @param max a string indicating the maximum memory according to the
     *        JVM conventions (e.g. 128m is 128 Megabytes)
     */
    public void setMaxmemory(String max) {
        maxMemory = max;
    }

    /**
     * the jar file to sign; required
     */
    public void setJar(final File jar) {
        this.jar = jar;
    }

    /**
     * the alias to sign under; required
     */
    public void setAlias(final String alias) {
        this.alias = alias;
    }

    /**
     * keystore location; required
     */
    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    }

    /**
     * password for keystore integrity; required
     */
    public void setStorepass(final String storepass) {
        this.storepass = storepass;
    }

    /**
     * keystore type; optional
     */
    public void setStoretype(final String storetype) {
        this.storetype = storetype;
    }

    /**
     * password for private key (if different); optional
     */
    public void setKeypass(final String keypass) {
        this.keypass = keypass;
    }

    /**
     * name of .SF/.DSA file; optional
     */
    public void setSigfile(final String sigfile) {
        this.sigfile = sigfile;
    }

    /**
     * name of signed JAR file; optional
     */
    public void setSignedjar(final File signedjar) {
        this.signedjar = signedjar;
    }

    /**
     * Enable verbose output when signing
     * ; optional: default false
     */
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Flag to include the .SF file inside the signature;
     * optional; default false
     */
    public void setInternalsf(final boolean internalsf) {
        this.internalsf = internalsf;
    }

    /**
     * flag to compute hash of entire manifest;
     * optional, default false
     */
    public void setSectionsonly(final boolean sectionsonly) {
        this.sectionsonly = sectionsonly;
    }

    /**
     * flag to control whether the presence of a signature
     * file means a JAR is signed;
     * optional, default false
     */
    public void setLazy(final boolean lazy) {
        this.lazy = lazy;
    }

    /**
     * Adds a set of files to sign
     * @since Ant 1.4
     */
    public void addFileset(final FileSet set) {
        filesets.addElement(set);
    }


    /**
     * sign the jar(s)
     */
    public void execute() throws BuildException {
        if (null == jar && null == filesets) {
            throw new BuildException("jar must be set through jar attribute "
                                     + "or nested filesets");
        }
        if (null != jar) {
            doOneJar(jar, signedjar);
            return;
        } else {
            //Assume null != filesets

            // deal with the filesets
            for (int i = 0; i < filesets.size(); i++) {
                FileSet fs = (FileSet) filesets.elementAt(i);
                DirectoryScanner ds = fs.getDirectoryScanner(project);
                String[] jarFiles = ds.getIncludedFiles();
                for (int j = 0; j < jarFiles.length; j++) {
                    doOneJar(new File(fs.getDir(project), jarFiles[j]), null);
                }
            }
        }
    }

    /**
     * sign one jar
     */
    private void doOneJar(File jarSource, File jarTarget)
        throws BuildException {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            throw new BuildException("The signjar task is only available on "
                                     + "JDK versions 1.2 or greater");
        }

        if (null == alias) {
            throw new BuildException("alias attribute must be set");
        }

        if (null == storepass) {
            throw new BuildException("storepass attribute must be set");
        }

        if (isUpToDate(jarSource, jarTarget)) {
          return;
        }

        final ExecTask cmd = (ExecTask) getProject().createTask("exec");
        cmd.setExecutable(JavaEnvUtils.getJdkExecutable("jarsigner"));

        if (maxMemory != null) {
            cmd.createArg().setValue("-J-Xmx" + maxMemory);
        }

        if (null != keystore) {
            // is the keystore a file
            File keystoreFile = getProject().resolveFile(keystore);
            if (keystoreFile.exists()) {
                cmd.createArg().setValue("-keystore");
                cmd.createArg().setValue(keystoreFile.getPath());
            } else {
                // must be a URL - just pass as is
                cmd.createArg().setValue("-keystore");
                cmd.createArg().setValue(keystore);
            }
        }

        if (null != storepass) {
            cmd.createArg().setValue("-storepass");
            cmd.createArg().setValue(storepass);
        }

        if (null != storetype) {
            cmd.createArg().setValue("-storetype");
            cmd.createArg().setValue(storetype);
        }

        if (null != keypass) {
            cmd.createArg().setValue("-keypass");
            cmd.createArg().setValue(keypass);
        }

        if (null != sigfile) {
            cmd.createArg().setValue("-sigfile");
            cmd.createArg().setValue(sigfile);
        }

        if (null != jarTarget) {
            cmd.createArg().setValue("-signedjar");
            cmd.createArg().setValue(jarTarget.toString());
        }

        if (verbose) {
            cmd.createArg().setValue("-verbose");
        }

        if (internalsf) {
            cmd.createArg().setValue("-internalsf");
        }

        if (sectionsonly) {
            cmd.createArg().setValue("-sectionsonly");
        }

        cmd.createArg().setValue(jarSource.toString());

        cmd.createArg().setValue(alias);

        log("Signing Jar : " + jarSource.getAbsolutePath());
        cmd.setFailonerror(true);
        cmd.setTaskName(getTaskName());
        cmd.execute();
    }

    protected boolean isUpToDate(File jarFile, File signedjarFile) {
        if (null == jarFile) {
            return false;
        }

        if (null != signedjarFile) {

            if (!jarFile.exists()) {
              return false;
            }
            if (!signedjarFile.exists()) {
              return false;
            }
            if (jarFile.equals(signedjarFile)) {
              return false;
            }
            if (signedjarFile.lastModified() > jarFile.lastModified()) {
                return true;
            }
        } else {
            if (lazy) {
                return isSigned(jarFile);
            }
        }

        return false;
    }

    protected boolean isSigned(File file) {
        final String SIG_START = "META-INF/";
        final String SIG_END = ".SF";

        if (!file.exists()) {
            return false;
        }
        ZipFile jarFile = null;
        try {
            jarFile = new ZipFile(file);
            if (null == alias) {
                Enumeration entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    String name = ((ZipEntry) entries.nextElement()).getName();
                    if (name.startsWith(SIG_START) && name.endsWith(SIG_END)) {
                        return true;
                    }
                }
                return false;
            } else {
                return jarFile.getEntry(SIG_START + alias.toUpperCase() +
                                        SIG_END) != null;
            }
        } catch (IOException e) {
            return false;
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
            }
        }
    }
}

