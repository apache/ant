/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util;

import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;

/**
 * Implementation of FileNameMapper that does regular expression replacements.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class RegexpPatternMapper implements FileNameMapper
{
    protected RegexpMatcher reg = null;
    protected char[] to = null;
    protected StringBuffer result = new StringBuffer();

    public RegexpPatternMapper()
        throws TaskException
    {
        reg = ( new RegexpMatcherFactory() ).newRegexpMatcher();
    }

    /**
     * Sets the &quot;from&quot; pattern. Required.
     *
     * @param from The new From value
     * @exception TaskException Description of Exception
     */
    public void setFrom( String from )
        throws TaskException
    {
        try
        {
            reg.setPattern( from );
        }
        catch( NoClassDefFoundError e )
        {
            // depending on the implementation the actual RE won't
            // get instantiated in the constructor.
            throw new TaskException( "Cannot load regular expression matcher",
                                     e );
        }
    }

    /**
     * Sets the &quot;to&quot; pattern. Required.
     *
     * @param to The new To value
     */
    public void setTo( String to )
    {
        this.to = to.toCharArray();
    }

    /**
     * Returns null if the source file name doesn't match the &quot;from&quot;
     * pattern, an one-element array containing the translated file otherwise.
     *
     * @param sourceFileName Description of Parameter
     * @return Description of the Returned Value
     */
    public String[] mapFileName( String sourceFileName )
        throws TaskException
    {
        if( reg == null || to == null || !reg.matches( sourceFileName ) )
        {
            return null;
        }
        return new String[]{replaceReferences( sourceFileName )};
    }

    /**
     * Replace all backreferences in the to pattern with the matched groups of
     * the source.
     *
     * @param source Description of Parameter
     * @return Description of the Returned Value
     */
    protected String replaceReferences( String source )
        throws TaskException
    {
        ArrayList v = reg.getGroups( source );

        result.setLength( 0 );
        for( int i = 0; i < to.length; i++ )
        {
            if( to[ i ] == '\\' )
            {
                if( ++i < to.length )
                {
                    int value = Character.digit( to[ i ], 10 );
                    if( value > -1 )
                    {
                        result.append( (String)v.get( value ) );
                    }
                    else
                    {
                        result.append( to[ i ] );
                    }
                }
                else
                {
                    // XXX - should throw an exception instead?
                    result.append( '\\' );
                }
            }
            else
            {
                result.append( to[ i ] );
            }
        }
        return result.toString();
    }

}
