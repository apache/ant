/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.zip.ZipOutputStream;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.FileReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.ByteArrayInputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.util.Enumeration;


/**
 * Creates a JAR archive.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Brian Deitte
 *         <a href="mailto:bdeitte@macromedia.com">bdeitte@macromedia.com</a>
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */
public class Jar extends Zip {
    /** The index file name. */
    private static final String INDEX_NAME = "META-INF/INDEX.LIST";

    /** merged manifests added through addConfiguredManifest */
    private Manifest configuredManifest;
    /** shadow of the above if upToDate check alters the value */
    private Manifest savedConfiguredManifest;

    /**  merged manifests added through filesets */
    private Manifest filesetManifest;

    /**
     *  whether to merge fileset manifests;
     *  value is true if filesetmanifest is 'merge' or 'mergewithoutmain'
     */
    private FilesetManifestConfig filesetManifestConfig;

    /**
     *  Whether to create manifest file on finalizeOutputStream?
     */
    private boolean manifestOnFinalize = true;

    /**
     * whether to merge the main section of fileset manifests;
     * value is true if filesetmanifest is 'merge'
     */
    private boolean mergeManifestsMain = false;

    /** the manifest specified by the 'manifest' attribute **/
    private Manifest manifest;

    /**
     * The file found from the 'manifest' attribute.  This can be
     * either the location of a manifest, or the name of a jar added
     * through a fileset.  If its the name of an added jar, the
     * manifest is looked for in META-INF/MANIFEST.MF
     */
    private File manifestFile;

    /** jar index is JDK 1.3+ only */
    private boolean index = false;

    /** constructor */
    public Jar() {
        super();
        archiveType = "jar";
        emptyBehavior = "create";
        setEncoding("UTF8");
    }

    public void setWhenempty(WhenEmpty we) {
        log("JARs are never empty, they contain at least a manifest file",
            Project.MSG_WARN);
    }

    /**
     * @deprecated Use setDestFile(File) instead
     */
    public void setJarfile(File jarFile) {
        setDestFile(jarFile);
    }

    /**
     * Set whether or not to create an index list for classes
     * to speed up classloading.
     */
    public void setIndex(boolean flag){
        index = flag;
    }

    public void addConfiguredManifest(Manifest newManifest)
        throws ManifestException {
        if (configuredManifest == null) {
            configuredManifest = newManifest;
        } else {
            configuredManifest.merge(newManifest);
        }
        savedConfiguredManifest = configuredManifest;
    }

    public void setManifest(File manifestFile) {
        if (!manifestFile.exists()) {
            throw new BuildException("Manifest file: " + manifestFile +
                                     " does not exist.", getLocation());
        }

        this.manifestFile = manifestFile;
    }

