/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.property;

import java.util.Map;
import org.apache.myrmidon.api.TaskException;

/**
 * This component stores and manages properties. It is also
 * responsible for instituting the various policies regarding
 * propertys. ie It will enforce rules regarding
 *
 * <ul>
 *   <li>Valid property names?</li>
 *   <li>Are propertys mutable?</li>
 *   <li>Are propertys scoped?</li>
 *   <li>Is mapping between name and value correct?</li>
 * </ul>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface PropertyStore
{
    /** Role name for this interface. */
    String ROLE = PropertyStore.class.getName();

    /**
     * Set the property with specified name to specified value.
     * The specific implementation will apply various rules
     * before setting the property.
     *
     * @param name the name of property
     * @param value the value of property
     * @throws TaskException if property can not be set
     */
    void setProperty( String name, Object value )
        throws TaskException;

    /**
     * Return <code>true</code> if the specified property is set.
     *
     * @param name the name of property
     */
    boolean isPropertySet( String name );

    /**
     * Retrieve the value of specified property.
     *
     * @param name the name of the property
     * @return the value of the property.  Never returns null.
     * @throws TaskException if there is no such property, or on error
     *         retrieving property, such as an invalid property name.
     */
    Object getProperty( String name )
        throws TaskException;

    /**
     * Retrieve a copy of all the properties that are "in-scope"
     * for store.
     *
     * @return a copy of all the properties that are "in-scope"
     *         for store.
     * @throws TaskException if theres an error retrieving propertys
     */
    Map getProperties()
        throws TaskException;

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
    PropertyStore createChildStore( String name )
        throws TaskException;
}
