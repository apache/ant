/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.stylebook;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.taskdefs.Java;
import org.apache.tools.todo.types.Argument;

/**
 * Basic task for apache stylebook.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @author <a href="mailto:marcus.boerger@post.rwth-aachen.de">Marcus
 *      B&ouml;rger</a>
 */
public class StyleBook
    extends Java
{
    private File m_book;
    private String m_loaderConfig;
    private File m_skinDirectory;
    private File m_targetDirectory;

    public StyleBook()
    {
        setClassname( "org.apache.stylebook.StyleBook" );
        setFork( true );
    }

    public void setBook( final File book )
    {
        m_book = book;
    }

    public void setLoaderConfig( final String loaderConfig )
    {
        m_loaderConfig = loaderConfig;
    }

    public void setSkinDirectory( final File skinDirectory )
    {
        m_skinDirectory = skinDirectory;
    }

    public void setTargetDirectory( final File targetDirectory )
    {
        m_targetDirectory = targetDirectory;
    }

    public void execute()
        throws TaskException
    {
        validate();

        addArg( new Argument( "targetDirectory=" + m_targetDirectory ) );
        addArg( new Argument( m_book.toString() ) );
        addArg( new Argument( m_skinDirectory.toString() ) );
        if( null != m_loaderConfig )
        {
            addArg( new Argument( "loaderConfig=" + m_loaderConfig ) );
        }

        super.execute();
    }

    private void validate() throws TaskException
    {
        if( null == m_targetDirectory )
        {
            throw new TaskException( "TargetDirectory attribute not set." );
        }

        if( null == m_skinDirectory )
        {
            throw new TaskException( "SkinDirectory attribute not set." );
        }

        if( null == m_book )
        {
            throw new TaskException( "book attribute not set." );
        }
    }
}

