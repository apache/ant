/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.archive;

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.avalon.excalibur.zip.ZipOutputStream;

/**
 * Creates a WAR archive.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class War
    extends Jar
{
    private File m_webxml;
    private boolean m_descriptorAdded;

    public War()
    {
        super();
        m_archiveType = "war";
        m_emptyBehavior = "create";
    }

    public void setWebxml( final File descr )
        throws TaskException
    {
        m_webxml = descr;
        if( !m_webxml.exists() )
        {
            final String message = "Deployment descriptor: " +
                m_webxml + " does not exist.";
            throw new TaskException( message );
        }

        addFileAs( descr, "WEB-INF/web.xml" );
    }

    public void addClasses( final ZipFileSet fs )
    {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix( "WEB-INF/classes/" );
        super.addFileset( fs );
    }

    public void addLib( final ZipFileSet fs )
    {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix( "WEB-INF/lib/" );
        super.addFileset( fs );
    }

    public void addWebinf( final ZipFileSet fs )
    {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix( "WEB-INF/" );
        super.addFileset( fs );
    }

    protected void initZipOutputStream( final ZipOutputStream zOut )
        throws IOException, TaskException
    {
        // If no webxml file is specified, it's an error.
        if( m_webxml == null && !isInUpdateMode() )
        {
            throw new TaskException( "webxml attribute is required" );
        }

        super.initZipOutputStream( zOut );
    }

    protected void zipFile( final File file,
                            final ZipOutputStream zOut,
                            final String vPath )
        throws IOException, TaskException
    {
        // If the file being added is WEB-INF/web.xml, we warn if it's not the
        // one specified in the "webxml" attribute - or if it's being added twice,
        // meaning the same file is specified by the "webxml" attribute and in
        // a <fileset> element.
        if( vPath.equalsIgnoreCase( "WEB-INF/web.xml" ) )
        {
            if( m_webxml == null || !m_webxml.equals( file ) || m_descriptorAdded )
            {
                final String message = "Warning: selected " + m_archiveType +
                    " files include a WEB-INF/web.xml which will be ignored " +
                    "(please use webxml attribute to " + m_archiveType + " task)";
                getContext().warn( message );
            }
            else
            {
                super.zipFile( file, zOut, vPath );
                m_descriptorAdded = true;
            }
        }
        else
        {
            super.zipFile( file, zOut, vPath );
        }
    }
}
