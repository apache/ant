/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Pattern;
import org.apache.myrmidon.framework.PatternSet;

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
{
    private File m_dir;
    private PatternSet m_patternSet = new PatternSet();
    private boolean m_useDefaultExcludes = true;
    private boolean m_isCaseSensitive = true;

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
        m_patternSet.setExcludes( excludes );
    }

    /**
     * add a name entry on the exclude list
     */
    public void addExclude( final Pattern pattern )
    {
        m_patternSet.addExclude( pattern );
    }

    /**
     * add a name entry on the include list
     */
    public void addInclude( final Pattern pattern )
    {
        m_patternSet.addInclude( pattern );
    }

    public void addPatternSet( final PatternSet set )
    {
        m_patternSet.append( set );
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes( final String includes )
    {
        m_patternSet.setIncludes( includes );
    }

    public final PatternSet getPatternSet()
    {
        return m_patternSet;
    }

    public boolean isCaseSensitive()
    {
        return m_isCaseSensitive;
    }

    public boolean useDefaultExcludes()
    {
        return m_useDefaultExcludes;
    }

    public File getDir()
    {
        return m_dir;
    }
}
