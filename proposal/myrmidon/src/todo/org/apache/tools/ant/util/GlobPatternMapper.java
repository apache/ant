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
public class GlobPatternMapper
    implements FileNameMapper
{
    /**
     * Part of &quot;from&quot; pattern before the *.
     */
    private String m_fromPrefix;

    /**
     * Part of &quot;from&quot; pattern after the *.
     */
    private String m_fromPostfix;

    /**
     * Part of &quot;to&quot; pattern before the *.
     */
    private String m_toPrefix;

    /**
     * Part of &quot;to&quot; pattern after the *.
     */
    private String m_toPostfix;

    /**
     * Length of the postfix (&quot;from&quot; pattern).
     */
    private int m_postfixLength;

    /**
     * Length of the prefix (&quot;from&quot; pattern).
     */
    private int m_prefixLength;

    /**
     * Sets the &quot;from&quot; pattern. Required.
     *
     * @param from The new From value
     */
    public void setFrom( final String from )
    {
        final int index = from.lastIndexOf( "*" );
        if( index == -1 )
        {
            m_fromPrefix = from;
            m_fromPostfix = "";
        }
        else
        {
            m_fromPrefix = from.substring( 0, index );
            m_fromPostfix = from.substring( index + 1 );
        }
        m_prefixLength = m_fromPrefix.length();
        m_postfixLength = m_fromPostfix.length();
    }

    /**
     * Sets the &quot;to&quot; pattern. Required.
     *
     * @param to The new To value
     */
    public void setTo( final String to )
    {
        final int index = to.lastIndexOf( "*" );
        if( index == -1 )
        {
            m_toPrefix = to;
            m_toPostfix = "";
        }
        else
        {
            m_toPrefix = to.substring( 0, index );
            m_toPostfix = to.substring( index + 1 );
        }
    }

    /**
     * Returns null if the source file name doesn't match the &quot;from&quot;
     * pattern, an one-element array containing the translated file otherwise.
     *
     * @param sourceFileName Description of Parameter
     * @return Description of the Returned Value
     */
    public String[] mapFileName( final String sourceFileName )
    {
        if( m_fromPrefix == null ||
            !sourceFileName.startsWith( m_fromPrefix ) ||
            !sourceFileName.endsWith( m_fromPostfix ) )
        {
            return null;
        }
        else
        {
            final String result = m_toPrefix +
                extractVariablePart( sourceFileName ) + m_toPostfix;
            return new String[]{result};
        }
    }

    /**
     * Returns the part of the given string that matches the * in the
     * &quot;from&quot; pattern.
     *
     * @param name Description of Parameter
     * @return Description of the Returned Value
     */
    protected String extractVariablePart( final String name )
    {
        return name.substring( m_prefixLength,
                               name.length() - m_postfixLength );
    }
}
