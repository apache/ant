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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
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

    private File deploymentDescriptor;
    private boolean descriptorAdded;

    public Ear()
    {
        super();
        archiveType = "ear";
        emptyBehavior = "create";
    }

    public void setAppxml( File descr )
    {
        deploymentDescriptor = descr;
        if( !deploymentDescriptor.exists() )
            throw new BuildException( "Deployment descriptor: " + deploymentDescriptor + " does not exist." );

        // Create a ZipFileSet for this file, and pass it up.
        ZipFileSet fs = new ZipFileSet();
        fs.setDir( new File( deploymentDescriptor.getParent() ) );
        fs.setIncludes( deploymentDescriptor.getName() );
        fs.setFullpath( "META-INF/application.xml" );
        super.addFileset( fs );
    }

    public void addArchives( ZipFileSet fs )
    {
        // We just set the prefix for this fileset, and pass it up.
        // Do we need to do this? LH
        log( "addArchives called", Project.MSG_DEBUG );
        fs.setPrefix( "/" );
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
        throws IOException, BuildException
    {
        // If no webxml file is specified, it's an error.
        if( deploymentDescriptor == null && !isInUpdateMode() )
        {
            throw new BuildException( "appxml attribute is required" );
        }

        super.initZipOutputStream( zOut );
    }

    protected void zipFile( File file, ZipOutputStream zOut, String vPath )
        throws IOException
    {
        // If the file being added is WEB-INF/web.xml, we warn if it's not the
        // one specified in the "webxml" attribute - or if it's being added twice,
        // meaning the same file is specified by the "webxml" attribute and in
        // a <fileset> element.
        if( vPath.equalsIgnoreCase( "META-INF/aplication.xml" ) )
        {
            if( deploymentDescriptor == null || !deploymentDescriptor.equals( file ) || descriptorAdded )
            {
                log( "Warning: selected " + archiveType + " files include a META-INF/application.xml which will be ignored " +
                    "(please use appxml attribute to " + archiveType + " task)", Project.MSG_WARN );
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
