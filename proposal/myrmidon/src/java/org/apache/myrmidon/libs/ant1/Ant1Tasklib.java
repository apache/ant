/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.libs.ant1;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.Task;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Method to register a tasklib.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Ant1Tasklib
    extends AbstractTask
    implements Composable
{
    private String       m_prefix  = "";
    private File         m_lib;
    private TypeManager  m_typeManager;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
    }

    public void setLib( final File lib )
    {
        m_lib = lib;
    }

    public void setPrefix( final String prefix )
    {
        m_prefix = prefix;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_lib )
        {
            throw new TaskException( "Must specify lib parameter" );
        }


        try
        {            
            final String location =  "jar:" + m_lib.toURL() + 
                "!/org/apache/tools/ant/taskdefs/defaults.properties";
            final URL url = new URL( location );
            final InputStream input = url.openStream();

            final Properties tasks = new Properties();
            tasks.load( input );
            
            input.close();

            final Ant1TypeFactory factory = new Ant1TypeFactory( m_lib.toURL() );

            final Enumeration enum = tasks.propertyNames();
            while( enum.hasMoreElements() )
            {
                final String rawName = (String)enum.nextElement();
                final String className = tasks.getProperty( rawName );    
                final String name = m_prefix + rawName;

                factory.addNameClassMapping( name, className );
                m_typeManager.registerType( Task.ROLE, name, factory );        
            }
        }
        catch( final Exception e )
        {
            throw new TaskException( "Failed to load task definitions", e );
        }
    }
}
