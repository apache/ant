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
package org.apache.tools.ant.types;

import java.io.File;
import java.util.Iterator;
import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.zip.UnixStat;

/**
 * A ArchiveFileSet is a FileSet with extra attributes useful in the
 * context of archiving tasks.
 *
 * It includes a prefix attribute which is prepended to each entry in
 * the output archive file as well as a fullpath attribute.  It also
 * supports Unix file permissions for files and directories.
 *
 * @since Ant 1.7
 */
public abstract class ArchiveFileSet extends FileSet {

    private static final int BASE_OCTAL = 8;

    /**
     * Default value for the dirmode attribute.
     *
     * @since Ant 1.5.2
     */
    public static final int DEFAULT_DIR_MODE =
        UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM;

    /**
     * Default value for the filemode attribute.
     *
     * @since Ant 1.5.2
     */
    public static final int DEFAULT_FILE_MODE =
        UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM;

    private Resource src          = null;
    private String prefix         = "";
    private String fullpath       = "";
    private boolean hasDir        = false;
    private int fileMode          = DEFAULT_FILE_MODE;
    private int dirMode           = DEFAULT_DIR_MODE;

    private boolean fileModeHasBeenSet = false;
    private boolean dirModeHasBeenSet  = false;
    private static final String ERROR_DIR_AND_SRC_ATTRIBUTES = "Cannot set both dir and src attributes";
    private static final String ERROR_PATH_AND_PREFIX = "Cannot set both fullpath and prefix attributes";

    private boolean errorOnMissingArchive = true;

    private String encoding = null;

    /** Constructor for ArchiveFileSet */
    public ArchiveFileSet() {
        super();
    }

    /**
     * Constructor using a fileset argument.
     * @param fileset the fileset to use
     */
    protected ArchiveFileSet(FileSet fileset) {
        super(fileset);
    }

    /**
     * Constructor using a archive fileset argument.
     * @param fileset the archivefileset to use
     */
    protected ArchiveFileSet(ArchiveFileSet fileset) {
        super(fileset);
        src = fileset.src;
        prefix = fileset.prefix;
        fullpath = fileset.fullpath;
        hasDir = fileset.hasDir;
        fileMode = fileset.fileMode;
        dirMode = fileset.dirMode;
        fileModeHasBeenSet = fileset.fileModeHasBeenSet;
        dirModeHasBeenSet = fileset.dirModeHasBeenSet;
        errorOnMissingArchive = fileset.errorOnMissingArchive;
        encoding = fileset.encoding;
    }

    /**
     * Set the directory for the fileset.
     * @param dir the directory for the fileset
     * @throws BuildException on error
     */
    @Override
    public void setDir(File dir) throws BuildException {
        checkAttributesAllowed();
        if (src != null) {
            throw new BuildException(ERROR_DIR_AND_SRC_ATTRIBUTES);
        }
        super.setDir(dir);
        hasDir = true;
    }

    /**
     * Set the source Archive file for the archivefileset.  Prevents both
     * "dir" and "src" from being specified.
     * @param a the archive as a single element Resource collection.
     */
    public void addConfigured(ResourceCollection a) {
        checkChildrenAllowed();
        if (a.size() != 1) {
            throw new BuildException("only single argument resource collections"
                                     + " are supported as archives");
        }
        setSrcResource(a.iterator().next());
    }

    /**
     * Set the source Archive file for the archivefileset.  Prevents both
     * "dir" and "src" from being specified.
     *
     * @param srcFile The archive from which to extract entries.
     */
    public void setSrc(File srcFile) {
        setSrcResource(new FileResource(srcFile));
    }

    /**
     * Set the source Archive file for the archivefileset.  Prevents both
     * "dir" and "src" from being specified.
     *
     * @param src The archive from which to extract entries.
     */
    public void setSrcResource(Resource src) {
        checkArchiveAttributesAllowed();
        if (hasDir) {
            throw new BuildException(ERROR_DIR_AND_SRC_ATTRIBUTES);
        }
        this.src = src;
        setChecked(false);
    }

