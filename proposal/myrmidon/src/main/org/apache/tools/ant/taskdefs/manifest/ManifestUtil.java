/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.manifest;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility methods for manifest stuff.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class ManifestUtil
{
    public static Attribute buildAttribute( final String line )
        throws ManifestException
    {
        final Attribute attribute = new Attribute();
        parse( attribute, line );
        return attribute;
    }

    /**
     * Parse a line into name and value pairs
     *
     * @param line the line to be parsed
     * @throws ManifestException if the line does not contain a colon
     *      separating the name and value
     */
    public static void parse( final Attribute attribute, final String line )
        throws ManifestException
    {
        final int index = line.indexOf( ": " );
        if( index == -1 )
        {
            throw new ManifestException( "Manifest line \"" + line + "\" is not valid as it does not " +
                                         "contain a name and a value separated by ': ' " );
        }
        final String name = line.substring( 0, index );
        final String value = line.substring( index + 2 );
        attribute.setName( name );
        attribute.setValue( value );
    }

    public static void write( final Attribute attribute, final PrintWriter writer )
        throws IOException
    {
        final String name = attribute.getName();
        final String value = attribute.getValue();
        String line = name + ": " + value;
        while( line.getBytes().length > Manifest.MAX_LINE_LENGTH )
        {
            // try to find a MAX_LINE_LENGTH byte section
            int breakIndex = Manifest.MAX_LINE_LENGTH;
            String section = line.substring( 0, breakIndex );
            while( section.getBytes().length > Manifest.MAX_LINE_LENGTH && breakIndex > 0 )
            {
                breakIndex--;
                section = line.substring( 0, breakIndex );
            }
            if( breakIndex == 0 )
            {
                throw new IOException( "Unable to write manifest line " + name + ": " + value );
            }
            writer.println( section );
            line = " " + line.substring( breakIndex );
        }
        writer.println( line );
    }
}
