/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

/**
 * Used to report exit status of classes which call System.exit()
 *
 * @author Conor MacNeill
 * @see NoExitSecurityManager
 */
public class ExitException extends SecurityException
{

    private int status;

    /**
     * Constructs an exit exception.
     *
     * @param status the status code returned via System.exit()
     */
    public ExitException( int status )
    {
        super( "ExitException: status " + status );
        this.status = status;
    }

    /**
     * @return the status code return via System.exit()
     */
    public int getStatus()
    {
        return status;
    }
}
