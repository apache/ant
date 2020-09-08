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

package org.apache.tools.ant.taskdefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Manifest.Section;
import org.apache.tools.ant.types.ArchiveFileSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.spi.Service;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StreamUtils;
import org.apache.tools.zip.JarMarker;
import org.apache.tools.zip.ZipExtraField;
import org.apache.tools.zip.ZipOutputStream;

/**
 * Creates a JAR archive.
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */
public class Jar extends Zip {
    /** The index file name. */
    private static final String INDEX_NAME = "META-INF/INDEX.LIST";

    /** The manifest file name. */
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    /**
     * List of all known SPI Services
     */
    private List<Service> serviceList = new ArrayList<>();

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

    /** Whether to index META-INF/ and its children */
    private boolean indexMetaInf = false;

    /**
     * whether to really create the archive in createEmptyZip, will
     * get set in getResourcesToAdd.
     */
    private boolean createEmpty = false;

    /**
     * Stores all files that are in the root of the archive (i.e. that
     * have a name that doesn't contain a slash) so they can get
     * listed in the index.
     *
     * Will not be filled unless the user has asked for an index.
     *
     * @since Ant 1.6
     */
    private List<String> rootEntries;

    /**
     * Path containing jars that shall be indexed in addition to this archive.
     *
     * @since Ant 1.6.2
     */
    private Path indexJars;

    /**
     * A mapper used to convert the jars to entries in the index.
     *
     * @since Ant 1.10.9
     */
    private FileNameMapper indexJarsMapper = null;


    // CheckStyle:LineLength OFF - Link is too long.
    /**
     * Strict mode for checking rules of the JAR-Specification.
     * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/versioning/spec/versioning2.html#wp89936"></a>
     */
    private StrictMode strict = new StrictMode("ignore");

    // CheckStyle:LineLength ON

    /**
     * whether to merge Class-Path attributes.
     */
    private boolean mergeClassPaths = false;

    /**
     * whether to flatten Class-Path attributes into a single one.
     */
    private boolean flattenClassPaths = false;

    /**
     * Extra fields needed to make Solaris recognize the archive as a jar file.
     *
     * @since Ant 1.6.3
     */
    private static final ZipExtraField[] JAR_MARKER = new ZipExtraField[] {
        JarMarker.getInstance()
    };

    /** constructor */
    public Jar() {
        super();
        archiveType = "jar";
        emptyBehavior = "create";
        setEncoding("UTF8");
        setZip64Mode(Zip64ModeAttribute.NEVER);
        rootEntries = new Vector<>();
    }

    /**
     * Not used for jar files.
     * @param we not used
     * @ant.attribute ignore="true"
     */
    @Override
    public void setWhenempty(WhenEmpty we) {
        log("JARs are never empty, they contain at least a manifest file",
            Project.MSG_WARN);
    }

    /**
     * Indicates if a jar file should be created when it would only contain a
     * manifest file.
     * Possible values are: <code>fail</code> (throw an exception
     * and halt the build); <code>skip</code> (do not create
     * any archive, but issue a warning); <code>create</code>
     * (make an archive with only a manifest file).
     * Default is <code>create</code>;
     * @param we a <code>WhenEmpty</code> enumerated value
     */
    public void setWhenmanifestonly(WhenEmpty we) {
        emptyBehavior = we.getValue();
    }

    /**
     * Activate the strict mode. When set to <i>true</i> a BuildException
     * will be thrown if the Jar-Packaging specification was broken.
     * @param strict New value of the strict mode.
     * @since Ant 1.7.1
     */
    public void setStrict(StrictMode strict) {
        this.strict = strict;
    }

    /**
     * Set the destination file.
     * @param jarFile the destination file
     * @deprecated since 1.5.x.
     *             Use setDestFile(File) instead.
     */
    @Deprecated
    public void setJarfile(File jarFile) {
        setDestFile(jarFile);
    }

