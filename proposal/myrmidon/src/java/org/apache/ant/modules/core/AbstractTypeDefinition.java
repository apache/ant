/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.core;

import java.io.File;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.components.deployer.Deployer;
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
    private Deployer            m_deployer;
    private TypeManager         m_typeManager;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        m_deployer = (Deployer)componentManager.lookup( Deployer.ROLE );
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

        final File file = getFile( m_lib );

        registerResource( m_name, m_classname, file );
    }

    protected final Deployer getDeployer()
    {
        return m_deployer;
    }

    protected final TypeManager getTypeManager()
    {
        return m_typeManager;
    }

    private final File getFile( final String libName )
        throws TaskException
    {
        if( null != libName )
        {
            return getContext().resolveFile( libName );
        }
        else
        {
            return null;
        }
    }

    protected abstract void registerResource( String name, String classname, File file )
        throws TaskException;
}
