/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.tools.ant.types.FileSet;

/**
 * An abstract base class for tasks that wish to operate on
 * a set of files. This class is based on the ant1.x MatchingTask and
 * should fullfill similar requirements.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractMatchingTask
    extends AbstractTask
{
    private FileSet m_fileset = new FileSet();

    /**
     * The attribute that contains a list of itesm to be included.
     */
    public void setIncludes( final String includes )
    {
        m_fileset.setIncludes( includes );
    }

    /**
     * The attribute that contains a list of items to be excluded.
     */
    public void setExcludes( final String excludes )
    {
        m_fileset.setExcludes( excludes );
    }

    /**
     * Set this to true to use the defaul exclude patterns.
     */
    public void setDefaultexcludes( final boolean useDefaultExcludes )
    {
        m_fileset.setDefaultExcludes( useDefaultExcludes );
    }

    public void addInclude( final Pattern pattern )
    {
        m_fileset.addInclude( pattern );
    }

    public void addExclude( final Pattern pattern )
    {
        m_fileset.addExclude( pattern );
    }

    public void addPatternSet( final PatternSet set )
    {
        m_fileset.addPatternSet( set );
    }
}
