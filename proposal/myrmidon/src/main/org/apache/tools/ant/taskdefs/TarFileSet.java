/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.myrmidon.api.TaskException;

public class TarFileSet
    extends FileSet
{
    private String[] m_files;
    private int m_mode = 0100644;

    private String m_userName = "";
    private String m_groupName = "";

    public void setGroup( final String groupName )
    {
        m_groupName = groupName;
    }

    public void setMode( final String octalString )
    {
        m_mode = 0100000 | Integer.parseInt( octalString, 8 );
    }

    public void setUserName( final String userName )
    {
        m_userName = userName;
    }

    /**
     * Get a list of files and directories specified in the fileset.
     *
     * @return a list of file and directory names, relative to the baseDir
     *      for the project.
     */
    protected String[] getFiles()
        throws TaskException
    {
        if( m_files == null )
        {
            final DirectoryScanner scanner = getDirectoryScanner();
            final String[] directories = scanner.getIncludedDirectories();
            final String[] filesPerSe = scanner.getIncludedFiles();
            m_files = new String[ directories.length + filesPerSe.length ];
            System.arraycopy( directories, 0, m_files, 0, directories.length );
            System.arraycopy( filesPerSe, 0, m_files, directories.length,
                              filesPerSe.length );
        }

        return m_files;
    }

    protected String getGroup()
    {
        return m_groupName;
    }

    protected int getMode()
    {
        return m_mode;
    }

    protected String getUserName()
    {
        return m_userName;
    }
}
