/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.io.File;
import org.apache.myrmidon.api.TaskException;

/**
 * An interface used to describe the actions required by any type of directory
 * scanner.
 */
public interface FileScanner
{
    /**
     * Adds an array with default exclusions to the current exclusions set.
     */
    void addDefaultExcludes();

    /**
     * Gets the basedir that is used for scanning. This is the directory that is
     * scanned recursively.
     *
     * @return the basedir that is used for scanning
     */
    File getBasedir();

    /**
     * Get the names of the directories that matched at least one of the include
     * patterns, an matched also at least one of the exclude patterns. The names
     * are relative to the basedir.
     *
     * @return the names of the directories
     */
    String[] getExcludedDirectories() throws TaskException;

    /**
     * Get the names of the files that matched at least one of the include
     * patterns, an matched also at least one of the exclude patterns. The names
     * are relative to the basedir.
     *
     * @return the names of the files
     */
    String[] getExcludedFiles() throws TaskException;

    /**
     * Get the names of the directories that matched at least one of the include
     * patterns, an matched none of the exclude patterns. The names are relative
     * to the basedir.
     *
     * @return the names of the directories
     */
    String[] getIncludedDirectories();

    /**
     * Get the names of the files that matched at least one of the include
     * patterns, an matched none of the exclude patterns. The names are relative
     * to the basedir.
     *
     * @return the names of the files
     */
    String[] getIncludedFiles() throws TaskException;

    /**
     * Get the names of the directories that matched at none of the include
     * patterns. The names are relative to the basedir.
     *
     * @return the names of the directories
     */
    String[] getNotIncludedDirectories() throws TaskException;

    /**
     * Get the names of the files that matched at none of the include patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the files
     */
    String[] getNotIncludedFiles() throws TaskException;

    /**
     * Scans the base directory for files that match at least one include
     * pattern, and don't match any exclude patterns.
     *
     */
    void scan()
        throws TaskException;

    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively.
     *
     * @param basedir the (non-null) basedir for scanning
     */
    void setBasedir( String basedir );

    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively.
     *
     * @param basedir the basedir for scanning
     */
    void setBasedir( File basedir );

    /**
     * Sets the set of exclude patterns to use.
     *
     * @param excludes list of exclude patterns
     */
    void setExcludes( String[] excludes );

    /**
     * Sets the set of include patterns to use.
     *
     * @param includes list of include patterns
     */
    void setIncludes( String[] includes );

    /**
     * Sets the case sensitivity of the file system
     *
     * @param isCaseSensitive The new CaseSensitive value
     */
    void setCaseSensitive( boolean isCaseSensitive );
}
