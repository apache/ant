/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional;
import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;

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
    protected File m_book;
    protected String m_loaderConfig;
    protected File m_skinDirectory;
    protected File m_targetDirectory;

    public StyleBook()
    {
        setClassname( "org.apache.stylebook.StyleBook" );
        setFork( true );
        setFailonerror( true );
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
        throws BuildException
    {

        if( null == m_targetDirectory )
        {
            throw new BuildException( "TargetDirectory attribute not set." );
        }

        if( null == m_skinDirectory )
        {
            throw new BuildException( "SkinDirectory attribute not set." );
        }

        if( null == m_book )
        {
            throw new BuildException( "book attribute not set." );
        }

        createArg().setValue( "targetDirectory=" + m_targetDirectory );
        createArg().setValue( m_book.toString() );
        createArg().setValue( m_skinDirectory.toString() );
        if( null != m_loaderConfig )
        {
            createArg().setValue( "loaderConfig=" + m_loaderConfig );
        }

        super.execute();
    }
}

