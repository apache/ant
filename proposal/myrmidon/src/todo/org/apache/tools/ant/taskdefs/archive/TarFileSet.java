/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.archive;

import org.apache.tools.ant.types.FileSet;

public class TarFileSet
    extends FileSet
{
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
