/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.ZipOutputStream;

/**
 * Creates a EAR archive. Based on WAR task
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:leslie.hughes@rubus.com">Les Hughes</a>
 */
public class Ear extends Jar
{
    private File m_appxml;
    private boolean m_descriptorAdded;

    public Ear()
    {
        m_archiveType = "ear";
        m_emptyBehavior = "create";
    }

    public void setAppxml( final File appxml )
        throws TaskException
    {
        m_appxml = appxml;
        if( !m_appxml.exists() )
        {
            final String message = "Deployment descriptor: " +
                m_appxml + " does not exist.";
            throw new TaskException( message );
        }

        addFileAs( m_appxml, "META-INF/application.xml" );
    }

    public void addArchives( ZipFileSet fs )
    {
        // We just set the prefix for this fileset, and pass it up.
        // Do we need to do this? LH
        getLogger().debug( "addArchives called" );
        fs.setPrefix( "/" );
        super.addFileset( fs );
    }

    protected void initZipOutputStream( final ZipOutputStream zOut )
        throws IOException, TaskException
    {
        if( m_appxml == null && !isInUpdateMode() )
        {
            final String message = "appxml attribute is required";
            throw new TaskException( message );
        }

        super.initZipOutputStream( zOut );
    }

    protected void zipFile( File file, ZipOutputStream zOut, String vPath )
        throws IOException, TaskException
    {
        // If the file being added is WEB-INF/web.xml, we warn if it's not the
        // one specified in the "webxml" attribute - or if it's being added twice,
        // meaning the same file is specified by the "webxml" attribute and in
        // a <fileset> element.
        if( vPath.equalsIgnoreCase( "META-INF/aplication.xml" ) )
        {
            if( m_appxml == null ||
                !m_appxml.equals( file ) ||
                m_descriptorAdded )
            {
                final String message = "Warning: selected " + m_archiveType +
                    " files include a META-INF/application.xml which will be ignored " +
                    "(please use appxml attribute to " + m_archiveType + " task)";
                getLogger().warn( message );
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
