/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.runtime;

import java.io.File;
import java.net.URL;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.converter.Converter;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Task to define a converter.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class ConverterDef
    extends AbstractTask
    implements Composable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( ConverterDef.class );

    private String m_sourceType;
    private String m_destinationType;
    private File m_lib;
    private String m_classname;
    private ConverterRegistry m_converterRegistry;
    private TypeManager m_typeManager;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_converterRegistry = (ConverterRegistry)componentManager.lookup( ConverterRegistry.ROLE );
        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
    }

    public void setLib( final File lib )
    {
        m_lib = lib;
    }

    public void setClassname( final String classname )
    {
        m_classname = classname;
    }

    public void setSourceType( final String sourceType )
    {
        m_sourceType = sourceType;
    }

    public void setDestinationType( final String destinationType )
    {
        m_destinationType = destinationType;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_classname )
        {
            final String message = REZ.getString( "converterdef.no-classname.error" );
            throw new TaskException( message );
        }
        else if( null == m_sourceType )
        {
            final String message = REZ.getString( "converterdef.no-source.error" );
            throw new TaskException( message );
        }
        else if( null == m_destinationType )
        {
            final String message = REZ.getString( "converterdef.no-destination.error" );
            throw new TaskException( message );
        }
        else if( null == m_lib )
        {
            final String message = REZ.getString( "converterdef.no-lib.error" );
            throw new TaskException( message );
        }

        try
        {
            m_converterRegistry.registerConverter( m_classname, m_sourceType, m_destinationType );

            final URL url = m_lib.toURL();
            final DefaultTypeFactory factory = new DefaultTypeFactory( new URL[]{url} );
            factory.addNameClassMapping( m_classname, m_classname );

            m_typeManager.registerType( Converter.ROLE, m_classname, factory );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "converterdef.no-register.error", m_classname );
            throw new TaskException( message, e );
        }
    }
}
