/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.executor;

/**
 * This interface is used to supply a root execution frame to a container
 * that executes tasks.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface ExecutionContainer
{
    /**
     * Sets the root execution frame for the container.
     */
    void setRootExecutionFrame( ExecutionFrame frame ) throws Exception;
}