    private Manifest getManifest(File manifestFile) {

        Manifest newManifest = null;
        Reader r = null;
        try {
            r = new FileReader(manifestFile);
            newManifest = getManifest(r);
        } catch (IOException e) {
            throw new BuildException("Unable to read manifest file: "
                                     + manifestFile
                                     + " (" + e.getMessage() + ")", e);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
        return newManifest;
    }

    private Manifest getManifest(Reader r) {

        Manifest newManifest = null;
        try {
            newManifest = new Manifest(r);
        } catch (ManifestException e) {
            log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException("Invalid Manifest: " + manifestFile,
                                     e, getLocation());
        } catch (IOException e) {
            throw new BuildException("Unable to read manifest file"
                                     + " (" + e.getMessage() + ")", e);
        }
        return newManifest;
    }

    public void setFilesetmanifest(FilesetManifestConfig config) {
        filesetManifestConfig = config;
        mergeManifestsMain = "merge".equals(config.getValue());
    }

    public void addMetainf(ZipFileSet fs) {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix("META-INF/");
        super.addFileset(fs);
    }

    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {
        if (filesetManifestConfig == null
            || filesetManifestConfig.getValue().equals("skip")) {
            manifestOnFinalize = false;
            createManifest(zOut);
        }
    }

    private void createManifest(ZipOutputStream zOut)
        throws IOException, BuildException {
        String ls = System.getProperty("line.separator");
        try {
            Manifest finalManifest = Manifest.getDefaultManifest();

            if (manifest == null) {
                if (manifestFile != null) {
                    // if we haven't got the manifest yet, attempt to
                    // get it now and have manifest be the final merge
                    manifest = getManifest(manifestFile);
                    finalManifest.merge(filesetManifest);
                    finalManifest.merge(configuredManifest);
                    finalManifest.merge(manifest, !mergeManifestsMain);
                } else if (configuredManifest != null) {
                    // configuredManifest is the final merge
                    finalManifest.merge(filesetManifest);
                    finalManifest.merge(configuredManifest,
                                        !mergeManifestsMain);
                } else if (filesetManifest != null) {
                    // filesetManifest is the final (and only) merge
                    finalManifest.merge(filesetManifest, !mergeManifestsMain);
                }
            } else {
                // manifest is the final merge
                finalManifest.merge(filesetManifest);
                finalManifest.merge(configuredManifest);
                finalManifest.merge(manifest, !mergeManifestsMain);
            }

            for (Enumeration e = finalManifest.getWarnings();
                 e.hasMoreElements();) {
                log("Manifest warning: " + (String) e.nextElement(),
                    Project.MSG_WARN);
            }

            // need to set the line.separator as \r\n due to a bug
            // with the jar verifier
            System.getProperties().put("line.separator", "\r\n");

            zipDir(null, zOut, "META-INF/");
            // time to write the manifest
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos);
            finalManifest.write(writer);
            writer.flush();

            ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            super.zipFile(bais, zOut, "META-INF/MANIFEST.MF",
                          System.currentTimeMillis(), null);
            super.initZipOutputStream(zOut);
        } catch (ManifestException e) {
            log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException("Invalid Manifest", e, getLocation());
        } finally {
            System.getProperties().put("line.separator", ls);
        }
    }

    protected void finalizeZipOutputStream(ZipOutputStream zOut)
            throws IOException, BuildException {
        if (manifestOnFinalize) {
            createManifest(zOut);
        }

        if (index) {
            createIndexList(zOut);
        }
    }