    /**
     * Set whether or not to create an index list for classes.
     * This may speed up classloading in some cases.
     * @param flag a <code>boolean</code> value
     */
    public void setIndex(boolean flag) {
        index = flag;
    }

    /**
     * Set whether or not to add META-INF and its children to the index.
     *
     * <p>Doesn't have any effect if index is false.</p>
     *
     * <p>Sun's jar implementation used to skip the META-INF directory
     * and Ant followed that example.  The behavior has been changed
     * with Java 5.  In order to avoid problems with Ant generated
     * jars on Java 1.4 or earlier Ant will not include META-INF
     * unless explicitly asked to.</p>
     *
     * @see <a href="https://bugs.openjdk.java.net/browse/JDK-4408526">
     * jar -i omits service providers in index.list</a>
     * @since Ant 1.8.0
     * @param flag a <code>boolean</code> value, defaults to false
     */
    public void setIndexMetaInf(boolean flag) {
        indexMetaInf = flag;
    }

    /**
     * The character encoding to use in the manifest file.
     *
     * @param manifestEncoding the character encoding
     */
    public void setManifestEncoding(String manifestEncoding) {
        this.manifestEncoding = manifestEncoding;
    }

    /**
     * Allows the manifest for the archive file to be provided inline
     * in the build file rather than in an external file.
     *
     * @param newManifest an embedded manifest element
     * @throws ManifestException on error
     */
    public void addConfiguredManifest(Manifest newManifest)
        throws ManifestException {
        if (configuredManifest == null) {
            configuredManifest = newManifest;
        } else {
            configuredManifest.merge(newManifest, false, mergeClassPaths);
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
            throw new BuildException("Manifest file: " + manifestFile
                                     + " does not exist.", getLocation());
        }

        this.manifestFile = manifestFile;
    }

    private Manifest getManifest(File manifestFile) {
        try (InputStreamReader isr = new InputStreamReader(
            Files.newInputStream(manifestFile.toPath()), getManifestCharset())) {
            return getManifest(isr);
        } catch (IOException e) {
            throw new BuildException("Unable to read manifest file: "
                + manifestFile + " (" + e.getMessage() + ")", e);
        }
    }

    /**
     * @return null if jarFile doesn't contain a manifest, the
     * manifest otherwise.
     * @since Ant 1.5.2
     */
    private Manifest getManifestFromJar(File jarFile) throws IOException {
        try (ZipFile zf = new ZipFile(jarFile)) {
            // must not use getEntry as "well behaving" applications
            // must accept the manifest in any capitalization
            ZipEntry ze = StreamUtils.enumerationAsStream(zf.entries())
                    .filter(entry -> MANIFEST_NAME.equalsIgnoreCase(entry.getName()))
                    .findFirst().orElse(null);
            if (ze == null) {
                return null;
            }
            try (InputStreamReader isr = new InputStreamReader(zf.getInputStream(ze),
                    StandardCharsets.UTF_8)) {
                return getManifest(isr);
            }
        }
    }

    private Manifest getManifest(Reader r) {
        try {
            return new Manifest(r);
        } catch (ManifestException e) {
            log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException("Invalid Manifest: " + manifestFile,
                                     e, getLocation());
        } catch (IOException e) {
            throw new BuildException("Unable to read manifest file"
                                     + " (" + e.getMessage() + ")", e);
        }
    }

