/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.extension.resolvers;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.extension.Extension;
import org.apache.tools.ant.taskdefs.optional.extension.ExtensionResolver;

/**
 * Resolver that just returns s specified location.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class LocationResolver
    implements ExtensionResolver
{
    private String m_location;

    public void setLocation( final String location )
    {
        m_location = location;
    }

    public File resolve( final Extension extension,
                         final Project project )
        throws BuildException
    {
        if( null == m_location )
        {
            final String message = "No location specified for resolver";
            throw new BuildException( message );
        }

        return project.resolveFile( m_location );
    }

    public String toString()
    {
        return "Location[" + m_location + "]";
    }
}
