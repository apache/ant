/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant;

import java.io.File;

/**
 * An interface used to describe the actions required of any type of 
 * directory scanner.
 */
public interface FileScanner {
    /**
     * Adds default exclusions to the current exclusions set.
     */
    void addDefaultExcludes();
    
    /**
     * Returns the base directory to be scanned. 
     * This is the directory which is scanned recursively.
     *
     * @return the base directory to be scanned
     */
    File getBasedir();
    
    /**
     * Returns the names of the directories which matched at least one of the 
     * include patterns and at least one of the exclude patterns.
     * The names are relative to the base directory.
     * 
     * @return the names of the directories which matched at least one of the 
     * include patterns and at least one of the exclude patterns.
     */
    String[] getExcludedDirectories();
    
    /**
     * Returns the names of the files which matched at least one of the 
     * include patterns and at least one of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the files which matched at least one of the 
     *         include patterns and at at least one of the exclude patterns.
     * 
     */    
    String[] getExcludedFiles();
    
    /**
     * Returns the names of the directories which matched at least one of the 
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the directories which matched at least one of the
     * include patterns and none of the exclude patterns.
     */
    String[] getIncludedDirectories();
    
    /**
     * Returns the names of the files which matched at least one of the 
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the files which matched at least one of the
     *         include patterns and none of the exclude patterns.
     */
    String[] getIncludedFiles();
    
    /**
     * Returns the names of the directories which matched none of the include
     * patterns. The names are relative to the base directory.
     *
     * @return the names of the directories which matched none of the include
     * patterns.
     */    
    String[] getNotIncludedDirectories();
    
    /**
     * Returns the names of the files which matched none of the include 
     * patterns. The names are relative to the base directory.
     *
     * @return the names of the files which matched none of the include 
     *         patterns.
     */
    String[] getNotIncludedFiles();
    
    /**
     * Scans the base directory for files which match at least one include
     * pattern and don't match any exclude patterns.
     *
     * @exception IllegalStateException if the base directory was set 
     *            incorrectly (i.e. if it is <code>null</code>, doesn't exist,
     *            or isn't a directory).
     */
    void scan() throws IllegalStateException;
    
    /**
     * Sets the base directory to be scanned. This is the directory which is
     * scanned recursively. All '/' and '\' characters should be replaced by
     * <code>File.separatorChar</code>, so the separator used need not match
     * <code>File.separatorChar</code>.
     *
     * @param basedir The base directory to scan. 
     *                Must not be <code>null</code>.
     */
    void setBasedir(String basedir);
    
    /**
     * Sets the base directory to be scanned. This is the directory which is 
     * scanned recursively.
     *
     * @param basedir The base directory for scanning. 
     *                Should not be <code>null</code>.
     */
    void setBasedir(File basedir);
    
    /**
     * Sets the list of exclude patterns to use.
     *
     * @param excludes A list of exclude patterns. 
     *                 May be <code>null</code>, indicating that no files 
     *                 should be excluded. If a non-<code>null</code> list is 
     *                 given, all elements must be non-<code>null</code>.
     */    
    void setExcludes(String[] excludes);
    
    /**
     * Sets the list of include patterns to use.
     *
     * @param includes A list of include patterns.
     *                 May be <code>null</code>, indicating that all files 
     *                 should be included. If a non-<code>null</code>
     *                 list is given, all elements must be 
     * non-<code>null</code>.
     */
    void setIncludes(String[] includes);

    /**
     * Sets whether or not the file system should be regarded as case sensitive.
     *
     * @param isCaseSensitive whether or not the file system should be 
     *                        regarded as a case sensitive one
     */
    void setCaseSensitive(boolean isCaseSensitive);
}
