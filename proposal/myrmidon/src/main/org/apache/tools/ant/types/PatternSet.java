/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

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
public class PatternSet extends DataType
{
    private Vector includeList = new Vector();
    private Vector excludeList = new Vector();
    private Vector includesFileList = new Vector();
    private Vector excludesFileList = new Vector();

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
        if( isReference() )
        {
            throw tooManyAttributes();
        }
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
     * @exception BuildException Description of Exception
     */
    public void setExcludesfile( File excludesFile )
        throws BuildException
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
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
        if( isReference() )
        {
            throw tooManyAttributes();
        }
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
     *
     * @param includesFile The file to fetch the include patterns from.
     * @exception BuildException Description of Exception
     */
    public void setIncludesfile( File includesFile )
        throws BuildException
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        createIncludesFile().setName( includesFile.getAbsolutePath() );
    }

    /**
     * Makes this instance in effect a reference to another PatternSet instance.
     * <p>
     *
     * You must not set another attribute or nest elements inside this element
     * if you make it a reference.</p>
     *
     * @param r The new Refid value
     * @exception BuildException Description of Exception
     */
    public void setRefid( Reference r )
        throws BuildException
    {
        if( !includeList.isEmpty() || !excludeList.isEmpty() )
        {
            throw tooManyAttributes();
        }
        super.setRefid( r );
    }

    /**
     * Returns the filtered include patterns.
     *
     * @param p Description of Parameter
     * @return The ExcludePatterns value
     */
    public String[] getExcludePatterns( Project p )
    {
        if( isReference() )
        {
            return getRef( p ).getExcludePatterns( p );
        }
        else
        {
            readFiles( p );
            return makeArray( excludeList, p );
        }
    }

    /**
     * Returns the filtered include patterns.
     *
     * @param p Description of Parameter
     * @return The IncludePatterns value
     */
    public String[] getIncludePatterns( Project p )
    {
        if( isReference() )
        {
            return getRef( p ).getIncludePatterns( p );
        }
        else
        {
            readFiles( p );
            return makeArray( includeList, p );
        }
    }

    /**
     * Adds the patterns of the other instance to this set.
     *
     * @param other Description of Parameter
     * @param p Description of Parameter
     */
    public void append( PatternSet other, Project p )
    {
        if( isReference() )
        {
            throw new BuildException( "Cannot append to a reference" );
        }

        String[] incl = other.getIncludePatterns( p );
        if( incl != null )
        {
            for( int i = 0; i < incl.length; i++ )
            {
                createInclude().setName( incl[i] );
            }
        }

        String[] excl = other.getExcludePatterns( p );
        if( excl != null )
        {
            for( int i = 0; i < excl.length; i++ )
            {
                createExclude().setName( excl[i] );
            }
        }
    }

    /**
     * add a name entry on the exclude list
     *
     * @return Description of the Returned Value
     */
    public NameEntry createExclude()
    {
        if( isReference() )
        {
            throw noChildrenAllowed();
        }
        return addPatternToList( excludeList );
    }

    /**
     * add a name entry on the exclude files list
     *
     * @return Description of the Returned Value
     */
    public NameEntry createExcludesFile()
    {
        if( isReference() )
        {
            throw noChildrenAllowed();
        }
        return addPatternToList( excludesFileList );
    }

    /**
     * add a name entry on the include list
     *
     * @return Description of the Returned Value
     */
    public NameEntry createInclude()
    {
        if( isReference() )
        {
            throw noChildrenAllowed();
        }
        return addPatternToList( includeList );
    }

    /**
     * add a name entry on the include files list
     *
     * @return Description of the Returned Value
     */
    public NameEntry createIncludesFile()
    {
        if( isReference() )
        {
            throw noChildrenAllowed();
        }
        return addPatternToList( includesFileList );
    }

    public String toString()
    {
        return "patternSet{ includes: " + includeList +
            " excludes: " + excludeList + " }";
    }

    /**
     * helper for FileSet.
     *
     * @return Description of the Returned Value
     */
    boolean hasPatterns()
    {
        return includesFileList.size() > 0 || excludesFileList.size() > 0
             || includeList.size() > 0 || excludeList.size() > 0;
    }

    /**
     * Performs the check for circular references and returns the referenced
     * PatternSet.
     *
     * @param p Description of Parameter
     * @return The Ref value
     */
    private PatternSet getRef( Project p )
    {
        if( !checked )
        {
            Stack stk = new Stack();
            stk.push( this );
            dieOnCircularReference( stk, p );
        }

        Object o = ref.getReferencedObject( p );
        if( !( o instanceof PatternSet ) )
        {
            String msg = ref.getRefId() + " doesn\'t denote a patternset";
            throw new BuildException( msg );
        }
        else
        {
            return ( PatternSet )o;
        }
    }

    /**
     * add a name entry to the given list
     *
     * @param list The feature to be added to the PatternToList attribute
     * @return Description of the Returned Value
     */
    private NameEntry addPatternToList( Vector list )
    {
        NameEntry result = new NameEntry();
        list.addElement( result );
        return result;
    }

    /**
     * Convert a vector of NameEntry elements into an array of Strings.
     *
     * @param list Description of Parameter
     * @param p Description of Parameter
     * @return Description of the Returned Value
     */
    private String[] makeArray( Vector list, Project p )
    {
        if( list.size() == 0 )
            return null;

        Vector tmpNames = new Vector();
        for( Enumeration e = list.elements(); e.hasMoreElements();  )
        {
            NameEntry ne = ( NameEntry )e.nextElement();
            String pattern = ne.evalName( p );
            if( pattern != null && pattern.length() > 0 )
            {
                tmpNames.addElement( pattern );
            }
        }

        String result[] = new String[tmpNames.size()];
        tmpNames.copyInto( result );
        return result;
    }

    /**
     * Read includesfile ot excludesfile if not already done so.
     *
     * @param p Description of Parameter
     */
    private void readFiles( Project p )
    {
        if( includesFileList.size() > 0 )
        {
            Enumeration e = includesFileList.elements();
            while( e.hasMoreElements() )
            {
                NameEntry ne = ( NameEntry )e.nextElement();
                String fileName = ne.evalName( p );
                if( fileName != null )
                {
                    File inclFile = p.resolveFile( fileName );
                    if( !inclFile.exists() )
                        throw new BuildException( "Includesfile "
                             + inclFile.getAbsolutePath()
                             + " not found." );
                    readPatterns( inclFile, includeList, p );
                }
            }
            includesFileList.removeAllElements();
        }

        if( excludesFileList.size() > 0 )
        {
            Enumeration e = excludesFileList.elements();
            while( e.hasMoreElements() )
            {
                NameEntry ne = ( NameEntry )e.nextElement();
                String fileName = ne.evalName( p );
                if( fileName != null )
                {
                    File exclFile = p.resolveFile( fileName );
                    if( !exclFile.exists() )
                        throw new BuildException( "Excludesfile "
                             + exclFile.getAbsolutePath()
                             + " not found." );
                    readPatterns( exclFile, excludeList, p );
                }
            }
            excludesFileList.removeAllElements();
        }
    }

    /**
     * Reads path matching patterns from a file and adds them to the includes or
     * excludes list (as appropriate).
     *
     * @param patternfile Description of Parameter
     * @param patternlist Description of Parameter
     * @param p Description of Parameter
     * @exception BuildException Description of Exception
     */
    private void readPatterns( File patternfile, Vector patternlist, Project p )
        throws BuildException
    {

        BufferedReader patternReader = null;
        try
        {
            // Get a FileReader
            patternReader =
                new BufferedReader( new FileReader( patternfile ) );

            // Create one NameEntry in the appropriate pattern list for each
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
            throw new BuildException( msg, ioe );
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

    /**
     * inner class to hold a name on list. "If" and "Unless" attributes may be
     * used to invalidate the entry based on the existence of a property
     * (typically set thru the use of the Available task).
     *
     * @author RT
     */
    public class NameEntry
    {
        private String ifCond;
        private String name;
        private String unlessCond;

        public void setIf( String cond )
        {
            ifCond = cond;
        }

        public void setName( String name )
        {
            this.name = name;
        }

        public void setUnless( String cond )
        {
            unlessCond = cond;
        }

        public String getName()
        {
            return name;
        }

        public String evalName( Project p )
        {
            return valid( p ) ? name : null;
        }

        public String toString()
        {
            StringBuffer buf = new StringBuffer( name );
            if( ( ifCond != null ) || ( unlessCond != null ) )
            {
                buf.append( ":" );
                String connector = "";

                if( ifCond != null )
                {
                    buf.append( "if->" );
                    buf.append( ifCond );
                    connector = ";";
                }
                if( unlessCond != null )
                {
                    buf.append( connector );
                    buf.append( "unless->" );
                    buf.append( unlessCond );
                }
            }

            return buf.toString();
        }

        private boolean valid( Project p )
        {
            if( ifCond != null && p.getProperty( ifCond ) == null )
            {
                return false;
            }
            else if( unlessCond != null && p.getProperty( unlessCond ) != null )
            {
                return false;
            }
            return true;
        }
    }

}
