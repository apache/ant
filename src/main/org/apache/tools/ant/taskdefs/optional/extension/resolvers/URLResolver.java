/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.extension.resolvers;

import java.io.File;
import java.net.URL;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Get;
import org.apache.tools.ant.taskdefs.optional.extension.Extension;
import org.apache.tools.ant.taskdefs.optional.extension.ExtensionResolver;

/**
 * Resolver that just returns s specified location.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class URLResolver
    implements ExtensionResolver
{
    private File m_destfile;
    private File m_destdir;
    private URL m_url;

    public void setUrl( final URL url )
    {
        m_url = url;
    }

    public void setDestfile( final File destfile )
    {
        m_destfile = destfile;
    }

    public void setDestdir( final File destdir )
    {
        m_destdir = destdir;
    }

    public File resolve( final Extension extension,
                         final Project project )
        throws BuildException
    {
        validate();

        final File file = getDest();

        final Get get = (Get)project.createTask( "get" );
        get.setDest( file );
        get.setSrc( m_url );
        get.execute();

        return file;
    }

    private File getDest()
    {
        if( null != m_destfile )
        {
            return m_destfile;
        }
        else
        {
            final String file = m_url.getFile();
            String filename = null;
            if( null == file || file.length() <= 1 )
            {
                filename = "default.file";
            }
            else
            {
                int index = file.lastIndexOf( '/' );
                if( -1 == index )
                {
                    index = 0;
                }
                filename = file.substring( index );
            }

            return new File( m_destdir, filename );
        }
    }

    private void validate()
    {
        if( null == m_url )
        {
            final String message = "Must specify URL";
            throw new BuildException( message );
        }

        if( null == m_destdir && null == m_destfile )
        {
            final String message = "Must specify destination file or directory";
            throw new BuildException( message );
        }
        else if( null != m_destdir && null != m_destfile )
        {
            final String message = "Must not specify both destination file or directory";
            throw new BuildException( message );
        }
    }

    public String toString()
    {
        return "URL[" + m_url + "]";
    }
}
