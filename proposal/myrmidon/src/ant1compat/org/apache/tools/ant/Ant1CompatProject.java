/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Ant1 Project proxy for Myrmidon. Provides hooks between Myrmidon TaskContext
 * and Ant1 project.
 * Note that there is no logical separation between Ant1Project and this extension -
 * they could easily be flattened. Ant1Project is barely modified from the
 * Ant1 original, this class contains the extensions.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class Ant1CompatProject extends Project
{
    private TaskContext m_context;
    public static final String ANT1_TASK_PREFIX = "ant1.";

    public Ant1CompatProject( TaskContext context )
    {
        super();
        m_context = context;
        setBaseDir( m_context.getBaseDirectory() );
        String projectName = (String)
            m_context.getProperty( org.apache.myrmidon.interfaces.model.Project.PROJECT );
        if( projectName != null )
        {
            setName( projectName );
        }
    }

    /**
     * Writes a project level message to the log with the given log level.
     * @param msg The text to log. Should not be <code>null</code>.
     * @param msgLevel The priority level to log at.
     */
    public void log( String msg, int msgLevel )
    {

        doLog( msg, msgLevel );
        super.log( msg, msgLevel );
    }

    /**
     * Writes a task level message to the log with the given log level.
     * @param task The task to use in the log. Must not be <code>null</code>.
     * @param msg The text to log. Should not be <code>null</code>.
     * @param msgLevel The priority level to log at.
     */
    public void log( Task task, String msg, int msgLevel )
    {
        doLog( msg, msgLevel );
        super.log( task, msg, msgLevel );
    }

    /**
     * Writes a target level message to the log with the given log level.
     * @param target The target to use in the log.
     *               Must not be <code>null</code>.
     * @param msg The text to log. Should not be <code>null</code>.
     * @param msgLevel The priority level to log at.
     */
    public void log( Target target, String msg, int msgLevel )
    {
        doLog( msg, msgLevel );
        super.log( target, msg, msgLevel );
    }

    private void doLog( String msg, int msgLevel )
    {
        switch( msgLevel )
        {
            case Ant1CompatProject.MSG_ERR:
                m_context.error( msg );
                break;
            case Ant1CompatProject.MSG_WARN:
                m_context.warn( msg );
                break;
            case Ant1CompatProject.MSG_INFO:
                m_context.info( msg );
                break;
            case Ant1CompatProject.MSG_VERBOSE:
            case Ant1CompatProject.MSG_DEBUG:
                m_context.debug( msg );
        }
    }

    /**
     * This is a copy of init() from the Ant1 Project, which adds Ant1 tasks and
     * DataTypes to the underlying Ant1 Project, but calling add methods on the
     * superclass to avoid adding everything to the TypeManager.
     *
     * @exception BuildException if the default task list cannot be loaded
     */
    public void init() throws BuildException
    {
        setJavaVersionProperty();

        String defs = "/org/apache/tools/ant/taskdefs/defaults.properties";

        try
        {
            Properties props = new Properties();
            InputStream in = this.getClass().getResourceAsStream( defs );
            if( in == null )
            {
                throw new BuildException( "Can't load default task list" );
            }
            props.load( in );
            in.close();

            Enumeration enum = props.propertyNames();
            while( enum.hasMoreElements() )
            {
                String key = (String)enum.nextElement();
                String value = props.getProperty( key );
                try
                {
                    Class taskClass = Class.forName( value );

                    // NOTE: Line modified from Ant1 Project.
                    super.addTaskDefinition( key, taskClass );

                }
                catch( NoClassDefFoundError ncdfe )
                {
                    log( "Could not load a dependent class ("
                         + ncdfe.getMessage() + ") for task " + key, MSG_DEBUG );
                }
                catch( ClassNotFoundException cnfe )
                {
                    log( "Could not load class (" + value
                         + ") for task " + key, MSG_DEBUG );
                }
            }
        }
        catch( IOException ioe )
        {
            throw new BuildException( "Can't load default task list" );
        }

        String dataDefs = "/org/apache/tools/ant/types/defaults.properties";

        try
        {
            Properties props = new Properties();
            InputStream in = this.getClass().getResourceAsStream( dataDefs );
            if( in == null )
            {
                throw new BuildException( "Can't load default datatype list" );
            }
            props.load( in );
            in.close();

            Enumeration enum = props.propertyNames();
            while( enum.hasMoreElements() )
            {
                String key = (String)enum.nextElement();
                String value = props.getProperty( key );
                try
                {
                    Class dataClass = Class.forName( value );

                    // NOTE: Line modified from Ant1 Project.
                    super.addDataTypeDefinition( key, dataClass );

                }
                catch( NoClassDefFoundError ncdfe )
                {
                    // ignore...
                }
                catch( ClassNotFoundException cnfe )
                {
                    // ignore...
                }
            }
        }
        catch( IOException ioe )
        {
            throw new BuildException( "Can't load default datatype list" );
        }

        setSystemProperties();
    }

    /**
     * Adds a new task definition to the project, registering it with the
     * TypeManager, as well as the underlying Ant1 Project.
     *
     * @param taskName The name of the task to add.
     *                 Must not be <code>null</code>.
     * @param taskClass The full name of the class implementing the task.
     *                  Must not be <code>null</code>.
     *
     * @exception BuildException if the class is unsuitable for being an Ant
     *                           task. An error level message is logged before
     *                           this exception is thrown.
     *
     * @see #checkTaskClass(Class)
     */
    public void addTaskDefinition( String taskName, Class taskClass )
        throws BuildException
    {
        String ant2name = ANT1_TASK_PREFIX + taskName;
        try
        {
            registerType( org.apache.myrmidon.api.Task.ROLE, ant2name, taskClass );
        }
        catch( Exception e )
        {
            throw new BuildException( e );
        }

        super.addTaskDefinition( taskName, taskClass );
    }

    /**
     * Utility method to register a type.
     */
    protected void registerType( final String roleType,
                                 final String typeName,
                                 final Class type )
        throws Exception
    {
        final ClassLoader loader = type.getClassLoader();
        final DefaultTypeFactory factory = new DefaultTypeFactory( loader );
        factory.addNameClassMapping( typeName, type.getName() );

        TypeManager typeManager = (TypeManager)m_context.getService( TypeManager.class );
        typeManager.registerType( roleType, typeName, factory );
    }

}
