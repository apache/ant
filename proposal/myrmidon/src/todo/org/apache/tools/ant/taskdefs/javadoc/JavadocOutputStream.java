/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.javadoc;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.LogOutputStream;

class JavadocOutputStream
    extends LogOutputStream
{

    //
    // Override the logging of output in order to filter out Generating
    // messages.  Generating messages are set to a priority of VERBOSE
    // unless they appear after what could be an informational message.
    //
    private String m_queuedLine;

    JavadocOutputStream( Task javadoc, int level )
    {
        super( javadoc, level );
    }

    protected void processLine( String line, int messageLevel )
    {
        if( messageLevel == Project.MSG_INFO && line.startsWith( "Generating " ) )
        {
            if( m_queuedLine != null )
            {
                super.processLine( m_queuedLine, Project.MSG_VERBOSE );
            }
            m_queuedLine = line;
        }
        else
        {
            if( m_queuedLine != null )
            {
                if( line.startsWith( "Building " ) )
                    super.processLine( m_queuedLine, Project.MSG_VERBOSE );
                else
                    super.processLine( m_queuedLine, Project.MSG_INFO );
                m_queuedLine = null;
            }
            super.processLine( line, messageLevel );
        }
    }
}