    /**
     * Get the archive from which entries will be extracted.
     * @param p the project to use
     * @return the source file
     */
    public File getSrc(Project p) {
        if (isReference()) {
            return ((ArchiveFileSet) getRef(p)).getSrc(p);
        }
        return getSrc();
    }

    /**
     * Sets whether an error is thrown if an archive does not exist.
     *
     * @param errorOnMissingArchive true if missing archives cause errors,
     *                        false if not.
     * @since Ant 1.8.0
     */
    public void setErrorOnMissingArchive(boolean errorOnMissingArchive) {
        checkAttributesAllowed();
        this.errorOnMissingArchive = errorOnMissingArchive;
    }

    /**
     * Get the archive file from which entries will be extracted.
     * @return the archive in case the archive is a file, null otherwise.
     */
    public File getSrc() {
        if (isReference()) {
            return getCheckedRef(ArchiveFileSet.class).getSrc();
        }
        dieOnCircularReference();
        if (src == null) {
            return null;
        }
        return src.asOptional(FileProvider.class).map(FileProvider::getFile).orElse(null);
    }

    /**
     * Performs the check for circular references and returns the
     * referenced object.
     * This method must be overridden together with
     * {@link AbstractFileSet#getRef(Project) getRef(Project)}
     * providing implementations containing the special support
     * for FileSet references, which can be handled by all ArchiveFileSets.
     * NB! This method cannot be implemented in AbstractFileSet in order to allow
     * FileSet and DirSet to implement it as a private method.
     * @return the dereferenced object.
     * @throws BuildException if the reference is invalid (circular ref, wrong class, etc).
     */
    protected AbstractFileSet getRef() {
        return getCheckedRef(AbstractFileSet.class);
    }

    /**
     * Prepend this prefix to the path for each archive entry.
     * Prevents both prefix and fullpath from being specified
     *
     * @param prefix The prefix to prepend to entries in the archive file.
     */
    public void setPrefix(String prefix) {
        checkArchiveAttributesAllowed();
        if (!prefix.isEmpty() && !fullpath.isEmpty()) {
            throw new BuildException(ERROR_PATH_AND_PREFIX);
        }
        this.prefix = prefix;
    }

    /**
     * Return the prefix prepended to entries in the archive file.
     * @param p the project to use
     * @return the prefix
     */
    public String getPrefix(Project p) {
        if (isReference()) {
            return ((ArchiveFileSet) getRef(p)).getPrefix(p);
        }
        dieOnCircularReference(p);
        return prefix;
    }

    /**
     * Set the full pathname of the single entry in this fileset.
     * Prevents both prefix and fullpath from being specified
     *
     * @param fullpath the full pathname of the single entry in this fileset.
     */
    public void setFullpath(String fullpath) {
        checkArchiveAttributesAllowed();
        if (!prefix.isEmpty() && !fullpath.isEmpty()) {
            throw new BuildException(ERROR_PATH_AND_PREFIX);
        }
        this.fullpath = fullpath;
    }

    /**
     * Return the full pathname of the single entry in this fileset.
     * @param p the project to use
     * @return the full path
     */
    public String getFullpath(Project p) {
        if (isReference()) {
            return ((ArchiveFileSet) getRef(p)).getFullpath(p);
        }
        dieOnCircularReference(p);
        return fullpath;
    }

    /**
     * Set the encoding used for this ZipFileSet.
     * @param enc encoding as String.
     * @since Ant 1.9.5
     */
    public void setEncoding(String enc) {
        checkAttributesAllowed();
        this.encoding = enc;
    }

    /**
     * Get the encoding used for this ZipFileSet.
     * @return String encoding.
     * @since Ant 1.9.5
     */
    public String getEncoding() {
        if (isReference()) {
            AbstractFileSet ref = getRef();
            return ref instanceof ArchiveFileSet ? ((ArchiveFileSet) ref).getEncoding() : null;
        }
        return encoding;
    }

