/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;

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
     * Sets the srcFile for scanning. This is the jar or zip file that
     * is scanned for matching entries.
     *
     * @param srcFile the (non-null) zip file name for scanning
     */
    public void setSrc(File srcFile) {
        this.srcFile = srcFile;
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
            for (Enumeration e = myentries.elements(); e.hasMoreElements() ;) {
                Resource myresource= (Resource) e.nextElement();
                if (!myresource.isDirectory() && match(myresource.getName())) {
                    myvector.addElement(myresource.getName());
                }
            }
            String[] files = new String[myvector.size()];
            myvector.copyInto(files);
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
            Vector myvector=new Vector();
            // first check if the archive needs to be scanned again
            scanme();
            for (Enumeration e = myentries.elements(); e.hasMoreElements() ;) {
                Resource myresource= (Resource) e.nextElement();
                if (myresource.isDirectory() && match(myresource.getName())) {
                    myvector.addElement(myresource.getName());
                }
            }
            String[] files = new String[myvector.size()];
            myvector.copyInto(files);
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
        ZipInputStream in = null;
        myentries = new Hashtable();
        try {
            try {
                in = new ZipInputStream(new FileInputStream(srcFile));
            } catch (IOException ex) {
                throw new BuildException("problem opening " + srcFile, ex);
            }

            while (true) {
                try {
                    entry = in.getNextEntry();
                    if (entry == null) {
                        break;
                    }
                    myentries.put(new String(entry.getName()),
                                  new Resource(entry.getName(), true,
                                               entry.getTime(), 
                                               entry.isDirectory()));
                } catch (ZipException ex) {
                    throw new BuildException("problem reading " + srcFile,
                                             ex);
                } catch (IOException e) {
                    throw new BuildException("problem reading zip entry from " 
                                             + srcFile, e);
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    // swallow
                }
            }
        }
        // record data about the last scanned resource
        lastScannedResource = thisresource;
    }
}
