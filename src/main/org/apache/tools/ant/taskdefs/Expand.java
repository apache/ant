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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

/**
 * Unzip a file.
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 *           name="unzip"
 *           name="unjar"
 *           name="unwar"
 */
public class Expand extends Task {
    public static final String NATIVE_ENCODING = "native-encoding";

    /** Error message when more that one mapper is defined */
    public static final String ERROR_MULTIPLE_MAPPERS = "Cannot define more than one mapper";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private static final int BUFFER_SIZE = 1024;
    private File dest; //req
    private File source; // req
    private boolean overwrite = true;
    private Mapper mapperElement = null;
    private List<PatternSet> patternsets = new Vector<>();
    private Union resources = new Union();
    private boolean resourcesSpecified = false;
    private boolean failOnEmptyArchive = false;
    private boolean stripAbsolutePathSpec = true;
    private boolean scanForUnicodeExtraFields = true;
    private Boolean allowFilesToEscapeDest = null;

    private String encoding;

    /**
     * Creates an Expand instance and sets encoding to UTF-8.
     */
    public Expand() {
        this("UTF8");
    }

    /**
     * Creates an Expand instance and sets the given encoding.
     *
     * @param encoding String
     * @since Ant 1.9.5
     */
    protected Expand(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Whether try ing to expand an empty archive would be an error.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setFailOnEmptyArchive(boolean b) {
        failOnEmptyArchive = b;
    }

    /**
     * Whether try ing to expand an empty archive would be an error.
     *
     * @return boolean
     * @since Ant 1.8.0
     */
    public boolean getFailOnEmptyArchive() {
        return failOnEmptyArchive;
    }

    /**
     * Do the work.
     *
     * @exception BuildException Thrown in unrecoverable error.
     */
    @Override
    public void execute() throws BuildException {
        if ("expand".equals(getTaskType())) {
            log("!! expand is deprecated. Use unzip instead. !!");
        }

        if (source == null && !resourcesSpecified) {
            throw new BuildException(
                "src attribute and/or resources must be specified");
        }

        if (dest == null) {
            throw new BuildException(
                "Dest attribute must be specified");
        }

        if (dest.exists() && !dest.isDirectory()) {
            throw new BuildException("Dest must be a directory.", getLocation());
        }

        if (source != null) {
            if (source.isDirectory()) {
                throw new BuildException("Src must not be a directory."
                    + " Use nested filesets instead.", getLocation());
            }
            if (!source.exists()) {
                throw new BuildException("src '" + source + "' doesn't exist.");
            }
            if (!source.canRead()) {
                throw new BuildException("src '" + source + "' cannot be read.");
            }
            expandFile(FILE_UTILS, source, dest);
        }
        for (Resource r : resources) {
            if (!r.isExists()) {
                log("Skipping '" + r.getName() + "' because it doesn't exist.");
                continue;
            }

            FileProvider fp = r.as(FileProvider.class);
            if (fp != null) {
                expandFile(FILE_UTILS, fp.getFile(), dest);
            } else {
                expandResource(r, dest);
            }
        }
    }

    /**
     * This method is to be overridden by extending unarchival tasks.
     *
     * @param fileUtils the fileUtils
     * @param srcF      the source file
     * @param dir       the destination directory
     */
    protected void expandFile(FileUtils fileUtils, File srcF, File dir) {
        log("Expanding: " + srcF + " into " + dir, Project.MSG_INFO);
        FileNameMapper mapper = getMapper();
        if (!srcF.exists()) {
            throw new BuildException("Unable to expand "
                    + srcF
                    + " as the file does not exist",
                    getLocation());
        }
        try (ZipFile zf = new ZipFile(srcF, encoding, scanForUnicodeExtraFields)) {
            boolean empty = true;
            Enumeration<ZipEntry> entries = zf.getEntries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                empty = false;
                InputStream is = null;
                log("extracting " + ze.getName(), Project.MSG_DEBUG);
                try {
                    extractFile(fileUtils, srcF, dir,
                                is = zf.getInputStream(ze), //NOSONAR
                                ze.getName(), new Date(ze.getTime()),
                                ze.isDirectory(), mapper);
                } finally {
                    FileUtils.close(is);
                }
            }
            if (empty && getFailOnEmptyArchive()) {
                throw new BuildException("archive '%s' is empty", srcF);
            }
            log("expand complete", Project.MSG_VERBOSE);
        } catch (IOException ioe) {
            throw new BuildException(
                "Error while expanding " + srcF.getPath()
                + "\n" + ioe.toString(),
                ioe);
        }
    }

