/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Pattern;
import org.apache.myrmidon.framework.PatternSet;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ScannerUtil;

/**
 * This is an abstract task that should be used by all those tasks that require
 * to include or exclude files based on pattern matching.
 *
 * @author <a href="mailto:ajkuiper@wxs.nl">Arnout J. Kuiper</a>
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public abstract class MatchingTask
    extends Task
{
    private FileSet m_fileset = new FileSet();

    /**
     * Sets whether default exclusions should be used or not.
     */
    public void setDefaultexcludes( final boolean useDefaultExcludes )
    {
        m_fileset.setDefaultExcludes( useDefaultExcludes );
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes( final String excludes )
    {
        m_fileset.setExcludes( excludes );
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes( final String includes )
    {
        m_fileset.setIncludes( includes );
    }

    /**
     * add a name entry on the exclude list
     */
    public void addExclude( final Pattern pattern )
    {
        m_fileset.addExclude( pattern );
    }

    /**
     * add a name entry on the include list
     */
    public void addInclude( final Pattern pattern )
        throws TaskException
    {
        m_fileset.addInclude( pattern );
    }

    /**
     * add a set of patterns
     */
    public void addPatternSet( final PatternSet set )
    {
        m_fileset.addPatternSet( set );
    }

    /**
     * Returns the directory scanner needed to access the files to process.
     *
     * @param baseDir Description of Parameter
     * @return The DirectoryScanner value
     */
    protected DirectoryScanner getDirectoryScanner( final File baseDir )
        throws TaskException
    {
        m_fileset.setDir( baseDir );
        return ScannerUtil.getDirectoryScanner( m_fileset );
    }
}
