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
package org.apache.tools.ant;

import java.io.*;

/**
 * An interface used to describe the actions required by any type of 
 * directory scanner.
 */
public interface FileScanner {
    /**
     * Adds an array with default exclusions to the current exclusions set.
     *
     */
    public void addDefaultExcludes();
    /**
     * Gets the basedir that is used for scanning. This is the directory that
     * is scanned recursively.
     *
     * @return the basedir that is used for scanning
     */
    public File getBasedir();
    /**
     * Get the names of the directories that matched at least one of the include
     * patterns, an matched also at least one of the exclude patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the directories
     */
    public String[] getExcludedDirectories();
    /**
     * Get the names of the files that matched at least one of the include
     * patterns, an matched also at least one of the exclude patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the files
     */
    public String[] getExcludedFiles();
    /**
     * Get the names of the directories that matched at least one of the include
     * patterns, an matched none of the exclude patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the directories
     */
    public String[] getIncludedDirectories();
    /**
     * Get the names of the files that matched at least one of the include
     * patterns, an matched none of the exclude patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the files
     */
    public String[] getIncludedFiles();
    /**
     * Get the names of the directories that matched at none of the include
     * patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the directories
     */
    public String[] getNotIncludedDirectories();
    /**
     * Get the names of the files that matched at none of the include patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the files
     */
    public String[] getNotIncludedFiles();
    /**
     * Scans the base directory for files that match at least one include
     * pattern, and don't match any exclude patterns.
     *
     * @exception IllegalStateException when basedir was set incorrecly
     */
    public void scan();
    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively. 
     *
     * @param basedir the (non-null) basedir for scanning
     */
    public void setBasedir(String basedir);
    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively.
     *
     * @param basedir the basedir for scanning
     */
    public void setBasedir(File basedir);
    /**
     * Sets the set of exclude patterns to use.
     *
     * @param excludes list of exclude patterns
     */
    public void setExcludes(String[] excludes);
    /**
     * Sets the set of include patterns to use.
     *
     * @param includes list of include patterns
     */
    public void setIncludes(String[] includes);
}
