/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Pattern;
import org.apache.tools.ant.ProjectComponent;

/**
 * Moved out of MatchingTask to make it a standalone object that could be
 * referenced (by scripts for example).
 *
 * @author Arnout J. Kuiper <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class FileSet
    extends ProjectComponent
    implements Cloneable
{
    private PatternSet m_defaultPatterns = new PatternSet();
    private ArrayList m_additionalPatterns = new ArrayList();
    private boolean m_useDefaultExcludes = true;
    private boolean m_isCaseSensitive = true;
    private File m_dir;

    /**
     * Sets case sensitivity of the file system
     */
    public void setCaseSensitive( final boolean isCaseSensitive )
    {
        m_isCaseSensitive = isCaseSensitive;
    }

    /**
     * Sets whether default exclusions should be used or not.
     */
    public void setDefaultexcludes( final boolean useDefaultExcludes )
    {
        m_useDefaultExcludes = useDefaultExcludes;
    }

    public void setDir( final File dir )
        throws TaskException
    {
        m_dir = dir;
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes( final String excludes )
    {
        m_defaultPatterns.setExcludes( excludes );
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes( final String includes )
    {
        m_defaultPatterns.setIncludes( includes );
    }

    public void setupDirectoryScanner( final FileScanner ds )
        throws TaskException
    {
        if( null == ds )
        {
            final String message = "ds cannot be null";
            throw new IllegalArgumentException( message );
        }

        ds.setBasedir( m_dir );

        final int size = m_additionalPatterns.size();
        for( int i = 0; i < size; i++ )
        {
            final Object o = m_additionalPatterns.get( i );
            m_defaultPatterns.append( (PatternSet)o );
        }

        final String message = "FileSet: Setup file scanner in dir " +
            m_dir + " with " + m_defaultPatterns;
        getLogger().debug( message );

        ds.setIncludes( m_defaultPatterns.getIncludePatterns( getContext() ) );
        ds.setExcludes( m_defaultPatterns.getExcludePatterns( getContext() ) );
        if( m_useDefaultExcludes )
        {
            ds.addDefaultExcludes();
        }
        ds.setCaseSensitive( m_isCaseSensitive );
    }

    public File getDir()
    {
        return m_dir;
    }

    /**
     * Returns the directory scanner needed to access the files to process.
     */
    public DirectoryScanner getDirectoryScanner()
        throws TaskException
    {
        if( m_dir == null )
        {
            throw new TaskException( "No directory specified for fileset." );
        }

        if( !m_dir.exists() )
        {
            throw new TaskException( m_dir.getAbsolutePath() + " not found." );
        }
        if( !m_dir.isDirectory() )
        {
            throw new TaskException( m_dir.getAbsolutePath() + " is not a directory." );
        }

        final DirectoryScanner scanner = new DirectoryScanner();
        setupDirectoryScanner( scanner );
        scanner.scan();
        return scanner;
    }

    /**
     * add a name entry on the exclude list
     */
    public void addExclude( final Pattern pattern )
    {
        m_defaultPatterns.addExclude( pattern );
    }

    /**
     * add a name entry on the include list
     */
    public void addInclude( final Pattern pattern )
    {
        m_defaultPatterns.addInclude( pattern );
    }

    public PatternSet createPatternSet()
    {
        final PatternSet patterns = new PatternSet();
        m_additionalPatterns.add( patterns );
        return patterns;
    }
}
