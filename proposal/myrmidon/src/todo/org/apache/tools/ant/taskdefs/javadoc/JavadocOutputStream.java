/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.javadoc;

import org.apache.avalon.framework.logger.Logger;
import org.apache.tools.ant.taskdefs.exec.LogOutputStream;

class JavadocOutputStream
    extends LogOutputStream
{
    // Override the logging of output in order to filter out Generating
    // messages.  Generating messages are set to a priority of VERBOSE
    // unless they appear after what could be an informational message.
    //
    private String m_queuedLine;

    JavadocOutputStream( final Logger logger, final boolean isError )
    {
        super( logger, isError );
    }

    protected void processLine( final String line )
    {
        if( !isError() && line.startsWith( "Generating " ) )
        {
            if( m_queuedLine != null )
            {
                getLogger().debug( m_queuedLine );
            }
            m_queuedLine = line;
        }
        else
        {
            if( m_queuedLine != null )
            {
                if( line.startsWith( "Building " ) )
                {
                    getLogger().debug( m_queuedLine );
                }
                else
                {
                    getLogger().info( m_queuedLine );
                }
                m_queuedLine = null;
            }
            getLogger().warn( line );
        }
    }
}
