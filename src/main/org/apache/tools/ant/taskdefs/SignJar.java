/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.IsSigned;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.FileUtils;

/**
 * Signs JAR or ZIP files with the javasign command line tool. The
 * tool detailed dependency checking: files are only signed if they
 * are not signed. The <tt>signjar</tt> attribute can point to the file to
 * generate; if this file exists then
 * its modification date is used as a cue as to whether to resign any JAR file.
 *
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
        if (null == jar && filesets.size() == 0) {
            throw new BuildException("jar must be set through jar attribute "
                                     + "or nested filesets");
        }
        if (null != jar) {
            if (filesets.size() != 0) {
                log("nested filesets will be ignored if the jar attribute has"
                    + " been specified.", Project.MSG_WARN);
            }

            doOneJar(jar, signedjar);
            return;
        } else {
            // deal with the filesets
            for (int i = 0; i < filesets.size(); i++) {
                FileSet fs = (FileSet) filesets.elementAt(i);
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                String[] jarFiles = ds.getIncludedFiles();
                for (int j = 0; j < jarFiles.length; j++) {
                    doOneJar(new File(fs.getDir(getProject()), jarFiles[j]), null);
                }
            }
        }
    }

    /**
     * sign one jar
     */
    private void doOneJar(File jarSource, File jarTarget)
        throws BuildException {

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

        log("Signing JAR: " + jarSource.getAbsolutePath());
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
            if (FileUtils.newFileUtils().isUpToDate(jarFile, signedjarFile)) {
                return true;
            }
        } else {
            if (lazy) {
                return isSigned(jarFile);
            }
        }

        return false;
    }

    /**
     * test for a file being signed, by looking for a signature in the META-INF
     * directory
     * @param file
     * @return true if the file is signed
     */
    protected boolean isSigned(File file) {
        try {
            return IsSigned.isSigned(file, alias);
        } catch (IOException e) {
            return false;
        }
    }
}

