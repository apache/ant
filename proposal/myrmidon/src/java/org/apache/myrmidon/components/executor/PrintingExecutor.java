/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.executor;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.api.TaskException;

public class PrintingExecutor
    extends AspectAwareExecutor
{
    protected void doExecute( final Configuration taskModel, final Task task )
        throws TaskException
    {
        final StringBuffer sb = new StringBuffer();
        printConfiguration( taskModel, 0, sb );

        System.out.println( sb.toString() );
    }

    private void printConfiguration( final Configuration taskModel,
                                     final int level,
                                     final StringBuffer sb )
    {
        for( int i = 0; i < level; i++ )
        {
            sb.append( ' ' );
        }

        sb.append( '<' );
        sb.append( taskModel.getName() );

        final String[] names = taskModel.getAttributeNames();
        for( int i = 0; i < names.length; i++ )
        {
            final String name = names[ i ];
            final String value = taskModel.getAttribute( name, null );

            sb.append( ' ' );
            sb.append( name );
            sb.append( "=\"" );
            sb.append( value );
            sb.append( '\"' );
        }

        final Configuration[] children = taskModel.getChildren();
        if( 0 == children.length )
        {
            sb.append( "/>\n" );
        }
        else
        {
            sb.append( ">\n" );

            for( int i = 0; i < children.length; i++ )
            {
                printConfiguration( children[ i ], level + 1, sb );
            }

            for( int i = 0; i < level; i++ )
            {
                sb.append( ' ' );
            }

            sb.append( "</" );
            sb.append( taskModel.getName() );
            sb.append( ">\n" );
        }
    }
}
