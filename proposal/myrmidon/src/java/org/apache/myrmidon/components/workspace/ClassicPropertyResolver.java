/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.myrmidon.interfaces.workspace.PropertyResolver;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;

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
                                       final Context context )
    {
        try
        {
            return context.get( propertyName );
        }
        catch( ContextException e )
        {
            return "${" + propertyName + "}";
        }
    }
}
