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
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.zip.ZipException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

/**
 * ZipScanner accesses the pattern matching algorithm in DirectoryScanner,
 * which are protected methods that can only be accessed by subclassing.
 *
 * This implementation of FileScanner defines getIncludedFiles to return
 * the matching Zip entries.
 *
 * @author Don Ferguson <a href="mailto:don@bea.com">don@bea.com</a>
 * @author <a href="mailto:levylambert@tiscali-dsl.de">Antoine Levy-Lambert</a>
 */
public class ZipScanner extends DirectoryScanner {

    /**
     * The zip file which should be scanned.
     */
    protected File srcFile;
    /**
     * to record the last scanned zip file with its modification date
     */
    private Resource lastScannedResource;
    /**
     * record list of all zip entries
     */
    private Hashtable myentries;

    /**
     * encoding of file names.
     *
     * @since Ant 1.6
     */
    private String encoding;

    /**
     * Sets the srcFile for scanning. This is the jar or zip file that
     * is scanned for matching entries.
     *
     * @param srcFile the (non-null) zip file name for scanning
     */
    public void setSrc(File srcFile) {
        this.srcFile = srcFile;
    }

    /**
     * Sets encoding of file names.
     *
     * @since Ant 1.6
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns the names of the files which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the files which matched at least one of the
     *         include patterns and none of the exclude patterns.
     */
    public String[] getIncludedFiles() {
        if (srcFile != null) {
            Vector myvector = new Vector();
            // first check if the archive needs to be scanned again
            scanme();
            for (Enumeration e = myentries.elements(); e.hasMoreElements();) {
                Resource myresource = (Resource) e.nextElement();
                if (!myresource.isDirectory() && match(myresource.getName())) {
                    myvector.addElement(myresource.getName());
                }
            }
            String[] files = new String[myvector.size()];
            myvector.copyInto(files);
            Arrays.sort(files);
            return files;
        } else {
            return super.getIncludedFiles();
        }
    }

    /**
     * Returns the names of the directories which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the directories which matched at least one of the
     * include patterns and none of the exclude patterns.
     */
    public String[] getIncludedDirectories() {
        if (srcFile != null) {
            Vector myvector = new Vector();
            // first check if the archive needs to be scanned again
            scanme();
            for (Enumeration e = myentries.elements(); e.hasMoreElements();) {
                Resource myresource = (Resource) e.nextElement();
                if (myresource.isDirectory() && match(myresource.getName())) {
                    myvector.addElement(myresource.getName());
                }
            }
            String[] files = new String[myvector.size()];
            myvector.copyInto(files);
            Arrays.sort(files);
            return files;
        } else {
            return super.getIncludedDirectories();
        }
    }

    /**
     * Initialize DirectoryScanner data structures.
     */
    public void init() {
        if (includes == null) {
            // No includes supplied, so set it to 'matches all'
            includes = new String[1];
            includes[0] = "**";
        }
        if (excludes == null) {
            excludes = new String[0];
        }
    }

    /**
     * Matches a jar entry against the includes/excludes list,
     * normalizing the path separator.
     *
     * @param path the (non-null) path name to test for inclusion
     *
     * @return <code>true</code> if the path should be included
     *         <code>false</code> otherwise.
     */
    public boolean match(String path) {
        String vpath = path.replace('/', File.separatorChar).
            replace('\\', File.separatorChar);
        return isIncluded(vpath) && !isExcluded(vpath);
    }

    /**
     * @param name path name of the file sought in the archive
     *
     * @since Ant 1.5.2
     */
    public Resource getResource(String name) {
        if (srcFile == null) {
            return super.getResource(name);
        } else if (name.equals("")) {
            // special case in ZIPs, we do not want this thing included
            return new Resource("", true, Long.MAX_VALUE, true);
        }

        // first check if the archive needs to be scanned again
        scanme();
        if (myentries.containsKey(name)) {
            return (Resource) myentries.get(name);
        } else if (myentries.containsKey(name + "/")) {
            return (Resource) myentries.get(name + "/");
        } else {
            return new Resource(name);
        }
    }

    /**
     * if the datetime of the archive did not change since
     * lastScannedResource was initialized returns immediately else if
     * the archive has not been scanned yet, then all the zip entries
     * are put into the vector myentries as a vector of the resource
     * type
     */
    private void scanme() {
        Resource thisresource = new Resource(srcFile.getAbsolutePath(),
                                             srcFile.exists(),
                                             srcFile.lastModified());

        // spare scanning again and again
        if (lastScannedResource != null
            && lastScannedResource.getName().equals(thisresource.getName())
            && lastScannedResource.getLastModified()
            == thisresource.getLastModified()) {
            return;
        }

        ZipEntry entry = null;
        ZipFile zf = null;
        myentries = new Hashtable();
        try {
            try {
                zf = new ZipFile(srcFile, encoding);
            } catch (ZipException ex) {
                throw new BuildException("problem reading " + srcFile, ex);
            } catch (IOException ex) {
                throw new BuildException("problem opening " + srcFile, ex);
            }

            Enumeration e = zf.getEntries();
            while (e.hasMoreElements()) {
                entry = (ZipEntry) e.nextElement();
                myentries.put(new String(entry.getName()),
                              new Resource(entry.getName(), true,
                                           entry.getTime(),
                                           entry.isDirectory()));
            }
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException ex) {
                    // swallow
                }
            }
        }
        // record data about the last scanned resource
        lastScannedResource = thisresource;
    }
}
