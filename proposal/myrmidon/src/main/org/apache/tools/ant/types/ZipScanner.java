/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types;
import java.io.File;
import org.apache.tools.ant.DirectoryScanner;

/**
 * ZipScanner accesses the pattern matching algorithm in DirectoryScanner, which
 * are protected methods that can only be accessed by subclassing. This
 * implementation of FileScanner defines getIncludedFiles to return only the Zip
 * File which is being scanned, not the matching Zip entries. Arguably, it
 * should return the matching entries, however this would complicate existing
 * code which assumes that FileScanners return a set of file system files that
 * can be accessed directly.
 *
 * @author Don Ferguson <a href="mailto:don@bea.com">don@bea.com</a>
 */
public class ZipScanner extends DirectoryScanner
{

    /**
     * The zip file which should be scanned.
     */
    protected File srcFile;

    /**
     * Sets the srcFile for scanning. This is the jar or zip file that is
     * scanned for matching entries.
     *
     * @param srcFile the (non-null) zip file name for scanning
     */
    public void setSrc( File srcFile )
    {
        this.srcFile = srcFile;
    }

    /**
     * Returns an empty list of directories to create.
     *
     * @return The IncludedDirectories value
     */
    public String[] getIncludedDirectories()
    {
        return new String[0];
    }

    /**
     * Returns the zip file itself, not the matching entries within the zip
     * file. This keeps the uptodate test in the Zip task simple; otherwise we'd
     * need to treat zip filesets specially.
     *
     * @return the source file from which entries will be extracted.
     */
    public String[] getIncludedFiles()
    {
        String[] result = new String[1];
        result[0] = srcFile.getAbsolutePath();
        return result;
    }

    /**
     * Initialize DirectoryScanner data structures.
     */
    public void init()
    {
        if( includes == null )
        {
            // No includes supplied, so set it to 'matches all'
            includes = new String[1];
            includes[0] = "**";
        }
        if( excludes == null )
        {
            excludes = new String[0];
        }
    }

    /**
     * Matches a jar entry against the includes/excludes list, normalizing the
     * path separator.
     *
     * @param path the (non-null) path name to test for inclusion
     * @return <code>true</code> if the path should be included <code>false</code>
     *      otherwise.
     */
    public boolean match( String path )
    {
        String vpath = path.replace( '/', File.separatorChar ).
            replace( '\\', File.separatorChar );
        return isIncluded( vpath ) && !isExcluded( vpath );
    }

}
