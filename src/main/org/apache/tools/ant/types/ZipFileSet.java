/*
 * Copyright  2001-2004 The Apache Software Foundation
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
package org.apache.tools.ant.types;

import java.io.File;
import java.util.Stack;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.zip.UnixStat;

/**
 * A ZipFileSet is a FileSet with extra attributes useful in the context of
 * Zip/Jar tasks.
 *
 * A ZipFileSet extends FileSets with the ability to extract a subset of the
 * entries of a Zip file for inclusion in another Zip file.  It also includes
 * a prefix attribute which is prepended to each entry in the output Zip file.
 *
 * Since ant 1.6 ZipFileSet can be defined with an id and referenced in packaging tasks
 *
 */
public class ZipFileSet extends FileSet {

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

    private File srcFile          = null;
    private String prefix         = "";
    private String fullpath       = "";
    private boolean hasDir        = false;
    private int fileMode          = DEFAULT_FILE_MODE;
    private int dirMode           = DEFAULT_DIR_MODE;

    private boolean fileModeHasBeenSet = false;
    private boolean dirModeHasBeenSet  = false;

    public ZipFileSet() {
        super();
    }

    protected ZipFileSet(FileSet fileset) {
        super(fileset);
    }

    protected ZipFileSet(ZipFileSet fileset) {
        super(fileset);
        srcFile = fileset.srcFile;
        prefix = fileset.prefix;
        fullpath = fileset.fullpath;
        hasDir = fileset.hasDir;
        fileMode = fileset.fileMode;
        dirMode = fileset.dirMode;
        fileModeHasBeenSet = fileset.fileModeHasBeenSet;
        dirModeHasBeenSet = fileset.dirModeHasBeenSet;
    }

    /**
     * Set the directory for the fileset.  Prevents both "dir" and "src"
     * from being specified.
     */
    public void setDir(File dir) throws BuildException {
        if (isReference()) {
             throw tooManyAttributes();
         }
        if (srcFile != null) {
            throw new BuildException("Cannot set both dir and src attributes");
        } else {
            super.setDir(dir);
            hasDir = true;
        }
    }

    /**
     * Set the source Zip file for the zipfileset.  Prevents both
     * "dir" and "src" from being specified.
     *
     * @param srcFile The zip file from which to extract entries.
     */
    public void setSrc(File srcFile) {
        if (isReference()) {
             throw tooManyAttributes();
         }
        if (hasDir) {
            throw new BuildException("Cannot set both dir and src attributes");
        }
        this.srcFile = srcFile;
    }

    /**
     * Get the zip file from which entries will be extracted.
     * References are not followed, since it is not possible
     * to have a reference to a ZipFileSet, only to a FileSet.
     */
    public File getSrc(Project p) {
        if (isReference()) {
            return ((ZipFileSet) getRef(p)).getSrc(p);
        }
        return srcFile;
    }

    /**
     * Prepend this prefix to the path for each zip entry.
     * Prevents both prefix and fullpath from being specified
     *
     * @param prefix The prefix to prepend to entries in the zip file.
     */
    public void setPrefix(String prefix) {
        if (!prefix.equals("") && !fullpath.equals("")) {
            throw new BuildException("Cannot set both fullpath and prefix attributes");
        }
        this.prefix = prefix;
    }

    /**
     * Return the prefix prepended to entries in the zip file.
     */
    public String getPrefix(Project p) {
        if (isReference()) {
            return ((ZipFileSet) getRef(p)).getPrefix(p);
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
        if (!prefix.equals("") && !fullpath.equals("")) {
            throw new BuildException("Cannot set both fullpath and prefix attributes");
        }
        this.fullpath = fullpath;
    }

    /**
     * Return the full pathname of the single entry in this fileset.
     */
    public String getFullpath(Project p) {
        if (isReference()) {
            return ((ZipFileSet) getRef(p)).getFullpath(p);
        }
        return fullpath;
    }

    /**
     * Return the DirectoryScanner associated with this FileSet.
     * If the ZipFileSet defines a source Zip file, then a ZipScanner
     * is returned instead.
     */
    public DirectoryScanner getDirectoryScanner(Project p) {
        if (isReference()) {
            return getRef(p).getDirectoryScanner(p);
        }
        if (srcFile != null) {
            ZipScanner zs = new ZipScanner();
            zs.setSrc(srcFile);
            super.setDir(p.getBaseDir());
            setupDirectoryScanner(zs, p);
            zs.init();
            return zs;
        } else {
            return super.getDirectoryScanner(p);
        }
    }

    /**
     * A 3 digit octal string, specify the user, group and
     * other modes in the standard Unix fashion;
     * optional, default=0644
     *
     * @since Ant 1.5.2
     */
    public void setFileMode(String octalString) {
        fileModeHasBeenSet = true;
        this.fileMode =
            UnixStat.FILE_FLAG | Integer.parseInt(octalString, 8);
    }

    /**
     * @since Ant 1.5.2
     */
    public int getFileMode(Project p) {
        if (isReference()) {
            return ((ZipFileSet) getRef(p)).getFileMode(p);
        }
        return fileMode;
    }

    /**
     * Whether the user has specified the mode explicitly.
     *
     * @since Ant 1.6
     */
    public boolean hasFileModeBeenSet() {
        if (isReference()) {
            return ((ZipFileSet) getRef(getProject())).hasFileModeBeenSet();
        }
        return fileModeHasBeenSet;
    }

    /**
     * A 3 digit octal string, specify the user, group and
     * other modes in the standard Unix fashion;
     * optional, default=0755
     *
     * @since Ant 1.5.2
     */
    public void setDirMode(String octalString) {
        dirModeHasBeenSet = true;
        this.dirMode =
            UnixStat.DIR_FLAG | Integer.parseInt(octalString, 8);
    }

    /**
     * @since Ant 1.5.2
     */
    public int getDirMode(Project p) {
        if (isReference()) {
            return ((ZipFileSet) getRef(p)).getDirMode(p);
        }
        return dirMode;
    }

    /**
     * Whether the user has specified the mode explicitly.
     *
     * @since Ant 1.6
     */
    public boolean hasDirModeBeenSet() {
        if (isReference()) {
            return ((ZipFileSet) getRef(getProject())).hasDirModeBeenSet();
        }
        return dirModeHasBeenSet;
    }

    /**
     * A ZipFileset accepts another ZipFileSet or a FileSet as reference
     * FileSets are often used by the war task for the lib attribute
     */
    protected AbstractFileSet getRef(Project p) {
        if (!isChecked()) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, p);
        }
        Object o = getRefid().getReferencedObject(p);
        if (o instanceof ZipFileSet) {
            return (AbstractFileSet) o;
        } else if (o instanceof FileSet) {
            ZipFileSet zfs = new ZipFileSet((FileSet) o);
            zfs.setPrefix(prefix);
            zfs.setFullpath(fullpath);
            zfs.fileModeHasBeenSet = fileModeHasBeenSet;
            zfs.fileMode = fileMode;
            zfs.dirModeHasBeenSet = dirModeHasBeenSet;
            zfs.dirMode = dirMode;
            return zfs;
        } else {
            String msg = getRefid().getRefId() + " doesn\'t denote a zipfileset or a fileset";
            throw new BuildException(msg);
        }
    }
    /**
     * Return a ZipFileSet that has the same properties
     * as this one.
     * @since Ant 1.6
     */
    public Object clone() {
        if (isReference()) {
            return ((ZipFileSet) getRef(getProject())).clone();
        } else {
            return super.clone();
        }
    }
}
