/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;

/**
 * Super class for all VAJ tasks. Contains common attributes (remoteServer) and
 * util methods
 *
 * @author: Wolf Siberski
 */

import org.apache.tools.ant.Task;

public class VAJTask extends Task
{

    // server name / port of VAJ remote tool api server
    protected String remoteServer = null;

    // holds the appropriate VAJUtil implementation
    private VAJUtil util = null;

    /**
     * Set remote server attribute
     *
     * @param remoteServer The new Remote value
     */
    public void setRemote( String remoteServer )
    {
        this.remoteServer = remoteServer;
    }

    /**
     * returns the VAJUtil implementation
     *
     * @return The Util value
     */
    protected VAJUtil getUtil()
    {
        if( util == null )
        {
            if( remoteServer == null )
            {
                util = new VAJLocalToolUtil( this );
            }
            else
            {
                util = new VAJRemoteUtil( this, remoteServer );
            }
        }
        return util;
    }

}
