/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util.regexp;
import java.util.Vector;
import org.apache.regexp.RE;
import org.apache.tools.ant.BuildException;

/**
 * Regular expression implementation using the Jakarta Regexp package
 *
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">
 *      mattinger@mindless.com</a>
 */
public class JakartaRegexpRegexp extends JakartaRegexpMatcher implements Regexp
{

    public JakartaRegexpRegexp()
    {
        super();
    }

    public String substitute( String input, String argument, int options )
        throws BuildException
    {
        Vector v = getGroups( input, options );

        // replace \1 with the corresponding group
        StringBuffer result = new StringBuffer();
        for( int i = 0; i < argument.length(); i++ )
        {
            char c = argument.charAt( i );
            if( c == '\\' )
            {
                if( ++i < argument.length() )
                {
                    c = argument.charAt( i );
                    int value = Character.digit( c, 10 );
                    if( value > -1 )
                    {
                        result.append( ( String )v.elementAt( value ) );
                    }
                    else
                    {
                        result.append( c );
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
                result.append( c );
            }
        }
        argument = result.toString();

        RE reg = getCompiledPattern( options );
        int sOptions = getSubsOptions( options );
        return reg.subst( input, argument, sOptions );
    }

    protected int getSubsOptions( int options )
    {
        int subsOptions = RE.REPLACE_FIRSTONLY;
        if( RegexpUtil.hasFlag( options, REPLACE_ALL ) )
            subsOptions = RE.REPLACE_ALL;
        return subsOptions;
    }
}