    private boolean jarHasIndex(File jarFile) throws IOException {
        try (ZipFile zf = new ZipFile(jarFile)) {
            return StreamUtils.enumerationAsStream(zf.entries())
                    .anyMatch(ze -> INDEX_NAME.equalsIgnoreCase(ze.getName()));
        }
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
    public void setFilesetmanifest(FilesetManifestConfig config) {
        filesetManifestConfig = config;
        mergeManifestsMain = config != null && "merge".equals(config.getValue());

        if (filesetManifestConfig != null
            && !"skip".equals(filesetManifestConfig.getValue())) {

            doubleFilePass = true;
        }
    }

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

    /**
     * Add a path to index jars.
     * @param p a path
     * @since Ant 1.6.2
     */
    public void addConfiguredIndexJars(Path p) {
        if (indexJars == null) {
            indexJars = new Path(getProject());
        }
        indexJars.append(p);
    }

    /**
     * Add a mapper used to convert the jars to entries in the index.
     *
     * @param mapper a mapper
     * @since Ant 1.10.9
     */
    public void addConfiguredIndexJarsMapper(Mapper mapper) {
        if (indexJarsMapper != null) {
            throw new BuildException("Cannot define more than one indexjar-mapper",
                    getLocation());
        }
        indexJarsMapper = mapper.getImplementation();
    }

    /**
     * Returns the mapper used to convert the jars to entries in the index. May be null.
     *
     * @since Ant 1.10.9
     */
    public FileNameMapper getIndexJarsMapper() {
        return indexJarsMapper;
    }


    /**
     * A nested SPI service element.
     * @param service the nested element.
     * @since Ant 1.7
     */
    public void addConfiguredService(Service service) {
        // Check if the service is configured correctly
        service.check();
        serviceList.add(service);
    }

    /**
     * Write SPI Information to JAR
     */
    private void writeServices(ZipOutputStream zOut) throws IOException {
        for (Service service : serviceList) {
            try (InputStream is = service.getAsStream()) {
                //stolen from writeManifest
               super.zipFile(is, zOut,
                             "META-INF/services/" + service.getType(),
                             System.currentTimeMillis(), null,
                             ZipFileSet.DEFAULT_FILE_MODE);
           }
        }
    }

    /**
     * Whether to merge Class-Path attributes.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setMergeClassPathAttributes(boolean b) {
        mergeClassPaths = b;
    }

    /**
     * Whether to flatten multi-valued attributes (i.e. Class-Path)
     * into a single one.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setFlattenAttributes(boolean b) {
        flattenClassPaths = b;
    }

    /**
     * Initialize the zip output stream.
     * @param zOut the zip output stream
     * @throws IOException on I/O errors
     * @throws BuildException on other errors
     */
    @Override
    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {

        if (!skipWriting) {
            Manifest jarManifest = createManifest();
            writeManifest(zOut, jarManifest);
            writeServices(zOut);
        }
    }

    private Manifest createManifest()
        throws BuildException {
        try {
            if (manifest == null && manifestFile != null) {
                // if we haven't got the manifest yet, attempt to
                // get it now and have manifest be the final merge
                manifest = getManifest(manifestFile);
            }

            // fileset manifest must come even before the default
            // manifest if mergewithoutmain is selected and there is
            // no explicit manifest specified - otherwise the Main
            // section of the fileset manifest is still merged to the
            // final manifest.
            boolean mergeFileSetFirst = !mergeManifestsMain
                && filesetManifest != null
                && configuredManifest == null && manifest == null;

            Manifest finalManifest;
            if (mergeFileSetFirst) {
                finalManifest = new Manifest();
                finalManifest.merge(filesetManifest, false, mergeClassPaths);
                finalManifest.merge(Manifest.getDefaultManifest(),
                                    true, mergeClassPaths);
            } else {
                finalManifest = Manifest.getDefaultManifest();
            }

            /*
             * Precedence: manifestFile wins over inline manifest,
             * over manifests read from the filesets over the original
             * manifest.
             *
             * merge with null argument is a no-op
             */

            if (isInUpdateMode()) {
                finalManifest.merge(originalManifest, false, mergeClassPaths);
            }
            if (!mergeFileSetFirst) {
                finalManifest.merge(filesetManifest, false, mergeClassPaths);
            }
            finalManifest.merge(configuredManifest, !mergeManifestsMain,
                                mergeClassPaths);
            finalManifest.merge(manifest, !mergeManifestsMain,
                                mergeClassPaths);

            return finalManifest;

        } catch (ManifestException e) {
            log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException("Invalid Manifest", e, getLocation());
        }
    }

    private void writeManifest(ZipOutputStream zOut, Manifest manifest)
        throws IOException {
        StreamUtils.enumerationAsStream(manifest.getWarnings())
                .forEach(warning -> log("Manifest warning: " + warning, Project.MSG_WARN));

        zipDir((Resource) null, zOut, "META-INF/", ZipFileSet.DEFAULT_DIR_MODE,
               JAR_MARKER);
        // time to write the manifest
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(baos, Manifest.JAR_CHARSET);
        PrintWriter writer = new PrintWriter(osw);
        manifest.write(writer, flattenClassPaths);
        if (writer.checkError()) {
            throw new IOException("Encountered an error writing the manifest");
        }
        writer.close();

        ByteArrayInputStream bais =
            new ByteArrayInputStream(baos.toByteArray());
        try {
            super.zipFile(bais, zOut, MANIFEST_NAME,
                          System.currentTimeMillis(), null,
                          ZipFileSet.DEFAULT_FILE_MODE);
        } finally {
            // not really required
            FileUtils.close(bais);
        }
        super.initZipOutputStream(zOut);
    }

    /**
     * Finalize the zip output stream.
     * This creates an index list if the index attribute is true.
     * @param zOut the zip output stream
     * @throws IOException on I/O errors
     * @throws BuildException on other errors
     */
    @Override
    protected void finalizeZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {

        if (index) {
            createIndexList(zOut);
        }
    }

    /**
     * Create the index list to speed up classloading.
     * This is a JDK 1.3+ specific feature and is enabled by default. See
     * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#JAR_Index">
     * the JAR index specification</a> for more details.
     *
     * @param zOut the zip stream representing the jar being built.
     * @throws IOException thrown if there is an error while creating the
     * index and adding it to the zip stream.
     */
    private void createIndexList(ZipOutputStream zOut) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // encoding must be UTF8 as specified in the specs.
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        // version-info blankline
        writer.println("JarIndex-Version: 1.0");
        writer.println();

        // header newline
        writer.println(zipFile.getName());

        writeIndexLikeList(new ArrayList<>(addedDirs.keySet()),
                           rootEntries, writer);
        writer.println();

        if (indexJars != null) {
            FileNameMapper mapper = indexJarsMapper;
            if (mapper == null) {
                mapper = createDefaultIndexJarsMapper();
            }
            for (String indexJarEntry : indexJars.list()) {
                String[] names = mapper.mapFileName(indexJarEntry);
                if (names != null && names.length > 0) {
                    ArrayList<String> dirs = new ArrayList<>();
                    ArrayList<String> files = new ArrayList<>();
                    grabFilesAndDirs(indexJarEntry, dirs, files);
                    if (dirs.size() + files.size() > 0) {
                        writer.println(names[0]);
                        writeIndexLikeList(dirs, files, writer);
                        writer.println();
                    }
                }
            }
        }

        if (writer.checkError()) {
            throw new IOException("Encountered an error writing jar index");
        }
        writer.close();
        try (ByteArrayInputStream bais =
            new ByteArrayInputStream(baos.toByteArray())) {
            super.zipFile(bais, zOut, INDEX_NAME, System.currentTimeMillis(),
                          null, ZipFileSet.DEFAULT_FILE_MODE);
        }
    }

