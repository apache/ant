/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.net;

import java.util.Locale;
import org.apache.tools.ant.types.EnumeratedAttribute;

public class Action
    extends EnumeratedAttribute
{
    private final static String[] validActions = new String[]
    {
        "send", "put", "recv", "get", "del", "delete", "list", "mkdir"
    };

    public int getAction()
    {
        String actionL = getValue().toLowerCase( Locale.US );
        if( actionL.equals( "send" ) ||
            actionL.equals( "put" ) )
        {
            return FTP.SEND_FILES;
        }
        else if( actionL.equals( "recv" ) ||
            actionL.equals( "get" ) )
        {
            return FTP.GET_FILES;
        }
        else if( actionL.equals( "del" ) ||
            actionL.equals( "delete" ) )
        {
            return FTP.DEL_FILES;
        }
        else if( actionL.equals( "list" ) )
        {
            return FTP.LIST_FILES;
        }
        else if( actionL.equals( "mkdir" ) )
        {
            return FTP.MK_DIR;
        }
        return FTP.SEND_FILES;
    }

    public String[] getValues()
    {
        return validActions;
    }
}
