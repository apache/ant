/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Abstract task to extend to define a type.
 *
 * TODO: Make this support classpath sub-element in future
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public abstract class AbstractTypeDef
    extends AbstractTask
    implements Composable
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( AbstractTypeDef.class );

    private File m_lib;
    private String m_name;
    private String m_className;
    private TypeManager m_typeManager;
    private RoleManager m_roleManager;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        m_roleManager = (RoleManager)componentManager.lookup( RoleManager.ROLE );
    }

    public void setLib( final File lib )
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
            final String message = REZ.getString( "typedef.no-name.error" );
            throw new TaskException( message );
        }
        else if( null == m_className )
        {
            final String message = REZ.getString( "typedef.no-classname.error" );
            throw new TaskException( message );
        }

        final String typeName = getTypeName();
        final String role = m_roleManager.getRoleForName( typeName );

        final ClassLoader classLoader = createClassLoader();
        final DefaultTypeFactory factory = new DefaultTypeFactory( classLoader );
        factory.addNameClassMapping( m_name, m_className );

        try
        {
            m_typeManager.registerType( role, m_name, factory );
        }
        catch( final TypeException te )
        {
            final String message = REZ.getString( "typedef.no-register.error" );
            throw new TaskException( message, te );
        }
    }

    protected ClassLoader createClassLoader()
        throws TaskException
    {
        //TODO: Make this support classpath sub-element in future
        try
        {
            final URL url = m_lib.toURL();
            final ClassLoader classLoader =
                Thread.currentThread().getContextClassLoader();

            return new URLClassLoader( new URL[]{url}, classLoader );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "typedef.bad-classloader.error", e );
            throw new TaskException( message, e );
        }
    }

    protected final TypeManager getTypeManager()
    {
        return m_typeManager;
    }

    protected abstract String getTypeName();
}
