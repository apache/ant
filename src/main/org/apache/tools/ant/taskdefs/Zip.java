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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.ZipScanner;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.zip.ZipOutputStream;
import org.apache.tools.zip.ZipEntry;

/**
 * Create a Zip file.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */
public class Zip extends MatchingTask {

    protected File zipFile;
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
        doUpdate = doUpdate && zipFile.exists();

        // Add the files found in groupfileset to fileset
        for (int i = 0; i < groupfilesets.size(); i++) {

            log("Processing groupfileset ", Project.MSG_VERBOSE);
            FileSet fs = (FileSet) groupfilesets.elementAt(i);
            FileScanner scanner = fs.getDirectoryScanner(project);
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

        // Create the scanners to pass to isUpToDate().
        Vector dss = new Vector();
        if (baseDir != null) {
            dss.addElement(getDirectoryScanner(baseDir));
        }
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            dss.addElement (fs.getDirectoryScanner(project));
        }
        int dssSize = dss.size();
        FileScanner[] scanners = new FileScanner[dssSize];
        dss.copyInto(scanners);

        boolean success = false;
        try {
            // quick exit if the target is up to date
            // can also handle empty archives
            if (isUpToDate(scanners, zipFile)) {
                return;
            }
            
            if (doUpdate) {
                FileUtils fileUtils = FileUtils.newFileUtils();
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

                // Add the implicit fileset to the archive.
                if (baseDir != null) {
                    addFiles(getDirectoryScanner(baseDir), zOut, "", "");
                }
                // Add the explicit filesets to the archive.
                addFiles(filesets, zOut);
                if (doUpdate) {
                    addingNewFiles = false;
                    ZipFileSet oldFiles = new ZipFileSet();
                    oldFiles.setSrc(renamedFile);

                    for (int i = 0; i < addedFiles.size(); i++) {
                        PatternSet.NameEntry ne = oldFiles.createExclude();
                        ne.setName((String) addedFiles.elementAt(i));
                    }
                    Vector tmp = new Vector(1);
                    tmp.addElement(oldFiles);
                    addFiles(tmp, zOut);
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

            throw new BuildException(msg, ioe, location);
        } finally {
            cleanUp();
        }
    }

    /**
     * Indicates if the task is adding new files into the archive as opposed to
     * copying back unchanged files from the backup copy
     */
    protected boolean isAddingNewFiles() {
        return addingNewFiles;
    }

    /**
     * Add all files of the given FileScanner to the ZipOutputStream
     * prependig the given prefix to each filename.
     *
     * <p>Ensure parent directories have been added as well.
     */
    protected void addFiles(FileScanner scanner, ZipOutputStream zOut,
                            String prefix, String fullpath) 
        throws IOException {

        if (prefix.length() > 0 && fullpath.length() > 0) {
            throw new BuildException("Both prefix and fullpath attributes must"
                                     + " not be set on the same fileset.");
        }

        File thisBaseDir = scanner.getBasedir();

        // directories that matched include patterns
        String[] dirs = scanner.getIncludedDirectories();
        if (dirs.length > 0 && fullpath.length() > 0) {
            throw new BuildException("fullpath attribute may only be specified"
                                     + " for filesets that specify a single"
                                     + " file.");
        }
        for (int i = 0; i < dirs.length; i++) {
            if ("".equals(dirs[i])) {
                continue;
            }
            String name = dirs[i].replace(File.separatorChar, '/');
            if (!name.endsWith("/")) {
                name += "/";
            }
            addParentDirs(thisBaseDir, name, zOut, prefix);
        }

        // files that matched include patterns
        String[] files = scanner.getIncludedFiles();
        if (files.length > 1 && fullpath.length() > 0) {
            throw new BuildException("fullpath attribute may only be specified"
                                     + " for filesets that specify a single"
                                     + "file.");
        }
        for (int i = 0; i < files.length; i++) {
            File f = new File(thisBaseDir, files[i]);
            if (fullpath.length() > 0) {
                // Add this file at the specified location.
                addParentDirs(null, fullpath, zOut, "");
                zipFile(f, zOut, fullpath);
            } else {
                // Add this file with the specified prefix.
                String name = files[i].replace(File.separatorChar, '/');
                addParentDirs(thisBaseDir, name, zOut, prefix);
                zipFile(f, zOut, prefix + name);
            }
        }
    }

    protected void addZipEntries(ZipFileSet fs, DirectoryScanner ds,
                                 ZipOutputStream zOut, String prefix, 
                                 String fullpath)
        throws IOException {
        log("adding zip entries: " + fullpath, Project.MSG_VERBOSE);

        if (prefix.length() > 0 && fullpath.length() > 0) {
            throw new BuildException("Both prefix and fullpath attributes must"
                                     + " not be set on the same fileset.");
        }

        ZipScanner zipScanner = (ZipScanner) ds;
        File zipSrc = fs.getSrc();

        ZipEntry entry;
        java.util.zip.ZipEntry origEntry;
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new FileInputStream(zipSrc));

            while ((origEntry = in.getNextEntry()) != null) {
                entry = new ZipEntry(origEntry);
                String vPath = entry.getName();
                if (zipScanner.match(vPath)) {
                    if (fullpath.length() > 0) {
                        addParentDirs(null, fullpath, zOut, "");
                        zipFile(in, zOut, fullpath, entry.getTime(), zipSrc);
                    } else {
                        addParentDirs(null, vPath, zOut, prefix);
                        if (!entry.isDirectory()) {
                            zipFile(in, zOut, prefix + vPath, entry.getTime(), 
                                    zipSrc);
                        }
                    }
                }
            }
        } finally {
            if (in != null) {
                in.close();
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
     * @return true if the file is then considered up to date.
     */
    protected boolean createEmptyZip(File zipFile) {
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
                                     location);
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
     * Check whether the archive is up-to-date; and handle behavior
     * for empty archives.
     * @param scanners list of prepared scanners containing files to archive
     * @param zipFile intended archive file (may or may not exist)
     * @return true if nothing need be done (may have done something
     *         already); false if archive creation should proceed
     * @exception BuildException if it likes
     */
    protected boolean isUpToDate(FileScanner[] scanners, File zipFile) 
        throws BuildException {
        String[][] fileNames = grabFileNames(scanners);
        File[] files = grabFiles(scanners, fileNames);
        if (files.length == 0) {
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
                return true;
            } else if (emptyBehavior.equals("fail")) {
                throw new BuildException("Cannot create " + archiveType
                                         + " archive " + zipFile +
                                         ": no files were included.", location);
            } else {
                // Create.
                return createEmptyZip(zipFile);
            }
        } else {
            for (int i = 0; i < files.length; ++i) {
                if (files[i].equals(zipFile)) {
                    throw new BuildException("A zip file cannot include " 
                        + "itself", location);
                }
            }

            if (!zipFile.exists()) {
                return false;
            }

            SourceFileScanner sfs = new SourceFileScanner(this);
            MergingMapper mm = new MergingMapper();
            mm.setTo(zipFile.getAbsolutePath());
            for (int i = 0; i < scanners.length; i++) {
                if (sfs.restrict(fileNames[i], scanners[i].getBasedir(), null,
                                 mm).length > 0) {
                    return false;
                }
            }
            return true;
        }
    }

    protected static File[] grabFiles(FileScanner[] scanners) {
        return grabFiles(scanners, grabFileNames(scanners));
    }

    protected static File[] grabFiles(FileScanner[] scanners,
                                      String[][] fileNames) {
        Vector files = new Vector();
        for (int i = 0; i < fileNames.length; i++) {
            File thisBaseDir = scanners[i].getBasedir();
            for (int j = 0; j < fileNames[i].length; j++) {
                files.addElement(new File(thisBaseDir, fileNames[i][j]));
            }
        }
        File[] toret = new File[files.size()];
        files.copyInto(toret);
        return toret;
    }

    protected static String[][] grabFileNames(FileScanner[] scanners) {
        String[][] result = new String[scanners.length][];
        for (int i = 0; i < scanners.length; i++) {
            String[] files = scanners[i].getIncludedFiles();
            String[] dirs = scanners[i].getIncludedDirectories();
            result[i] = new String[files.length + dirs.length];
            System.arraycopy(files, 0, result[i], 0, files.length);
            System.arraycopy(dirs, 0, result[i], files.length, dirs.length);
        }
        return result;
    }

    protected void zipDir(File dir, ZipOutputStream zOut, String vPath)
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
            ze.setTime(dir.lastModified());
        } else {
            ze.setTime(System.currentTimeMillis());
        }
        ze.setSize (0);
        ze.setMethod (ZipEntry.STORED);
        // This is faintly ridiculous:
        ze.setCrc (EMPTY_CRC);

