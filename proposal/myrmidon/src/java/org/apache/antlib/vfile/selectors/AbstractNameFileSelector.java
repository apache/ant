/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile.selectors;

import org.apache.antlib.vfile.FileSelector;
import org.apache.aut.vfs.FileObject;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * An abstract file selector that selects files based on name.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractNameFileSelector
    implements FileSelector
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( AbstractNameFileSelector.class );

    private Object m_type;
    private String m_pattern;

    private static final Object TYPE_GLOB = "glob";
    private static final Object TYPE_REGEXP = "regexp";

    /**
     * Sets the GLOB pattern to match the name against.
     */
    public void setPattern( final String pattern )
        throws TaskException
    {
        setPattern( TYPE_GLOB, pattern );
    }

    /**
     * Sets the Regexp pattern to match the file basename against.
     */
    public void setRegexp( final String pattern )
        throws TaskException
    {
        setPattern( TYPE_REGEXP, pattern );
    }

    /**
     * Sets the pattern and type to match
     */
    private void setPattern( final Object type, final String pattern )
        throws TaskException
    {
        if( m_type != null )
        {
            final String message = REZ.getString( "nameselector.too-many-patterns.error" );
            throw new TaskException( message );
        }
        m_type = type;
        m_pattern = pattern;
    }

    /**
     * Accepts the file.
     */
    public boolean accept( final FileObject file,
                           final String path,
                           final TaskContext context )
        throws TaskException
    {
        if( m_type == null )
        {
            final String message = REZ.getString( "nameselector.no-pattern.error" );
            throw new TaskException( message );
        }

        // Create the pattern to match against
        final Pattern pattern;
        try
        {
            if( m_type == TYPE_GLOB )
            {
                pattern = createGlobPattern( m_pattern );
            }
            else
            {
                pattern = createRegexpPattern( m_pattern );
            }
        }
        catch( MalformedPatternException e )
        {
            final String message = REZ.getString( "nameselector.bad-pattern.error", m_pattern );
            throw new TaskException( message );
        }

        // Get the name to match against
        final String name = getNameForMatch( path, file );

        // Compare the name against the pattern
        return new Perl5Matcher().matches( name, pattern );
    }

    /**
     * Creates a GLOB pattern for matching the name against.
     */
    protected Pattern createGlobPattern( final String pattern )
        throws MalformedPatternException
    {
        // TODO - need to implement Ant-style patterns
        return new GlobCompiler().compile( pattern );
    }

    /**
     * Creates a Regexp pattern for matching the name against.
     */
    protected Pattern createRegexpPattern( final String pattern )
        throws MalformedPatternException
    {
        return new Perl5Compiler().compile( pattern );
    }

    /**
     * Returns the name to match against.
     */
    protected abstract String getNameForMatch( final String path,
                                               final FileObject file )
        throws TaskException;
}