    /**
     * Create the index list to speed up classloading.
     * This is a JDK 1.3+ specific feature and is enabled by default. See
     * <a href="http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#JAR+Index">
     * the JAR index specification</a> for more details.
     *
     * @param zOut the zip stream representing the jar being built.
     * @throws IOException thrown if there is an error while creating the
     * index and adding it to the zip stream.
     */
    private void createIndexList(ZipOutputStream zOut) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // encoding must be UTF8 as specified in the specs.
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos,
                                                                    "UTF8"));

        // version-info blankline
        writer.println("JarIndex-Version: 1.0");
        writer.println();

        // header newline
        writer.println(zipFile.getName());

        // JarIndex is sorting the directories by ascending order.
        // it's painful to do in JDK 1.1 and it has no value but cosmetic
        // since it will be read into a hashtable by the classloader.
        Enumeration enum = addedDirs.keys();
        while (enum.hasMoreElements()) {
            String dir = (String) enum.nextElement();

            // try to be smart, not to be fooled by a weird directory name
            // @fixme do we need to check for directories starting by ./ ?
            dir = dir.replace('\\', '/');
            int pos = dir.lastIndexOf('/');
            if (pos != -1){
                dir = dir.substring(0, pos);
            }

            // looks like nothing from META-INF should be added
            // and the check is not case insensitive.
            // see sun.misc.JarIndex
            if (dir.startsWith("META-INF")) {
                continue;
            }
            // name newline
            writer.println(dir);
        }

        writer.flush();
        ByteArrayInputStream bais =
            new ByteArrayInputStream(baos.toByteArray());
        super.zipFile(bais, zOut, INDEX_NAME, System.currentTimeMillis(), null);
    }

    /**
     * Overriden from Zip class to deal with manifests
     */
    protected void zipFile(File file, ZipOutputStream zOut, String vPath)
        throws IOException {
        if ("META-INF/MANIFEST.MF".equalsIgnoreCase(vPath))  {
            filesetManifest(file, null);
        } else {
            super.zipFile(file, zOut, vPath);
        }
    }

    /**
     * Overriden from Zip class to deal with manifests
     */
    protected void zipFile(InputStream is, ZipOutputStream zOut, String vPath,
                           long lastModified, File file)
        throws IOException {
        if ("META-INF/MANIFEST.MF".equalsIgnoreCase(vPath))  {
            filesetManifest(file, is);
        } else {
            super.zipFile(is, zOut, vPath, lastModified, null);
        }
    }

    private void filesetManifest(File file, InputStream is) {
        if (manifestFile != null && manifestFile.equals(file)) {
            // If this is the same name specified in 'manifest', this
            // is the manifest to use
            log("Found manifest " + file, Project.MSG_VERBOSE);
            if (is != null) {
                manifest = getManifest(new InputStreamReader(is));
            } else {
                manifest = getManifest(file);
            }
        } else if (filesetManifestConfig != null && 
                   !filesetManifestConfig.getValue().equals("skip")) {
            // we add this to our group of fileset manifests
            log("Found manifest to merge in file " + file,
                Project.MSG_VERBOSE);

            try {
                Manifest newManifest = getManifest(new InputStreamReader(is));
                if (filesetManifest == null) {
                    filesetManifest = newManifest;
                } else {
                    filesetManifest.merge(newManifest);
                }
            } catch (ManifestException e) {
                log("Manifest in file " + file + " is invalid: "
                    + e.getMessage(), Project.MSG_ERR);
                throw new BuildException("Invalid Manifest", e, getLocation());
            }
        } else {
            // assuming 'skip' otherwise
            // don't warn if skip has been requested explicitly, warn if user
            // didn't set the attribute
            int logLevel = filesetManifestConfig == null ?
                Project.MSG_WARN : Project.MSG_VERBOSE;
            log("File " + file
                + " includes a META-INF/MANIFEST.MF which will be ignored. "
                + "To include this file, set filesetManifest to a value other "
                + "than 'skip'.", logLevel);
        }
    }

    /**
     * Check whether the archive is up-to-date;
     * @param scanners list of prepared scanners containing files to archive
     * @param zipFile intended archive file (may or may not exist)
     * @return true if nothing need be done (may have done something
     *         already); false if archive creation should proceed
     * @exception BuildException if it likes
     */
    protected boolean isUpToDate(FileScanner[] scanners, File zipFile)
        throws BuildException {
        // need to handle manifest as a special check
        if (configuredManifest != null || manifestFile == null) {
            java.util.zip.ZipFile theZipFile = null;
            try {
                theZipFile = new java.util.zip.ZipFile(zipFile);
                java.util.zip.ZipEntry entry =
                    theZipFile.getEntry("META-INF/MANIFEST.MF");
                if (entry == null) {
                    log("Updating jar since the current jar has no manifest",
                        Project.MSG_VERBOSE);
                    return false;
                }
                Manifest currentManifest =
                    new Manifest(new InputStreamReader(theZipFile
                                                       .getInputStream(entry)));
                if (configuredManifest == null) {
                    configuredManifest = Manifest.getDefaultManifest();
                }
                if (!currentManifest.equals(configuredManifest)) {
                    log("Updating jar since jar manifest has changed",
                        Project.MSG_VERBOSE);
                    return false;
                }
            } catch (Exception e) {
                // any problems and we will rebuild
                log("Updating jar since cannot read current jar manifest: "
                    + e.getClass().getName() + " - " + e.getMessage(),
                    Project.MSG_VERBOSE);
                return false;
            } finally {
                if (theZipFile != null) {
                    try {
                        theZipFile.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        } else if (manifestFile.lastModified() > zipFile.lastModified()) {
            return false;
        }
        return super.isUpToDate(scanners, zipFile);
    }

    protected boolean createEmptyZip(File zipFile) {
        // Jar files always contain a manifest and can never be empty
        return true;
    }

    /**
     * Make sure we don't think we already have a MANIFEST next time this task
     * gets executed.
     *
     * @see Zip#cleanUp
     */
    protected void cleanUp() {
        super.cleanUp();

        manifest = null;
        configuredManifest = savedConfiguredManifest;
        filesetManifest = null;
    }

    /**
     * reset to default values.
     *
     * @see Zip#reset
     *
     * @since 1.44, Ant 1.5
     */
    public void reset() {
        super.reset();
        configuredManifest = null;
        filesetManifestConfig = null;
        mergeManifestsMain = false;
        manifestFile = null;
        index = false;
    }

    public static class FilesetManifestConfig extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"skip", "merge", "mergewithoutmain"};
        }
    }
}