    /**
     * Creates a scanner for this type of archive.
     * @return the scanner.
     */
    protected abstract ArchiveScanner newArchiveScanner();

    /**
     * Return the DirectoryScanner associated with this FileSet.
     * If the ArchiveFileSet defines a source Archive file, then an ArchiveScanner
     * is returned instead.
     * @param p the project to use
     * @return a directory scanner
     */
    @Override
    public DirectoryScanner getDirectoryScanner(Project p) {
        if (isReference()) {
            return getRef(p).getDirectoryScanner(p);
        }
        dieOnCircularReference();
        if (src == null) {
            return super.getDirectoryScanner(p);
        }
        if (!src.isExists() && errorOnMissingArchive) {
            throw new BuildException(
                "The archive " + src.getName() + " doesn't exist");
        }
        if (src.isDirectory()) {
            throw new BuildException("The archive " + src.getName()
                                     + " can't be a directory");
        }
        ArchiveScanner as = newArchiveScanner();
        as.setErrorOnMissingArchive(errorOnMissingArchive);
        as.setSrc(src);
        super.setDir(p.getBaseDir());
        setupDirectoryScanner(as, p);
        as.init();
        return as;
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return Iterator of Resources.
     * @since Ant 1.7
     */
    @Override
    public Iterator<Resource> iterator() {
        if (isReference()) {
            return ((ResourceCollection) getRef()).iterator();
        }
        if (src == null) {
            return super.iterator();
        }
        return ((ArchiveScanner) getDirectoryScanner()).getResourceFiles(getProject());
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return size of the collection as int.
     * @since Ant 1.7
     */
    @Override
    public int size() {
        if (isReference()) {
            return ((ResourceCollection) getRef()).size();
        }
        if (src == null) {
            return super.size();
        }
        return getDirectoryScanner().getIncludedFilesCount();
    }

    /**
     * Indicate whether this ResourceCollection is composed entirely of
     * Resources accessible via local filesystem conventions.  If true,
     * all Resources returned from this ResourceCollection should be
     * instances of FileResource.
     * @return whether this is a filesystem-only resource collection.
     * @since Ant 1.7
     */
    @Override
    public boolean isFilesystemOnly() {
        if (isReference()) {
            return ((ArchiveFileSet) getRef()).isFilesystemOnly();
        }
        dieOnCircularReference();
        return src == null;
    }

    /**
     * A 3 digit octal string, specify the user, group and
     * other modes in the standard Unix fashion;
     * optional, default=0644
     * @param octalString a <code>String</code> value
     */
    public void setFileMode(String octalString) {
        checkArchiveAttributesAllowed();
        integerSetFileMode(Integer.parseInt(octalString, BASE_OCTAL));
    }

    /**
     * specify the user, group and
     * other modes in the standard Unix fashion;
     * optional, default=0644
     *
     * <p>We use the strange name so this method doesn't appear in
     * IntrospectionHelpers list of attribute setters.</p>
     * @param mode a <code>int</code> value
     * @since Ant 1.7
     */
    public void integerSetFileMode(int mode) {
        fileModeHasBeenSet = true;
        this.fileMode = UnixStat.FILE_FLAG | mode;
    }

    /**
     * Get the mode of the archive fileset
     * @param p the project to use
     * @return the mode
     */
    public int getFileMode(Project p) {
        if (isReference()) {
            return ((ArchiveFileSet) getRef(p)).getFileMode(p);
        }
        dieOnCircularReference();
        return fileMode;
    }

    /**
     * Whether the user has specified the mode explicitly.
     * @return true if it has been set
     */
    public boolean hasFileModeBeenSet() {
        if (isReference()) {
            return ((ArchiveFileSet) getRef()).hasFileModeBeenSet();
        }
        dieOnCircularReference();
        return fileModeHasBeenSet;
    }

    /**
     * A 3 digit octal string, specify the user, group and
     * other modes in the standard Unix fashion;
     * optional, default=0755
     * @param octalString a <code>String</code> value
     */
    public void setDirMode(String octalString) {
        checkArchiveAttributesAllowed();
        integerSetDirMode(Integer.parseInt(octalString, BASE_OCTAL));
    }

    /**
     * specify the user, group and
     * other modes in the standard Unix fashion;
     * optional, default=0755
     * <p>We use the strange name so this method doesn't appear in
     * IntrospectionHelpers list of attribute setters.</p>
     * @param mode a <code>int</code> value
     * @since Ant 1.7
     */
    public void integerSetDirMode(int mode) {
        dirModeHasBeenSet = true;
        this.dirMode = UnixStat.DIR_FLAG | mode;
    }

    /**
     * Get the dir mode of the archive fileset
     * @param p the project to use
     * @return the mode
     */
    public int getDirMode(Project p) {
        if (isReference()) {
            return ((ArchiveFileSet) getRef(p)).getDirMode(p);
        }
        dieOnCircularReference();
        return dirMode;
    }

    /**
     * Whether the user has specified the mode explicitly.
     *
     * @return true if it has been set
     */
    public boolean hasDirModeBeenSet() {
        if (isReference()) {
            return ((ArchiveFileSet) getRef()).hasDirModeBeenSet();
        }
        dieOnCircularReference();
        return dirModeHasBeenSet;
    }

    /**
     * A ArchiveFileset accepts another ArchiveFileSet or a FileSet as reference
     * FileSets are often used by the war task for the lib attribute
     * @param zfs the project to use
     */
    protected void configureFileSet(ArchiveFileSet zfs) {
        zfs.setPrefix(prefix);
        zfs.setFullpath(fullpath);
        zfs.fileModeHasBeenSet = fileModeHasBeenSet;
        zfs.fileMode = fileMode;
        zfs.dirModeHasBeenSet = dirModeHasBeenSet;
        zfs.dirMode = dirMode;
    }

    /**
     * Return a ArchiveFileSet that has the same properties
     * as this one.
     * @return the cloned archiveFileSet
     * @since Ant 1.6
     */
    @Override
    public Object clone() {
        if (isReference()) {
            return getCheckedRef(ArchiveFileSet.class).clone();
        }
        return super.clone();
    }

    /**
     * For file-based archivefilesets, return the same as for normal filesets;
     * else just return the path of the zip.
     * @return for file based archivefilesets, included files as a list
     * of semicolon-separated filenames. else just the name of the zip.
     */
    @Override
    public String toString() {
        if (hasDir && getProject() != null) {
            return super.toString();
        }
        return src == null ? null : src.getName();
    }

    /**
     * Return the prefix prepended to entries in the archive file.
     * @return the prefix.
     * @deprecated since 1.7.
     */
    @Deprecated
    public String getPrefix() {
        return prefix;
    }

    /**
     * Return the full pathname of the single entryZ in this fileset.
     * @return the full pathname.
     * @deprecated since 1.7.
     */
    @Deprecated
    public String getFullpath() {
        return fullpath;
    }

    /**
     * @return the file mode.
     * @deprecated since 1.7.
     */
    @Deprecated
    public int getFileMode() {
        return fileMode;
    }

    /**
     * @return the dir mode.
     * @deprecated since 1.7.
     */
    @Deprecated
    public int getDirMode() {
        return dirMode;
    }

    /**
     * A check attributes for archiveFileSet.
     * If there is a reference, and
     * it is a ArchiveFileSet, the archive fileset attributes
     * cannot be used.
     * (Note, we can only see if the reference is an archive
     * fileset if the project has been set).
     */
    private void checkArchiveAttributesAllowed() {
        if (getProject() == null
            || (isReference()
                && (getRefid().getReferencedObject(
                        getProject())
                    instanceof ArchiveFileSet))) {
            checkAttributesAllowed();
        }
    }

    @Override
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }

        // takes care of nested selectors
        super.dieOnCircularReference(stk, p);

        if (!isReference()) {
            if (src != null) {
                pushAndInvokeCircularReferenceCheck(src, stk, p);
            }
            setChecked(true);
        }
    }
}
