/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Use introspection to "adapt" an arbitrary Bean ( not extending Task, but with
 * similar patterns).
 *
 * @author costin@dnt.ro
 */
public class TaskAdapter
    extends AbstractTask
{
    private Object m_proxy;

    public void setProxy( final Object proxy )
    {
        this.m_proxy = proxy;
    }

    public void execute()
        throws TaskException
    {
        try
        {
            final Class clazz = m_proxy.getClass();
            final Method method = clazz.getMethod( "execute", new Class[ 0 ] );
            method.invoke( m_proxy, null );
        }
        catch( final NoSuchMethodException nsme )
        {
            final String message = "No public execute() in " + m_proxy.getClass();
            getLogger().error( message );
            throw new TaskException( message );
        }
        catch( final Exception e )
        {
            Throwable target = e;
            if( e instanceof InvocationTargetException )
            {
                target = ( (InvocationTargetException)e ).getTargetException();
            }

            final String message = "Error invoking " + m_proxy.getClass();
            getLogger().error( message, target );
            throw new TaskException( message, target );
        }
    }
}
