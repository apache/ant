/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.framework;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.role.RoleManager;
import org.apache.myrmidon.components.type.DefaultTypeFactory;
import org.apache.myrmidon.components.type.TypeException;
import org.apache.myrmidon.components.type.TypeManager;

/**
 * Abstract task to extend to define a type.
 *
 * TODO: Make this support classpath sub-element in future
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public abstract class AbstractTypeDef
    extends AbstractTask
    implements Composable
{
    private String              m_lib;
    private String              m_name;
    private String              m_className;
    private TypeManager         m_typeManager;
    private RoleManager         m_roleManager;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        m_roleManager = (RoleManager)componentManager.lookup( RoleManager.ROLE );
    }

    public void setLib( final String lib )
    {
        //In the future this would be replaced by ClassPath sub-element
        m_lib = lib;
    }

    public void setName( final String name )
    {
        m_name = name;
    }

    public void setClassname( final String className )
    {
        m_className = className;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_name )
        {
            throw new TaskException( "Must specify name parameter" );
        }
        else if( null == m_className )
        {
            throw new TaskException( "Must specify classname parameter" );
        }

        final String typeName = getTypeName();
        final String role = m_roleManager.getRoleForName( typeName );

        final ClassLoader classLoader = createClassLoader();
        final DefaultTypeFactory factory = new DefaultTypeFactory( classLoader );
        factory.addNameClassMapping( m_name, m_className );

        try { m_typeManager.registerType( role, m_name, factory ); }
        catch( final TypeException te )
        {
            throw new TaskException( "Failed to register type", te );
        }
    }

    protected ClassLoader createClassLoader()
        throws TaskException
    {
        //TODO: Make this support classpath sub-element in future
        try
        {
            final File file = getContext().resolveFile( m_lib );
            final URL url = file.getCanonicalFile().toURL();
            final ClassLoader classLoader =
                Thread.currentThread().getContextClassLoader();

            return new URLClassLoader( new URL[] { url }, classLoader );
        }
        catch( final Exception e )
        {
            throw new TaskException( "Failed to build classLoader due to: " + e, e );
        }
    }

    protected final TypeManager getTypeManager()
    {
        return m_typeManager;
    }

    protected abstract String getTypeName();
}
