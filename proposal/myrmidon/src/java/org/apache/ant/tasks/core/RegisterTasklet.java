/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasks.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.ant.AntException;
import org.apache.ant.tasklet.AbstractTasklet;
import org.apache.ant.tasklet.engine.DefaultTaskletInfo;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.ant.tasklet.engine.TaskletRegistry;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentNotAccessibleException;
import org.apache.avalon.ComponentNotFoundException;
import org.apache.avalon.Composer;
import org.apache.avalon.camelot.RegistryException;

/**
 * Method to register a single tasklet.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class RegisterTasklet 
    extends AbstractTasklet
    implements Composer
{
    protected TaskletRegistry     m_taskletRegistry;
    protected String              m_tasklib;
    protected String              m_taskName;
    protected String              m_classname;

    public void compose( final ComponentManager componentManager )
        throws ComponentNotFoundException, ComponentNotAccessibleException
    {
        final TaskletEngine engine = (TaskletEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TaskletEngine" );
        m_taskletRegistry = engine.getTaskletRegistry();
    }

    public void setTaskLib( final String tasklib )
    {
        m_tasklib = tasklib;
    }
    
    public void setTaskName( final String taskName )
    {
        m_taskName = taskName;
    }
    
    public void setClassname( final String classname )
    {
        m_classname = classname;
    }

    public void run()
        throws AntException
    {
/*
        if( null == m_tasklib )
        {
            throw new AntException( "Must specify tasklib parameter" );
        }
*/
        if( null == m_taskName )
        {
            throw new AntException( "Must specify taskname parameter" );
        }

        if( null == m_tasklib && null == m_classname )
        {
            throw new AntException( "Must specify classname if don't specify " + 
                                    "tasklib parameter" );
        }

        if( null == m_classname )
        {
            m_classname = getDefaultClassName();
        }
        
        try
        {
            URL url = null;

            if( null != m_tasklib )
            {
                final File tasklib = new File( getContext().resolveFilename( m_tasklib ) );
                url = tasklib.toURL();
            }
            
            final DefaultTaskletInfo info = new DefaultTaskletInfo( m_classname, url );
        
            m_taskletRegistry.register( m_taskName, info ); 
        }
        catch( final MalformedURLException mue )
        {
            throw new AntException( "Malformed task-lib parameter " + m_tasklib, mue );
        }
        catch( final RegistryException re )
        {
            throw new AntException( "Error registering " + m_taskName + " due to " + re, re );
        }
    }

    protected String getDefaultClassName()
        throws AntException
    {
        //TODO:
        throw new AntException( "Not yet capable of automagically finding classname" );
    }
}
