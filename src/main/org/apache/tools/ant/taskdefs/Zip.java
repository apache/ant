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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.ZipScanner;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.ResourceUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

/**
 * Create a Zip file.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author Stefan Bodewig
 * @author <a href="mailto:levylambert@tiscali-dsl.de">Antoine Levy-Lambert</a>
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */
public class Zip extends MatchingTask {

    protected File zipFile;
    // use to scan own archive
    private ZipScanner zs;
    private File baseDir;
    protected Hashtable entries = new Hashtable();
    private Vector groupfilesets = new Vector();
    private Vector filesetsFromGroupfilesets = new Vector();
    protected String duplicate = "add";
    private boolean doCompress = true;
    private boolean doUpdate = false;
    // shadow of the above if the value is altered in execute
    private boolean savedDoUpdate = false;
    private boolean doFilesonly = false;
    protected String archiveType = "zip";

    // For directories:
    private static final long EMPTY_CRC = new CRC32 ().getValue ();
    protected String emptyBehavior = "skip";
    private Vector filesets = new Vector ();
    protected Hashtable addedDirs = new Hashtable();
    private Vector addedFiles = new Vector();

    private static FileUtils fileUtils = FileUtils.newFileUtils();

    /** 
     * true when we are adding new files into the Zip file, as opposed
     * to adding back the unchanged files 
     */
    private boolean addingNewFiles = false;

    /**
     * Encoding to use for filenames, defaults to the platform's
     * default encoding.
     */
    private String encoding;

    /**
     * This is the name/location of where to
     * create the .zip file.
     *
     * @deprecated Use setDestFile(File) instead.
     * @ant.attribute ignore="true"
     */
    public void setZipfile(File zipFile) {
        setDestFile(zipFile);
    }

    /**
     * This is the name/location of where to
     * create the file.
     * @since Ant 1.5
     * @deprecated Use setDestFile(File) instead
     * @ant.attribute ignore="true"
     */
    public void setFile(File file) {
        setDestFile(file);
    }


    /**
     * The file to create; required.
     * @since Ant 1.5
     * @param destFile The new destination File
     */
    public void setDestFile(File destFile) {
       this.zipFile = destFile;
    }

    /**
     * The file to create.
     * @since Ant 1.5.2
     */
    public File getDestFile() {
        return zipFile;
    }


    /**
     * Directory from which to archive files; optional.
     */
    public void setBasedir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Whether we want to compress the files or only store them;
     * optional, default=true;
     */
    public void setCompress(boolean c) {
        doCompress = c;
    }

    /**
     * Whether we want to compress the files or only store them;
     *
     * @since Ant 1.5.2
     */
    public boolean isCompress() {
        return doCompress;
    }

    /**
     * If true, emulate Sun's jar utility by not adding parent directories;
     * optional, defaults to false.
     */
    public void setFilesonly(boolean f) {
        doFilesonly = f;
    }

    /**
     * If true, updates an existing file, otherwise overwrite
     * any existing one; optional defaults to false.
     */
    public void setUpdate(boolean c) {
        doUpdate = c;
        savedDoUpdate = c;
    }

    /**
     * Are we updating an existing archive?
     */
    public boolean isInUpdateMode() {
        return doUpdate;
    }

    /**
     * Adds a set of files.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Adds a set of files that can be
     * read from an archive and be given a prefix/fullpath.
     */
    public void addZipfileset(ZipFileSet set) {
        filesets.addElement(set);
    }

    /**
     * Adds a group of zip files.
     */
    public void addZipGroupFileset(FileSet set) {
        groupfilesets.addElement(set);
    }

    /**
     * Sets behavior for when a duplicate file is about to be added -
     * one of <code>keep</code>, <code>skip</code> or <code>overwrite</code>.
     * Possible values are: <code>keep</code> (keep both
     * of the files); <code>skip</code> (keep the first version
     * of the file found); <code>overwrite</code> overwrite the file
     * with the new file
     * Default for zip tasks is <code>keep</code>
     */
    public void setDuplicate(Duplicate df) {
        duplicate = df.getValue();
    }