    /**
     * Creates a mapper for the index based on the classpath attribute in the manifest.
     * See {@link #findJarName(String, String[])} for more details.
     *
     * @return a mapper
     * @since Ant 1.10.9
     */
    private FileNameMapper createDefaultIndexJarsMapper() {
        Manifest mf = createManifest();
        Manifest.Attribute classpath =
                mf.getMainSection().getAttribute(Manifest.ATTRIBUTE_CLASSPATH);
        String[] cpEntries = null;
        if (classpath != null && classpath.getValue() != null) {
            StringTokenizer tok = new StringTokenizer(classpath.getValue(),
                    " ");
            cpEntries = new String[tok.countTokens()];
            int c = 0;
            while (tok.hasMoreTokens()) {
                cpEntries[c++] = tok.nextToken();
            }
        }

        return new IndexJarsFilenameMapper(cpEntries);
    }

    /**
     * Overridden from Zip class to deal with manifests and index lists.
     * @param is the stream to read data for the entry from.  The
     * caller of the method is responsible for closing the stream.
     * @param zOut the zip output stream
     * @param vPath the name this entry shall have in the archive
     * @param lastModified last modification time for the entry.
     * @param fromArchive the original archive we are copying this
     *                    entry from, will be null if we are not copying from an archive.
     * @param mode the Unix permissions to set.
     * @throws IOException on error
     */
    @Override
    protected void zipFile(InputStream is, ZipOutputStream zOut, String vPath,
                           long lastModified, File fromArchive, int mode)
        throws IOException {
        if (MANIFEST_NAME.equalsIgnoreCase(vPath))  {
            if (isFirstPass()) {
                filesetManifest(fromArchive, is);
            }
        } else if (INDEX_NAME.equalsIgnoreCase(vPath) && index) {
            logWhenWriting("Warning: selected " + archiveType
                           + " files include a " + INDEX_NAME + " which will"
                           + " be replaced by a newly generated one.",
                           Project.MSG_WARN);
        } else {
            if (index && !vPath.contains("/")) {
                rootEntries.add(vPath);
            }
            super.zipFile(is, zOut, vPath, lastModified, fromArchive, mode);
        }
    }

