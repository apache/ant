/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.ant.core.types;

import java.io.*;
import java.util.*;
import org.apache.ant.core.execution.*;

/**
 * A DirectoryFileset is a fileset where the files come from a directory and
 * its subdirectories. 
 */
public class DirectoryScanner extends AbstractScanner {
    /** The directory which is the root of the search space. */
    File basedir;
    
    /**
     * The files that where found and matched at least one includes, and matched
     * no excludes.
     */
    protected List filesIncluded;

    /**
     * The files that where found and did not match any includes.
     */
    protected List filesNotIncluded;

    /**
     * The files that where found and matched at least one includes, and also
     * matched at least one excludes.
     */
    protected List filesExcluded;

    /**
     * The directories that where found and matched at least one includes, and
     * matched no excludes.
     */
    protected List dirsIncluded;

    /**
     * The directories that where found and did not match any includes.
     */
    protected List dirsNotIncluded;

    /**
     * The files that where found and matched at least one includes, and also
     * matched at least one excludes.
     */
    protected List dirsExcluded;

    /** Map to map filenames to actual File objects */
    private Map filenameMap = null;

    public DirectoryScanner(File basedir, List patternSets, 
                            boolean useDefaultExcludes) throws ExecutionException {
        super(patternSets, useDefaultExcludes);
        this.basedir = basedir;
    }
    
    public String[] getIncludedFiles() throws ExecutionException {
        if (filesIncluded == null) {
            scan();
        }
        
        return (String[])filesIncluded.toArray(new String[0]);
    }

    /**
     * Scans the base directory for files that match at least one include
     * pattern, and don't match any exclude patterns.
     *
     * @throws ExecutionException when basedir was set incorrecly
     */
    public void scan() throws ExecutionException  {
        if (basedir == null) {
            throw new ExecutionException("The directory to scan has not been set");
        }
        if (!basedir.exists()) {
            throw new ExecutionException("basedir \"" + basedir
                                            + "\" does not exist");
        }
        if (!basedir.isDirectory()) {
            throw new ExecutionException("basedir \"" + basedir
                                            + "\" is not a directory");
        }

        filesIncluded = new ArrayList();
        filesNotIncluded = new ArrayList();
        filesExcluded = new ArrayList();
        dirsIncluded = new ArrayList();
        dirsNotIncluded = new ArrayList();
        dirsExcluded = new ArrayList();
        filenameMap = new HashMap();

        String root = "";
        String mappedRoot = mapName(root);
        filenameMap.put(mappedRoot, root);
        if (isIncluded(root)) {
            if (!isExcluded(root)) {
                dirsIncluded.add(mappedRoot);
            } else {
                dirsExcluded.add(mappedRoot);
            }
        } else {
            dirsNotIncluded.add(mappedRoot);
        }
        scandir(basedir, root, true);
    }

    /**
     * Scans the passed dir for files and directories. Found files and
     * directories are placed in their respective collections, based on the
     * matching of includes and excludes. When a directory is found, it is
     * scanned recursively.
     *
     * @param dir   the directory to scan
     * @param vpath the path relative to the basedir (needed to prevent
     *              problems with an absolute path when using dir)
     *
     * @see #filesIncluded
     * @see #filesNotIncluded
     * @see #filesExcluded
     * @see #dirsIncluded
     * @see #dirsNotIncluded
     * @see #dirsExcluded
     */
    protected void scandir(File dir, String vpath, boolean fast) 
        throws ExecutionException {
        String[] newfiles = dir.list();

        if (newfiles == null) {
            /*
             * two reasons are mentioned in the API docs for File.list
             * (1) dir is not a directory. This is impossible as
             *     we wouldn't get here in this case.
             * (2) an IO error occurred (why doesn't it throw an exception 
             *     then???)
             */
            throw new ExecutionException ("IO error scanning directory "
                                           + dir.getAbsolutePath());
        }

        for (int i = 0; i < newfiles.length; i++) {
            String name = vpath+newfiles[i];
            String mappedName = mapName(name);
            filenameMap.put(mappedName, name);
            File   file = new File(dir,newfiles[i]);
            if (file.isDirectory()) {
                if (isIncluded(name)) {
                    if (!isExcluded(name)) {
                        dirsIncluded.add(mappedName);
                        if (fast) {
                            scandir(file, name+File.separator, fast);
                        }
                    } else {
                        dirsExcluded.add(mappedName);
                    }
                } else {
                    dirsNotIncluded.add(mappedName);
                    if (fast && couldHoldIncluded(name)) {
                        scandir(file, name+File.separator, fast);
                    }
                }
                if (!fast) {
                    scandir(file, name+File.separator, fast);
                }
            } else if (file.isFile()) {
                if (isIncluded(name)) {
                    if (!isExcluded(name)) {
                        filesIncluded.add(mappedName);
                    } else {
                        filesExcluded.add(mappedName);
                    }
                } else {
                    filesNotIncluded.add(mappedName);
                }
            }
        }
    }

    private String mapName(String rawName) {
        return "bozo/" + rawName;
    }

    public File getLocalFile(String mappedName) throws ExecutionException {
        if (filesIncluded == null) {
            scan();
        }
        
        String realName = (String)filenameMap.get(mappedName);
        if (realName == null) {
            throw new ExecutionException("\"" + mappedName + "\" was not included in the scan.");
        }
        
        return new File(basedir, realName);
    }

    public String toString() {
        try {
            String[] files = getIncludedFiles();
            StringBuffer sb = new StringBuffer();
            String lsep = System.getProperty("line.separator");
            for (int i = 0; i < files.length; ++i) {
                sb.append(files[i]);
                sb.append(lsep);
            }
            
            return sb.toString();
        }
        catch (ExecutionException e) {
            return "Fileset from \"" + basedir + "\"";
        }
    }
}
