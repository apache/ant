/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.io.FilenameFilter;

public class InnerClassFilenameFilter implements FilenameFilter
{
    private String baseClassName;

    InnerClassFilenameFilter( String baseclass )
    {
        int extidx = baseclass.lastIndexOf( ".class" );
        if( extidx == -1 )
        {
            extidx = baseclass.length() - 1;
        }
        baseClassName = baseclass.substring( 0, extidx );
    }

    public boolean accept( File Dir, String filename )
    {
        if( ( filename.lastIndexOf( "." ) != filename.lastIndexOf( ".class" ) )
            || ( filename.indexOf( baseClassName + "$" ) != 0 ) )
        {
            return false;
        }
        return true;
    }
}
