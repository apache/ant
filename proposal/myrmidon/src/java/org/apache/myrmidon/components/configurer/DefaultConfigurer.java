/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.property.PropertyException;
import org.apache.avalon.excalibur.property.PropertyUtil;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.myrmidon.converter.ConverterException;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.converter.MasterConverter;

/**
 * Class used to configure tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultConfigurer
    extends AbstractLogEnabled
    implements Configurer, Composable, LogEnabled
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultConfigurer.class );

    ///Compile time constant to turn on extreme debugging
    private final static boolean DEBUG = false;

    /*
     * TODO: Should reserved names be "configurable" ?
     */
    ///Element names that are reserved
    private final static String[] RESERVED_ELEMENTS =
        {
            "content"
        };

    ///Converter to use for converting between values
    private MasterConverter m_converter;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_converter = (MasterConverter)componentManager.lookup( MasterConverter.ROLE );
    }

    /**
     * Configure a task based on a configuration in a particular context.
     * This configuring can be done in different ways for different
     * configurers.
     * This one does it by first checking if object implements Configurable
     * and if it does will pass the task the configuration - else it will use
     * mapping rules to map configuration to types
     *
     * @param object the object
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
            final String message = REZ.getString( "configuring-object.notice", object );
            getLogger().debug( "Configuring " + object );
        }

        if( object instanceof Configurable )
        {
            if( DEBUG )
            {
                final String message = REZ.getString( "configurable.notice" );
                getLogger().debug( "Configuring object via Configurable interface" );
            }

            ( (Configurable)object ).configure( configuration );
        }
        else
        {
            if( DEBUG )
            {
                final String message = REZ.getString( "reflection.notice" );
                getLogger().debug( message );
            }

            final String[] attributes = configuration.getAttributeNames();
            for( int i = 0; i < attributes.length; i++ )
            {
                final String name = attributes[ i ];
                final String value = configuration.getAttribute( name );

                if( DEBUG )
                {
                    final String message = REZ.getString( "configure-attribute.notice", name, value );
                    getLogger().debug( message );
                }

                configureAttribute( object, name, value, context );
            }

            final Configuration[] children = configuration.getChildren();

            for( int i = 0; i < children.length; i++ )
            {
                final Configuration child = children[ i ];

                if( DEBUG )
                {
                    final String message =
                        REZ.getString( "configure-subelement.notice", child.getName() );
                    getLogger().debug( message );
                }

                configureElement( object, child, context );
            }

            final String content = configuration.getValue( null );
            if( null != content )
            {
                if( !content.trim().equals( "" ) )
                {
                    if( DEBUG )
                    {
                        final String message =
                            REZ.getString( "configure-content.notice", content );
                        getLogger().debug( message );
                    }

                    configureContent( object, content, context );
                }
            }
        }
    }

    /**
     * Configure named attribute of object in a particular context.
     * This configuring can be done in different ways for different
     * configurers.
     *
     * @param object the object
     * @param name the attribute name
     * @param value the attribute value
     * @param context the Context
     * @exception ConfigurationException if an error occurs
     */
    public void configure( final Object object,
                           final String name,
                           final String value,
                           final Context context )
        throws ConfigurationException
    {
        configureAttribute( object, name, value, context );
    }

    /**
     * Try to configure content of an object.
     *
     * @param object the object
     * @param content the content value to be set
     * @param context the Context
     * @exception ConfigurationException if an error occurs
     */
    private void configureContent( final Object object,
                                   final String content,
                                   final Context context )
        throws ConfigurationException
    {
        setValue( object, "addContent", content, context );
    }

    private void configureAttribute( final Object object,
                                     final String name,
                                     final String value,
                                     final Context context )
        throws ConfigurationException
    {
        final String methodName = getMethodNameFor( name );
        setValue( object, methodName, value, context );
    }

    private void setValue( final Object object,
                           final String methodName,
                           final String value,
                           final Context context )
        throws ConfigurationException
    {
        // OMFG the rest of this is soooooooooooooooooooooooooooooooo
        // slow. Need to cache results per class etc.

        final Class clazz = object.getClass();
        final Method[] methods = getMethodsFor( clazz, methodName );
        if( 0 == methods.length )
        {
            final String message =
                REZ.getString( "no-attribute-method.error", methodName );
            throw new ConfigurationException( message );
        }

        setValue( object, value, context, methods );
    }

    private void setValue( final Object object,
                           final String value,
                           final Context context,
                           final Method methods[] )
        throws ConfigurationException
    {
        try
        {
            final Object objectValue =
                PropertyUtil.resolveProperty( value, context, false );

            setValue( object, objectValue, methods, context );
        }
        catch( final PropertyException pe )
        {
            final String message =
                REZ.getString( "bad-property-resolve.error", value );
            throw new ConfigurationException( message, pe );
        }
    }

    private void setValue( final Object object,
                           Object value,
                           final Method methods[],
                           final Context context )
        throws ConfigurationException
    {
        final Class sourceClass = value.getClass();
        final String source = sourceClass.getName();

        for( int i = 0; i < methods.length; i++ )
        {
            if( setValue( object, value, methods[ i ], sourceClass, source, context ) )
            {
                return;
            }
        }

        final String message =
            REZ.getString( "no-can-convert.error", methods[ 0 ].getName(), source );
        throw new ConfigurationException( message );
    }

    private boolean setValue( final Object object,
                              Object value,
                              final Method method,
                              final Class sourceClass,
                              final String source,
                              final Context context )
        throws ConfigurationException
    {
        Class parameterType = method.getParameterTypes()[ 0 ];
        if( parameterType.isPrimitive() )
        {
            parameterType = getComplexTypeFor( parameterType );
        }

        try
        {
            value = m_converter.convert( parameterType, value, context );
        }
        catch( final ConverterException ce )
        {
            if( DEBUG )
            {
                final String message = REZ.getString( "no-converter.error" );
                getLogger().debug( message, ce );
            }

            return false;
        }
        catch( final Exception e )
        {
            final String message =
                REZ.getString( "bad-convert-for-attribute.error", method.getName() );
            throw new ConfigurationException( message, e );
        }

        try
        {
            method.invoke( object, new Object[]{value} );
        }
        catch( final IllegalAccessException iae )
        {
            //should never happen ....
            final String message = REZ.getString( "illegal-access.error" );
            throw new ConfigurationException( message, iae );
        }
        catch( final InvocationTargetException ite )
        {
            final String message = REZ.getString( "invoke-target.error", method.getName() );
            throw new ConfigurationException( message, ite );
        }

        return true;
    }

    private Class getComplexTypeFor( final Class clazz )
    {
        if( String.class == clazz )
            return String.class;
        else if( Integer.TYPE.equals( clazz ) )
            return Integer.class;
        else if( Long.TYPE.equals( clazz ) )
            return Long.class;
        else if( Short.TYPE.equals( clazz ) )
            return Short.class;
        else if( Byte.TYPE.equals( clazz ) )
            return Byte.class;
        else if( Boolean.TYPE.equals( clazz ) )
            return Boolean.class;
        else if( Float.TYPE.equals( clazz ) )
            return Float.class;
        else if( Double.TYPE.equals( clazz ) )
            return Double.class;
        else
        {
            final String message = REZ.getString( "no-complex-type.error", clazz.getName() );
            throw new IllegalArgumentException( message );
        }
    }

    private Method[] getMethodsFor( final Class clazz, final String methodName )
    {
        final Method methods[] = clazz.getMethods();
        final ArrayList matches = new ArrayList();

        for( int i = 0; i < methods.length; i++ )
        {
            final Method method = methods[ i ];
            if( methodName.equals( method.getName() ) &&
                Method.PUBLIC == ( method.getModifiers() & Method.PUBLIC ) )
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

        return (Method[])matches.toArray( new Method[ 0 ] );
    }

    private Method[] getCreateMethodsFor( final Class clazz, final String methodName )
    {
        final Method methods[] = clazz.getMethods();
        final ArrayList matches = new ArrayList();

        for( int i = 0; i < methods.length; i++ )
        {
            final Method method = methods[ i ];
            if( methodName.equals( method.getName() ) &&
                Method.PUBLIC == ( method.getModifiers() & Method.PUBLIC ) )
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

        return (Method[])matches.toArray( new Method[ 0 ] );
    }

    private String getMethodNameFor( final String attribute )
    {
        return "set" + getJavaNameFor( attribute.toLowerCase() );
    }

    private String getJavaNameFor( final String name )
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

    private void configureElement( final Object object,
                                   final Configuration configuration,
                                   final Context context )
        throws ConfigurationException
    {
        final String name = configuration.getName();
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
                final String message =
                    REZ.getString( "no-element-method.error", javaName );
                throw new ConfigurationException( message );
            }

            //guess it is first method ????
            createElement( object, methods[ 0 ], configuration, context );
        }
    }

    private void createElement( final Object object,
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
            final String message = REZ.getString( "subelement-create.error" );
            throw new ConfigurationException( message, e );
        }
    }

    private void addElement( final Object object,
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
            method.invoke( object, new Object[]{created} );
        }
        catch( final ConfigurationException ce )
        {
            throw ce;
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "subelement-create.error" );
            throw new ConfigurationException( message, e );
        }
    }
}
