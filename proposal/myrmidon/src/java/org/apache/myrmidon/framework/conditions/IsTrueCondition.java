/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.conditions;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.aut.converter.Converter;
import org.apache.aut.converter.ConverterException;

/**
 * A {@link Condition} that is true when a property is set, but not set to
 * 'false'.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="condition" name="is-true"
 */
public class IsTrueCondition
    implements Condition
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( IsTrueCondition.class );

    private String m_property;

    public IsTrueCondition( final String propName )
    {
        m_property = propName;
    }

    public IsTrueCondition()
    {
    }

    /**
     * Set the property name to test.
     */
    public void setProperty( final String propName )
    {
        m_property = propName;
    }

    /**
     * Evaluates the condition.
     */
    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        if( m_property == null )
        {
            final String message = REZ.getString( "isset.no-property.error" );
            throw new TaskException( message );
        }

        // Resolve the property name
        final Object object = context.getProperty( m_property );
        if( object == null )
        {
            return false;
        }

        // Convert value to boolean
        try
        {
            final Converter converter = (Converter)context.getService( Converter.class );
            final Boolean value = (Boolean)converter.convert( Boolean.class, object, context );
            return value.booleanValue();
        }
        catch( final ConverterException e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }
}
