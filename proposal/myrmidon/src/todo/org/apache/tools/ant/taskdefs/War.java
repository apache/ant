/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.ZipOutputStream;

/**
 * Creates a WAR archive.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class War extends Jar
{

    private File deploymentDescriptor;
    private boolean descriptorAdded;

    public War()
    {
        super();
        archiveType = "war";
        emptyBehavior = "create";
    }

    public void setWebxml( File descr )
        throws TaskException
    {
        deploymentDescriptor = descr;
        if( !deploymentDescriptor.exists() )
            throw new TaskException( "Deployment descriptor: " + deploymentDescriptor + " does not exist." );

        // Create a ZipFileSet for this file, and pass it up.
        ZipFileSet fs = new ZipFileSet();
        fs.setDir( new File( deploymentDescriptor.getParent() ) );
        fs.setIncludes( deploymentDescriptor.getName() );
        fs.setFullpath( "WEB-INF/web.xml" );
        super.addFileset( fs );
    }

    public void addClasses( ZipFileSet fs )
    {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix( "WEB-INF/classes/" );
        super.addFileset( fs );
    }

    public void addLib( ZipFileSet fs )
    {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix( "WEB-INF/lib/" );
        super.addFileset( fs );
    }

    public void addWebinf( ZipFileSet fs )
    {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix( "WEB-INF/" );
        super.addFileset( fs );
    }

    /**
     * Make sure we don't think we already have a web.xml next time this task
     * gets executed.
     */
    protected void cleanUp()
    {
        descriptorAdded = false;
        super.cleanUp();
    }

    protected void initZipOutputStream( ZipOutputStream zOut )
        throws IOException, TaskException
    {
        // If no webxml file is specified, it's an error.
        if( deploymentDescriptor == null && !isInUpdateMode() )
        {
            throw new TaskException( "webxml attribute is required" );
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
        if( vPath.equalsIgnoreCase( "WEB-INF/web.xml" ) )
        {
            if( deploymentDescriptor == null || !deploymentDescriptor.equals( file ) || descriptorAdded )
            {
                log( "Warning: selected " + archiveType + " files include a WEB-INF/web.xml which will be ignored " +
                     "(please use webxml attribute to " + archiveType + " task)", Project.MSG_WARN );
            }
            else
            {
                super.zipFile( file, zOut, vPath );
                descriptorAdded = true;
            }
        }
        else
        {
            super.zipFile( file, zOut, vPath );
        }
    }
}
