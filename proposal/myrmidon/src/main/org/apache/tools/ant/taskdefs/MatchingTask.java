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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.NameEntry;
import org.apache.tools.ant.types.PatternSet;

/**
 * This is an abstract task that should be used by all those tasks that require
 * to include or exclude files based on pattern matching.
 *
 * @author Arnout J. Kuiper <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public abstract class MatchingTask
    extends Task
{
    private boolean m_useDefaultExcludes = true;
    private FileSet m_fileset = new FileSet();

    /**
     * Sets whether default exclusions should be used or not.
     */
    public void setDefaultexcludes( final boolean useDefaultExcludes )
    {
        m_useDefaultExcludes = useDefaultExcludes;
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes( final String excludes )
        throws TaskException
    {
        m_fileset.setExcludes( excludes );
    }

    /**
     * Sets the name of the file containing the includes patterns.
     *
     * @param excludesfile A string containing the filename to fetch the include
     *      patterns from.
     */
    public void setExcludesfile( final File excludesfile )
        throws TaskException
    {
        m_fileset.setExcludesfile( excludesfile );
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes( final String includes )
        throws TaskException
    {
        m_fileset.setIncludes( includes );
    }

    /**
     * Sets the name of the file containing the includes patterns.
     *
     * @param includesfile A string containing the filename to fetch the include
     *      patterns from.
     */
    public void setIncludesfile( final File includesfile )
        throws TaskException
    {
        m_fileset.setIncludesfile( includesfile );
    }

    /**
     * add a name entry on the exclude list
     *
     * @return Description of the Returned Value
     */
    public NameEntry createExclude()
        throws TaskException
    {
        return m_fileset.createExclude();
    }

    /**
     * add a name entry on the include files list
     *
     * @return Description of the Returned Value
     */
    public NameEntry createExcludesFile()
        throws TaskException
    {
        return m_fileset.createExcludesFile();
    }

    /**
     * add a name entry on the include list
     *
     * @return Description of the Returned Value
     */
    public NameEntry createInclude()
        throws TaskException
    {
        return m_fileset.createInclude();
    }

    /**
     * add a name entry on the include files list
     *
     * @return Description of the Returned Value
     */
    public NameEntry createIncludesFile()
        throws TaskException
    {
        return m_fileset.createIncludesFile();
    }

    /**
     * add a set of patterns
     *
     * @return Description of the Returned Value
     */
    public PatternSet createPatternSet()
        throws TaskException
    {
        return m_fileset.createPatternSet();
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
        m_fileset.setDefaultexcludes( m_useDefaultExcludes );
        return m_fileset.getDirectoryScanner();
    }
}
