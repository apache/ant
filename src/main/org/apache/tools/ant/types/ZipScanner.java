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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * ZipScanner accesses the pattern matching algorithm in DirectoryScanner,
 * which are protected methods that can only be accessed by subclassing.
 *
 * This implementation of FileScanner defines getIncludedFiles to return
 * the matching Zip entries.
 *
 * @author Don Ferguson <a href="mailto:don@bea.com">don@bea.com</a>
 */
public class ZipScanner extends DirectoryScanner {

    /**
     * The zip file which should be scanned.
     */
    protected File srcFile;
    /**
     *  The current task, used to report errors, ...
     */
    private Task task;
    /**
     * to record the last scanned zip file with its modification date
     */
    private Resource lastScannedResource;
    /**
     * record list of all zip entries
     */
    private Vector myentries;

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
     * Sets the current task. This is used to provide proper logging
     * for exceptions
     *
     * @param task the current task
     *
     * @since Ant 1.5.2
     */
    public void setTask(Task task) {
        this.task = task;
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
        Vector myvector = new Vector();
        // first check if the archive needs to be scanned again
        scanme();
        for (int counter = 0; counter < myentries.size(); counter++) {
            Resource myresource= (Resource) myentries.elementAt(counter);
            if (!myresource.isDirectory() && match(myresource.getName())) {
                myvector.addElement(myresource.getName());
            }
        }
        String[] files = new String[myvector.size()];
        myvector.copyInto(files);
        return files;
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
        Vector myvector=new Vector();
        // first check if the archive needs to be scanned again
        scanme();
        for (int counter = 0; counter < myentries.size(); counter++) {
            Resource myresource = (Resource) myentries.elementAt(counter);
            if (myresource.isDirectory() && match(myresource.getName())) {
                myvector.addElement(myresource.getName());
            }
        }
        String[] files = new String[myvector.size()];
        myvector.copyInto(files);
        return files;
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
     * Returns the resources of the files which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return resource information for the files which matched at
     * least one of the include patterns and none of the exclude
     * patterns.
     *
     * @since Ant 1.5.2
     */
    public Resource[] getIncludedFileResources() {
        Vector myvector = new Vector();
        // first check if the archive needs to be scanned again
        scanme();
        for (int counter = 0; counter < myentries.size(); counter++) {
             Resource myresource = (Resource) myentries.elementAt(counter);
             if (!myresource.isDirectory() && match(myresource.getName())) {
                 myvector.addElement(myresource.clone());
             }
         }
         Resource[] resources = new Resource[myvector.size()];
         myvector.copyInto(resources);
         return resources;
    }

    /**
     * Returns the resources of the files which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return resource information for the files which matched at
     * least one of the include patterns and none of the exclude
     * patterns.
     *
     * @since Ant 1.5.2
     */
    public Resource[] getIncludedDirectoryResources() {
        Vector myvector = new Vector();
         // first check if the archive needs to be scanned again
         scanme();
         for (int counter = 0; counter < myentries.size(); counter++) {
             Resource myresource = (Resource) myentries.elementAt(counter);
             if (myresource.isDirectory() && match(myresource.getName())) {
                 myvector.add(myresource.clone());
             }
         }
         Resource[] resources = new Resource[myvector.size()];
         myvector.copyInto(resources);
         return resources;
    }

    /**
     * @param name path name of the file sought in the archive
     *
     * @since Ant 1.5.2
     */
    public Resource getResource(String name) {
        // first check if the archive needs to be scanned again
        scanme();
        for (int counter = 0; counter < myentries.size(); counter++) {
            Resource myresource=(Resource)myentries.elementAt(counter);
            if (myresource.getName().equals(name)) {
                return myresource;
            }
        }
        return new Resource(name);
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

        Vector vResult = new Vector();
        if (task != null) {
            task.log("checking zip entries: " + srcFile, Project.MSG_VERBOSE);
        }

        ZipEntry entry = null;
        ZipInputStream in = null;
        myentries = new Vector();
        try {
            try {
                in = new ZipInputStream(new FileInputStream(srcFile));
                if (task != null) {
                    task.log("opening input stream from " + srcFile, 
                             Project.MSG_DEBUG);
                }
            } catch (IOException ex) {
                // XXX - throw a BuildException instead ??
                if (task != null) {
                    task.log("problem opening "+srcFile,Project.MSG_ERR);
                }
            }
            
            while (true) {
                try {
                    entry = in.getNextEntry();
                    if (entry == null) {
                        break;
                    }
                    myentries.add(new Resource(entry.getName(),
                                               true,
                                               entry.getTime(),
                                               entry.isDirectory()));
                    if (task != null) {
                        task.log("adding entry " + entry.getName() + " from "
                                 + srcFile, Project.MSG_DEBUG);
                    }
                    
                } catch (ZipException ex) {
                    // XXX - throw a BuildException instead ??
                    if (task != null ) {
                        task.log("problem reading " + srcFile,
                                 Project.MSG_ERR);
                    }

                } catch (IOException e) {
                    // XXX - throw a BuildException instead ??
                    if (task != null) {
                        task.log("problem reading zip entry from " + srcFile,
                                 Project.MSG_ERR);
                    }
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                    if (task != null) {
                        task.log("closing input stream from " + srcFile,
                                 Project.MSG_DEBUG);
                    }
                } catch (IOException ex) {
                    if (task != null) {
                        task.log("problem closing input stream from "
                                 + srcFile, Project.MSG_ERR);
                    }
                }
            }
        }
        // record data about the last scanned resource
        lastScannedResource = thisresource;
    }
}
