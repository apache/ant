/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util.regexp;

/**
 * Regular expression utilities class which handles flag operations
 *
 * @author <a href="mailto:mattinger@mindless.com">Matthew Inger</a>
 */
public class RegexpUtil extends Object
{
    public final static boolean hasFlag( int options, int flag )
    {
        return ( ( options & flag ) > 0 );
    }

    public final static int removeFlag( int options, int flag )
    {
        return ( options & ( 0xFFFFFFFF - flag ) );
    }
}
