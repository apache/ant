/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.cvslib;

import java.util.ArrayList;
import java.util.Date;

/**
 * CVS Entry.
 *
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class CVSEntry
{
    private Date m_date;
    private final String m_author;
    private final String m_comment;
    private final ArrayList m_files = new ArrayList();

    public CVSEntry( Date date, String author, String comment )
    {
        m_date = date;
        m_author = author;
        m_comment = comment;
    }

    public void addFile( String file, String revision )
    {
        m_files.add( new RCSFile( file, revision ) );
    }

    public void addFile( String file, String revision, String previousRevision )
    {
        m_files.add( new RCSFile( file, revision, previousRevision ) );
    }

    Date getDate()
    {
        return m_date;
    }

    String getAuthor()
    {
        return m_author;
    }

    String getComment()
    {
        return m_comment;
    }

    ArrayList getFiles()
    {
        return m_files;
    }

    public String toString()
    {
        return getAuthor() + "\n" + getDate() + "\n" + getFiles() + "\n" + getComment();
    }
}
