/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.apache.tools.ant.types;

import java.io.File;
import java.util.Iterator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.zip.UnixStat;

/**
 * A ArchiveFileSet is a FileSet with extra attributes useful in the
 * context of archiving tasks.
 *
 * It includes a prefix attribute which is prepended to each entry in
 * the output archive file as well as a fullpath ttribute.  It also
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
        UnixStat.DIR_FLAG  | UnixStat.DEFAULT_DIR_PERM;

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

    /** Constructor for ArchiveFileSet */
    public ArchiveFileSet() {
        super();
    }

    /**
     * Constructor using a fileset arguement.
     * @param fileset the fileset to use
     */
    protected ArchiveFileSet(FileSet fileset) {
        super(fileset);
    }

    /**
     * Constructor using a archive fileset arguement.
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
    }

    /**
     * Set the directory for the fileset.
     * @param dir the directory for the fileset
     * @throws BuildException on error
     */
    public void setDir(File dir) throws BuildException {
        checkAttributesAllowed();
        if (src != null) {
            throw new BuildException("Cannot set both dir and src attributes");
        } else {
            super.setDir(dir);
            hasDir = true;
        }
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
        setSrcResource((Resource) a.iterator().next());
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
            throw new BuildException("Cannot set both dir and src attributes");
        }
        this.src = src;
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
     * Get the archive file from which entries will be extracted.
     * @return the archive in case the archive is a file, null otherwise.
     */
    public File getSrc() {
        if (src instanceof FileResource) {
            return ((FileResource) src).getFile();
        }
        return null;
    }

    /**
     * Prepend this prefix to the path for each archive entry.
     * Prevents both prefix and fullpath from being specified
     *
     * @param prefix The prefix to prepend to entries in the archive file.
     */
    public void setPrefix(String prefix) {
        checkArchiveAttributesAllowed();
        if (!prefix.equals("") && !fullpath.equals("")) {
            throw new BuildException("Cannot set both fullpath and prefix attributes");
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
        if (!prefix.equals("") && !fullpath.equals("")) {
            throw new BuildException("Cannot set both fullpath and prefix attributes");
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
        return fullpath;
    }

    /**
     * Creates a scanner for this type of archive.
     * @return the scanner.
     */
    protected abstract ArchiveScanner newArchiveScanner();

    /**
     * Return the DirectoryScanner associated with this FileSet.
     * If the ArchiveFileSet defines a source Archive file, then a ArchiveScanner
     * is returned instead.
     * @param p the project to use
     * @return a directory scanner
     */
    public DirectoryScanner getDirectoryScanner(Project p) {
        if (isReference()) {
            return getRef(p).getDirectoryScanner(p);
        }
        if (src == null) {
            return super.getDirectoryScanner(p);
        }
        if (!src.isExists()) {
            throw new BuildException("the archive doesn't exist");
        }
        if (src.isDirectory()) {
            throw new BuildException("the archive can't be a directory");
        }
        ArchiveScanner as = newArchiveScanner();
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
    public Iterator iterator() {
        if (isReference()) {
            return ((ResourceCollection) (getRef(getProject()))).iterator();
        }
        if (src == null) {
            return super.iterator();
        }
        ArchiveScanner as = (ArchiveScanner) getDirectoryScanner(getProject());
        return as.getResourceFiles();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return size of the collection as int.
     * @since Ant 1.7
     */
    public int size() {
        if (isReference()) {
            return ((ResourceCollection) (getRef(getProject()))).size();
        }
        if (src == null) {
            return super.size();
        }
        ArchiveScanner as = (ArchiveScanner) getDirectoryScanner(getProject());
        return as.getIncludedFilesCount();
    }

    /**
     * Indicate whether this ResourceCollection is composed entirely of
     * Resources accessible via local filesystem conventions.  If true,
     * all Resources returned from this ResourceCollection should be
     * instances of FileResource.
     * @return whether this is a filesystem-only resource collection.
     * @since Ant 1.7
     */
    public boolean isFilesystemOnly() {
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
        return fileMode;
    }

    /**
     * Whether the user has specified the mode explicitly.
     * @return true if it has been set
     */
    public boolean hasFileModeBeenSet() {
        if (isReference()) {
            return ((ArchiveFileSet) getRef(getProject())).hasFileModeBeenSet();
        }
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
        return dirMode;
    }

    /**
     * Whether the user has specified the mode explicitly.
     *
     * @return true if it has been set
     */
    public boolean hasDirModeBeenSet() {
        if (isReference()) {
            return ((ArchiveFileSet) getRef(getProject())).hasDirModeBeenSet();
        }
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
    public Object clone() {
        if (isReference()) {
            return ((ArchiveFileSet) getRef(getProject())).clone();
        } else {
            return super.clone();
        }
    }

    /**
     * for file based zipfilesets, return the same as for normal filesets
     * else just return the path of the zip
     * @return  for file based archivefilesets, included files as a list
     * of semicolon-separated filenames. else just the name of the zip.
     */
    public String toString() {
        if (hasDir && getProject() != null) {
            return super.toString();
        } else if (src != null) {
            return src.getName();
        } else {
            return null;
        }
    }

    /**
     * Return the prefix prepended to entries in the archive file.
     * @return the prefix.
     * @deprecated since 1.7.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Return the full pathname of the single entryZ in this fileset.
     * @return the full pathname.
     * @deprecated since 1.7.
     */
    public String getFullpath() {
        return fullpath;
    }

    /**
     * @return the file mode.
     * @deprecated since 1.7.
     */
    public int getFileMode() {
        return fileMode;
    }

    /**
     * @return the dir mode.
     * @deprecated since 1.7.
     */
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
}
