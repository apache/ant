/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;

import org.apache.myrmidon.api.TaskException;

/**
 * Type class. Holds information about a project edition.
 *
 * @author RT
 * @author: Wolf Siberski
 */
public class VAJProjectDescription
{
    private String name;
    private boolean projectFound;
    private String version;

    public VAJProjectDescription()
    {
    }

    public VAJProjectDescription( String n, String v )
    {
        name = n;
        version = v;
    }

    public void setName( String newName )
    {
        if( newName == null || newName.equals( "" ) )
        {
            throw new TaskException( "name attribute must be set" );
        }
        name = newName;
    }

    public void setProjectFound()
    {
        projectFound = true;
    }

    public void setVersion( String newVersion )
    {
        if( newVersion == null || newVersion.equals( "" ) )
        {
            throw new TaskException( "version attribute must be set" );
        }
        version = newVersion;
    }

    public String getName()
    {
        return name;
    }

    public String getVersion()
    {
        return version;
    }

    public boolean projectFound()
    {
        return projectFound;
    }
}
