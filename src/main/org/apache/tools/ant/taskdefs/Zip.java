/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.*;

/**
 * Create a ZIP archive.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 */

public class Zip extends MatchingTask {

    private File zipFile;
    private File baseDir;
    private boolean doCompress = true;
    protected String archiveType = "zip";
    // For directories:
    private static long emptyCrc = new CRC32 ().getValue ();
    protected String emptyBehavior = null;
    private Vector filesets = new Vector ();
    
    /**
     * This is the name/location of where to 
     * create the .zip file.
     */
    public void setZipfile(String zipFilename) {
        zipFile = project.resolveFile(zipFilename);
    }
    
    /**
     * This is the base directory to look in for 
     * things to zip.
     */
    public void setBasedir(String baseDirname) {
        baseDir = project.resolveFile(baseDirname);
    }

    /**
     * Sets whether we want to compress the files or only store them.
     */
    public void setCompress(String compress) {
        doCompress = Project.toBoolean(compress);
    }

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Adds a reference to a set of files (nested filesetref element).
     */
    public void addFilesetref(Reference ref) {
        filesets.addElement(ref);
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
    public void setWhenempty(String we) throws BuildException {
        we = we.toLowerCase();
        // XXX could instead be using EnumeratedAttribute, but this works
        if (!"fail".equals(we) && !"skip".equals(we) && !"create".equals(we))
            throw new BuildException("Unrecognized whenempty attribute: " + we);
        emptyBehavior = we;
    }

    public void execute() throws BuildException {
        if (baseDir == null && filesets.size() == 0)
            throw new BuildException("basedir attribute must be set, or at least one fileset must be given!");

        Vector dss = new Vector ();
        if (baseDir != null)
            dss.addElement(getDirectoryScanner(baseDir));
        for (int i=0; i<filesets.size(); i++) {
            Object o = filesets.elementAt(i);
            FileSet fs;
            if (o instanceof FileSet) {
                fs = (FileSet) o;
            } else {
                Reference r = (Reference) o;
                o = r.getReferencedObject(project);
                if (o instanceof FileSet) {
                    fs = (FileSet) o;
                } else {
                    throw new BuildException(r.getRefId() + " does not denote a fileset", location);
                }
            }
            dss.addElement (fs.getDirectoryScanner(project));
        }
        FileScanner[] scanners = new FileScanner[dss.size()];
        dss.copyInto(scanners);

        // quick exit if the target is up to date
        // can also handle empty archives
        if (isUpToDate(scanners, zipFile)) return;

        log("Building "+ archiveType +": "+ zipFile.getAbsolutePath());

	try {
	    ZipOutputStream zOut = new ZipOutputStream(new FileOutputStream(zipFile));
	    try {
		if (doCompress) {
		    zOut.setMethod(ZipOutputStream.DEFLATED);
		} else {
		    zOut.setMethod(ZipOutputStream.STORED);
		}
		initZipOutputStream(zOut);

                // XXX ideally would also enter includedDirectories to the archive
		Hashtable parentDirs = new Hashtable();

                for (int j = 0; j < scanners.length; j++) {
                    String[] files = scanners[j].getIncludedFiles();
                    File thisBaseDir = scanners[j].getBasedir();
                    for (int i = 0; i < files.length; i++) {
                        File f = new File(thisBaseDir,files[i]);
                        String name = files[i].replace(File.separatorChar,'/');
                        // Look for & create parent dirs as needed.
                        int slashPos = -1;
                        while ((slashPos = name.indexOf((int)'/', slashPos + 1)) != -1) {
                            String dir = name.substring(0, slashPos);
                            if (!parentDirs.contains(dir)) {
                                parentDirs.put(dir, dir);
                                zipDir(new File(thisBaseDir, dir.replace('/', File.separatorChar)),
                                       zOut, dir + '/');
                            }
                        }
                        zipFile(f, zOut, name);
                    }
                }
	    } finally {
		zOut.close ();
	    }
	} catch (IOException ioe) {
	    String msg = "Problem creating " + archiveType + ": " + ioe.getMessage();

            // delete a bogus ZIP file
	    if (!zipFile.delete()) {
		msg += " (and the archive is probably corrupt but I could not delete it)";
	    }

            throw new BuildException(msg, ioe, location);
        }
    }

    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException
    {
    }

    /**
     * Check whether the archive is up-to-date; and handle behavior for empty archives.
     * @param scanners list of prepared scanners containing files to archive
     * @param zipFile intended archive file (may or may not exist)
     * @return true if nothing need be done (may have done something already); false if
     *         archive creation should proceed
     * @exception BuildException if it likes
     */
    protected boolean isUpToDate(FileScanner[] scanners, File zipFile) throws BuildException
    {
        if (emptyBehavior == null) emptyBehavior = "skip";
        File[] files = grabFiles(scanners);
        if (files.length == 0) {
            if (emptyBehavior.equals("skip")) {
                log("Warning: skipping ZIP archive " + zipFile +
                    " because no files were included.", Project.MSG_WARN);
                return true;
            } else if (emptyBehavior.equals("fail")) {
                throw new BuildException("Cannot create ZIP archive " + zipFile +
                                         ": no files were included.", location);
            } else {
                // Create.
                if (zipFile.exists()) return true;
                // In this case using java.util.zip will not work
                // because it does not permit a zero-entry archive.
                // Must create it manually.
                log("Note: creating empty ZIP archive " + zipFile, Project.MSG_INFO);
                try {
                    OutputStream os = new FileOutputStream(zipFile);
                    try {
                        // Cf. PKZIP specification.
                        byte[] empty = new byte[22];
                        empty[0] = 80; // P
                        empty[1] = 75; // K
                        empty[2] = 5;
                        empty[3] = 6;
                        // remainder zeros
                        os.write(empty);
                    } finally {
                        os.close();
                    }
                } catch (IOException ioe) {
                    throw new BuildException("Could not create empty ZIP archive", ioe, location);
                }
                return true;
            }
        } else {
            // Probably unnecessary but just for clarity:
            if (!zipFile.exists()) return false;
            for (int i=0; i<files.length; i++) {
                if (files[i].lastModified() > zipFile.lastModified()) {
                    return false;
                }
            }
            return true;
        }
    }

    protected static File[] grabFiles(FileScanner[] scanners) {
        Vector files = new Vector ();
        for (int i = 0; i < scanners.length; i++) {
            File thisBaseDir = scanners[i].getBasedir();
            String[] ifiles = scanners[i].getIncludedFiles();
            for (int j = 0; j < ifiles.length; j++)
                files.addElement(new File(thisBaseDir, ifiles[j]));
        }
        File[] toret = new File[files.size()];
        files.copyInto(toret);
        return toret;
    }

    protected void zipDir(File dir, ZipOutputStream zOut, String vPath)
        throws IOException
    {
	ZipEntry ze = new ZipEntry (vPath);
	if (dir != null) ze.setTime (dir.lastModified ());
	ze.setSize (0);
	ze.setMethod (ZipEntry.STORED);
	// This is faintly ridiculous:
	ze.setCrc (emptyCrc);
	zOut.putNextEntry (ze);
    }

    protected void zipFile(InputStream in, ZipOutputStream zOut, String vPath,
                           long lastModified)
        throws IOException
    {
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
            zOut.write(buffer, 0, count);
            count = in.read(buffer, 0, buffer.length);
        } while (count != -1);
    }

    protected void zipFile(File file, ZipOutputStream zOut, String vPath)
        throws IOException
    {
        FileInputStream fIn = new FileInputStream(file);
        try {
            zipFile(fIn, zOut, vPath, file.lastModified());
        } finally {
            fIn.close();
        }
    }
}
