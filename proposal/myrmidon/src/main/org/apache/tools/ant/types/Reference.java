/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;

/**
 * Class to hold a reference to another object in the project.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Reference
{

    private String refid;

    public Reference()
    {
        super();
    }

    public Reference( String id )
    {
        this();
        setRefId( id );
    }

    public void setRefId( String id )
    {
        refid = id;
    }

    public String getRefId()
    {
        return refid;
    }

    public Object getReferencedObject( Project project )
        throws TaskException
    {
        if( refid == null )
        {
            throw new TaskException( "No reference specified" );
        }

        Object o = project.getReference( refid );
        if( o == null )
        {
            throw new TaskException( "Reference " + refid + " not found." );
        }
        return o;
    }
}
