/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 * Helper class that collects the methods a task or nested element holds to set
 * attributes, create nested elements or hold PCDATA elements.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class IntrospectionHelper implements BuildListener
{

    /**
     * instances we've already created
     */
    private static Hashtable helpers = new Hashtable();

    /**
     * The method to add PCDATA stuff.
     */
    private Method addText = null;

    /**
     * holds the attribute setter methods.
     */
    private Hashtable attributeSetters;

    /**
     * holds the types of the attributes that could be set.
     */
    private Hashtable attributeTypes;

    /**
     * The Class that's been introspected.
     */
    private Class bean;

    /**
     * Holds methods to create nested elements.
     */
    private Hashtable nestedCreators;

    /**
     * Holds methods to store configured nested elements.
     */
    private Hashtable nestedStorers;

    /**
     * Holds the types of nested elements that could be created.
     */
    private Hashtable nestedTypes;

    private IntrospectionHelper( final Class bean )
        throws TaskException
    {
        attributeTypes = new Hashtable();
        attributeSetters = new Hashtable();
        nestedTypes = new Hashtable();
        nestedCreators = new Hashtable();
        nestedStorers = new Hashtable();

        this.bean = bean;

        Method[] methods = bean.getMethods();
        for( int i = 0; i < methods.length; i++ )
        {
            final Method m = methods[ i ];
            final String name = m.getName();
            Class returnType = m.getReturnType();
            Class[] args = m.getParameterTypes();

            // not really user settable properties on tasks
            if( org.apache.tools.ant.Task.class.isAssignableFrom( bean )
                && args.length == 1 &&
                (
                (
                "setLocation".equals( name ) && org.apache.tools.ant.Location.class.equals( args[ 0 ] )
                ) || (
                "setTaskType".equals( name ) && java.lang.String.class.equals( args[ 0 ] )
                )
                ) )
            {
                continue;
            }

            // hide addTask for TaskContainers
            if( org.apache.tools.ant.TaskContainer.class.isAssignableFrom( bean )
                && args.length == 1 && "addTask".equals( name )
                && org.apache.tools.ant.Task.class.equals( args[ 0 ] ) )
            {
                continue;
            }

            if( "addText".equals( name )
                && java.lang.Void.TYPE.equals( returnType )
                && args.length == 1
                && java.lang.String.class.equals( args[ 0 ] ) )
            {

                addText = methods[ i ];

            }
            else if( name.startsWith( "set" )
                && java.lang.Void.TYPE.equals( returnType )
                && args.length == 1
                && !args[ 0 ].isArray() )
            {

                String propName = getPropertyName( name, "set" );
                if( attributeSetters.get( propName ) != null )
                {
                    if( java.lang.String.class.equals( args[ 0 ] ) )
                    {
                        /*
                         * Ignore method m, as there is an overloaded
                         * form of this method that takes in a
                         * non-string argument, which gains higher
                         * priority.
                         */
                        continue;
                    }
                    /*
                     * If the argument is not a String, and if there
                     * is an overloaded form of this method already defined,
                     * we just override that with the new one.
                     * This mechanism does not guarantee any specific order
                     * in which the methods will be selected: so any code
                     * that depends on the order in which "set" methods have
                     * been defined, is not guaranteed to be selected in any
                     * particular order.
                     */
                }
                AttributeSetter as = createAttributeSetter( m, args[ 0 ] );
                if( as != null )
                {
                    attributeTypes.put( propName, args[ 0 ] );
                    attributeSetters.put( propName, as );
                }

            }
            else if( name.startsWith( "create" )
                && !returnType.isArray()
                && !returnType.isPrimitive()
                && args.length == 0 )
            {

                String propName = getPropertyName( name, "create" );
                nestedTypes.put( propName, returnType );
                nestedCreators.put( propName,
                                    new NestedCreator()
                                    {

                                        public Object create( Object parent )
                                            throws InvocationTargetException,
                                            IllegalAccessException
                                        {

                                            return m.invoke( parent, new Object[]{} );
                                        }

                                    } );

            }
            else if( name.startsWith( "addConfigured" )
                && java.lang.Void.TYPE.equals( returnType )
                && args.length == 1
                && !java.lang.String.class.equals( args[ 0 ] )
                && !args[ 0 ].isArray()
                && !args[ 0 ].isPrimitive() )
            {

                try
                {
                    final Constructor c =
                        args[ 0 ].getConstructor( new Class[]{} );
                    String propName = getPropertyName( name, "addConfigured" );
                    nestedTypes.put( propName, args[ 0 ] );
                    nestedCreators.put( propName,
                                        new NestedCreator()
                                        {

                                            public Object create( Object parent )
                                                throws InvocationTargetException, IllegalAccessException, InstantiationException
                                            {

                                                Object o = c.newInstance( new Object[]{} );
                                                return o;
                                            }

                                        } );
                    nestedStorers.put( propName,
                                       new NestedStorer()
                                       {

                                           public void store( Object parent, Object child )
                                               throws InvocationTargetException, IllegalAccessException, InstantiationException
                                           {

                                               m.invoke( parent, new Object[]{child} );
                                           }

                                       } );
                }
                catch( NoSuchMethodException nse )
                {
                }
            }
            else if( name.startsWith( "add" )
                && java.lang.Void.TYPE.equals( returnType )
                && args.length == 1
                && !java.lang.String.class.equals( args[ 0 ] )
                && !args[ 0 ].isArray()
                && !args[ 0 ].isPrimitive() )
            {

                try
                {
                    final Constructor c =
                        args[ 0 ].getConstructor( new Class[]{} );
                    String propName = getPropertyName( name, "add" );
                    nestedTypes.put( propName, args[ 0 ] );
                    nestedCreators.put( propName,
                                        new NestedCreator()
                                        {

                                            public Object create( Object parent )
                                                throws InvocationTargetException, IllegalAccessException, InstantiationException
                                            {

                                                Object o = c.newInstance( new Object[]{} );
                                                m.invoke( parent, new Object[]{o} );
                                                return o;
                                            }

                                        } );
                }
                catch( NoSuchMethodException nse )
                {
                }
            }
        }
    }

    /**
     * Factory method for helper objects.
     *
     * @param c Description of Parameter
     * @return The Helper value
     */
    public static synchronized IntrospectionHelper getHelper( Class c )
    {
        IntrospectionHelper ih = (IntrospectionHelper)helpers.get( c );
        if( ih == null )
        {
            ih = new IntrospectionHelper( c );
            helpers.put( c, ih );
        }
        return ih;
    }

    /**
     * Sets the named attribute.
     *
     * @param p The new Attribute value
     * @param element The new Attribute value
     * @param attributeName The new Attribute value
     * @param value The new Attribute value
     * @exception BuildException Description of Exception
     */
    public void setAttribute( Project p, Object element, String attributeName,
                              String value )
        throws TaskException
    {
        AttributeSetter as = (AttributeSetter)attributeSetters.get( attributeName );
        if( as == null )
        {
            String msg = getElementName( p, element ) +
                //String msg = "Class " + element.getClass().getName() +
                " doesn't support the \"" + attributeName + "\" attribute.";
            throw new TaskException( msg );
        }
        try
        {
            as.set( p, element, value );
        }
        catch( IllegalAccessException ie )
        {
            // impossible as getMethods should only return public methods
            throw new TaskException( ie.toString(), ie );
        }
        catch( InvocationTargetException ite )
        {
            Throwable t = ite.getTargetException();
            if( t instanceof TaskException )
            {
                throw (TaskException)t;
            }
            throw new TaskException( t.toString(), t );
        }
    }

    /**
     * returns the type of a named attribute.
     *
     * @param attributeName Description of Parameter
     * @return The AttributeType value
     * @exception TaskException Description of Exception
     */
    public Class getAttributeType( String attributeName )
        throws TaskException
    {
        Class at = (Class)attributeTypes.get( attributeName );
        if( at == null )
        {
            String msg = "Class " + bean.getName() +
                " doesn't support the \"" + attributeName + "\" attribute.";
            throw new TaskException( msg );
        }
        return at;
    }

    /**
     * Return all attribues supported by the introspected class.
     *
     * @return The Attributes value
     */
    public Enumeration getAttributes()
    {
        return attributeSetters.keys();
    }

    /**
     * returns the type of a named nested element.
     *
     * @param elementName Description of Parameter
     * @return The ElementType value
     * @exception TaskException Description of Exception
     */
    public Class getElementType( String elementName )
        throws TaskException
    {
        Class nt = (Class)nestedTypes.get( elementName );
        if( nt == null )
        {
            String msg = "Class " + bean.getName() +
                " doesn't support the nested \"" + elementName + "\" element.";
            throw new TaskException( msg );
        }
        return nt;
    }

    /**
     * Return all nested elements supported by the introspected class.
     *
     * @return The NestedElements value
     */
    public Enumeration getNestedElements()
    {
        return nestedTypes.keys();
    }

    /**
     * Adds PCDATA areas.
     *
     * @param project The feature to be added to the Text attribute
     * @param element The feature to be added to the Text attribute
     * @param text The feature to be added to the Text attribute
     */
    public void addText( Project project, Object element, String text )
        throws TaskException
    {
        if( addText == null )
        {
            String msg = getElementName( project, element ) +
                //String msg = "Class " + element.getClass().getName() +
                " doesn't support nested text data.";
            throw new TaskException( msg );
        }
        try
        {
            addText.invoke( element, new String[]{text} );
        }
        catch( IllegalAccessException ie )
        {
            // impossible as getMethods should only return public methods
            throw new TaskException( ie.getMessage(), ie );
        }
        catch( InvocationTargetException ite )
        {
            Throwable t = ite.getTargetException();
            if( t instanceof TaskException )
            {
                throw (TaskException)t;
            }
            throw new TaskException( t.getMessage(), t );
        }
    }

    public void buildFinished( BuildEvent event )
    {
        attributeTypes.clear();
        attributeSetters.clear();
        nestedTypes.clear();
        nestedCreators.clear();
        addText = null;
        helpers.clear();
    }

    public void buildStarted( BuildEvent event )
    {
    }

    /**
     * Creates a named nested element.
     *
     * @param project Description of Parameter
     * @param element Description of Parameter
     * @param elementName Description of Parameter
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    public Object createElement( Project project, Object element, String elementName )
        throws TaskException
    {
        NestedCreator nc = (NestedCreator)nestedCreators.get( elementName );
        if( nc == null )
        {
            String msg = getElementName( project, element ) +
                " doesn't support the nested \"" + elementName + "\" element.";
            throw new TaskException( msg );
        }
        try
        {
            Object nestedElement = nc.create( element );
            if( nestedElement instanceof ProjectComponent )
            {
                ( (ProjectComponent)nestedElement ).setProject( project );
            }
            return nestedElement;
        }
        catch( IllegalAccessException ie )
        {
            // impossible as getMethods should only return public methods
            throw new TaskException( ie.getMessage(), ie );
        }
        catch( InstantiationException ine )
        {
            // impossible as getMethods should only return public methods
            throw new TaskException( ine.getMessage(), ine );
        }
        catch( InvocationTargetException ite )
        {
            Throwable t = ite.getTargetException();
            if( t instanceof TaskException )
            {
                throw (TaskException)t;
            }
            throw new TaskException( t.getMessage(), t );
        }
    }

    public void messageLogged( BuildEvent event )
    {
    }

    /**
     * Creates a named nested element.
     *
     * @param project Description of Parameter
     * @param element Description of Parameter
     * @param child Description of Parameter
     * @param elementName Description of Parameter
     * @exception TaskException Description of Exception
     */
    public void storeElement( Project project, Object element, Object child, String elementName )
        throws TaskException
    {
        if( elementName == null )
        {
            return;
        }
        NestedStorer ns = (NestedStorer)nestedStorers.get( elementName );
        if( ns == null )
        {
            return;
        }
        try
        {
            ns.store( element, child );
        }
        catch( IllegalAccessException ie )
        {
            // impossible as getMethods should only return public methods
            throw new TaskException( ie.getMessage(), ie );
        }
        catch( InstantiationException ine )
        {
            // impossible as getMethods should only return public methods
            throw new TaskException( ine.getMessage(), ine );
        }
        catch( InvocationTargetException ite )
        {
            Throwable t = ite.getTargetException();
            if( t instanceof TaskException )
            {
                throw (TaskException)t;
            }
            throw new TaskException( t.getMessage(), t );
        }
    }

    /**
     * Does the introspected class support PCDATA?
     *
     * @return Description of the Returned Value
     */
    public boolean supportsCharacters()
    {
        return addText != null;
    }

    public void targetFinished( BuildEvent event )
    {
    }

    public void targetStarted( BuildEvent event )
    {
    }

    public void taskFinished( BuildEvent event )
    {
    }

    public void taskStarted( BuildEvent event )
    {
    }

    protected String getElementName( Project project, Object element )
    {
        Hashtable elements = project.getTaskDefinitions();
        String typeName = "task";
        if( !elements.contains( element.getClass() ) )
        {
            elements = project.getDataTypeDefinitions();
            typeName = "data type";
            if( !elements.contains( element.getClass() ) )
            {
                elements = null;
            }
        }

        if( elements != null )
        {
            Enumeration e = elements.keys();
            while( e.hasMoreElements() )
            {
                String elementName = (String)e.nextElement();
                Class elementClass = (Class)elements.get( elementName );
                if( element.getClass().equals( elementClass ) )
                {
                    return "The <" + elementName + "> " + typeName;
                }
            }
        }

        return "Class " + element.getClass().getName();
    }

    /**
     * extract the name of a property from a method name - subtracting a given
     * prefix.
     *
     * @param methodName Description of Parameter
     * @param prefix Description of Parameter
     * @return The PropertyName value
     */
    private String getPropertyName( String methodName, String prefix )
    {
        int start = prefix.length();
        return methodName.substring( start ).toLowerCase( Locale.US );
    }

    /**
     * Create a proper implementation of AttributeSetter for the given attribute
     * type.
     *
     * @param m Description of Parameter
     * @param arg Description of Parameter
     * @return Description of the Returned Value
     */
    private AttributeSetter createAttributeSetter( final Method m,
                                                   final Class arg )
        throws TaskException
    {

        // simplest case - setAttribute expects String
        if( java.lang.String.class.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        m.invoke( parent, new String[]{value} );
                    }
                };
            // now for the primitive types, use their wrappers
        }
        else if( java.lang.Character.class.equals( arg )
            || java.lang.Character.TYPE.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        m.invoke( parent, new Character[]{new Character( value.charAt( 0 ) )} );
                    }

                };
        }
        else if( java.lang.Byte.TYPE.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        m.invoke( parent, new Byte[]{new Byte( value )} );
                    }

                };
        }
        else if( java.lang.Short.TYPE.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        m.invoke( parent, new Short[]{new Short( value )} );
                    }

                };
        }
        else if( java.lang.Integer.TYPE.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        m.invoke( parent, new Integer[]{new Integer( value )} );
                    }

                };
        }
        else if( java.lang.Long.TYPE.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        m.invoke( parent, new Long[]{new Long( value )} );
                    }

                };
        }
        else if( java.lang.Float.TYPE.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        m.invoke( parent, new Float[]{new Float( value )} );
                    }

                };
        }
        else if( java.lang.Double.TYPE.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        m.invoke( parent, new Double[]{new Double( value )} );
                    }

                };
            // boolean gets an extra treatment, because we have a nice method
            // in Project
        }
        else if( java.lang.Boolean.class.equals( arg )
            || java.lang.Boolean.TYPE.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        m.invoke( parent,
                                  new Boolean[]{new Boolean( Project.toBoolean( value ) )} );
                    }

                };
            // Class doesn't have a String constructor but a decent factory method
        }
        else if( java.lang.Class.class.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException, TaskException
                    {
                        try
                        {
                            m.invoke( parent, new Class[]{Class.forName( value )} );
                        }
                        catch( ClassNotFoundException ce )
                        {
                            throw new TaskException( ce.toString(), ce );
                        }
                    }
                };
            // resolve relative paths through Project
        }
        else if( java.io.File.class.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        final File file =
                            FileUtils.newFileUtils().resolveFile( p.getBaseDir(), value );
                        m.invoke( parent, new File[]{file} );
                    }

                };
            // resolve relative paths through Project
        }
        else if( org.apache.tools.ant.types.Path.class.equals( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException
                    {
                        m.invoke( parent, new Path[]{new Path( p, value )} );
                    }

                };
            // EnumeratedAttributes have their own helper class
        }
        else if( org.apache.tools.ant.types.EnumeratedAttribute.class.isAssignableFrom( arg ) )
        {
            return
                new AttributeSetter()
                {
                    public void set( Project p, Object parent, String value )
                        throws InvocationTargetException, IllegalAccessException, TaskException
                    {
                        try
                        {
                            org.apache.tools.ant.types.EnumeratedAttribute ea = (org.apache.tools.ant.types.EnumeratedAttribute)arg.newInstance();
                            ea.setValue( value );
                            m.invoke( parent, new EnumeratedAttribute[]{ea} );
                        }
                        catch( InstantiationException ie )
                        {
                            throw new TaskException( ie.getMessage(), ie );
                        }
                    }
                };

            // worst case. look for a public String constructor and use it
        }
        else
        {

            try
            {
                final Constructor c =
                    arg.getConstructor( new Class[]{java.lang.String.class} );

                return
                    new AttributeSetter()
                    {
                        public void set( Project p, Object parent,
                                         String value )
                            throws InvocationTargetException, IllegalAccessException, TaskException
                        {
                            try
                            {
                                Object attribute = c.newInstance( new String[]{value} );
                                if( attribute instanceof ProjectComponent )
                                {
                                    ( (ProjectComponent)attribute ).setProject( p );
                                }
                                m.invoke( parent, new Object[]{attribute} );
                            }
                            catch( InstantiationException ie )
                            {
                                throw new TaskException( ie.getMessage(), ie );
                            }
                        }
                    };
            }
            catch( NoSuchMethodException nme )
            {
            }
        }

        return null;
    }

    private interface AttributeSetter
    {
        void set( Project p, Object parent, String value )
            throws InvocationTargetException, IllegalAccessException,
            TaskException;
    }

    private interface NestedCreator
    {
        Object create( Object parent )
            throws InvocationTargetException, IllegalAccessException, InstantiationException;
    }

    private interface NestedStorer
    {
        void store( Object parent, Object child )
            throws InvocationTargetException, IllegalAccessException, InstantiationException;
    }
}
