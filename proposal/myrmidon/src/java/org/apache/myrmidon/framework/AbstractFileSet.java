/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

/**
 * An abstract base class for all FileSets.
 * FileSets represent a pattern anchored by a root.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class AbstractFileSet
    extends PatternSet
{
    private boolean m_defaultExcludes = true;

    /**
     * Add the default excludes to FileSet.
     */
    public final void setDefaultExcludes( final boolean defaultExcludes )
    {
        m_defaultExcludes = defaultExcludes;
    }

    public final boolean includeDefaultExcludes()
    {
        return m_defaultExcludes;
    }

    /**
     * Merge specified PatternSet into this patternSet.
     */
    public final void addPatternSet( final PatternSet set )
    {
        append( set );
    }
}
