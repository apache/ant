/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import org.apache.aut.nativelib.ExecOutputHandler;
import org.apache.myrmidon.api.TaskContext;

/**
 * An {@link ExecOutputHandler} adaptor, that writes output to the logging
 * methods of a {@link TaskContext}.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class LoggingExecOutputHandler
    implements ExecOutputHandler
{
    private final TaskContext m_context;

    public LoggingExecOutputHandler( final TaskContext context )
    {
        m_context = context;
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     */
    public void stdout( final String line )
    {
        // TODO - should be using info(), but currently that is only used
        // when ant is run in verbose mode
        m_context.warn( line );
    }

    /**
     * Receive notification about the process writing
     * to standard error.
     */
    public void stderr( final String line )
    {
        m_context.error( line );
    }
}
