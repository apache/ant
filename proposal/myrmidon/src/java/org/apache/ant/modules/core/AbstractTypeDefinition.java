/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.components.deployer.TskDeployer;
import org.apache.myrmidon.components.executor.Executor;
import org.apache.myrmidon.components.type.TypeManager;

/**
 * Method to register a a typeet.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public abstract class AbstractTypeDefinition
    extends AbstractTask
    implements Composable
{
    private String              m_lib;
    private String              m_name;
    private String              m_classname;
    private TskDeployer         m_tskDeployer;
    private TypeManager         m_typeManager;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        m_tskDeployer = (TskDeployer)componentManager.lookup( TskDeployer.ROLE );
    }

    public void setLib( final String lib )
    {
        m_lib = lib;
    }

    public void setName( final String name )
    {
        m_name = name;
    }

    public void setClassname( final String classname )
    {
        m_classname = classname;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_name )
        {
            throw new TaskException( "Must specify name parameter" );
        }
        else if( null == m_lib && null == m_classname )
        {
            throw new TaskException( "Must specify classname if you don't specify " +
                                    "lib parameter" );
        }

        final URL url = getURL( m_lib );

        registerResource( m_name, m_classname, url );
    }

    protected final TskDeployer getDeployer()
    {
        return m_tskDeployer;
    }

    protected final TypeManager getTypeManager()
    {
        return m_typeManager;
    }

    private final URL getURL( final String libName )
        throws TaskException
    {
        if( null != libName )
        {
            final File lib = getContext().resolveFile( libName );
            try { return lib.toURL(); }
            catch( final MalformedURLException mue )
            {
                throw new TaskException( "Malformed task-lib parameter " + m_lib, mue );
            }
        }
        else
        {
            return null;
        }
    }

    protected abstract void registerResource( String name, String classname, URL url )
        throws TaskException;
}
