/*
 * Copyright  2000-2005 The Apache Software Foundation
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
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.IsSigned;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.RedirectorElement;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * Signs JAR or ZIP files with the javasign command line tool. The tool detailed
 * dependency checking: files are only signed if they are not signed. The
 * <tt>signjar</tt> attribute can point to the file to generate; if this file
 * exists then its modification date is used as a cue as to whether to resign
 * any JAR file.
 *
 * @ant.task category="java"
 * @since Ant 1.1
 */
public class SignJar extends Task {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

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
    private boolean preserveLastModified;
    private RedirectorElement redirector;

    /**
     * The maximum amount of memory to use for Jar signer
     */
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
     * the output directory when using filesets.
     */
    protected File destDir;

    /**
     * mapper for todir work
     */
    private Mapper mapper;

    /** error string for unit test verification: {@value} */
    public static final String ERROR_SIGNEDJAR_AND_FILESET =
            "The signedjar attribute is not supported with filesets";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_TODIR_AND_SIGNEDJAR
            = "'destdir' and 'signedjar' cannot both be set";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_TOO_MANY_MAPPERS = "Too many mappers";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_SIGNEDJAR_AND_FILESETS = "You cannot specify the signed JAR when using filesets";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String WARN_JAR_AND_FILESET = "nested filesets will be ignored if the jar attribute has"
            + " been specified.";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_BAD_MAP = "Cannot map source file to anything sensible: ";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_MAPPER_WITHOUT_DEST = "The destDir attribute is required if a mapper is set";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_NO_SOURCE = "jar must be set through jar attribute "
            + "or nested filesets";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_NO_ALIAS = "alias attribute must be set";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_NO_STOREPASS = "storepass attribute must be set";

    /**
     * name of JDK program we are looking for
     */
    protected static final String JARSIGNER_COMMAND = "jarsigner";

    /**
     * Set the maximum memory to be used by the jarsigner process
     *
     * @param max a string indicating the maximum memory according to the JVM
     *            conventions (e.g. 128m is 128 Megabytes)
     */
    public void setMaxmemory(String max) {
        maxMemory = max;
    }

    /**
     * the jar file to sign; required
     *
     * @param jar the jar file to sign
     */
    public void setJar(final File jar) {
        this.jar = jar;
    }

    /**
     * the alias to sign under; required
     *
     * @param alias the alias to sign under
     */
    public void setAlias(final String alias) {
        this.alias = alias;
    }

