/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.javadoc;

import java.io.File;

public class LinkArgument
{
    private boolean m_offline;
    private String m_href;
    private File m_packagelistLoc;

    public void setHref( String hr )
    {
        m_href = hr;
    }

    public void setOffline( boolean offline )
    {
        this.m_offline = offline;
    }

    public void setPackagelistLoc( File src )
    {
        m_packagelistLoc = src;
    }

    public String getHref()
    {
        return m_href;
    }

    public File getPackagelistLoc()
    {
        return m_packagelistLoc;
    }

    public boolean isLinkOffline()
    {
        return m_offline;
    }
}
