/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.util.regexp;

import org.apache.myrmidon.api.TaskException;
import org.apache.oro.text.regex.Perl5Substitution;
import org.apache.oro.text.regex.Substitution;
import org.apache.oro.text.regex.Util;
import org.apache.tools.todo.util.regexp.JakartaOroMatcher;

/**
 * Regular expression implementation using the Jakarta Oro package
 *
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">
 *      mattinger@mindless.com</a>
 */
public class JakartaOroRegexp extends JakartaOroMatcher implements Regexp
{

    public JakartaOroRegexp()
    {
        super();
    }

    public String substitute( String input, String argument, int options )
        throws TaskException
    {
        // translate \1 to $1 so that the Perl5Substitution will work
        StringBuffer subst = new StringBuffer();
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
                        subst.append( "$" ).append( value );
                    }
                    else
                    {
                        subst.append( c );
                    }
                }
                else
                {
                    // XXX - should throw an exception instead?
                    subst.append( '\\' );
                }
            }
            else
            {
                subst.append( c );
            }
        }

        // Do the substitution
        Substitution s =
            new Perl5Substitution( subst.toString(),
                                   Perl5Substitution.INTERPOLATE_ALL );
        return Util.substitute( matcher,
                                getCompiledPattern( options ),
                                s,
                                input,
                                getSubsOptions( options ) );
    }

    protected int getSubsOptions( int options )
    {
        boolean replaceAll = RegexpUtil.hasFlag( options, REPLACE_ALL );
        int subsOptions = 1;
        if( replaceAll )
        {
            subsOptions = Util.SUBSTITUTE_ALL;
        }
        return subsOptions;
    }

}
