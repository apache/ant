/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ccm;

import java.util.Date;

/**
 * Task to perform Checkin command to Continuus
 *
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public class CCMCheckin extends CCMCheck
{

    public CCMCheckin()
    {
        super();
        setCcmAction( COMMAND_CHECKIN );
        setComment( "Checkin " + new Date() );
    }

}

