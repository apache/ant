/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.build;

import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.BuildException;
import java.io.File;

/**
 * An Ant 1.x task to assemble a Myrmidon Antlib.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class AntlibJarTask
    extends Jar
{
    private File m_roleDescriptor;
    private File m_typeDescriptor;
    private File m_serviceDescriptor;

    public void setRolesDescriptor( final File roleDescriptor )
    {
        m_roleDescriptor = roleDescriptor;
    }

    public void setDescriptor( final File typeDescriptor )
    {
        m_typeDescriptor = typeDescriptor;
    }

    public void setServicesDescriptor( final File serviceDescriptor )
    {
        m_serviceDescriptor = serviceDescriptor;
    }

    public void execute() throws BuildException
    {
        maybeAddFile( m_roleDescriptor, "META-INF/ant-roles.xml" );
        maybeAddFile( m_typeDescriptor, "META-INF/ant-descriptor.xml" );
        maybeAddFile( m_serviceDescriptor, "META-INF/ant-services.xml" );

        super.execute();
    }

    private void maybeAddFile( final File file, final String path )
    {
        if( file == null )
        {
            return;
        }
        if( ! file.isFile() )
        {
            throw new BuildException( "File \"" + file + "\" does not exist or is not a file." );
        }

        // Create a ZipFileSet for this file, and pass it up.
        final ZipFileSet fs = new ZipFileSet();
        fs.setDir( file.getParentFile() );
        fs.setIncludes( file.getName() );
        fs.setFullpath( path );
        addFileset( fs );
    }
}
