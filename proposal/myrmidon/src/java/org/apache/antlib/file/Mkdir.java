/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.file;

import java.io.File;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Creates specified directory.
 *
 * @ant.task name="mkdir"
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author duncan@x180.com
 * @version $Revision$ $Date$
 */
public class Mkdir
    extends AbstractTask
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( Mkdir.class );

    private File m_dir;

    public void setDir( final File dir )
    {
        m_dir = dir;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_dir )
        {
            final String message = REZ.getString( "mkdir.missing-dir.error" );
            throw new TaskException( message );
        }

        if( m_dir.isFile() )
        {
            final String message =
                REZ.getString( "mkdir.file-exists.error", m_dir.getAbsolutePath() );
            throw new TaskException( message );
        }

        if( !m_dir.exists() )
        {
            final boolean result = m_dir.mkdirs();
            if( !result )
            {
                final String message =
                    REZ.getString( "mkdir.nocreate.error", m_dir.getAbsolutePath() );
                throw new TaskException( message );
            }
            final String message =
                REZ.getString( "mkdir.create.notice", m_dir.getAbsolutePath() );
            getContext().info( message );
        }
    }
}
