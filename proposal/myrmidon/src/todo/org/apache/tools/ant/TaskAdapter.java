/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


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
     * Checks a class, whether it is suitable to be adapted by TaskAdapter.
     * Checks conditions only, which are additionally required for a tasks
     * adapted by TaskAdapter. Thus, this method should be called by {@link
     * Project#checkTaskClass}. Throws a BuildException and logs as
     * Project.MSG_ERR for conditions, that will cause the task execution to
     * fail. Logs other suspicious conditions with Project.MSG_WARN.
     *
     * @param taskClass Description of Parameter
     * @param project Description of Parameter
     */
    public static void checkTaskClass( final Class taskClass, final Project project )
    {
        // don't have to check for interface, since then
        // taskClass would be abstract too.
        try
        {
            final Method executeM = taskClass.getMethod( "execute", null );
            // don't have to check for public, since
            // getMethod finds public method only.
            // don't have to check for abstract, since then
            // taskClass would be abstract too.
            if( !Void.TYPE.equals( executeM.getReturnType() ) )
            {
                final String message = "return type of execute() should be void but was \"" + executeM.getReturnType() + "\" in " + taskClass;
                project.log( message, Project.MSG_WARN );
            }
        }
        catch( NoSuchMethodException e )
        {
            final String message = "No public execute() in " + taskClass;
            project.log( message, Project.MSG_ERR );
            throw new BuildException( message );
        }
    }

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
     * @exception BuildException Description of Exception
     */
    public void execute()
        throws BuildException
    {
        Method setProjectM = null;
        try
        {
            Class c = proxy.getClass();
            setProjectM =
                c.getMethod( "setProject", new Class[]{Project.class} );
            if( setProjectM != null )
            {
                setProjectM.invoke( proxy, new Object[]{project} );
            }
        }
        catch( NoSuchMethodException e )
        {
            // ignore this if the class being used as a task does not have
            // a set project method.
        }
        catch( Exception ex )
        {
            log( "Error setting project in " + proxy.getClass(),
                Project.MSG_ERR );
            throw new BuildException( ex );
        }

        Method executeM = null;
        try
        {
            Class c = proxy.getClass();
            executeM = c.getMethod( "execute", new Class[0] );
            if( executeM == null )
            {
                log( "No public execute() in " + proxy.getClass(), Project.MSG_ERR );
                throw new BuildException( "No public execute() in " + proxy.getClass() );
            }
            executeM.invoke( proxy, null );
            return;
        }
        catch( Exception ex )
        {
            log( "Error in " + proxy.getClass(), Project.MSG_ERR );
            throw new BuildException( ex );
        }

    }

}
