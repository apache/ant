/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import org.apache.ant.tasklet.Tasklet;
import org.apache.avalon.camelot.AbstractEntry;

public class TaskletEntry
    extends AbstractEntry
{
    public TaskletEntry( final TaskletInfo info, final Tasklet tasklet )
    {
        super( info, tasklet );
    }

    /**
     * Retrieve instance of tasklet.
     *
     * @return the component instance
     */
    public Tasklet getTasklet()
    {
        return (Tasklet)getInstance();
    }
}

