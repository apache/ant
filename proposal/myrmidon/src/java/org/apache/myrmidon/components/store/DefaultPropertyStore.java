/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.store;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.model.DefaultNameValidator;
import org.apache.myrmidon.interfaces.model.NameValidator;
import org.apache.myrmidon.interfaces.property.PropertyStore;

/**
 * This is the Default implementation of PropertyStore. It follows
 * the following rules;
 *
 * <ul>
 *   <li>The property names must pass DefaultNameValidator checks</li>
 *   <li>The store is mutable</li>
 *   <li>If the key is TaskContext.NAME then value must be a string.</li>
 *   <li>If the key is TaskContext.BASE_DIRECTORY then value must be a key.</li>
 * </ul>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @see org.apache.myrmidon.interfaces.property.PropertyStore
 */
public class DefaultPropertyStore
    implements PropertyStore
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultPropertyStore.class );

    /**
     * The parent store (may be null).
     */
    private final PropertyStore m_parent;

    /**
     * The name validator to check property names against.
     */
    private final NameValidator m_validator;

    /**
     * The underlying map where propertys are actually stored.
     */
    private final Map m_contextData = new Hashtable();

    /**
     * Construct a PropertyStore with no parent and
     * default name-validator.
     */
    public DefaultPropertyStore()
    {
        this( "", null, null );
    }

    /**
     * Construct a PropertyStore with specified parent.
     *
     * @param parent the parent PropertyStore (may be null).
     * @param validator the validator to use to check property names (may be null).
     */
    public DefaultPropertyStore( final String name,
                                 final PropertyStore parent,
                                 final NameValidator validator )
    {
        m_parent = parent;

        NameValidator candidateValidator = validator;
        if( null == candidateValidator )
        {
            candidateValidator = createDefaultNameValidator();
        }

        m_validator = candidateValidator;

        m_contextData.put( TaskContext.NAME, name );
    }

    /**
     * Set the property with specified name to specified value.
     * The specific implementation will apply various rules
     * before setting the property.
     *
     * @param name the name of property
     * @param value the value of property
     * @throws TaskException if property can not be set
     */
    public void setProperty( final String name, final Object value )
        throws TaskException
    {
        checkPropertyName( name );
        checkPropertyValid( name, value );

        if ( value == null )
        {
            m_contextData.remove( name );
        }
        else
        {
            m_contextData.put( name, value );
        }
    }

    /**
     * Return <code>true</code> if the specified property is set.
     *
     * @param name the name of property
     */
    public boolean isPropertySet( final String name )
    {
        try
        {
            getProperty( name );
            return true;
        }
        catch( Exception e )
        {
        }
        return false;
    }

    /**
     * Retrieve the value of specified property.
     * Will return null if no such property exists.
     *
     * @param name the name of the property
     * @return the value of the property, or null if no such property
     * @throws TaskException if theres an error retrieving property, such
     *         as an invalid property name
     */
    public Object getProperty( final String name )
        throws TaskException
    {
        Object value = m_contextData.get( name );
        if( value != null )
        {
            return value;
        }
        if( m_parent != null )
        {
            return m_parent.getProperty( name );
        }

        final String message = REZ.getString( "unknown-prop.error", name );
        throw new TaskException( message );
    }

    /**
     * Retrieve a copy of all the properties that are "in-scope"
     * for store.
     *
     * @return a copy of all the properties that are "in-scope"
     *         for store.
     * @throws TaskException if theres an error retrieving propertys
     */
    public Map getProperties()
        throws TaskException
    {
        final Map properties = new HashMap();
        if( m_parent != null )
        {
            properties.putAll( m_parent.getProperties() );
        }
        properties.putAll( m_contextData );
        return properties;
    }

    /**
     * Return a child PropertyStore with specified name.
     * This is to allow support for scoped stores. However a
     * store may choose to be unscoped and just return a
     * reference to itself.
     *
     * @param name the name of child store
     * @return the child store
     * @throws TaskException if theres an error creating child store
     */
    public PropertyStore createChildStore( final String name )
        throws TaskException
    {
        // Build the name for the new store
        final String thisName = (String)m_contextData.get( TaskContext.NAME );
        final String newName;
        if( name == null || name.length() == 0 )
        {
            newName = thisName;
        }
        else if( thisName.length() == 0 )
        {
            newName = name;
        }
        else
        {
            newName = thisName + "." + name;
        }

        return new DefaultPropertyStore( newName, this, m_validator );
    }

    /**
     * Checks that the supplied property name is valid.
     */
    private void checkPropertyName( final String name )
        throws TaskException
    {
        try
        {
            m_validator.validate( name );
        }
        catch( Exception e )
        {
            String message = REZ.getString( "bad-property-name.error", name );
            throw new TaskException( message, e );
        }
    }

    /**
     * Make sure property is valid if it is one of the "magic" properties.
     *
     * @param name the name of property
     * @param value the value of proeprty
     * @exception TaskException if an error occurs
     */
    private void checkPropertyValid( final String name, final Object value )
        throws TaskException
    {
        if( TaskContext.BASE_DIRECTORY.equals( name ) && !( value instanceof File ) )
        {
            final String message =
                REZ.getString( "bad-property.error",
                               TaskContext.BASE_DIRECTORY,
                               File.class.getName() );
            throw new TaskException( message );
        }
        else if( TaskContext.NAME.equals( name ) && !( value instanceof String ) )
        {
            final String message =
                REZ.getString( "bad-property.error",
                               TaskContext.NAME,
                               String.class.getName() );
            throw new TaskException( message );
        }
    }

    /**
     * Create an instance of the default the name validator.
     *
     * @return the default NameValidator
     */
    private static NameValidator createDefaultNameValidator()
    {
        final DefaultNameValidator defaultValidator = new DefaultNameValidator();
        defaultValidator.setAllowInternalWhitespace( false );
        defaultValidator.setAdditionalInternalCharacters( "_-.+" );
        return defaultValidator;
    }
}
