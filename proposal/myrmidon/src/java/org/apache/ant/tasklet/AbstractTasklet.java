/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet;

import org.apache.ant.AntException;
import org.apache.avalon.Context;
import org.apache.avalon.Initializable;
import org.apache.log.Logger;

/**
 * This is abstract base class for tasklets.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public abstract class AbstractTasklet
    implements Tasklet, Initializable
{
    //the user should set this in constructors of sub-classes
    protected JavaVersion             m_requiredJavaVersion;

    private TaskletContext            m_context;
    private Logger                    m_logger;

    /**
     * Receive logger from container.
     *
     * @param logger the logger
     */
    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    /**
     * Retrieve context from container.
     *
     * @param context the context
     */
    public void contextualize( final Context context )
    {
        m_context = (TaskletContext)context;
    }

    /**
     * This will be called before run() method and checks any preconditions.
     *
     * Intially preconditions just include JVM version but in future it 
     * will automagically also check if all required parameters are present.
     *
     * @exception AntException if an error occurs
     */
    public void init()
        throws AntException
    {
        if( null != m_requiredJavaVersion )
        {
            final JavaVersion suppliedVersion = m_context.getJavaVersion();

            if( m_requiredJavaVersion.isLessThan( suppliedVersion ) )
            {
                throw new AntException( "Task requires a JavaVersion of at least " + 
                                        m_requiredJavaVersion + " but current version is " +
                                        suppliedVersion );
            }
        }
    }

    /**
     * Convenience method for sub-class to retrieve context.
     *
     * @return the context
     */
    protected TaskletContext getContext()
    {
        return m_context;
    }

    /**
     * Convenience method for subclass to get logger.
     *
     * @return the Logger
     */
    protected Logger getLogger()
    {
        return m_logger;
    }
}