    /**
     * Possible behaviors when there are no matching files for the task:
     * "fail", "skip", or "create".
     */
    public static class WhenEmpty extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"fail", "skip", "create"};
        }
    }

    /**
     * Sets behavior of the task when no files match.
     * Possible values are: <code>fail</code> (throw an exception
     * and halt the build); <code>skip</code> (do not create
     * any archive, but issue a warning); <code>create</code>
     * (make an archive with no entries).
     * Default for zip tasks is <code>skip</code>;
     * for jar tasks, <code>create</code>.
     */
    public void setWhenempty(WhenEmpty we) {
        emptyBehavior = we.getValue();
    }

    /**
     * Encoding to use for filenames, defaults to the platform's
     * default encoding.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html</a>.</p>
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Encoding to use for filenames.
     *
     * @since Ant 1.5.2
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * validate and build
     */
    public void execute() throws BuildException {
        if (baseDir == null && filesets.size() == 0
            && groupfilesets.size() == 0 && "zip".equals(archiveType)) {
            throw new BuildException("basedir attribute must be set, "
                                     + "or at least " 
                                     + "one fileset must be given!");
        }

        if (zipFile == null) {
            throw new BuildException("You must specify the " 
                                     + archiveType + " file to create!");
        }

        // Renamed version of original file, if it exists
        File renamedFile = null;
        // Whether or not an actual update is required -
        // we don't need to update if the original file doesn't exist

        addingNewFiles = true;
        if (doUpdate && !zipFile.exists()) {
            doUpdate = false;
            log("ignoring update attribute as " + archiveType
                + " doesn't exist.", Project.MSG_DEBUG);
        }

        // Add the files found in groupfileset to fileset
        for (int i = 0; i < groupfilesets.size(); i++) {

            log("Processing groupfileset ", Project.MSG_VERBOSE);
            FileSet fs = (FileSet) groupfilesets.elementAt(i);
            FileScanner scanner = fs.getDirectoryScanner(getProject());
            String[] files = scanner.getIncludedFiles();
            File basedir = scanner.getBasedir();
            for (int j = 0; j < files.length; j++) {

                log("Adding file " + files[j] + " to fileset", 
                    Project.MSG_VERBOSE);
                ZipFileSet zf = new ZipFileSet();
                zf.setSrc(new File(basedir, files[j]));
                filesets.addElement(zf);
                filesetsFromGroupfilesets.addElement(zf);
            }
        }

        // collect filesets to pass them to getResourcesToAdd
        Vector vfss = new Vector();
        if (baseDir != null) {
            FileSet fs = (FileSet) getImplicitFileSet().clone();
            fs.setDir(baseDir);
            vfss.addElement(fs);
        }
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            vfss.addElement(fs);
        }

        FileSet[] fss = new FileSet[vfss.size()];
        vfss.copyInto(fss);
        boolean success = false;
        try {
            // can also handle empty archives
            ArchiveState state = getResourcesToAdd(fss, zipFile, false);

            // quick exit if the target is up to date
            if (!state.isOutOfDate()) {
                return;
            }

            Resource[][] addThem = state.getResourcesToAdd();

            if (doUpdate) {
                renamedFile =
                    fileUtils.createTempFile("zip", ".tmp",
                                             fileUtils.getParentFile(zipFile));

                try {
                    if (!zipFile.renameTo(renamedFile)) {
                        throw new BuildException("Unable to rename old file "
                                                 + "to temporary file");
                    }
                } catch (SecurityException e) {
                    throw new BuildException("Not allowed to rename old file "
                                             + "to temporary file");
                }
            }

            String action = doUpdate ? "Updating " : "Building ";

            log(action + archiveType + ": " + zipFile.getAbsolutePath());

            ZipOutputStream zOut =
                new ZipOutputStream(new FileOutputStream(zipFile));
            zOut.setEncoding(encoding);
            try {
                if (doCompress) {
                    zOut.setMethod(ZipOutputStream.DEFLATED);
                } else {
                    zOut.setMethod(ZipOutputStream.STORED);
                }
                initZipOutputStream(zOut);

                // Add the explicit filesets to the archive.
                for (int i = 0; i < fss.length; i++) {
                    if (addThem[i].length != 0) {
                        addResources(fss[i], addThem[i], zOut);
                    }
                }
                
                if (doUpdate) {
                    addingNewFiles = false;
                    ZipFileSet oldFiles = new ZipFileSet();
                    oldFiles.setSrc(renamedFile);

                    for (int i = 0; i < addedFiles.size(); i++) {
                        PatternSet.NameEntry ne = oldFiles.createExclude();
                        ne.setName((String) addedFiles.elementAt(i));
                    }
                    DirectoryScanner ds = 
                        oldFiles.getDirectoryScanner(getProject());
                    String[] f = ds.getIncludedFiles();
                    Resource[] r = new Resource[f.length];
                    for (int i = 0; i < f.length; i++) {
                        r[i] = ds.getResource(f[i]);
                    }
                    
                    addResources(oldFiles, r, zOut);
                }
                finalizeZipOutputStream(zOut);

                // If we've been successful on an update, delete the
                // temporary file
                if (doUpdate) {
                    if (!renamedFile.delete()) {
                        log ("Warning: unable to delete temporary file " +
                             renamedFile.getName(), Project.MSG_WARN);
                    }
                }
                success = true;
            } finally {
                // Close the output stream.
                try {
                    if (zOut != null) {
                        zOut.close();
                    }
                } catch (IOException ex) {
                    // If we're in this finally clause because of an
                    // exception, we don't really care if there's an
                    // exception when closing the stream. E.g. if it
                    // throws "ZIP file must have at least one entry",
                    // because an exception happened before we added
                    // any files, then we must swallow this
                    // exception. Otherwise, the error that's reported
                    // will be the close() error, which is not the
                    // real cause of the problem.
                    if (success) {
                        throw ex;
                    }
                }
            }
        } catch (IOException ioe) {
            String msg = "Problem creating " + archiveType + ": " 
                + ioe.getMessage();

            // delete a bogus ZIP file (but only if it's not the original one)
            if ((!doUpdate || renamedFile != null) && !zipFile.delete()) {
                msg += " (and the archive is probably corrupt but I could not "
                    + "delete it)";
            }

            if (doUpdate && renamedFile != null) {
                if (!renamedFile.renameTo(zipFile)) {
                    msg += " (and I couldn't rename the temporary file " +
                        renamedFile.getName() + " back)";
                }
            }

            throw new BuildException(msg, ioe, getLocation());
        } finally {
            cleanUp();
        }
    }

    /**
     * Indicates if the task is adding new files into the archive as opposed to
     * copying back unchanged files from the backup copy
     */
    protected final boolean isAddingNewFiles() {
        return addingNewFiles;
    }

    /**
     * Add the given resources.
     *
     * @param fileset may give additional information like fullpath or
     * permissions.
     * @param resources the resources to add
     * @param zOut the stream to write to
     *
     * @since Ant 1.5.2
     */
    protected final void addResources(FileSet fileset, Resource[] resources,
                                      ZipOutputStream zOut)
        throws IOException {

        String prefix = "";
        String fullpath = "";
        int dirMode = ZipFileSet.DEFAULT_DIR_MODE;
        int fileMode = ZipFileSet.DEFAULT_FILE_MODE;

        ZipFileSet zfs = null;
        if (fileset instanceof ZipFileSet) {
            zfs = (ZipFileSet) fileset;
            prefix = zfs.getPrefix();
            fullpath = zfs.getFullpath();
            dirMode = zfs.getDirMode();
            fileMode = zfs.getFileMode();
        }

        if (prefix.length() > 0 && fullpath.length() > 0) {
            throw new BuildException("Both prefix and fullpath attributes must"
                                     + " not be set on the same fileset.");
        }

        if (resources.length != 1 && fullpath.length() > 0) {
            throw new BuildException("fullpath attribute may only be specified"
                                     + " for filesets that specify a single"
                                     + " file.");
        }

        if (prefix.length() > 0) {
            if (!prefix.endsWith("/") && !prefix.endsWith("\\")) {
                prefix += "/";
            }
            addParentDirs(null, prefix, zOut, "", dirMode);
        }

        ZipFile zf = null;
        try {
            boolean dealingWithFiles = false;
            File base = null;

            if (zfs == null || zfs.getSrc() == null) {
                dealingWithFiles = true;
                base = fileset.getDir(getProject());
            } else {
                zf = new ZipFile(zfs.getSrc());
            }
            
            for (int i = 0; i < resources.length; i++) {
                String name = null;
                if (fullpath.length() > 0) {
                    name = fullpath;
                } else {
                    name = resources[i].getName();
                }
                name = name.replace(File.separatorChar, '/');
                
                if ("".equals(name)) {
                    continue;
                }
                if (resources[i].isDirectory() && ! name.endsWith("/")) {
                    name = name + "/";
                }
                
                addParentDirs(base, name, zOut, prefix, dirMode);
                
                if (!resources[i].isDirectory() && dealingWithFiles) {
                    File f = fileUtils.resolveFile(base, 
                                                   resources[i].getName());
                    zipFile(f, zOut, prefix + name, fileMode);
                } else if (!resources[i].isDirectory()) {
                    java.util.zip.ZipEntry ze = 
                        zf.getEntry(resources[i].getName());
                    if (ze != null) {
                        zipFile(zf.getInputStream(ze), zOut, prefix + name, 
                                ze.getTime(), zfs.getSrc(), fileMode);
                    }
                }
            }
        } finally {
            if (zf != null) {
                zf.close();
            }
        }
    }

    /**
     * method for subclasses to override
     */
    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {
    }

    /**
     * method for subclasses to override
     */
    protected void finalizeZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {
    }

    /**
     * Create an empty zip file
     *
     * @return true for historic reasons
     */
    protected boolean createEmptyZip(File zipFile) throws BuildException {
        // In this case using java.util.zip will not work
        // because it does not permit a zero-entry archive.
        // Must create it manually.
        log("Note: creating empty " + archiveType + " archive " + zipFile,
            Project.MSG_INFO);
        OutputStream os = null;
        try {
            os = new FileOutputStream(zipFile);
            // Cf. PKZIP specification.
            byte[] empty = new byte[22];
            empty[0] = 80; // P
            empty[1] = 75; // K
            empty[2] = 5;
            empty[3] = 6;
            // remainder zeros
            os.write(empty);
        } catch (IOException ioe) {
            throw new BuildException("Could not create empty ZIP archive "
                                     + "(" + ioe.getMessage() + ")", ioe,
                                     getLocation());
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
        return true;
    }

    /**
     * @since Ant 1.5.2
     */
    private synchronized ZipScanner getZipScanner() {
        if (zs == null) {
            zs = new ZipScanner();
            zs.setSrc(zipFile);
        }
        return zs;
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

        Resource[][] initialResources = grabResources(filesets);
        if (isEmpty(initialResources)) {
            if (needsUpdate && doUpdate) {
                /*
                 * This is a rather hairy case.
                 *
                 * One of our subclasses knows that we need to update the
                 * archive, but at the same time, there are no resources
                 * known to us that would need to be added.  Only the
                 * subclass seems to know what's going on.
                 *
                 * This happens if <jar> detects that the manifest has changed,
                 * for example.  The manifest is not part of any resources
                 * because of our support for inline <manifest>s.
                 *
                 * If we invoke createEmptyZip like Ant 1.5.2 did,
                 * we'll loose all stuff that has been in the original
                 * archive (bugzilla report 17780).
                 */
                return new ArchiveState(true, initialResources);
            }

            if (emptyBehavior.equals("skip")) {
                if (doUpdate) {
                    log(archiveType + " archive " + zipFile 
                        + " not updated because no new files were included.", 
                        Project.MSG_VERBOSE);
                } else {
                    log("Warning: skipping " + archiveType + " archive " 
                        + zipFile + " because no files were included.", 
                        Project.MSG_WARN);
                }
            } else if (emptyBehavior.equals("fail")) {
                throw new BuildException("Cannot create " + archiveType
                                         + " archive " + zipFile +
                                         ": no files were included.", 
                                         getLocation());
            } else {
                // Create.
                createEmptyZip(zipFile);
            }
            return new ArchiveState(needsUpdate, initialResources);
        }

        // initialResources is not empty

        if (!zipFile.exists()) {
            return new ArchiveState(true, initialResources);
        }

        if (needsUpdate && !doUpdate) {
            // we are recreating the archive, need all resources
            return new ArchiveState(true, initialResources);
        }

        Resource[][] newerResources = new Resource[filesets.length][];

        for (int i = 0; i < filesets.length; i++) {
            if (!(fileset instanceof ZipFileSet) 
                || ((ZipFileSet) fileset).getSrc() == null) {
                File base = filesets[i].getDir(getProject());
            
                for (int j = 0; j < initialResources[i].length; j++) {
                    File resourceAsFile = 
                        fileUtils.resolveFile(base, 
                                              initialResources[i][j].getName());
                    if (resourceAsFile.equals(zipFile)) {
                        throw new BuildException("A zip file cannot include "
                                                 + "itself", getLocation());
                    }
                }
            }
        }

        for (int i = 0; i < filesets.length; i++) {
            if (initialResources[i].length == 0) {
                newerResources[i] = new Resource[] {};
                continue;
            }
            
            FileNameMapper myMapper = new IdentityMapper();
            if (filesets[i] instanceof ZipFileSet) {
                ZipFileSet zfs = (ZipFileSet) filesets[i];
                if (zfs.getFullpath() != null
                    && !zfs.getFullpath().equals("") ) {
                    // in this case all files from origin map to
                    // the fullPath attribute of the zipfileset at
                    // destination
                    MergingMapper fm = new MergingMapper();
                    fm.setTo(zfs.getFullpath());
                    myMapper = fm;

                } else if (zfs.getPrefix() != null 
                           && !zfs.getPrefix().equals("")) {
                    GlobPatternMapper gm=new GlobPatternMapper();
                    gm.setFrom("*");
                    String prefix = zfs.getPrefix();
                    if (!prefix.endsWith("/") && !prefix.endsWith("\\")) {
                        prefix += "/";
                    }
                    gm.setTo(prefix + "*");
                    myMapper = gm;
                }
            }
            newerResources[i] = 
                ResourceUtils.selectOutOfDateSources(this,
                                                     initialResources[i],
                                                     myMapper,
                                                     getZipScanner());
            needsUpdate = needsUpdate || (newerResources[i].length > 0);

            if (needsUpdate && !doUpdate) {
                // we will return initialResources anyway, no reason
                // to scan further.
                break;
            }
        }

        if (needsUpdate && !doUpdate) {
            // we are recreating the archive, need all resources
            return new ArchiveState(true, initialResources);
        }
        
        return new ArchiveState(needsUpdate, newerResources);
    }

    /**
     * Fetch all included and not excluded resources from the sets.
     *
     * <p>Included directories will preceede included files.</p> 
     *
     * @since Ant 1.5.2
     */
    protected Resource[][] grabResources(FileSet[] filesets) {
        Resource[][] result = new Resource[filesets.length][];
        for (int i = 0; i < filesets.length; i++) {
            DirectoryScanner rs = 
                filesets[i].getDirectoryScanner(getProject());
            Vector resources = new Vector();
            String[] directories = rs.getIncludedDirectories();
            for (int j = 0; j < directories.length; j++) {
                resources.addElement(rs.getResource(directories[j]));
            }
            String[] files = rs.getIncludedFiles();
            for (int j = 0; j < files.length; j++) {
                resources.addElement(rs.getResource(files[j]));
            }
            
            result[i] = new Resource[resources.size()];
            resources.copyInto(result[i]);
        }
        return result;
    }

    /**
     * @since Ant 1.5.2
     */
    protected void zipDir(File dir, ZipOutputStream zOut, String vPath,
                          int mode)
        throws IOException {
        if (addedDirs.get(vPath) != null) {
            // don't add directories we've already added.
            // no warning if we try, it is harmless in and of itself
            return;
        }

        log("adding directory " + vPath, Project.MSG_VERBOSE);
        addedDirs.put(vPath, vPath);

        ZipEntry ze = new ZipEntry (vPath);
        if (dir != null && dir.exists()) {
            // ZIPs store time with a granularity of 2 seconds, round up
            ze.setTime(dir.lastModified() + 1999);
        } else {
            // ZIPs store time with a granularity of 2 seconds, round up
            ze.setTime(System.currentTimeMillis() + 1999);
        }
        ze.setSize (0);
        ze.setMethod (ZipEntry.STORED);
        // This is faintly ridiculous:
        ze.setCrc (EMPTY_CRC);
        ze.setUnixMode(mode);

        zOut.putNextEntry (ze);
    }

    /**
     * Adds a new entry to the archive, takes care of duplicates as well.
     *
     * @param in the stream to read data for the entry from.
     * @param zOut the stream to write to.
     * @param vPath the name this entry shall have in the archive.
     * @param lastModified last modification time for the entry.
     * @param fromArchive the original archive we are copying this
     * entry from, will be null if we are not copying from an archive.
     * @param mode the Unix permissions to set.
     *
     * @since Ant 1.5.2
     */
    protected void zipFile(InputStream in, ZipOutputStream zOut, String vPath,
                           long lastModified, File fromArchive, int mode)
        throws IOException {
        if (entries.contains(vPath)) {

            if (duplicate.equals("preserve")) {
                log(vPath + " already added, skipping", Project.MSG_INFO);
                return;
            } else if (duplicate.equals("fail")) {
                throw new BuildException("Duplicate file " + vPath
                                         + " was found and the duplicate "
                                         + "attribute is 'fail'.");
            } else {
                // duplicate equal to add, so we continue
                log("duplicate file " + vPath
                    + " found, adding.", Project.MSG_VERBOSE);
            }
        } else {
            log("adding entry " + vPath, Project.MSG_VERBOSE);
        }

        entries.put(vPath, vPath);

        ZipEntry ze = new ZipEntry(vPath);
        ze.setTime(lastModified);

        /*
         * XXX ZipOutputStream.putEntry expects the ZipEntry to know its
         * size and the CRC sum before you start writing the data when using
         * STORED mode.
         *
         * This forces us to process the data twice.
         *
         * I couldn't find any documentation on this, just found out by try
         * and error.
         */
        if (!doCompress) {
            long size = 0;
            CRC32 cal = new CRC32();
            if (!in.markSupported()) {
                // Store data into a byte[]
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                byte[] buffer = new byte[8 * 1024];
                int count = 0;
                do {
                    size += count;
                    cal.update(buffer, 0, count);
                    bos.write(buffer, 0, count);
                    count = in.read(buffer, 0, buffer.length);
                } while (count != -1);
                in = new ByteArrayInputStream(bos.toByteArray());

            } else {
                in.mark(Integer.MAX_VALUE);
                byte[] buffer = new byte[8 * 1024];
                int count = 0;
                do {
                    size += count;
                    cal.update(buffer, 0, count);
                    count = in.read(buffer, 0, buffer.length);
                } while (count != -1);
                in.reset();
            }
            ze.setSize(size);
            ze.setCrc(cal.getValue());
        }

        ze.setUnixMode(mode);
        zOut.putNextEntry(ze);

        byte[] buffer = new byte[8 * 1024];
        int count = 0;
        do {
            if (count != 0) {
                zOut.write(buffer, 0, count);
            }
            count = in.read(buffer, 0, buffer.length);
        } while (count != -1);
        addedFiles.addElement(vPath);
    }

    /**
     * Method that gets called when adding from java.io.File instances.
     *
     * <p>This implementation delegates to the six-arg version.</p>
     *
     * @param file the file to add to the archive
     * @param zOut the stream to write to
     * @param vPath the name this entry shall have in the archive
     * @param mode the Unix permissions to set.
     *
     * @since Ant 1.5.2
     */
    protected void zipFile(File file, ZipOutputStream zOut, String vPath, 
                           int mode)
        throws IOException {
        if (file.equals(zipFile)) {
            throw new BuildException("A zip file cannot include itself",
                                     getLocation());
        }

        FileInputStream fIn = new FileInputStream(file);
        try {
            // ZIPs store time with a granularity of 2 seconds, round up
            zipFile(fIn, zOut, vPath, file.lastModified() + 1999, null, mode);
        } finally {
            fIn.close();
        }
    }

    /**
     * Ensure all parent dirs of a given entry have been added.
     *
     * @since Ant 1.5.2
     */
    protected final void addParentDirs(File baseDir, String entry,
                                       ZipOutputStream zOut, String prefix,
                                       int dirMode)
        throws IOException {
        if (!doFilesonly) {
            Stack directories = new Stack();
            int slashPos = entry.length();

            while ((slashPos = entry.lastIndexOf('/', slashPos - 1)) != -1) {
                String dir = entry.substring(0, slashPos + 1);
                if (addedDirs.get(prefix + dir) != null) {
                    break;
                }
                directories.push(dir);
            }

            while (!directories.isEmpty()) {
                String dir = (String) directories.pop();
                File f = null;
                if (baseDir != null) {
                    f = new File(baseDir, dir);
                } else {
                    f = new File(dir);
                }
                zipDir(f, zOut, prefix + dir, dirMode);
            }
        }
    }

    /**
     * Do any clean up necessary to allow this instance to be used again.
     *
     * <p>When we get here, the Zip file has been closed and all we
     * need to do is to reset some globals.</p>
     *
     * <p>This method will only reset globals that have been changed
     * during execute(), it will not alter the attributes or nested
     * child elements.  If you want to reset the instance so that you
     * can later zip a completely different set of files, you must use
     * the reset method.</p>
     *
     * @see #reset
     */
    protected void cleanUp() {
        addedDirs.clear();
        addedFiles.removeAllElements();
        entries.clear();
        addingNewFiles = false;
        doUpdate = savedDoUpdate;
        Enumeration enum = filesetsFromGroupfilesets.elements();
        while (enum.hasMoreElements()) {
            ZipFileSet zf = (ZipFileSet) enum.nextElement();
            filesets.removeElement(zf);
        }
        filesetsFromGroupfilesets.removeAllElements();
    }

    /**
     * Makes this instance reset all attributes to their default
     * values and forget all children.
     *
     * @since Ant 1.5
     *
     * @see #cleanUp
     */
    public void reset() {
        filesets.removeAllElements();
        zipFile = null;
        baseDir = null;
        groupfilesets.removeAllElements();
        duplicate = "add";
        archiveType = "zip";
        doCompress = true;
        emptyBehavior = "skip";
        doUpdate = false;
        doFilesonly = false;
        encoding = null;
    }

    /**
     * @return true if all individual arrays are empty
     * 
     * @since Ant 1.5.2
     */
    protected final static boolean isEmpty(Resource[][] r) {
        for (int i = 0; i < r.length; i++) {
            if (r[i].length > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Possible behaviors when a duplicate file is added:
     * "add", "preserve" or "fail"
     */
    public static class Duplicate extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"add", "preserve", "fail"};
        }
    }

    /**
     * Holds the up-to-date status and the out-of-date resources of
     * the original archive.
     *
     * @since Ant 1.5.3
     */
    public static class ArchiveState {
        private boolean outOfDate;
        private Resource[][] resourcesToAdd;

        ArchiveState(boolean state, Resource[][] r) {
            outOfDate = state;
            resourcesToAdd = r;
        }

        public boolean isOutOfDate() {
            return outOfDate;
        }

        public Resource[][] getResourcesToAdd() {
            return resourcesToAdd;
        }
    }
}