    private void filesetManifest(File file, InputStream is) throws IOException {
        if (manifestFile != null && manifestFile.equals(file)) {
            // If this is the same name specified in 'manifest', this
            // is the manifest to use
            log("Found manifest " + file, Project.MSG_VERBOSE);
            if (is == null) {
                manifest = getManifest(file);
            } else {
                try (InputStreamReader isr =
                    new InputStreamReader(is, getManifestCharset())) {
                    manifest = getManifest(isr);
                }
            }
        } else if (filesetManifestConfig != null
                    && !"skip".equals(filesetManifestConfig.getValue())) {
            // we add this to our group of fileset manifests
            logWhenWriting("Found manifest to merge in file " + file,
                           Project.MSG_VERBOSE);

            try {
                Manifest newManifest;
                if (is == null) {
                    newManifest = getManifest(file);
                } else {
                    try (InputStreamReader isr =
                        new InputStreamReader(is, getManifestCharset())) {
                        newManifest = getManifest(isr);
                    }
                }

                if (filesetManifest == null) {
                    filesetManifest = newManifest;
                } else {
                    filesetManifest.merge(newManifest, false, mergeClassPaths);
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
     * @param rcs The resource collections to grab resources from
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
    @Override
    protected ArchiveState getResourcesToAdd(ResourceCollection[] rcs,
                                             File zipFile,
                                             boolean needsUpdate)
        throws BuildException {

        if (skipWriting) {
            // this pass is only there to construct the merged
            // manifest this means we claim an update was needed and
            // only include the manifests, skipping any uptodate
            // checks here deferring them for the second run
            Resource[][] manifests = grabManifests(rcs);
            int count = 0;
            for (Resource[] mf : manifests) {
                count += mf.length;
            }
            log("found a total of " + count + " manifests in "
                + manifests.length + " resource collections",
                Project.MSG_VERBOSE);
            return new ArchiveState(true, manifests);
        }

        // need to handle manifest as a special check
        if (zipFile.exists()) {
            // if it doesn't exist, it will get created anyway, don't
            // bother with any up-to-date checks.

            try {
                originalManifest = getManifestFromJar(zipFile);
                if (originalManifest == null) {
                    log("Updating jar since the current jar has"
                                   + " no manifest", Project.MSG_VERBOSE);
                    needsUpdate = true;
                } else {
                    Manifest mf = createManifest();
                    if (!mf.equals(originalManifest)) {
                        log("Updating jar since jar manifest has"
                                       + " changed", Project.MSG_VERBOSE);
                        needsUpdate = true;
                    }
                }
            } catch (Throwable t) {
                log("error while reading original manifest in file: "
                    + zipFile.toString() + " due to " + t.getMessage(),
                    Project.MSG_WARN);
                needsUpdate = true;
            }

        } else {
            // no existing archive
            needsUpdate = true;
        }

        createEmpty = needsUpdate;
        if (!needsUpdate && index) {
            try {
                needsUpdate = !jarHasIndex(zipFile);
            } catch (IOException e) {
                //if we couldn't read it, we might as well recreate it?
                needsUpdate = true;
            }
        }
        return super.getResourcesToAdd(rcs, zipFile, needsUpdate);
    }

    /**
     * Create an empty jar file.
     * @param zipFile the file to create
     * @return true for historic reasons
     * @throws BuildException on error
     */
    @Override
    protected boolean createEmptyZip(File zipFile) throws BuildException {
        if (!createEmpty) {
            return true;
        }

        if ("skip".equals(emptyBehavior)) {
            if (!skipWriting) {
                log("Warning: skipping " + archiveType + " archive "
                    + zipFile + " because no files were included.",
                    Project.MSG_WARN);
            }
            return true;
        }
        if ("fail".equals(emptyBehavior)) {
            throw new BuildException("Cannot create " + archiveType
                                     + " archive " + zipFile
                                     + ": no files were included.",
                                     getLocation());
        }

        if (!skipWriting) {
            log("Building MANIFEST-only jar: "
                    + getDestFile().getAbsolutePath());

            try (ZipOutputStream zOut = new ZipOutputStream(getDestFile())) {
                zOut.setEncoding(getEncoding());
                zOut.setUseZip64(getZip64Mode().getMode());
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
                createEmpty = false;
            }
        }
        return true;
    }

    /**
     * Make sure we don't think we already have a MANIFEST next time this task
     * gets executed.
     *
     * @see Zip#cleanUp
     */
    @Override
    protected void cleanUp() {
        super.cleanUp();
        checkJarSpec();

        // we want to save this info if we are going to make another pass
        if (!doubleFilePass || !skipWriting) {
            manifest = null;
            configuredManifest = savedConfiguredManifest;
            filesetManifest = null;
            originalManifest = null;
        }
        rootEntries.clear();
    }

    // CheckStyle:LineLength OFF - Link is too long.
    /**
     * Check against packaging spec
     * @see "https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html"
     */
    // CheckStyle:LineLength ON
    private void checkJarSpec() {
        StringBuilder message = new StringBuilder();
        Section mainSection = (configuredManifest == null)
                            ? null
                            : configuredManifest.getMainSection();

        if (mainSection == null) {
            message.append("No Implementation-Title set.");
            message.append("No Implementation-Version set.");
            message.append("No Implementation-Vendor set.");
        } else {
            if (mainSection.getAttribute("Implementation-Title") == null) {
                message.append("No Implementation-Title set.");
            }
            if (mainSection.getAttribute("Implementation-Version") == null) {
                message.append("No Implementation-Version set.");
            }
            if (mainSection.getAttribute("Implementation-Vendor") == null) {
                message.append("No Implementation-Vendor set.");
            }
        }

        if (message.length() > 0) {
            message.append(String.format("%nLocation: %s%n", getLocation()));
            if ("fail".equalsIgnoreCase(strict.getValue())) {
                throw new BuildException(message.toString(), getLocation());
            }
            logWhenWriting(message.toString(), strict.getLogLevel());
        }
    }

    /**
     * reset to default values.
     *
     * @see Zip#reset
     *
     * @since 1.44, Ant 1.5
     */
    @Override
    public void reset() {
        super.reset();
        emptyBehavior = "create";
        configuredManifest = null;
        filesetManifestConfig = null;
        mergeManifestsMain = false;
        manifestFile = null;
        index = false;
    }

    /**
     * The manifest config enumerated type.
     */
    public static class FilesetManifestConfig extends EnumeratedAttribute {
        /**
         * Get the list of valid strings.
         * @return the list of values - "skip", "merge" and "mergewithoutmain"
         */
        @Override
        public String[] getValues() {
            return new String[] {"skip", "merge", "mergewithoutmain"};
        }
    }

    /**
     * Writes the directory entries from the first and the filenames
     * from the second list to the given writer, one entry per line.
     *
     * @param dirs a list of directories
     * @param files a list of files
     * @param writer the writer to write to
     * @since Ant 1.6.2
     */
    protected final void writeIndexLikeList(List<String> dirs, List<String> files,
                                            PrintWriter writer) {
        // JarIndex is sorting the directories by ascending order.
        // it has no value but cosmetic since it will be read into a
        // hashtable by the classloader, but we'll do so anyway.
        Collections.sort(dirs);
        Collections.sort(files);
        for (String dir : dirs) {
            // try to be smart, not to be fooled by a weird directory name
            dir = dir.replace('\\', '/');
            if (dir.startsWith("./")) {
                dir = dir.substring(2);
            }
            while (dir.startsWith("/")) {
                dir = dir.substring(1);
            }
            int pos = dir.lastIndexOf('/');
            if (pos != -1) {
                dir = dir.substring(0, pos);
            }

            // looks like nothing from META-INF should be added
            // and the check is not case insensitive.
            // see sun.misc.JarIndex
            // see also
            // https://bugs.openjdk.java.net/browse/JDK-4408526
            if (!indexMetaInf && dir.startsWith("META-INF")) {
                continue;
            }
            // name newline
            writer.println(dir);
        }

        files.forEach(writer::println);
    }

    /**
     * try to guess the name of the given file.
     *
     * <p>If this jar has a classpath attribute in its manifest, we
     * can assume that it will only require an index of jars listed
     * there.  try to find which classpath entry is most likely the
     * one the given file name points to.</p>
     *
     * <p>In the absence of a classpath attribute, assume the other
     * files will be placed inside the same directory as this jar and
     * use their basename.</p>
     *
     * <p>if there is a classpath and the given file doesn't match any
     * of its entries, return null.</p>
     *
     * @param fileName the name to look for
     * @param classpath the classpath to look in (may be null)
     * @return the matching entry, or null if the file is not found
     * @since Ant 1.6.2
     */
    protected static String findJarName(String fileName,
                                              String[] classpath) {
        if (classpath == null) {
            return new File(fileName).getName();
        }
        fileName = fileName.replace(File.separatorChar, '/');
        SortedMap<String, String> matches = new TreeMap<>(Comparator
            .<String> comparingInt(s -> s == null ? 0 : s.length()).reversed());

        for (String element : classpath) {
            String candidate = element;
            while (true) {
                if (fileName.endsWith(candidate)) {
                    matches.put(candidate, element);
                    break;
                }
                int slash = candidate.indexOf('/');
                if (slash < 0) {
                    break;
                }
                candidate = candidate.substring(slash + 1);
            }
        }
        return matches.isEmpty() ? null : matches.get(matches.firstKey());
    }

    /**
     * Grab lists of all root-level files and all directories
     * contained in the given archive.
     * @param file the zip file to examine
     * @param dirs where to place the directories found
     * @param files where to place the files found
     * @since Ant 1.7
     * @throws IOException on error
     */
    protected static void grabFilesAndDirs(String file, List<String> dirs,
                                                 List<String> files)
        throws IOException {
        try (org.apache.tools.zip.ZipFile zf = new org.apache.tools.zip.ZipFile(file, "utf-8")) {
            Set<String> dirSet = new HashSet<>();
            StreamUtils.enumerationAsStream(zf.getEntries()).forEach(ze -> {
                String name = ze.getName();
                if (ze.isDirectory()) {
                    dirSet.add(name);
                } else if (!name.contains("/")) {
                    files.add(name);
                } else {
                    // a file, not in the root
                    // since the jar may be one without directory
                    // entries, add the parent dir of this file as
                    // well.
                    dirSet.add(name.substring(0, name.lastIndexOf('/') + 1));
                }
            });
            dirs.addAll(dirSet);
        }
    }

    private Resource[][] grabManifests(ResourceCollection[] rcs) {
        Resource[][] manifests = new Resource[rcs.length][];
        for (int i = 0; i < rcs.length; i++) {
            Resource[][] resources;
            if (rcs[i] instanceof FileSet) {
                resources = grabResources(new FileSet[] {(FileSet) rcs[i]});
            } else {
                resources = grabNonFileSetResources(
                    new ResourceCollection[] {rcs[i]});
            }
            for (int j = 0; j < resources[0].length; j++) {
                String name = resources[0][j].getName().replace('\\', '/');
                if (rcs[i] instanceof ArchiveFileSet) {
                    ArchiveFileSet afs = (ArchiveFileSet) rcs[i];
                    if (!afs.getFullpath(getProject()).isEmpty()) {
                        name = afs.getFullpath(getProject());
                    } else if (!afs.getPrefix(getProject()).isEmpty()) {
                        String prefix = afs.getPrefix(getProject());
                        if (!prefix.endsWith("/") && !prefix.endsWith("\\")) {
                            prefix += "/";
                        }
                        name = prefix + name;
                    }
                }
                if (MANIFEST_NAME.equalsIgnoreCase(name)) {
                    manifests[i] = new Resource[] {resources[0][j]};
                    break;
                }
            }
            if (manifests[i] == null) {
                manifests[i] = new Resource[0];
            }
        }
        return manifests;
    }

    private Charset getManifestCharset() {
        if (manifestEncoding == null) {
            return Charset.defaultCharset();
        }
        try {
            return Charset.forName(manifestEncoding);
        } catch (IllegalArgumentException e) {
            throw new BuildException(
                "Unsupported encoding while reading manifest: "
                    + e.getMessage(),
                e);
        }
    }

    /** The strict enumerated type. */
    public static class StrictMode extends EnumeratedAttribute {
        /** Public no arg constructor. */
        public StrictMode() {
        }

        /**
         * Constructor with an arg.
         * @param value the enumerated value as a string.
         */
        public StrictMode(String value) {
            setValue(value);
        }

        /**
         * Get List of valid strings.
         * @return the list of values.
         */
        @Override
        public String[] getValues() {
            return new String[] {"fail", "warn", "ignore"};
        }

        /**
         * @return The log level according to the strict mode.
         */
        public int getLogLevel() {
            return "ignore".equals(getValue()) ? Project.MSG_VERBOSE : Project.MSG_WARN;
        }
    }

    /**
     * A mapper for the index based on the classpath attribute in the manifest.
     * See {@link #findJarName(String, String[])} for more details.
     *
     * @since Ant 1.10.9
     */
    private static class IndexJarsFilenameMapper implements FileNameMapper {

        private String[] classpath;

        IndexJarsFilenameMapper(String[] classpath) {
            this.classpath = classpath;
        }

        /**
         * Empty implementation.
         */
        @Override
        public void setFrom(String from) {
        }

        /**
         * Empty implementation.
         */
        @Override
        public void setTo(String to) {
        }

        @Override
        public String[] mapFileName(String sourceFileName) {
            String result = findJarName(sourceFileName, classpath);
            return result == null ? null : new String[] {result};
        }
    }
}
