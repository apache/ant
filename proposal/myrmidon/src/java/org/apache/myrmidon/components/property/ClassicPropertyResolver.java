/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.property;

import org.apache.myrmidon.interfaces.property.PropertyResolver;
import org.apache.myrmidon.api.TaskContext;

/**
 * A {@link PropertyResolver} implementation which resolves properties
 * as per Ant1, ignoring undefined properties.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class ClassicPropertyResolver
    extends DefaultPropertyResolver
    implements PropertyResolver
{
    /**
     * Retrieve a value from the specified context using the specified key.
     * If there is no such value, returns the original property reference.
     *
     * @param propertyName the name of the property to retrieve
     * @param context the set of known properties
     */
    protected Object getPropertyValue( final String propertyName,
                                       final TaskContext context )
    {
        Object propertyValue = context.getProperty( propertyName );
        if ( propertyValue == null )
        {
            return "${" + propertyName + "}";
        }
        else
        {
            return propertyValue;
        }
    }
}
