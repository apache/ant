/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Pattern;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * Named collection of include/exclude tags. <p>
 *
 * Moved out of MatchingTask to make it a standalone object that could be
 * referenced (by scripts for example).
 *
 * @author Arnout J. Kuiper <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class PatternSet
    extends ProjectComponent
{
    private ArrayList m_includeList = new ArrayList();
    private ArrayList m_excludeList = new ArrayList();
    private ArrayList m_includesFileList = new ArrayList();
    private ArrayList m_excludesFileList = new ArrayList();

    public PatternSet()
    {
        super();
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes( String excludes )
    {
        if( excludes != null && excludes.length() > 0 )
        {
            StringTokenizer tok = new StringTokenizer( excludes, ", ", false );
            while( tok.hasMoreTokens() )
            {
                createExclude().setName( tok.nextToken() );
            }
        }
    }

    /**
     * Sets the name of the file containing the excludes patterns.
     *
     * @param excludesFile The file to fetch the exclude patterns from.
     * @exception TaskException Description of Exception
     */
    public void setExcludesfile( File excludesFile )
    {
        createExcludesFile().setName( excludesFile.getAbsolutePath() );
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes( String includes )
    {
        if( includes != null && includes.length() > 0 )
        {
            StringTokenizer tok = new StringTokenizer( includes, ", ", false );
            while( tok.hasMoreTokens() )
            {
                createInclude().setName( tok.nextToken() );
            }
        }
    }

    /**
     * Sets the name of the file containing the includes patterns.
     */
    public void setIncludesfile( File includesFile )
    {
        createIncludesFile().setName( includesFile.getAbsolutePath() );
    }

    public String[] getExcludePatterns( final TaskContext context )
        throws TaskException
    {
        readFiles( context );
        return makeArray( m_excludeList, context );
    }

    /**
     * Returns the filtered include patterns.
     */
    public String[] getIncludePatterns( final TaskContext context )
        throws TaskException
    {
        readFiles( context );
        return makeArray( m_includeList, context );
    }

    /**
     * Adds the patterns of the other instance to this set.
     */
    protected void append( PatternSet other )
        throws TaskException
    {
        String[] incl = other.getIncludePatterns( (TaskContext)null );
        if( incl != null )
        {
            for( int i = 0; i < incl.length; i++ )
            {
                createInclude().setName( incl[ i ] );
            }
        }

        String[] excl = other.getExcludePatterns( (TaskContext)null );
        if( excl != null )
        {
            for( int i = 0; i < excl.length; i++ )
            {
                createExclude().setName( excl[ i ] );
            }
        }
    }

    /**
     * add a name entry on the exclude list
     *
     * @return Description of the Returned Value
     */
    public Pattern createExclude()
    {
        return addPatternToList( m_excludeList );
    }

    /**
     * add a name entry on the exclude files list
     *
     * @return Description of the Returned Value
     */
    public Pattern createExcludesFile()
    {
        return addPatternToList( m_excludesFileList );
    }

    /**
     * add a name entry on the include list
     */
    public Pattern createInclude()
    {
        return addPatternToList( m_includeList );
    }

    /**
     * add a name entry on the include files list
     */
    public Pattern createIncludesFile()
    {
        return addPatternToList( m_includesFileList );
    }

    public String toString()
    {
        return "patternSet{ includes: " + m_includeList +
            " excludes: " + m_excludeList + " }";
    }

    /**
     * helper for FileSet.
     */
    boolean hasPatterns()
    {
        return m_includesFileList.size() > 0 || m_excludesFileList.size() > 0 ||
            m_includeList.size() > 0 || m_excludeList.size() > 0;
    }

    /**
     * add a name entry to the given list
     */
    private Pattern addPatternToList( final ArrayList list )
    {
        final Pattern result = new Pattern();
        list.add( result );
        return result;
    }

    /**
     * Convert a vector of Pattern elements into an array of Strings.
     */
    private String[] makeArray( final ArrayList list, final TaskContext context )
    {
        if( list.size() == 0 )
        {
            return null;
        }

        final ArrayList tmpNames = new ArrayList();
        for( Iterator e = list.iterator(); e.hasNext(); )
        {
            final Pattern ne = (Pattern)e.next();
            final String pattern = ne.evaluateName( context );
            if( pattern != null && pattern.length() > 0 )
            {
                tmpNames.add( pattern );
            }
        }

        final String[] result = new String[ tmpNames.size() ];
        return (String[])tmpNames.toArray( result );
    }

    /**
     * Read includesfile ot excludesfile if not already done so.
     */
    private void readFiles( final TaskContext context )
        throws TaskException
    {
        if( m_includesFileList.size() > 0 )
        {
            Iterator e = m_includesFileList.iterator();
            while( e.hasNext() )
            {
                Pattern ne = (Pattern)e.next();
                String fileName = ne.evaluateName( (TaskContext)null );
                if( fileName != null )
                {
                    File inclFile = resolveFile( fileName );
                    if( !inclFile.exists() )
                    {
                        throw new TaskException( "Includesfile "
                                                 + inclFile.getAbsolutePath()
                                                 + " not found." );
                    }
                    readPatterns( inclFile, m_includeList, null );
                }
            }
            m_includesFileList.clear();
        }

        if( m_excludesFileList.size() > 0 )
        {
            Iterator e = m_excludesFileList.iterator();
            while( e.hasNext() )
            {
                Pattern ne = (Pattern)e.next();
                String fileName = ne.evaluateName( null );
                if( fileName != null )
                {
                    File exclFile = resolveFile( fileName );
                    if( !exclFile.exists() )
                    {
                        throw new TaskException( "Excludesfile "
                                                 + exclFile.getAbsolutePath()
                                                 + " not found." );
                    }
                    readPatterns( exclFile, m_excludeList, null );
                }
            }
            m_excludesFileList.clear();
        }
    }

    /**
     * Reads path matching patterns from a file and adds them to the includes or
     * excludes list (as appropriate).
     *
     * @param patternfile Description of Parameter
     * @param patternlist Description of Parameter
     * @param p Description of Parameter
     * @exception TaskException Description of Exception
     */
    private void readPatterns( File patternfile, ArrayList patternlist, Project p )
        throws TaskException
    {

        BufferedReader patternReader = null;
        try
        {
            // Get a FileReader
            patternReader =
                new BufferedReader( new FileReader( patternfile ) );

            // Create one Pattern in the appropriate pattern list for each
            // line in the file.
            String line = patternReader.readLine();
            while( line != null )
            {
                if( line.length() > 0 )
                {
                    line = p.replaceProperties( line );
                    addPatternToList( patternlist ).setName( line );
                }
                line = patternReader.readLine();
            }
        }
        catch( IOException ioe )
        {
            String msg = "An error occured while reading from pattern file: "
                + patternfile;
            throw new TaskException( msg, ioe );
        }
        finally
        {
            if( null != patternReader )
            {
                try
                {
                    patternReader.close();
                }
                catch( IOException ioe )
                {
                    //Ignore exception
                }
            }
        }
    }

}