    /**
     * This method is to be overridden by extending unarchival tasks.
     *
     * @param srcR      the source resource
     * @param dir       the destination directory
     */
    protected void expandResource(Resource srcR, File dir) {
        throw new BuildException(
            "only filesystem based resources are supported by this task.");
    }

    /**
     * get a mapper for a file
     * @return a filenamemapper for a file
     */
    protected FileNameMapper getMapper() {
        if (mapperElement != null) {
            return mapperElement.getImplementation();
        }
        return new IdentityMapper();
    }

    // CheckStyle:ParameterNumberCheck OFF - bc
    /**
     * extract a file to a directory
     * @param fileUtils             a fileUtils object
     * @param srcF                  the source file
     * @param dir                   the destination directory
     * @param compressedInputStream the input stream
     * @param entryName             the name of the entry
     * @param entryDate             the date of the entry
     * @param isDirectory           if this is true the entry is a directory
     * @param mapper                the filename mapper to use
     * @throws IOException on error
     */
    protected void extractFile(FileUtils fileUtils, File srcF, File dir,
                               InputStream compressedInputStream,
                               String entryName, Date entryDate,
                               boolean isDirectory, FileNameMapper mapper)
                               throws IOException {

        final boolean entryNameStartsWithPathSpec = !entryName.isEmpty()
            && (entryName.charAt(0) == File.separatorChar
                || entryName.charAt(0) == '/'
                || entryName.charAt(0) == '\\');
        if (stripAbsolutePathSpec && entryNameStartsWithPathSpec) {
            log("stripped absolute path spec from " + entryName,
                Project.MSG_VERBOSE);
            entryName = entryName.substring(1);
        }
        boolean allowedOutsideOfDest = Boolean.TRUE == getAllowFilesToEscapeDest()
            || null == getAllowFilesToEscapeDest() && !stripAbsolutePathSpec && entryNameStartsWithPathSpec;

        if (patternsets != null && !patternsets.isEmpty()) {
            String name = entryName.replace('/', File.separatorChar)
                .replace('\\', File.separatorChar);

            Set<String> includePatterns = new HashSet<>();
            Set<String> excludePatterns = new HashSet<>();
            for (PatternSet p : patternsets) {
                String[] incls = p.getIncludePatterns(getProject());
                if (incls == null || incls.length == 0) {
                    // no include pattern implicitly means includes="**"
                    incls = new String[]{"**"};
                }

                for (String incl : incls) {
                    String pattern = incl.replace('/', File.separatorChar)
                            .replace('\\', File.separatorChar);
                    if (pattern.endsWith(File.separator)) {
                        pattern += "**";
                    }
                    includePatterns.add(pattern);
                }

                String[] excls = p.getExcludePatterns(getProject());
                if (excls != null) {
                    for (String excl : excls) {
                        String pattern = excl.replace('/', File.separatorChar)
                                .replace('\\', File.separatorChar);
                        if (pattern.endsWith(File.separator)) {
                            pattern += "**";
                        }
                        excludePatterns.add(pattern);
                    }
                }
            }

            boolean included = false;
            for (String pattern : includePatterns) {
                if (SelectorUtils.matchPath(pattern, name)) {
                    included = true;
                    break;
                }
            }

            for (String pattern : excludePatterns) {
                if (SelectorUtils.matchPath(pattern, name)) {
                    included = false;
                    break;
                }
            }

            if (!included) {
                // Do not process this file
                log("skipping " + entryName
                    + " as it is excluded or not included.",
                    Project.MSG_VERBOSE);
                return;
            }
        }
        String[] mappedNames = mapper.mapFileName(entryName);
        if (mappedNames == null || mappedNames.length == 0) {
            mappedNames = new String[] {entryName};
        }
        File f = fileUtils.resolveFile(dir, mappedNames[0]);
        if (!allowedOutsideOfDest && !fileUtils.isLeadingPath(dir, f, true)) {
            log("skipping " + entryName + " as its target " + f.getCanonicalPath()
                + " is outside of " + dir.getCanonicalPath() + ".", Project.MSG_VERBOSE);
                return;
        }

        try {
            if (!overwrite && f.exists()
                && f.lastModified() >= entryDate.getTime()) {
                log("Skipping " + f + " as it is up-to-date",
                    Project.MSG_DEBUG);
                return;
            }

            log("expanding " + entryName + " to " + f,
                Project.MSG_VERBOSE);
            // create intermediary directories - sometimes zip don't add them
            File dirF = f.getParentFile();
            if (dirF != null) {
                dirF.mkdirs();
            }

            if (isDirectory) {
                f.mkdirs();
            } else {
                byte[] buffer = new byte[BUFFER_SIZE];
                try (OutputStream fos = Files.newOutputStream(f.toPath())) {
                    int length;
                    while ((length = compressedInputStream.read(buffer)) >= 0) {
                        fos.write(buffer, 0, length);
                    }
                }
            }

            fileUtils.setFileLastModified(f, entryDate.getTime());
        } catch (FileNotFoundException ex) {
            log("Unable to expand to file " + f.getPath(),
                    ex,
                    Project.MSG_WARN);
        }

    }
    // CheckStyle:ParameterNumberCheck ON

