/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.ant.convert.Converter;
import org.apache.ant.convert.ConverterException;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentNotAccessibleException;
import org.apache.avalon.ComponentNotFoundException;
import org.apache.avalon.Composer;
import org.apache.avalon.ConfigurationException;
import org.apache.avalon.Context;
import org.apache.avalon.Loggable;
import org.apache.avalon.util.PropertyException;
import org.apache.avalon.util.PropertyUtil;
import org.apache.log.Logger;

/**
 * Class used to configure tasks.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultConfigurer
    implements Configurer, Composer, Loggable
{
    protected final static String  RESERVED_ATTRIBUTES[] = 
    {
        "id"
    };

    protected final static String  RESERVED_ELEMENTS[] = 
    {
        "content"
    };

    protected final static boolean DEBUG         = false;
    protected Converter            m_converter;
    protected Logger               m_logger;

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentNotFoundException, ComponentNotAccessibleException
    {
        m_converter = (Converter)componentManager.lookup( "org.apache.ant.convert.Converter" );
    }
    
    /**
     * Configure a task based on a configuration in a particular context.
     * This configuring can be done in different ways for different 
     * configurers. 
     * This one does it by first checking if object implements Configurable 
     * and if it does will pass the task the configuration - else it will use
     * ants rules to map configuration to types
     *
     * @param tasklet the tasklet
     * @param configuration the configuration
     * @param context the Context
     * @exception ConfigurationException if an error occurs
     */
    public void configure( final Object object, 
                           final Configuration configuration,
                           final Context context )
        throws ConfigurationException
    {
        if( DEBUG )
        {
            m_logger.debug( "Configuring " + object );
        }

        if( object instanceof Configurable )
        {
            if( DEBUG ) 
            {
                m_logger.debug( "Configuring object via Configurable interface" );
            }

            ((Configurable)object).configure( configuration );
        }
        else
        {
            if( DEBUG ) 
            {
                m_logger.debug( "Configuring object via Configurable reflection" );
            }

            final Iterator attributes = configuration.getAttributeNames();
            while( attributes.hasNext() )
            {
                final String name = (String)attributes.next();
                final String value = configuration.getAttribute( name );
             
                if( DEBUG ) 
                {
                    m_logger.debug( "Configuring attribute name=" + name +
                                    " value=" + value );
                }
                
                configureAttribute( object, name, value, context );
            }

            final Iterator elements = configuration.getChildren();
            
            while( elements.hasNext() )
            {
                final Configuration element = (Configuration)elements.next();
                
                if( DEBUG ) 
                {
                    m_logger.debug( "Configuring subelement name=" + element.getName() );
                }
                
                configureElement( object, element, context );
            }

            final String content = configuration.getValue( null );

            if( null != content )
            {
                if( !content.trim().equals( "" ) )
                {
                    if( DEBUG ) 
                    {
                        m_logger.debug( "Configuring content " + content );
                    }
                    
                    configureContent( object, content, context );
                }
            }
        }
    }

    /**
     * Try to configure content of an object.
     *
     * @param object the object
     * @param content the content value to be set
     * @param context the Context
     * @exception ConfigurationException if an error occurs
     */
    protected void configureContent( final Object object, 
                                     final String content,
                                     final Context context )
        throws ConfigurationException
    {
        setValue( object, "addContent", content, context );
    }

    protected void configureAttribute( final Object object, 
                                       final String name, 
                                       final String value,
                                       final Context context )
        throws ConfigurationException
    {
        for( int i = 0; i < RESERVED_ATTRIBUTES.length; i++ )
        {
            if( RESERVED_ATTRIBUTES[ i ].equals( name ) ) return;
        }

        final String methodName = getMethodNameFor( name );
        setValue( object, methodName, value, context );
    }

    protected void setValue( final Object object, 
                             final String methodName,
                             final String value,
                             final Context context )
        throws ConfigurationException
    {
        // OMFG the rest of this is soooooooooooooooooooooooooooooooo
        // slow. Need to cache results per class etc.

        final Class clazz = object.getClass();
        final Method methods[] = getMethodsFor( clazz, methodName );

        if( 0 == methods.length )
        {
            throw new ConfigurationException( "Unable to set attribute via " + methodName + 
                                              " due to not finding any appropriate " + 
                                              "accessor method" );
        }

        setValue( object, value, context, methods );
    }

    protected void setValue( final Object object,
                             final String value,
                             final Context context,
                             final Method methods[] )
        throws ConfigurationException
    {
        try
        {
            final Object objectValue = 
                PropertyUtil.resolveProperty( value, context, false );

            setValue( object, objectValue, methods );
        }
        catch( final PropertyException pe )
        {
            throw new ConfigurationException( "Error resolving property " + value,
                                              pe );
        }
    }

    protected void setValue( final Object object, Object value, final Method methods[] )
        throws ConfigurationException
    {
        final Class sourceClass = value.getClass();
        final String source = sourceClass.getName();

        for( int i = 0; i < methods.length; i++ )
        {
            if( setValue( object, value, methods[ i ], sourceClass, source ) )
            {
                return;
            }
        }
        
        throw new ConfigurationException( "Unable to set attribute via " + 
                                          methods[ 0 ].getName() + " as could not convert " + 
                                          source + " to a matching type" );
    }

    protected boolean setValue( final Object object, 
                                Object value, 
                                final Method method,
                                final Class sourceClass,
                                final String source )
        throws ConfigurationException
    {
        Class parameterType = method.getParameterTypes()[ 0 ];
        if( parameterType.isPrimitive() )
        {
            parameterType = getComplexTypeFor( parameterType );
        }
        
        try
        {
            value = m_converter.convert( parameterType, value );
        }
        catch( final ConverterException ce )
        {
            if( DEBUG )
            {
                m_logger.debug( "Failed to find converter ", ce );
            }

            return false;
        }
        catch( final Exception e )
        {
            throw new ConfigurationException( "Error converting attribute for " + 
                                              method.getName(),
                                              e );
        }
        
        try
        {
            method.invoke( object, new Object[] { value } );
        }
        catch( final IllegalAccessException iae )
        {
            //should never happen ....
            throw new ConfigurationException( "Error retrieving methods with " + 
                                              "correct access specifiers",
                                              iae );
        }
        catch( final InvocationTargetException ite )
        {
            throw new ConfigurationException( "Error calling method attribute " + 
                                              method.getName(),
                                              ite );
        }

        return true;
    }

    protected Class getComplexTypeFor( final Class clazz )
    {
        if( String.class == clazz ) return String.class;
        else if( Integer.TYPE.equals( clazz ) ) return Integer.class;
        else if( Long.TYPE.equals( clazz ) ) return Long.class;
        else if( Short.TYPE.equals( clazz ) ) return Short.class;
        else if( Byte.TYPE.equals( clazz ) ) return Byte.class;
        else if( Boolean.TYPE.equals( clazz ) ) return Boolean.class;
        else if( Float.TYPE.equals( clazz ) ) return Float.class;
        else if( Double.TYPE.equals( clazz ) ) return Double.class;
        else
        {
            throw new IllegalArgumentException( "Can not get complex type for non-primitive " +
                                                "type " + clazz.getName() );
        }
    }

    protected Method[] getMethodsFor( final Class clazz, final String methodName )
    {
        final Method methods[] = clazz.getMethods();
        final ArrayList matches = new ArrayList();
        
        for( int i = 0; i < methods.length; i++ )
        {
            final Method method = methods[ i ];
            if( methodName.equals( method.getName() ) && 
                Method.PUBLIC == (method.getModifiers() & Method.PUBLIC) )
            {
                if( method.getReturnType().equals( Void.TYPE ) )
                {
                    final Class parameters[] = method.getParameterTypes();
                    if( 1 == parameters.length )
                    {
                        matches.add( method );
                    }
                }
            }
        }

        return (Method[])matches.toArray( new Method[0] );
    }

    protected Method[] getCreateMethodsFor( final Class clazz, final String methodName )
    {
        final Method methods[] = clazz.getMethods();
        final ArrayList matches = new ArrayList();
        
        for( int i = 0; i < methods.length; i++ )
        {
            final Method method = methods[ i ];
            if( methodName.equals( method.getName() ) && 
                Method.PUBLIC == (method.getModifiers() & Method.PUBLIC) )
            {
                final Class returnType = method.getReturnType();
                if( !returnType.equals( Void.TYPE ) &&
                    !returnType.isPrimitive() )
                {
                    final Class parameters[] = method.getParameterTypes();
                    if( 0 == parameters.length )
                    {
                        matches.add( method );
                    }
                }
            }
        }
        
        return (Method[])matches.toArray( new Method[0] );
    }

    protected String getMethodNameFor( final String attribute )
    {
        return "set" + getJavaNameFor( attribute );
    }

    protected String getJavaNameFor( final String name )
    {
        final StringBuffer sb = new StringBuffer();
        
        int index = name.indexOf( '-' );
        int last = 0;
        
        while( -1 != index )
        {
            final String word = name.substring( last, index ).toLowerCase();
            sb.append( Character.toUpperCase( word.charAt( 0 ) ) );
            sb.append( word.substring( 1, word.length() ) );
            last = index + 1;
            index = name.indexOf( '-', last );
        }
        
        index = name.length();
        final String word = name.substring( last, index ).toLowerCase();
        sb.append( Character.toUpperCase( word.charAt( 0 ) ) );
        sb.append( word.substring( 1, word.length() ) );
        
        return sb.toString();
    }    

    protected void configureElement( final Object object, 
                                     final Configuration configuration,
                                     final Context context )
        throws ConfigurationException
    {
        final String name = configuration.getName();

        for( int i = 0; i < RESERVED_ELEMENTS.length; i++ )
        {
            if( RESERVED_ATTRIBUTES[ i ].equals( name ) ) return;
        }

        final String javaName = getJavaNameFor( name );

        // OMFG the rest of this is soooooooooooooooooooooooooooooooo
        // slow. Need to cache results per class etc.
        final Class clazz = object.getClass();
        Method methods[] = getMethodsFor( clazz, "add" + javaName );
        
        if( 0 != methods.length )
        {
            //guess it is first method ????
            addElement( object, methods[ 0 ], configuration, context );
        }
        else
        {
            methods = getCreateMethodsFor( clazz, "create" + javaName );

            if( 0 == methods.length )
            {
                throw new ConfigurationException( "Unable to set attribute " + javaName + 
                                                  " due to not finding any appropriate " + 
                                                  "accessor method" );
            }

            //guess it is first method ????
            createElement( object, methods[ 0 ], configuration, context );
        }
    }

    protected void createElement( final Object object, 
                                  final Method method,
                                  final Configuration configuration,
                                  final Context context )
        throws ConfigurationException
    {
        try
        {
            final Object created = method.invoke( object, new Object[ 0 ] );
            configure( created, configuration, context );
        }
        catch( final ConfigurationException ce )
        {
            throw ce;
        }
        catch( final Exception e )
        {
            throw new ConfigurationException( "Error creating sub-element", e );
        }
    }

    protected void addElement( final Object object, 
                               final Method method,
                               final Configuration configuration,
                               final Context context )
        throws ConfigurationException
    {
        try
        {
            final Class clazz = method.getParameterTypes()[ 0 ];
            final Object created = clazz.newInstance();

            configure( created, configuration, context );
            method.invoke( object, new Object[] { created } );
        }
        catch( final ConfigurationException ce )
        {
            throw ce;
        }
        catch( final Exception e )
        {
            throw new ConfigurationException( "Error creating sub-element", e );
        }
    }
}
