/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.myrmidon.api.TaskException;

/**
 * FileList represents an explicitly named list of files. FileLists are useful
 * when you want to capture a list of files regardless of whether they currently
 * exist. By contrast, FileSet operates as a filter, only returning the name of
 * a matched file if it currently exists in the file system.
 *
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a>
 * @version $Revision$ $Date$
 */
public class SimpleFileList
{
    private final ArrayList m_filenames = new ArrayList();
    private File m_dir;

    public SimpleFileList()
    {
    }

    public void setDir( File dir )
    {
        m_dir = dir;
    }

    public void setFiles( String filenames )
    {
        if( filenames != null && filenames.length() > 0 )
        {
            StringTokenizer tok = new StringTokenizer( filenames, ", \t\n\r\f", false );
            while( tok.hasMoreTokens() )
            {
                m_filenames.add( tok.nextToken() );
            }
        }
    }

    public File getDir()
    {
        return m_dir;
    }

    /**
     * Returns the list of files represented by this FileList.
     */
    public String[] getFiles()
        throws TaskException
    {
        if( m_dir == null )
        {
            throw new TaskException( "No directory specified for filelist." );
        }

        if( m_filenames.size() == 0 )
        {
            throw new TaskException( "No files specified for filelist." );
        }

        return (String[])m_filenames.toArray( new String[ m_filenames.size() ] );
    }
}
