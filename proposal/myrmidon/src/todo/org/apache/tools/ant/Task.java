/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

public abstract class Task
    extends ProjectComponent
    implements org.apache.myrmidon.api.Task
{
    protected void handleErrorOutput( String line )
    {
        log( line, Project.MSG_ERR );
    }

    protected void handleOutput( String line )
    {
        log( line, Project.MSG_INFO );
    }
}

