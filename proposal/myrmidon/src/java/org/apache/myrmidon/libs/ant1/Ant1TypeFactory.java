/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.libs.ant1;

import java.net.URL;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.tools.ant.Task;

/**
 * Factory used to create adaptors for Ant1 tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class Ant1TypeFactory
    extends DefaultTypeFactory
{
    public Ant1TypeFactory( final URL url )
    {
        super( url );
    }

    public Ant1TypeFactory( final URL[] urls )
    {
        super( urls );
    }

    public Ant1TypeFactory( final URL[] urls, final ClassLoader parent )
    {
        super( urls, parent );
    }

    public Ant1TypeFactory( final ClassLoader classLoader )
    {
        super( classLoader );
    }

    public Object create( final String name )
        throws TypeException
    {
        final Object object = super.create( name );

        if( !(object instanceof Task) )
        {
            throw new TypeException( "Expected an Ant1 task but received an " +
                                     "object of type : " + object.getClass().getName() );
        }

        return new TaskAdapter( (Task)object );
    }
}