        // this is 040775 | MS-DOS directory flag in reverse byte order
        ze.setExternalAttributes(0x41FD0010L);

        zOut.putNextEntry (ze);
    }

    protected void zipFile(InputStream in, ZipOutputStream zOut, String vPath,
                           long lastModified, File file)
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

    protected void zipFile(File file, ZipOutputStream zOut, String vPath)
        throws IOException {
        if (file.equals(zipFile)) {
            throw new BuildException("A zip file cannot include itself", 
                                     location);
        }

        FileInputStream fIn = new FileInputStream(file);
        try {
            zipFile(fIn, zOut, vPath, file.lastModified(), null);
        } finally {
            fIn.close();
        }
    }

    /**
     * Ensure all parent dirs of a given entry have been added.
     */
    protected void addParentDirs(File baseDir, String entry,
                                 ZipOutputStream zOut, String prefix)
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
                zipDir(f, zOut, prefix + dir);
            }
        }
    }

    /**
     * Iterate over the given Vector of (zip)filesets and add
     * all files to the ZipOutputStream using the given prefix
     * or fullpath.
     */
    protected void addFiles(Vector filesets, ZipOutputStream zOut)
        throws IOException {
        // Add each fileset in the Vector.
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);

            String prefix = "";
            String fullpath = "";
            if (fs instanceof ZipFileSet) {
                ZipFileSet zfs = (ZipFileSet) fs;
                prefix = zfs.getPrefix();
                fullpath = zfs.getFullpath();
            }

            if (prefix.length() > 0
                && !prefix.endsWith("/")
                && !prefix.endsWith("\\")) {
                prefix += "/";
            }

            // Need to manually add either fullpath's parent directory, or
            // the prefix directory, to the archive.
            if (prefix.length() > 0) {
                addParentDirs(null, prefix, zOut, "");
                zipDir(null, zOut, prefix);
            } else if (fullpath.length() > 0) {
                addParentDirs(null, fullpath, zOut, "");
            }

            if (fs instanceof ZipFileSet
                && ((ZipFileSet) fs).getSrc() != null) {
                addZipEntries((ZipFileSet) fs, ds, zOut, prefix, fullpath);
            } else {
                // Add the fileset.
                addFiles(ds, zOut, prefix, fullpath);
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
     * Possible behaviors when a duplicate file is added:
     * "add", "preserve" or "fail"
     */
    public static class Duplicate extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"add", "preserve", "fail"};
        }
    }
}
