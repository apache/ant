/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.interfaces.property.PropertyResolver;
import org.apache.myrmidon.components.property.ClassicPropertyResolver;

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
    public static final String ANT1_TASK_PREFIX = "ant1.";

    private static final PropertyResolver c_ant1PropertyResolver =
        new ClassicPropertyResolver();

    private Set m_userProperties = new HashSet();
    private TaskContext m_context;

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

    /**
     * Sets a property. Any existing property of the same name
     * is overwritten, unless it is a user property.
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     */
    public void setProperty( String name, String value )
    {
        if( m_userProperties.contains( name ) )
        {
            log( "Override ignored for user property " + name, MSG_VERBOSE );
            return;
        }

        if( null != m_context.getProperty( name ) )
        {
            log( "Overriding previous definition of property " + name,
                 MSG_VERBOSE );
        }

        log( "Setting project property: " + name + " -> " +
             value, MSG_DEBUG );
        doSetProperty( name, value );
    }

    /**
     * Sets a property if no value currently exists. If the property
     * exists already, a message is logged and the method returns with
     * no other effect.
     *
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @since 1.5
     */
    public void setNewProperty( String name, String value )
    {
        if( null != m_context.getProperty( name ) )
        {
            log( "Override ignored for property " + name, MSG_VERBOSE );
            return;
        }

        log( "Setting project property: " + name + " -> " +
             value, MSG_DEBUG );
        doSetProperty( name, value );
    }

    /**
     * Sets a user property, which cannot be overwritten by
     * set/unset property calls. Any previous value is overwritten.
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @see #setProperty(String,String)
     */
    public void setUserProperty( String name, String value )
    {
        log( "Setting ro project property: " + name + " -> " +
             value, MSG_DEBUG );
        m_userProperties.add( name );
        doSetProperty( name, value );
    }

    /**
     * Sets a property value in the context, wrapping exceptions as
     * Ant1 BuildExceptions.
     * @param name property name
     * @param value property value
     */
    private void doSetProperty( String name, String value )
    {
        try
        {
            m_context.setProperty( name, value );
        }
        catch( TaskException e )
        {
            throw new BuildException( "Could not set property: " + name, e );
        }
    }

    /**
     * Returns the value of a property, if it is set.
     *
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public String getProperty( String name )
    {
        Object value = m_context.getProperty( name );

        // In Ant1, all properties are strings.
        if( value instanceof String )
        {
            return (String)value;
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns the value of a user property, if it is set.
     *
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public String getUserProperty( String name )
    {
        if( m_userProperties.contains( name ) )
        {
            return getProperty( name );
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns a copy of the properties table.
     * @return a hashtable containing all properties
     *         (including user properties).
     */
    public Hashtable getProperties()
    {
        Hashtable propsCopy = new Hashtable();

        Map contextProps = m_context.getProperties();
        Iterator propNames = contextProps.keySet().iterator();
        while( propNames.hasNext() )
        {
            String name = (String)propNames.next();

            // Use getProperty() to only return Strings.
            String value = getProperty( name );
            if( value != null )
            {
                propsCopy.put( name, value );
            }
        }

        return propsCopy;
    }

    /**
     * Returns a copy of the user property hashtable
     * @return a hashtable containing just the user properties
     */
    public Hashtable getUserProperties()
    {
        Hashtable propsCopy = new Hashtable();

        Iterator userPropNames = m_userProperties.iterator();
        while( userPropNames.hasNext() )
        {
            String name = (String)userPropNames.next();
            String value = getProperty( name );
            propsCopy.put( name, value );
        }

        return propsCopy;
    }

    /**
     * Replaces ${} style constructions in the given value with the
     * string value of the corresponding data types.
     *
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>.
     *
     * @return the given string with embedded property names replaced
     *         by values, or <code>null</code> if the given string is
     *         <code>null</code>.
     *
     * @exception BuildException if the given value has an unclosed
     *                           property name, e.g. <code>${xxx</code>
     */
    public String replaceProperties( String value )
        throws BuildException
    {
        try
        {
            return (String)c_ant1PropertyResolver.resolveProperties( value,
                                                                     m_context );
        }
        catch( TaskException e )
        {
            throw new BuildException( "Error resolving value: '" + value + "'", e );
        }
    }

    /**
     * Make the Ant1 project set the java version property, and then
     * copy it into the context properties.
     *
     * @exception BuildException if this Java version is not supported
     *
     * @see #getJavaVersion()
     */
    public void setJavaVersionProperty() throws BuildException
    {
        String javaVersion = getJavaVersion();
        doSetProperty( "ant.java.version", javaVersion );

        log( "Detected Java version: " + javaVersion + " in: "
             + System.getProperty( "java.home" ), MSG_VERBOSE );

        log( "Detected OS: " + System.getProperty( "os.name" ), MSG_VERBOSE );
    }

    /**
     * Sets the base directory for the project, checking that
     * the given filename exists and is a directory.
     *
     * @param baseD The project base directory.
     *              Must not be <code>null</code>.
     *
     * @exception BuildException if the directory if invalid
     */
    public void setBaseDir( File baseD ) throws BuildException
    {
        super.setBaseDir( baseD );
        doSetProperty( "basedir", super.getProperty( "basedir" ) );
    }

}