    /**
     * keystore location; required
     *
     * @param keystore the keystore location
     */
    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    }

    /**
     * password for keystore integrity; required
     *
     * @param storepass the password for the keystore
     */
    public void setStorepass(final String storepass) {
        this.storepass = storepass;
    }

    /**
     * keystore type; optional
     *
     * @param storetype the keystore type
     */
    public void setStoretype(final String storetype) {
        this.storetype = storetype;
    }

    /**
     * password for private key (if different); optional
     *
     * @param keypass the password for the key (if different)
     */
    public void setKeypass(final String keypass) {
        this.keypass = keypass;
    }

    /**
     * name of .SF/.DSA file; optional
     *
     * @param sigfile the name of the .SF/.DSA file
     */
    public void setSigfile(final String sigfile) {
        this.sigfile = sigfile;
    }

    /**
     * name of signed JAR file; optional
     *
     * @param signedjar the name of the signed jar file
     */
    public void setSignedjar(final File signedjar) {
        this.signedjar = signedjar;
    }

    /**
     * Enable verbose output when signing ; optional: default false
     *
     * @param verbose if true enable verbose output
     */
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Flag to include the .SF file inside the signature; optional; default
     * false
     *
     * @param internalsf if true include the .SF file inside the signature
     */
    public void setInternalsf(final boolean internalsf) {
        this.internalsf = internalsf;
    }

    /**
     * flag to compute hash of entire manifest; optional, default false
     *
     * @param sectionsonly flag to compute hash of entire manifest
     */
    public void setSectionsonly(final boolean sectionsonly) {
        this.sectionsonly = sectionsonly;
    }

    /**
     * flag to control whether the presence of a signature file means a JAR is
     * signed; optional, default false
     *
     * @param lazy flag to control whether the presence of a signature
     */
    public void setLazy(final boolean lazy) {
        this.lazy = lazy;
    }

    /**
     * Adds a set of files to sign
     *
     * @param set a set of files to sign
     * @since Ant 1.4
     */
    public void addFileset(final FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Optionally sets the output directory to be used.
     *
     * @param destDir the directory in which to place signed jars
     * @since Ant 1.7
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }


    /**
     * add a mapper to determine file naming policy. Only used with toDir
     * processing.
     *
     * @param newMapper
     * @since Ant 1.7
     */
    public void addMapper(Mapper newMapper) {
        if (mapper != null) {
            throw new BuildException(ERROR_TOO_MANY_MAPPERS);
        }
        mapper = newMapper;
    }

    public Mapper getMapper() {
        return mapper;
    }

    /**
     * sign the jar(s)
     *
     * @throws BuildException on errors
     */
    public void execute() throws BuildException {
        //validation logic
        final boolean hasFileset = filesets.size() > 0;
        final boolean hasJar = jar != null;
        final boolean hasSignedJar = signedjar != null;
        final boolean hasDestDir = destDir != null;
        final boolean hasMapper = mapper != null;

        if (!hasJar && !hasFileset) {
            throw new BuildException(ERROR_NO_SOURCE);
        }
        if (null == alias) {
            throw new BuildException(ERROR_NO_ALIAS);
        }

        if (null == storepass) {
            throw new BuildException(ERROR_NO_STOREPASS);
        }

        if (hasDestDir && hasSignedJar) {
            throw new BuildException(ERROR_TODIR_AND_SIGNEDJAR);
        }


        if (hasFileset && hasSignedJar) {
            throw new BuildException(ERROR_SIGNEDJAR_AND_FILESETS);
        }

        //this isnt strictly needed, but by being fussy now,
        //we can change implementation details later
        if (!hasDestDir && hasMapper) {
            throw new BuildException(ERROR_MAPPER_WITHOUT_DEST);
        }

        //init processing logic; this is retained through our execution(s)
        redirector = createRedirector();


        //special case single jar handling with signedjar attribute set
        if (hasJar && hasSignedJar) {
            // single jar processing
            signOneJar(jar, signedjar);
            //return here.
            return;
        }

        //the rest of the method treats single jar like
        //a nested fileset with one file

        if (hasJar) {
            //we create a fileset with the source file.
            //this lets us combine our logic for handling output directories,
            //mapping etc.
            FileSet sourceJar = new FileSet();
            sourceJar.setFile(jar);
            sourceJar.setDir(jar.getParentFile());
            addFileset(sourceJar);
        }
        //set up our mapping policy
        FileNameMapper destMapper;
        if (hasMapper) {
            destMapper = mapper.getImplementation();
        } else {
            //no mapper? use the identity policy
            destMapper = new IdentityMapper();
        }


        //at this point the filesets are set up with lists of files,
        //and the mapper is ready to map from source dirs to dest files
        //now we iterate through every JAR giving source and dest names
        // deal with the filesets
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            //get all included files in a fileset
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            String[] jarFiles = ds.getIncludedFiles();
            File baseDir = fs.getDir(getProject());

            //calculate our destination directory; it is either the destDir
            //attribute, or the base dir of the fileset (for in situ updates)
            File toDir = hasDestDir ? destDir : baseDir;

            //loop through all jars in the fileset
            for (int j = 0; j < jarFiles.length; j++) {
                String jarFile = jarFiles[j];
                //determine the destination filename via the mapper
                String[] destFilenames = destMapper.mapFileName(jarFile);
                if (destFilenames == null || destFilenames.length != 1) {
                    //we only like simple mappers.
                    throw new BuildException(ERROR_BAD_MAP + jarFile);
                }
                File destFile = new File(toDir, destFilenames[0]);
                File jarSource = new File(baseDir, jarFile);
                signOneJar(jarSource, destFile);
            }
        }
    }

    /**
     * Create the redirector to use, if any.
     *
     * @return a configured RedirectorElement.
     */
    private RedirectorElement createRedirector() {
        RedirectorElement result = new RedirectorElement();
        StringBuffer input = new StringBuffer(storepass).append('\n');
        if (keypass != null) {
            input.append(keypass).append('\n');
        }
        result.setInputString(input.toString());
        result.setLogInputString(false);
        return result;
    }

    /**
     * Sign one jar.
     * <p/>
     * The signing only takes place if {@link #isUpToDate(File, File)} indicates
     * that it is needed.
     *
     * @param jarSource source to sign
     * @param jarTarget target; may be null
     * @throws BuildException
     */
    private void signOneJar(File jarSource, File jarTarget)
            throws BuildException {


        File target = jarTarget;
        if (target == null) {
            target = jarSource;
        }
        if (isUpToDate(jarSource, target)) {
            return;
        }

        long lastModified = jarSource.lastModified();
        final ExecTask cmd = new ExecTask(this);
        cmd.setExecutable(JavaEnvUtils.getJdkExecutable(JARSIGNER_COMMAND));
        cmd.setTaskType(JARSIGNER_COMMAND);

        if (maxMemory != null) {
            cmd.createArg().setValue("-J-Xmx" + maxMemory);
        }

        if (null != keystore) {
            // is the keystore a file
            cmd.createArg().setValue("-keystore");
            String location;
            File keystoreFile = getProject().resolveFile(keystore);
            if (keystoreFile.exists()) {
                location = keystoreFile.getPath();
            } else {
                // must be a URL - just pass as is
                location = keystore;
            }
            cmd.createArg().setValue(location);
        }
        if (null != storetype) {
            cmd.createArg().setValue("-storetype");
            cmd.createArg().setValue(storetype);
        }
        if (null != sigfile) {
            cmd.createArg().setValue("-sigfile");
            cmd.createArg().setValue(sigfile);
        }

        //DO NOT SET THE -signedjar OPTION if source==dest
        //unless you like fielding hotspot crash reports
        if (null != target && !jarSource.equals(target)) {
            cmd.createArg().setValue("-signedjar");
            cmd.createArg().setValue(target.getPath());
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

        //JAR source is required
        cmd.createArg().setValue(jarSource.getPath());

        //alias is required for signing
        cmd.createArg().setValue(alias);

        log("Signing JAR: " +
                jarSource.getAbsolutePath()
                +" to " +
                target.getAbsolutePath()
                + " as " + alias);
        cmd.setFailonerror(true);
        cmd.addConfiguredRedirector(redirector);
        cmd.execute();

        // restore the lastModified attribute
        if (preserveLastModified) {
            target.setLastModified(lastModified);
        }
    }

    /**
     * Compare a jar file with its corresponding signed jar. The logic for this
     * is complex, and best explained in the source itself. Essentially if
     * either file doesnt exist, or the destfile has an out of date timestamp,
     * then the return value is false.
     * <p/>
     * If we are signing ourself, the check {@link #isSigned(File)} is used to
     * trigger the process.
     *
     * @param jarFile       the unsigned jar file
     * @param signedjarFile the result signed jar file
     * @return true if the signedjarFile is considered up to date
     */
    protected boolean isUpToDate(File jarFile, File signedjarFile) {
        if (null == jarFile && !jarFile.exists()) {
            //these are pathological case, but retained in case somebody
            //subclassed us.
            return false;
        }

        //we normally compare destination with source
        File destFile = signedjarFile;
        if (destFile == null) {
            //but if no dest is specified, compare source to source
            destFile = jarFile;
        }

        //if, by any means, the destfile and source match,
        if (jarFile.equals(destFile)) {
            if (lazy) {
                //we check the presence of signatures on lazy signing
                return isSigned(jarFile);
            }
            //unsigned or non-lazy self signings are always false
            return false;
        }

        //if they are different, the timestamps are used
        return FILE_UTILS.isUpToDate(jarFile, destFile);
    }

    /**
     * test for a file being signed, by looking for a signature in the META-INF
     * directory
     *
     * @param file the file to be checked
     * @return true if the file is signed
     * @see IsSigned#isSigned(File, String)
     */
    protected boolean isSigned(File file) {
        try {
            return IsSigned.isSigned(file, alias);
        } catch (IOException e) {
            //just log this
            log(e.toString(), Project.MSG_VERBOSE);
            return false;
        }
    }

    /**
     * true to indicate that the signed jar modification date remains the same
     * as the original. Defaults to false
     *
     * @param preserveLastModified if true preserve the last modified time
     */
    public void setPreserveLastModified(boolean preserveLastModified) {
        this.preserveLastModified = preserveLastModified;
    }
}
