/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util;

/**
 * Implementation of FileNameMapper that does simple wildcard pattern
 * replacements. <p>
 *
 * This does simple translations like *.foo -> *.bar where the prefix to .foo
 * will be left unchanged. It only handles a single * character, use regular
 * expressions for more complicated situations.</p> <p>
 *
 * This is one of the more useful Mappers, it is used by javac for example.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class GlobPatternMapper implements FileNameMapper
{
    /**
     * Part of &quot;from&quot; pattern before the *.
     */
    protected String fromPrefix = null;

    /**
     * Part of &quot;from&quot; pattern after the *.
     */
    protected String fromPostfix = null;

    /**
     * Part of &quot;to&quot; pattern before the *.
     */
    protected String toPrefix = null;

    /**
     * Part of &quot;to&quot; pattern after the *.
     */
    protected String toPostfix = null;

    /**
     * Length of the postfix (&quot;from&quot; pattern).
     */
    protected int postfixLength;

    /**
     * Length of the prefix (&quot;from&quot; pattern).
     */
    protected int prefixLength;

    /**
     * Sets the &quot;from&quot; pattern. Required.
     *
     * @param from The new From value
     */
    public void setFrom( String from )
    {
        int index = from.lastIndexOf( "*" );
        if( index == -1 )
        {
            fromPrefix = from;
            fromPostfix = "";
        }
        else
        {
            fromPrefix = from.substring( 0, index );
            fromPostfix = from.substring( index + 1 );
        }
        prefixLength = fromPrefix.length();
        postfixLength = fromPostfix.length();
    }

    /**
     * Sets the &quot;to&quot; pattern. Required.
     *
     * @param to The new To value
     */
    public void setTo( String to )
    {
        int index = to.lastIndexOf( "*" );
        if( index == -1 )
        {
            toPrefix = to;
            toPostfix = "";
        }
        else
        {
            toPrefix = to.substring( 0, index );
            toPostfix = to.substring( index + 1 );
        }
    }

    /**
     * Returns null if the source file name doesn't match the &quot;from&quot;
     * pattern, an one-element array containing the translated file otherwise.
     *
     * @param sourceFileName Description of Parameter
     * @return Description of the Returned Value
     */
    public String[] mapFileName( String sourceFileName )
    {
        if( fromPrefix == null
            || !sourceFileName.startsWith( fromPrefix )
            || !sourceFileName.endsWith( fromPostfix ) )
        {
            return null;
        }
        return new String[]{toPrefix
            + extractVariablePart( sourceFileName )
            + toPostfix};
    }

    /**
     * Returns the part of the given string that matches the * in the
     * &quot;from&quot; pattern.
     *
     * @param name Description of Parameter
     * @return Description of the Returned Value
     */
    protected String extractVariablePart( String name )
    {
        return name.substring( prefixLength,
                               name.length() - postfixLength );
    }
}
