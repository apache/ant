/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.executor;

import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Frames in which tasks are executed.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public interface ExecutionFrame
{
    String ROLE = ExecutionFrame.class.getName();

    TypeManager getTypeManager();

    Logger getLogger();

    TaskContext getContext();
}