    /**
     * Set the destination directory. File will be unzipped into the
     * destination directory.
     *
     * @param d Path to the directory.
     */
    public void setDest(File d) {
        this.dest = d;
    }

    /**
     * Set the path to zip-file.
     *
     * @param s Path to zip-file.
     */
    public void setSrc(File s) {
        this.source = s;
    }

    /**
     * Should we overwrite files in dest, even if they are newer than
     * the corresponding entries in the archive?
     * @param b a <code>boolean</code> value
     */
    public void setOverwrite(boolean b) {
        overwrite = b;
    }

    /**
     * Add a patternset.
     * @param set a pattern set
     */
    public void addPatternset(PatternSet set) {
        patternsets.add(set);
    }

    /**
     * Add a fileset
     * @param set a file set
     */
    public void addFileset(FileSet set) {
        add(set);
    }

    /**
     * Add a resource collection.
     * @param rc a resource collection.
     * @since Ant 1.7
     */
    public void add(ResourceCollection rc) {
        resourcesSpecified = true;
        resources.add(rc);
    }

    /**
     * Defines the mapper to map source entries to destination files.
     * @return a mapper to be configured
     * @exception BuildException if more than one mapper is defined
     * @since Ant1.7
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException(ERROR_MULTIPLE_MAPPERS,
                                     getLocation());
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    /**
     * A nested filenamemapper
     * @param fileNameMapper the mapper to add
     * @since Ant 1.6.3
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }


    /**
     * Sets the encoding to assume for file names and comments.
     *
     * <p>Set to <code>native-encoding</code> if you want your
     * platform's native encoding, defaults to UTF8.</p>
     * @param encoding the name of the character encoding
     * @since Ant 1.6
     */
    public void setEncoding(String encoding) {
        internalSetEncoding(encoding);
    }

    /**
     * Supports grand-children that want to support the attribute
     * where the child-class doesn't (i.e. Unzip in the compress
     * Antlib).
     *
     * @param encoding String
     * @since Ant 1.8.0
     */
    protected void internalSetEncoding(String encoding) {
        if (NATIVE_ENCODING.equals(encoding)) {
            encoding = null;
        }
        this.encoding = encoding;
    }

    /**
     * @return String
     * @since Ant 1.8.0
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Whether leading path separators should be stripped.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setStripAbsolutePathSpec(boolean b) {
        stripAbsolutePathSpec = b;
    }

    /**
     * Whether unicode extra fields will be used if present.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setScanForUnicodeExtraFields(boolean b) {
        internalSetScanForUnicodeExtraFields(b);
    }

    /**
     * Supports grand-children that want to support the attribute
     * where the child-class doesn't (i.e. Unzip in the compress
     * Antlib).
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    protected void internalSetScanForUnicodeExtraFields(boolean b) {
        scanForUnicodeExtraFields = b;
    }

    /**
     * @return boolean
     * @since Ant 1.8.0
     */
    public boolean getScanForUnicodeExtraFields() {
        return scanForUnicodeExtraFields;
    }

    /**
     * Whether to allow the extracted file or directory to be outside of the dest directory.
     *
     * @param b the flag
     * @since Ant 1.10.4
     */
    public void setAllowFilesToEscapeDest(boolean b) {
        allowFilesToEscapeDest = b;
    }

    /**
     * Whether to allow the extracted file or directory to be outside of the dest directory.
     *
     * @return {@code null} if the flag hasn't been set explicitly,
     * otherwise the value set by the user.
     * @since Ant 1.10.4
     */
    public Boolean getAllowFilesToEscapeDest() {
        return allowFilesToEscapeDest;
    }

}
