/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ccm;

/**
 * Task to perform Checkin Default task command to Continuus
 *
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public class CCMCheckinDefault extends CCMCheck
{

    public final static String DEFAULT_TASK = "default";

    public CCMCheckinDefault()
    {
        super();
        setCcmAction( COMMAND_CHECKIN );
        setTask( DEFAULT_TASK );
    }
}

