/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Base class for components of a project, including tasks and data types.
 * Provides common facilities.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public abstract class ProjectComponent
    extends AbstractTask
{
    public void execute()
        throws TaskException
    {
        //HACK: NOOP execute - should be deleted in the future!
    }
}

