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
