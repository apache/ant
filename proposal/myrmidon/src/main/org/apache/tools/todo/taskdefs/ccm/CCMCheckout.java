/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.ccm;

/**
 * Task to perform Checkout command to Continuus
 *
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public class CCMCheckout extends CCMCheck
{

    public CCMCheckout()
    {
        super();
        setCcmAction( COMMAND_CHECKOUT );
    }
}

