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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.ZipOutputStream;


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

    /** The mainfest file name. */
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    /** merged manifests added through addConfiguredManifest */
    private Manifest configuredManifest;
    /** shadow of the above if upToDate check alters the value */
    private Manifest savedConfiguredManifest;

    /**  merged manifests added through filesets */
    private Manifest filesetManifest;

    /** 
     * Manifest of original archive, will be set to null if not in
     * update mode.
     */
    private Manifest originalManifest;

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
    private boolean mergeManifestsMain = true;

    /** the manifest specified by the 'manifest' attribute **/
    private Manifest manifest;

    /** The encoding to use when reading in a manifest file */
    private String manifestEncoding;
    
    /**
     * The file found from the 'manifest' attribute.  This can be
     * either the location of a manifest, or the name of a jar added
     * through a fileset.  If its the name of an added jar, the
     * manifest is looked for in META-INF/MANIFEST.MF
     */
    private File manifestFile;

    /** jar index is JDK 1.3+ only */
    private boolean index = false;

    /** 
     * whether to really create the archive in createEmptyZip, will
     * get set in getResourcesToAdd.
     */
    private boolean createEmpty = false;

    /** constructor */
    public Jar() {
        super();
        archiveType = "jar";
        emptyBehavior = "create";
        setEncoding("UTF8");
    }

    /**
     * @ant.attribute ignore="true"
     */
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
     * Set whether or not to create an index list for classes.
     * This may speed up classloading in some cases.
     */
    public void setIndex(boolean flag){
        index = flag;
    }

    /**
     * Set whether or not to create an index list for classes.
     * This may speed up classloading in some cases.
     */
    public void setManifestEncoding(String manifestEncoding) {
        this.manifestEncoding = manifestEncoding;
    }

    /**
     * Allows the manifest for the archive file to be provided inline
     * in the build file rather than in an external file.
     *
     * @param newManifest
     * @throws ManifestException
     */
    public void addConfiguredManifest(Manifest newManifest)
        throws ManifestException {
        if (configuredManifest == null) {
            configuredManifest = newManifest;
        } else {
            configuredManifest.merge(newManifest);
        }
        savedConfiguredManifest = configuredManifest;
    }

    /**
     * The manifest file to use. This can be either the location of a manifest,
     * or the name of a jar added through a fileset. If its the name of an added
     * jar, the task expects the manifest to be in the jar at META-INF/MANIFEST.MF.
     *
     * @param manifestFile the manifest file to use.
     */
    public void setManifest(File manifestFile) {
        if (!manifestFile.exists()) {
            throw new BuildException("Manifest file: " + manifestFile +
                                     " does not exist.", getLocation());
        }

        this.manifestFile = manifestFile;
    }

    private Manifest getManifest(File manifestFile) {

        Manifest newManifest = null;
        FileInputStream fis = null;
        InputStreamReader isr = null;
        try {
            fis = new FileInputStream(manifestFile);
            if (manifestEncoding == null) {
                isr = new InputStreamReader(fis);
            } else {
                isr = new InputStreamReader(fis, manifestEncoding);
            }
            newManifest = getManifest(isr);
        } catch (UnsupportedEncodingException e) {
            throw new BuildException("Unsupported encoding while reading manifest: "
                                     + e.getMessage(), e);
        } catch (IOException e) {
            throw new BuildException("Unable to read manifest file: "
                                     + manifestFile
                                     + " (" + e.getMessage() + ")", e);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
        return newManifest;
    }

    /**
     * @return null if jarFile doesn't contain a manifest, the
     * manifest otherwise.
     * @since Ant 1.5.2
     */
    private Manifest getManifestFromJar(File jarFile) throws IOException {
        ZipFile zf = null;
        try {
            zf = new ZipFile(jarFile);
            
            // must not use getEntry as "well behaving" applications
            // must accept the manifest in any capitalization
            Enumeration enum = zf.entries();
            while (enum.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) enum.nextElement();
                if (ze.getName().equalsIgnoreCase(MANIFEST_NAME)) {
                    InputStreamReader isr =
                        new InputStreamReader(zf.getInputStream(ze), "UTF-8");
                    return getManifest(isr);
                }
            }
            return null;
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException e) {
                    // XXX - log an error?  throw an exception?
                }
            }
        }
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

    /**
     * Behavior when a Manifest is found in a zipfileset or zipgroupfileset file.
     * Valid values are "skip", "merge", and "mergewithoutmain".
     * "merge" will merge all of manifests together, and merge this into any
     * other specified manifests.
     * "mergewithoutmain" merges everything but the Main section of the manifests.
     * Default value is "skip".
     *
     * Note: if this attribute's value is not "skip", the created jar will not
     * be readable by using java.util.jar.JarInputStream
     *
     * @param config setting for found manifest behavior.
     */
    /*
    public void setFilesetmanifest(FilesetManifestConfig config) {
        filesetManifestConfig = config;
        mergeManifestsMain = "merge".equals(config.getValue());
    }
    */

    /**
     * Adds a zipfileset to include in the META-INF directory.
     *
     * @param fs zipfileset to add
     */
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
            Manifest jarManifest = createManifest();
            writeManifest(zOut, jarManifest);
        }
    }

    private Manifest createManifest()
        throws BuildException {
        try {
            Manifest finalManifest = Manifest.getDefaultManifest();

            if (manifest == null) {
                if (manifestFile != null) {
                    // if we haven't got the manifest yet, attempt to
                    // get it now and have manifest be the final merge
                    manifest = getManifest(manifestFile);
                }
            }

            /*
             * Precedence: manifestFile wins over inline manifest,
             * over manifests read from the filesets over the original
             * manifest.
             *
             * merge with null argument is a no-op
             */

            if (isInUpdateMode()) {
                finalManifest.merge(originalManifest);
            }
            finalManifest.merge(filesetManifest);
            finalManifest.merge(configuredManifest);
            finalManifest.merge(manifest, !mergeManifestsMain);

            return finalManifest;

        } catch (ManifestException e) {
            log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException("Invalid Manifest", e, getLocation());
        }
    }

    private void writeManifest(ZipOutputStream zOut, Manifest manifest)
         throws IOException {
        for (Enumeration e = manifest.getWarnings();
             e.hasMoreElements();) {
            log("Manifest warning: " + (String) e.nextElement(),
                Project.MSG_WARN);
        }

        zipDir(null, zOut, "META-INF/", ZipFileSet.DEFAULT_DIR_MODE);
        // time to write the manifest
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF-8");
        PrintWriter writer = new PrintWriter(osw);
        manifest.write(writer);
        writer.flush();

        ByteArrayInputStream bais =
            new ByteArrayInputStream(baos.toByteArray());
        super.zipFile(bais, zOut, MANIFEST_NAME,
                      System.currentTimeMillis(), null,
                      ZipFileSet.DEFAULT_FILE_MODE);
        super.initZipOutputStream(zOut);
    }

    protected void finalizeZipOutputStream(ZipOutputStream zOut)
            throws IOException, BuildException {
        if (manifestOnFinalize) {
            Manifest jarManifest = createManifest();
            writeManifest(zOut, jarManifest);
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
        super.zipFile(bais, zOut, INDEX_NAME, System.currentTimeMillis(), null,
                      ZipFileSet.DEFAULT_FILE_MODE);
    }

    /**
     * Overriden from Zip class to deal with manifests
     */
    protected void zipFile(InputStream is, ZipOutputStream zOut, String vPath,
                           long lastModified, File fromArchive, int mode)
        throws IOException {
        if (MANIFEST_NAME.equalsIgnoreCase(vPath))  {
            filesetManifest(fromArchive, is);
        } else {
            super.zipFile(is, zOut, vPath, lastModified, fromArchive, mode);
        }
    }

    private void filesetManifest(File file, InputStream is) throws IOException {
        if (manifestFile != null && manifestFile.equals(file)) {
            // If this is the same name specified in 'manifest', this
            // is the manifest to use
            log("Found manifest " + file, Project.MSG_VERBOSE);
            try {
                if (is != null) {
                    InputStreamReader isr;
                    if (manifestEncoding == null) {
                        isr = new InputStreamReader(is);
                    } else {
                        isr = new InputStreamReader(is, manifestEncoding);
                    }
                    manifest = getManifest(isr);
                } else {
                    manifest = getManifest(file);
                }
            } catch (UnsupportedEncodingException e) {
                throw new BuildException("Unsupported encoding while reading " 
                    + "manifest: " + e.getMessage(), e);
            }
        } else if (filesetManifestConfig != null &&
                   !filesetManifestConfig.getValue().equals("skip")) {
            // we add this to our group of fileset manifests
            log("Found manifest to merge in file " + file,
                Project.MSG_VERBOSE);

            try {
                Manifest newManifest = null;
                if (is != null) {
                    InputStreamReader isr;
                    if (manifestEncoding == null) {
                        isr = new InputStreamReader(is);
                    } else {
                        isr = new InputStreamReader(is, manifestEncoding);
                    }
                    newManifest = getManifest(isr);
                } else {
                    newManifest = getManifest(file);
                }

                if (filesetManifest == null) {
                    filesetManifest = newManifest;
                } else {
                    filesetManifest.merge(newManifest);
                }
            } catch (UnsupportedEncodingException e) {
                throw new BuildException("Unsupported encoding while reading " 
                    + "manifest: " + e.getMessage(), e);
            } catch (ManifestException e) {
                log("Manifest in file " + file + " is invalid: "
                    + e.getMessage(), Project.MSG_ERR);
                throw new BuildException("Invalid Manifest", e, getLocation());
            }
        } else {
            // assuming 'skip' otherwise
            // don't warn if skip has been requested explicitly, warn if user
            // didn't set the attribute

            // Hide warning also as it makes no sense since
            // the filesetmanifest attribute itself has been
            // hidden

            //int logLevel = filesetManifestConfig == null ?
            //    Project.MSG_WARN : Project.MSG_VERBOSE;
            //log("File " + file
            //    + " includes a META-INF/MANIFEST.MF which will be ignored. "
            //    + "To include this file, set filesetManifest to a value other "
            //    + "than 'skip'.", logLevel);
        }
    }

    /**
     * Collect the resources that are newer than the corresponding
     * entries (or missing) in the original archive.
     *
     * <p>If we are going to recreate the archive instead of updating
     * it, all resources should be considered as new, if a single one
     * is.  Because of this, subclasses overriding this method must
     * call <code>super.getResourcesToAdd</code> and indicate with the
     * third arg if they already know that the archive is
     * out-of-date.</p>
     *
     * @param filesets The filesets to grab resources from
     * @param zipFile intended archive file (may or may not exist)
     * @param needsUpdate whether we already know that the archive is
     * out-of-date.  Subclasses overriding this method are supposed to
     * set this value correctly in their call to
     * super.getResourcesToAdd.
     * @return an array of resources to add for each fileset passed in as well
     *         as a flag that indicates whether the archive is uptodate.
     *
     * @exception BuildException if it likes
     */
    protected ArchiveState getResourcesToAdd(FileSet[] filesets,
                                             File zipFile,
                                             boolean needsUpdate)
        throws BuildException {

        // need to handle manifest as a special check
        if (zipFile.exists()) {
            // if it doesn't exist, it will get created anyway, don't
            // bother with any up-to-date checks.

            try {
                originalManifest = getManifestFromJar(zipFile);
                if (originalManifest == null) {
                    log("Updating jar since the current jar has no manifest",
                        Project.MSG_VERBOSE);
                    needsUpdate = true;
                } else {
                    Manifest mf = createManifest();
                    if (!mf.equals(originalManifest)) {
                        log("Updating jar since jar manifest has changed", 
                            Project.MSG_VERBOSE);
                        needsUpdate = true;
                    }
                }
            } catch (Throwable t) {
                log("error while reading original manifest: " + t.getMessage(),
                    Project.MSG_WARN);
                needsUpdate = true;
            }

        } else {
            // no existing archive
            needsUpdate = true;
        }

        createEmpty = needsUpdate;
        return super.getResourcesToAdd(filesets, zipFile, needsUpdate);
    }

    protected boolean createEmptyZip(File zipFile) throws BuildException {
        if (!createEmpty) {
            return true;
        }
        
        ZipOutputStream zOut = null;
        try {
            log("Building MANIFEST-only jar: " 
                + getDestFile().getAbsolutePath());
            zOut = new ZipOutputStream(new FileOutputStream(getDestFile()));

            zOut.setEncoding(getEncoding());
            if (isCompress()) {
                zOut.setMethod(ZipOutputStream.DEFLATED);
            } else {
                zOut.setMethod(ZipOutputStream.STORED);
            }
            initZipOutputStream(zOut);
            finalizeZipOutputStream(zOut);
        } catch (IOException ioe) {
            throw new BuildException("Could not create almost empty JAR archive"
                                     + " (" + ioe.getMessage() + ")", ioe,
                                     getLocation());
        } finally {
            // Close the output stream.
            try {
                if (zOut != null) {
                    zOut.close();
                }
            } catch (IOException ex) {
            }
            createEmpty = false;
        }
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
