/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

/**
 * Stores the file name and line number in a file.
 *
 * @author RT
 */
public class Location
{

    public final static Location UNKNOWN_LOCATION = new Location();
    private int columnNumber;
    private String fileName;
    private int lineNumber;

    /**
     * Creates a location consisting of a file name but no line number.
     *
     * @param fileName Description of Parameter
     */
    public Location( String fileName )
    {
        this( fileName, 0, 0 );
    }

    /**
     * Creates a location consisting of a file name and line number.
     *
     * @param fileName Description of Parameter
     * @param lineNumber Description of Parameter
     * @param columnNumber Description of Parameter
     */
    public Location( String fileName, int lineNumber, int columnNumber )
    {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Creates an "unknown" location.
     */
    private Location()
    {
        this( null, 0, 0 );
    }

    /**
     * Returns the file name, line number and a trailing space. An error message
     * can be appended easily. For unknown locations, returns an empty string.
     *
     * @return Description of the Returned Value
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();

        if( fileName != null )
        {
            buf.append( fileName );

            if( lineNumber != 0 )
            {
                buf.append( ":" );
                buf.append( lineNumber );
            }

            buf.append( ": " );
        }

        return buf.toString();
    }
}
