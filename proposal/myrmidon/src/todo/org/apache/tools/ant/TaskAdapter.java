/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.lang.reflect.Method;
import org.apache.myrmidon.api.TaskException;

/**
 * Use introspection to "adapt" an arbitrary Bean ( not extending Task, but with
 * similar patterns).
 *
 * @author costin@dnt.ro
 */
public class TaskAdapter extends Task
{
    Object proxy;

    /**
     * Set the target object class
     *
     * @param o The new Proxy value
     */
    public void setProxy( Object o )
    {
        this.proxy = o;
    }

    public Object getProxy()
    {
        return this.proxy;
    }

    /**
     * Do the execution.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        Method setProjectM = null;
        try
        {
            Class c = proxy.getClass();
            setProjectM =
                c.getMethod( "setProject", new Class[]{Project.class} );
            if( setProjectM != null )
            {
                setProjectM.invoke( proxy, new Object[]{getProject()} );
            }
        }
        catch( NoSuchMethodException e )
        {
            // ignore this if the class being used as a task does not have
            // a set project method.
        }
        catch( Exception ex )
        {
            final String message = "Error setting project in " + proxy.getClass();
            getLogger().error( message, ex );
            throw new TaskException( "Error", ex );
        }

        Method executeM = null;
        try
        {
            Class c = proxy.getClass();
            executeM = c.getMethod( "execute", new Class[ 0 ] );
            if( executeM == null )
            {
                getLogger().error( "No public execute() in " + proxy.getClass() );
                throw new TaskException( "No public execute() in " + proxy.getClass() );
            }
            executeM.invoke( proxy, null );
            return;
        }
        catch( Exception ex )
        {
            getLogger().error( "Error in " + proxy.getClass() );
            throw new TaskException( "Error", ex );
        }

    }
}
